package ru.mail.polis.cheaterok

import java.math.BigInteger
import java.security.MessageDigest
import java.util.Base64

import org.slf4j.LoggerFactory

import spark.kotlin.*

import khttp.*

import ru.mail.polis.KVService
import ru.mail.polis.KVDao


// https://www.baeldung.com/kotlin-logging
class Logger {
    companion object {
        @Suppress("JAVA_CLASS_ON_COMPANION")
        @JvmStatic
        private val logger = LoggerFactory.getLogger(javaClass.enclosingClass)

        fun log(s: String) {
            logger.info(s)
        }
    }
}


class Data(val isAlive: Boolean, val timestamp: Long, val payload: ByteArray) {
    /*
        По сути это маска на массив байт.
        Искал аналог модуля struct из Python - не нашёл.
        Навелосипедил сам.

        Разложение такое:
            1) 1 байт - бит состояния (жив/мёртв)
            2) байт 1-6 - временная метка в 64 бита (long), засунутая в BigInteger (наверное, отсюда ещё 2 байта)
            3) байты с 7 и до конца - полезная нагрузка
    */

    fun toByteArray(): ByteArray {
        val timestampBytes = timestamp.toBigInteger().toByteArray()
        val aliveBit = if (isAlive) byteArrayOf(1) else byteArrayOf(0)

        return aliveBit + timestampBytes + payload
    }

    fun toBase64String(): String = this.toByteArray().toBase64()

    companion object {
        // Из массива байт, в котором "Data" - в нормальную Data
        fun fromByteArray(dataBytes: ByteArray): Data {
            val isAlive = dataBytes[0].toInt() != 0
            val timestamp = BigInteger(dataBytes.sliceArray(1..6)).toLong()
            val payload = dataBytes.sliceArray(7..dataBytes.size - 1)

            return Data(isAlive, timestamp, payload)
        }

        fun fromBase64String(str: String): Data = Data.fromByteArray(str.fromBase64())

        // Оборачиваем payload в Data (генерируем метку и ставим бит состояния)
        fun wrap(payload: ByteArray, isAlive: Boolean = true): Data {
            return Data(isAlive, System.currentTimeMillis(), payload)
        }
    }
}


data class Response(val port: Int, val data: Data?)


fun ByteArray.toBase64(): String = String(Base64.getEncoder().encode(this))
fun String.fromBase64(): ByteArray = Base64.getDecoder().decode(this.toByteArray())


/*****************************************
    Функции доступа к внутреннему API

    Опрос нод последовательный, 
    потому что я не знаю,
    как на JVM устраивать пулы потоков или 
    кооперативную многозадачность.

    Но пока это крутится на локалхосте - 
    можно представить, что всё асинхронно.
*****************************************/

fun localGet(nodes: Set<Int>, key: ByteArray): List<Response> {
    return nodes.mapNotNull { port ->
        try {
            val res = get("http://localhost:$port/local", params=mapOf("id" to String(key)), timeout=1.0)
            val data = if (res.statusCode == 200) Data.fromBase64String(res.text) else null
            Response(port, data)
        }
        catch (e: java.net.SocketTimeoutException) {null}
        catch (e: java.net.ConnectException) {null}
    }
}

fun localPut(nodes: Set<Int>, key:ByteArray, data: Data): List<Int> {
    return nodes.mapNotNull { port ->
        try {
            put("http://localhost:$port/local",
                params=mapOf("id" to String(key)),
                data=data.toBase64String(), 
                timeout=1.0).statusCode
        }
        catch (e: java.net.SocketTimeoutException) {null}
        catch (e: java.net.ConnectException) {null}
    }
}


class KVServiceImpl(val port: Int, val dao: KVDao, topology: Set<String>) : KVService {

    val server = ignite()

    // Всё равно там localhost
    val portsTopology = topology.map {it.split(":")[2].toInt()}

    val nodesCount = portsTopology.size
    val quorum = nodesCount / 2 + 1
    val defaultAckFrom = Pair(quorum, nodesCount)


    // Парсим параметр replicas
    fun splitReplicas(replicas: String?): Pair<Int, Int> {
        if (replicas == null) {
            return defaultAckFrom
        } else {
            val (ack, from) = replicas.split("/").map{it.toInt()}
            return Pair(ack, from)
        }
    } 

