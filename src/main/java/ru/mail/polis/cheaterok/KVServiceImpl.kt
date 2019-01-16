package ru.mail.polis.cheaterok

import java.math.BigInteger
import java.security.MessageDigest

import spark.kotlin.*

import ru.mail.polis.KVService
import ru.mail.polis.KVDao


fun quorum(nodesCount: Int): Int = if ((nodesCount % 2) == 0) {nodesCount / 2} else {nodesCount / 2 + 1}


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

            if (key == null) {
                status(400)
                "Empty request"
            } else if (key.isEmpty()) {
                status(400)
                "No key specified"
            } else {
                if (replicas == null) {
                    val data = try {dao.get(key)} catch (e: NoSuchElementException) {null}
                    if (data == null) {
                        status(404)
                        "Value not found"
                    } else {
                        status(200)
                        data
                    }
                } else {
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
            try { 
                val key = queryParams("id").toByteArray()

                if (key.isEmpty()) {
                    status(400)
                    "No key specified"
                } else {
                    dao.upsert(key, request.bodyAsBytes())

                    status(201)
                    "Key added"
                }
            }
            catch (e: IllegalStateException) {
                status(400)
                "Empty request"
            }

        }

        server.delete("/v0/entity") {
            try { 
                val key = queryParams("id").toByteArray()

                if (key.isEmpty()) {
                    status(400)
                    "Key is empty"
                } else {
                    dao.remove(key)

                    status(202)
                    "Key removed"
                }
            }
            catch (e: IllegalStateException) {
                status(400)
                "Empty request"
            }
        }
    }

    override fun stop() {
        server.stop()
    }
}