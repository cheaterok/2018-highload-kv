package ru.mail.polis.cheaterok

import java.io.File

import org.rocksdb.RocksDB
import org.rocksdb.Options

import ru.mail.polis.KVDao


class KVDaoImpl(val data: File) : KVDao {

    val options = Options().setCreateIfMissing(true)
    val database = RocksDB.open(options, data.path)


    @Throws(NoSuchElementException::class)
    override fun get(key: ByteArray): ByteArray {
        return database.get(key) ?: throw NoSuchElementException("No value for key '$key'")
    }

    override fun upsert(key: ByteArray, value: ByteArray) {
        database.put(key, value)
    }

    override fun remove(key: ByteArray) {
        database.singleDelete(key)
    }

    override fun close() {
        database.close()
    }


    companion object {
        init {
            RocksDB.loadLibrary()
        }
    }
}