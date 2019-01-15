package ru.mail.polis.cheaterok

import spark.kotlin.*

import ru.mail.polis.KVService
import ru.mail.polis.KVDao


class KVServiceImpl(val port: Int, val dao: KVDao) : KVService {

    val server = ignite()

    override fun start() {
        server.ipAddress("0.0.0.0")
              .port(port)


        server.get("/v0/status") {
            status(200)
            "Everything is OK"
        }

        server.get("/v0/entity") {
            try { 
                val key = queryParams("id").toByteArray()

                if (key.isEmpty()) {
                    status(400)
                    "No key specified"
                } else {
                    val data = dao.get(key)

                    status(200)
                    data
                }
            }
            catch (e: IllegalStateException) {
                status(400)
                "Empty request"
            }
            catch (e: NoSuchElementException) {
                status(404)
                "Value not found"
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