    // Вычисляем, у каких нод нужно спрашивать данные для конкретного ключа
    fun getNodesToAsk(key: ByteArray, from: Int): Set<Int> {
        val hash = run {
            val md = MessageDigest.getInstance("SHA-256")
            val hash = md.digest(key)
            BigInteger(1, hash)
        }

        val startIndex = (hash % nodesCount.toBigInteger()).toInt()

        // Нужно от начала startIndex взять from элементов
        // При этом, дойдя до конца списка, нужно брать с его начала
        // https://stackoverflow.com/questions/40938716/how-to-cycle-a-list-infinitely-and-lazily-in-kotlin/40940840#40940840
        return generateSequence {portsTopology}.flatten().drop(startIndex).take(from).toSet()
    }


    override fun start() {

        server.ipAddress("0.0.0.0")
              .port(port)


        /******************
            Внешнее API
        *******************/
        server.get("/v0/status") {
            status(200)
            "Everything is OK"
        }

        server.get("/v0/entity") {
            val key = try {queryParams("id").toByteArray()} catch (e: IllegalStateException) {null}
            val replicas = try {queryParams("replicas")} catch (e: IllegalStateException) {null}

            val (ack, from) = splitReplicas(replicas)

            if (key == null || key.isEmpty() || (ack < 1 || ack > from || from > nodesCount)) {
                status(400)
                "Invalid request"
            } else {
                val nodesToAsk = getNodesToAsk(key, from)
                val results = localGet(nodesToAsk, key)

                if (ack > results.size) {
                    status(504)
                    "Not enough replicas"
                } else {
                    val freshest = results.mapNotNull{ it.data }.maxBy{ it.timestamp }
                    if (freshest == null || !freshest.isAlive) {
                        status(404)
                        "Value not found"
                    } else {
                        status(200)
                        freshest.payload
                    }
                }
            }
        }
        
        server.put("/v0/entity") {
            val key = try {queryParams("id").toByteArray()} catch (e: IllegalStateException) {null}
            val replicas = try {queryParams("replicas")} catch (e: IllegalStateException) {null}

            val (ack, from) = splitReplicas(replicas)

            if (key == null || key.isEmpty() || (ack < 1 || ack > from || from > nodesCount)) {
                status(400)
                "Invalid request"
            } else {
                val data = Data.wrap(request.bodyAsBytes())

                val nodesToAsk = getNodesToAsk(key, from)
                val results = localPut(nodesToAsk, key, data)

                if (ack > results.size) {
                    status(504)
                    "Not enough replicas"
                } else {
                    status(201)
                    "Created"
                }
            }
        }

        server.delete("/v0/entity") {
            val key = try {queryParams("id").toByteArray()} catch (e: IllegalStateException) {null}
            val replicas = try {queryParams("replicas")} catch (e: IllegalStateException) {null}

            val (ack, from) = splitReplicas(replicas)

            if (key == null || key.isEmpty() || (ack < 1 || ack > from || from > nodesCount)) {
                status(400)
                "Invalid request"
            } else {
                val nodesToAsk = getNodesToAsk(key, from)
                // Используем PUT с пустым объектом-могилой
                val results = localPut(nodesToAsk, key, Data.wrap(byteArrayOf(), isAlive=false))

                if (ack > results.size) {
                    status(504)
                    "Not enough replicas"
                } else {
                    status(202)
                    "Accepted"
                }
            }
        }


        /******************
            Внутреннее API
        *******************/

        // Предполагаем, что наши ноды не формируют неправильных запросов

        server.get("/local") {
            /*
                Царь опрашивать народ изволил

                Если данных нет - 404
                Если хоть что-то есть - 200 и отправляем
            */
            
            val key = queryParams("id").toByteArray()
            val data = try {Data.fromByteArray(dao.get(key))} catch (e: NoSuchElementException) {null}

            Logger.log("Local API :GET: Key ${String(key)}")

            if (data == null) {
                Logger.log("Local API :GET: NO DATA")
                status(404)
                // "Value not found"
                ""
            } else {
                Logger.log("Local API :GET: ${data.isAlive} + ${data.timestamp}")
                status(200)
                data.toBase64String()
            }
        }

        server.put("/local") {
            /*
                Царь скомандовал "Записать!"

                Пишем и отправляем 200

                NB: Удаление тоже происходит через этот запрос - просто в body приходит могила
            */

            val key = queryParams("id").toByteArray()
            val value = request.body().fromBase64()

            Logger.log("Local API :PUT: Key ${String(key)}")
            val data = Data.fromByteArray(value)
            Logger.log("Local API :PUT: Data ${data.isAlive} + ${data.timestamp}")

            dao.upsert(key, value)

            status(200)
            // "Value recorded"
            ""
        }

        server.get("*") {
            status(400)
            "Bad request"
        }
    }

    override fun stop() {
        server.stop()
    }
}