package ru.mail.polis.cheaterok

import java.math.BigInteger
import java.security.MessageDigest
import java.io.ByteArrayInputStream

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


fun quorum(nodesCount: Int): Int = nodesCount / 2 + 1


data class Response(val port: Int, val statusCode: Int, val data: Data)

fun localGet(nodes: Set<Int>, key: ByteArray): List<Response> {
    return nodes.map { port -> 
        try {
            val res = get("http://localhost:${port}/local", params=mapOf("id" to String(key)), timeout=1.0)
            Response(port, res.statusCode, Data.fromByteArray(res.content))
        }
        catch (e: java.net.SocketTimeoutException) {null}
    }.filterNotNull()
}

fun asyncPut(nodes: Set<Int>, key:ByteArray, data: Data) {
    val inputStream = ByteArrayInputStream(data.toByteArray())
    nodes.map { port ->
        async.put("http://localhost:${port}/local", 
                  params=mapOf("id" to String(key)), 
                  data=inputStream, 
                  headers = mapOf("Content-Type" to "application/octet-stream"),
                  timeout=1.0, onError = {})
    }
}

fun localPut(nodes: Set<Int>, key:ByteArray, data: Data): List<Int> {
    val inputStream = ByteArrayInputStream(data.toByteArray())
    return nodes.map { port ->
        try {
            put("http://localhost:${port}/local", 
            params=mapOf("id" to String(key)), 
            data=inputStream, 
            headers = mapOf("Content-Type" to "application/octet-stream"),
            timeout=1.0).statusCode
        }
        catch (e: java.net.SocketTimeoutException) {null}
    }.filterNotNull()
}


class Data(var payload: ByteArray, var timestamp: Long, var isAlive: Boolean) {

    fun toByteArray(): ByteArray {
        val timestampBytes = timestamp.toBigInteger().toByteArray();
        val aliveBit = if (isAlive) byteArrayOf(1) else byteArrayOf(0)

        return aliveBit + timestampBytes + payload
    }

    companion object {
        // Из массива байт, в котором "Data" - в нормальную Data
        fun fromByteArray(dataBytes: ByteArray): Data {
            val payload = dataBytes.sliceArray(7..dataBytes.size - 1)
            val timestamp = BigInteger(dataBytes.sliceArray(1..6)).toLong()
            val isAlive = dataBytes[0].toInt() != 0
            return Data(payload, timestamp, isAlive)
        }

        // Оборачиваем payload в Data (генерируем метку и ставим "живой")
        fun wrap(dataBytes: ByteArray): Data = Data(dataBytes, System.currentTimeMillis(), true)
    }
}


class KVServiceImpl(val port: Int, val dao: KVDao, val topology: Set<String>) : KVService {

    val server = ignite()

    // Всё равно там localhost
    val portsTopology = topology.map {it.split(":")[2].toInt()}

    val nodesCount = portsTopology.size
    val defaultAckFrom = Pair(quorum(nodesCount), nodesCount)

    fun splitReplicas(replicas: String?): Pair<Int, Int> {
        if (replicas == null) {
            return defaultAckFrom
        } else {
            val (ack, from) = replicas.split("/").map{it.toInt()}
            return Pair(ack, from)
        }
    } 

    fun getNodesToAsk(key: ByteArray, from: Int): Set<Int> {
        val hash = run {
            val md = MessageDigest.getInstance("SHA-256")
            val hash = md.digest(key)
            BigInteger(1, hash)
        }

        fun getStartIndex(collectionSize: Int): Int = (hash % collectionSize.toBigInteger()).toInt()

        val startIndex = getStartIndex(nodesCount) + getStartIndex(from)

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

            // Неправильные запросы
            if (key == null) {
                status(400)
                "Empty request"
            } else if (key.isEmpty()) {
                status(400)
                "No key specified"
            } 
            // Мякотка
            else {
                val (ack, from) = splitReplicas(replicas)
                val nodesToAsk = getNodesToAsk(key, from)
                Logger.log(nodesToAsk.joinToString(", "))

                val results = localGet(nodesToAsk, key)
                Logger.log(results.joinToString(", "))

                if (ack > results.size) {
                    status(504)
                    "Not enough replicas"
                } else {
                    val someData = results.filter { it.statusCode == 200 }.map {it.data}
                    if (someData.isEmpty()) {
                        status(404)
                        "Value not found"
                    } else {
                        val freshest = someData.maxBy { it.timestamp }
                        if (freshest == null) throw IllegalStateException("А вот и null-safety подвезли")
                        val notSoFreshList = someData.filter { freshest.timestamp > it.timestamp }
                        // Асинхронно приказываем всем обновить данные
                        // asyncPut(notSoFreshList.map{it.port}.toSet(), key, freshest)
                        if (!freshest.isAlive) {
                            status(404)
                            "Value not found"
                        } else {
                            status(200)
                            freshest.payload
                        }
                    }
                }
            }
        }
        
        server.put("/v0/entity") {
            val key = try {queryParams("id").toByteArray()} catch (e: IllegalStateException) {null}
            val replicas = try {queryParams("replicas")} catch (e: IllegalStateException) {null}

            // Неправильные запросы
            if (key == null) {
                status(400)
                "Empty request"
            } else if (key.isEmpty()) {
                status(400)
                "No key specified"
            } 
            // Мякотка
            else {
                val data = Data.wrap(request.bodyAsBytes())

                val (ack, from) = splitReplicas(replicas)
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

            if (key == null) {
                status(400)
                "Empty request"
            } else if (key.isEmpty()) {
                status(400)
                "Key is empty"
            } else {
                status(202)
                "Key removed"
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
            val value = try {dao.get(key)} catch (e: NoSuchElementException) {null}
            
            if (value == null) {
                status(404)
                "Value not found"
            } else {
                status(200)
                value 
            }
        }

        server.put("/local") {
            /*
                Царь скомандовал "Записать!"

                Пишем и отправляем 200
            */

            val key = queryParams("id").toByteArray()
            val value = request.body().toByteArray()

            dao.upsert(key, value)

            status(200)
            "Value recorded"
        }

        server.delete("/local") {
            /*
                Царь скомандовал "Удалить!"

                Проверяем, что у нас чисто случайно не оказалось инфы свежее.
                Оказалось - игнорируем.
                Иначе - удаляем.
            */

            val key = queryParams("id").toByteArray()

            // Для удаления пересылаем весь пакет, но данных там уже нет
            val theirs = Data.fromByteArray(request.bodyAsBytes())
            val ours = try { Data.fromByteArray(dao.get(key)) } catch (e: NoSuchElementException) {null}


            if (ours == null || theirs.timestamp > ours.timestamp) {
                // Предполагаем, что гробик Царь там уже выставил
                dao.upsert(key, theirs.toByteArray())
            } 

            status(200)
            "Value removed"
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