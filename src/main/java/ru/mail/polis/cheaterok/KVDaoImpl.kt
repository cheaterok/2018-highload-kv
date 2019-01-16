package ru.mail.polis.cheaterok

import java.io.File

import org.rocksdb.RocksDB
import org.rocksdb.Options
import org.rocksdb.WriteOptions
import org.rocksdb.WriteBatch

import ru.mail.polis.KVDao


fun aliveKey(key: ByteArray): ByteArray = byteArrayOf(0) + key
fun deadKey(key: ByteArray): ByteArray = byteArrayOf(1) + key


class KVDaoImpl(val data: File) : KVDao {

    val options = Options().setCreateIfMissing(true)
    val database = RocksDB.open(options, data.path)

    val writeOptions = WriteOptions()

    @Throws(NoSuchElementException::class)
    override fun get(key: ByteArray): ByteArray {
        return database.get(aliveKey(key)) ?: throw NoSuchElementException("No value for key '$key'")
    }

    override fun upsert(key: ByteArray, value: ByteArray) {
        database.put(aliveKey(key), value)
    }

    override fun remove(key: ByteArray) {
        val data = database.get(aliveKey(key)) ?: return

        val writeBatch = WriteBatch()

        writeBatch.remove(aliveKey(key))
        writeBatch.put(deadKey(key), data)

        database.write(writeOptions, writeBatch)
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