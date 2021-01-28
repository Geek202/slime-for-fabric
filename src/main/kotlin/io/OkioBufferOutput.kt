package me.geek.tom.slimeforfabric.io

import okio.BufferedSink

@ExperimentalUnsignedTypes
class OkioBufferOutput(
    private val buffer: BufferedSink,
) : DataOutput {
    override fun writeHeader(a: UByte, b: UByte) {
        buffer.writeByte(a.toInt()).writeByte(b.toInt())
    }

    override fun writeUByte(b: UByte) {
        buffer.writeByte(b.toInt())
    }

    override fun writeShort(s: Short) {
        buffer.writeShort(s.toInt())
    }
}
