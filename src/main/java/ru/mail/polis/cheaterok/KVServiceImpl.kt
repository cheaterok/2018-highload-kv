package ru.mail.polis.cheaterok

import java.math.BigInteger
import java.security.MessageDigest

import spark.kotlin.*

import ru.mail.polis.KVService
import ru.mail.polis.KVDao


fun quorum(nodesCount: Int): Int = nodesCount / 2 + 1

fun wrapData(data: ByteArray, isAlive: Boolean = true): ByteArray {
    val timestamp = System.currentTimeMillis().toBigInteger().toByteArray();
    val aliveBit = if (isAlive) byteArrayOf(1) else byteArrayOf(0)

    return aliveBit + timestamp + data
}

fun unwrapData(data: ByteArray): Triple<Boolean, Long, ByteArray> {
    val aliveBit = data[0].toInt() != 0
    // В душе не знаю почему, но Long.toBigInteger().toByteArray() в результате имеет такой размер
    val timestamp = BigInteger(data.sliceArray(1..6)).toLong()
    val payload = data.sliceArray(7..data.size - 1)

    return Triple(aliveBit, timestamp, payload)
}


class KVServiceImpl(val port: Int, val dao: KVDao, val topology: Set<String>) : KVService {

    val server = ignite()

    // Всё равно там localhost
    val portsTopology = topology.map {it.split(":")[2].toInt()}

    val nodesCount = portsTopology.size
    val defaultAckFrom = listOf(quorum(nodesCount), nodesCount)

    fun getNodesToAsk(id: ByteArray, replicas: String?): Set<Int> {
        val (ack, from) = replicas?.split("/")?.map{it.toInt()} ?: defaultAckFrom 

        val hash = run {
            val md = MessageDigest.getInstance("SHA-256")
            val hash = md.digest(id)
            BigInteger(1, hash)
        }

        fun getStartIndex(collectionSize: Int): Int = (hash % collectionSize.toBigInteger()).toInt()

        val startIndex = getStartIndex(nodesCount) + getStartIndex(from)

        // https://stackoverflow.com/questions/40938716/how-to-cycle-a-list-infinitely-and-lazily-in-kotlin/40940840#40940840
        return generateSequence {portsTopology}.flatten().drop(startIndex - 1).take(ack).toSet()
    }

    override fun start() {
        server.ipAddress("0.0.0.0")
              .port(port)


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
            // Мякотка начинается здесь
            else {
                val wrappedData = try {dao.get(key)} catch (e: NoSuchElementException) {null}
                // Старое API
                if (replicas == null) {
                    if (wrappedData == null) {
                        status(404)
                        "Value not found"
                    } else {
                        val (isAlive, _, data) = unwrapData(wrappedData)
                        if (isAlive) {
                            status(200)
                            data
                        }
                        else {
                            status(404)
                            "Value is deleted"
                        }
                    }
                } 
                // Новое API
                else {
                    val nodesToAsk = getNodesToAsk(key, replicas)
                    "TODO"
                }
            }
        }

        server.get("*") {
            status(400)
            "Nothing here"
        }

        server.put("/v0/entity") {
            val key = try {queryParams("id").toByteArray()} catch (e: IllegalStateException) {null}

            if (key == null) {
                status(400)
                "Empty request"
            } else if (key.isEmpty()) {
                status(400)
                "No key specified"
            } else {
                val data = request.bodyAsBytes()
                dao.upsert(key, wrapData(data))

                status(201)
                "Key added"
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
                val wrappedData = try {dao.get(key)} catch (e: NoSuchElementException) {null}

                if (wrappedData != null) {
                    val (_, _, data) = unwrapData(wrappedData)
                    dao.upsert(key, wrapData(data, isAlive=false))
                }

                status(202)
                "Key removed"
            }
        }
    }

    override fun stop() {
        server.stop()
    }
}