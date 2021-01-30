package me.geek.tom.slimeforfabric.util

import kotlin.experimental.and
import kotlin.experimental.or
import kotlin.math.ceil

data class Bitset(
    private val bits: MutableList<Boolean> = mutableListOf()
) {

    fun packToBytes(): ByteArray {
        val length = ceil(bits.size / 8.0).toInt()
        val array = ByteArray(length)
        array.fill(0)

        for ((i, value) in bits.withIndex()) {
            val byte = i shr 3 // divide by 8
            val bit = i % 8
            if (value) {
                array[byte] = array[byte] or (1 shl bit).toByte()
            }
        }

        return array
    }

    operator fun get(bit: Int): Boolean? {
        return if (bit < bits.size) bits[bit] else null
    }

    operator fun set(bit: Int, value: Boolean) {
        if (bits.size < (bit + 1)) {
            for (i in 0..(bit - bits.size)) bits += false
        }
        bits[bit] = value
    }

    fun size(): Int {
        return bits.size
    }

    companion object {
        @JvmStatic
        fun fromBytes(bytes: ByteArray, length: Int): Bitset {
            val bits = ArrayList<Boolean>()
            var i = 0
            for (byte in bytes) {
                for (bit in 0..7) {
                    if (i > length) break
                    val value = (byte and (1 shl bit).toByte()) != (0).toByte()
                    bits += value
                    i++
                }
            }
            return Bitset(bits)
        }
    }
}
