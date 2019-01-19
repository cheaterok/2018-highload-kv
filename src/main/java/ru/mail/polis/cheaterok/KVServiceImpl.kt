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


fun quorum(nodesCount: Int): Int = nodesCount / 2 + 1


data class Response(val port: Int, val data: Data?)

fun localGet(nodes: Set<Int>, key: ByteArray): List<Response> {
    return nodes.map { port -> 
        try {
            val res = get("http://localhost:${port}/local", params=mapOf("id" to String(key)), timeout=1.0)
            val data = if (res.statusCode == 200) Data.fromBase64String(res.text) else null
            Response(port, data)
        }
        catch (e: java.net.SocketTimeoutException) {null}
        catch (e: java.net.ConnectException) {null}
    }.filterNotNull()
}

fun asyncPut(nodes: Set<Int>, key:ByteArray, data: Data) {
    nodes.map { port ->
        async.put("http://localhost:${port}/local", 
                  params=mapOf("id" to String(key)), 
                  data=data.toBase64String(),
                  timeout=1.0, onError = {})
    }
}

fun localPut(nodes: Set<Int>, key:ByteArray, data: Data): List<Int> {
    return nodes.map { port ->
        try {
            put("http://localhost:${port}/local", 
            params=mapOf("id" to String(key)),
            data=data.toBase64String(), 
            timeout=1.0).statusCode
        }
        catch (e: java.net.SocketTimeoutException) {null}
        catch (e: java.net.ConnectException) {null}
    }.filterNotNull()
}

fun localDelete(nodes: Set<Int>, key: ByteArray): List<Int> {
    return nodes.map { port ->
        try {
            delete("http://localhost:${port}/local", 
            params=mapOf("id" to String(key)),
            timeout=1.0).statusCode
        }
        catch (e: java.net.SocketTimeoutException) {null}
        catch (e: java.net.ConnectException) {null}
    }.filterNotNull()
}


fun ByteArray.toBase64(): String = String(Base64.getEncoder().encode(this))
fun String.fromBase64(): ByteArray = Base64.getDecoder().decode(this.toByteArray())


class Data(var payload: ByteArray, var timestamp: Long, var isAlive: Boolean) {

    fun toByteArray(): ByteArray {
        val timestampBytes = timestamp.toBigInteger().toByteArray();
        val aliveBit = if (isAlive) byteArrayOf(1) else byteArrayOf(0)

        return aliveBit + timestampBytes + payload
    }

    fun toBase64String(): String = this.toByteArray().toBase64()

    companion object {
        // Из массива байт, в котором "Data" - в нормальную Data
        fun fromByteArray(dataBytes: ByteArray): Data {
            val payload = dataBytes.sliceArray(7..dataBytes.size - 1)
            val timestamp = BigInteger(dataBytes.sliceArray(1..6)).toLong()
            val isAlive = dataBytes[0].toInt() != 0
            return Data(payload, timestamp, isAlive)
        }

        fun fromBase64String(str: String): Data = Data.fromByteArray(str.fromBase64())

        // Оборачиваем payload в Data (генерируем метку и ставим "живой")
        fun wrap(dataBytes: ByteArray, isAlive: Boolean = true): Data = Data(dataBytes, System.currentTimeMillis(), isAlive)
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

            val (ack, from) = splitReplicas(replicas)

            // Неправильные запросы
            if (key == null) {
                status(400)
                "Empty request"
            } else if (key.isEmpty()) {
                status(400)
                "No key specified"
            } else if (ack < 1 || ack > from || from > nodesCount) {
                status(400)
                "Replicas are malformed"
            }
            // Мякотка
            else {
                val nodesToAsk = getNodesToAsk(key, from)

                val results = localGet(nodesToAsk, key)

                if (ack > results.size) {
                    status(504)
                    "Not enough replicas"
                } else {
                    val freshest = results.mapNotNull{ it.data }.maxBy{ it.timestamp }
                    if (freshest == null) {
                        status(404)
                        "Value not found"
                    } else {
                        // asyncPut((noData + staleData).map{it.port}.toSet(), key, freshest ?: throw IllegalStateException("Теперь ещё безопаснее"))
                        if (!freshest.isAlive) {
                            status(404)
                            "Value is dead"
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

            val (ack, from) = splitReplicas(replicas)

            // Неправильные запросы
            if (key == null) {
                status(400)
                "Empty request"
            } else if (key.isEmpty()) {
                status(400)
                "No key specified"
            } else if (ack < 1 || ack > from || from > nodesCount) {
                status(400)
                "Replicas are malformed"
            }
    
            // Мякотка
            else {
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

            // Неправильные запросы
            if (key == null) {
                status(400)
                "Empty request"
            } else if (key.isEmpty()) {
                status(400)
                "No key specified"
            } else if (ack < 1 || ack > from || from > nodesCount) {
                status(400)
                "Replicas are malformed"
            }
            // Мякотка
            else {
                val nodesToAsk = getNodesToAsk(key, from)

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

        // server.delete("/local") {
        //     /*
        //         Царь скомандовал "Удалить!"

        //         Удаляем и отправляем 200
        //     */

        //     val key = queryParams("id").toByteArray()
        //     val data = try {
        //             Data.fromByteArray(dao.get(key))
        //         } catch (e: NoSuchElementException) { 
        //             Data.wrap(byteArrayOf()) 
        //         }


        //     Logger.log("Local API :DELETE: Key ${String(key)}")
        //     Logger.log("Local API :DELETE: Data ${data.isAlive} + ${data.timestamp}")

        //     data.isAlive = false
        //     dao.upsert(key, data.toByteArray())

        //     status(200)
        //     // "Value removed"
        //     ""
        // }

        server.get("*") {
            status(400)
            "Bad request"
        }
    }

    override fun stop() {
        server.stop()
    }
}