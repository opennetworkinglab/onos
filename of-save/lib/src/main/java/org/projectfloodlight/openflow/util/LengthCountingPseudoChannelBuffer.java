package org.projectfloodlight.openflow.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.nio.charset.Charset;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferFactory;
import org.jboss.netty.buffer.ChannelBufferIndexFinder;

public class LengthCountingPseudoChannelBuffer implements ChannelBuffer {

    int writerIndex = 0;
    private int markedWriterIndex;

    @Override
    public ChannelBufferFactory factory() {
        return null;
    }

    @Override
    public int capacity() {
        return Integer.MAX_VALUE;
    }

    @Override
    public ByteOrder order() {
        return ByteOrder.BIG_ENDIAN;
    }

    @Override
    public boolean isDirect() {
        return true;
    }

    @Override
    public int readerIndex() {
        return 0;
    }

    @Override
    public void readerIndex(int readerIndex) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int writerIndex() {
        return writerIndex;
    }

    @Override
    public void writerIndex(int writerIndex) {
        this.writerIndex = writerIndex;
    }

    @Override
    public void setIndex(int readerIndex, int writerIndex) {
        if(readerIndex != 0)
            throw new UnsupportedOperationException();
        this.writerIndex = writerIndex;
    }

    @Override
    public int readableBytes() {
        return writerIndex;
    }

    @Override
    public int writableBytes() {
        return Integer.MAX_VALUE - writerIndex;
    }

    @Override
    public boolean readable() {
        return writerIndex > 0;
    }

    @Override
    public boolean writable() {
        return writerIndex < Integer.MAX_VALUE;
    }

    @Override
    public void clear() {
        writerIndex = 0;

    }

    @Override
    public void markReaderIndex() {
    }

    @Override
    public void resetReaderIndex() {
    }

    @Override
    public void markWriterIndex() {
        markedWriterIndex = writerIndex;
    }

    @Override
    public void resetWriterIndex() {
        writerIndex = markedWriterIndex;
    }

    @Override
    public void discardReadBytes() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void ensureWritableBytes(int writableBytes) {
        if(!((Integer.MAX_VALUE - writableBytes) > writerIndex))
            throw new IllegalStateException();
    }

    @Override
    public byte getByte(int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    public short getUnsignedByte(int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    public short getShort(int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getUnsignedShort(int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getMedium(int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getUnsignedMedium(int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getInt(int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getUnsignedInt(int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    public long getLong(int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    public char getChar(int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    public float getFloat(int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    public double getDouble(int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void getBytes(int index, ChannelBuffer dst) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void getBytes(int index, ChannelBuffer dst, int length) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void getBytes(int index, ChannelBuffer dst, int dstIndex, int length) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void getBytes(int index, byte[] dst) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void getBytes(int index, byte[] dst, int dstIndex, int length) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void getBytes(int index, ByteBuffer dst) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void getBytes(int index, OutputStream out, int length)
            throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getBytes(int index, GatheringByteChannel out, int length)
            throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setByte(int index, int value) {
    }

    @Override
    public void setShort(int index, int value) {
    }

    @Override
    public void setMedium(int index, int value) {
    }

    @Override
    public void setInt(int index, int value) {
    }

    @Override
    public void setLong(int index, long value) {
    }

    @Override
    public void setChar(int index, int value) {
    }

    @Override
    public void setFloat(int index, float value) {
    }

    @Override
    public void setDouble(int index, double value) {
    }

    @Override
    public void setBytes(int index, ChannelBuffer src) {
    }

    @Override
    public void setBytes(int index, ChannelBuffer src, int length) {
    }

    @Override
    public void setBytes(int index, ChannelBuffer src, int srcIndex, int length) {
    }

    @Override
    public void setBytes(int index, byte[] src) {
    }

    @Override
    public void setBytes(int index, byte[] src, int srcIndex, int length) {
    }

    @Override
    public void setBytes(int index, ByteBuffer src) {

    }

    @Override
    public int setBytes(int index, InputStream in, int length)
            throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public int setBytes(int index, ScatteringByteChannel in, int length)
            throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setZero(int index, int length) {
    }

    @Override
    public byte readByte() {
        throw new UnsupportedOperationException();
    }

    @Override
    public short readUnsignedByte() {
        throw new UnsupportedOperationException();
    }

    @Override
    public short readShort() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int readUnsignedShort() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int readMedium() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int readUnsignedMedium() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int readInt() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long readUnsignedInt() {
        throw new UnsupportedOperationException();
    }

    @Override
    public long readLong() {
        throw new UnsupportedOperationException();
    }

    @Override
    public char readChar() {
        throw new UnsupportedOperationException();
    }

    @Override
    public float readFloat() {
        throw new UnsupportedOperationException();
    }

    @Override
    public double readDouble() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ChannelBuffer readBytes(int length) {
        throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    public ChannelBuffer readBytes(ChannelBufferIndexFinder indexFinder) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ChannelBuffer readSlice(int length) {
        throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    public
    ChannelBuffer readSlice(ChannelBufferIndexFinder indexFinder) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void readBytes(ChannelBuffer dst) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void readBytes(ChannelBuffer dst, int length) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void readBytes(ChannelBuffer dst, int dstIndex, int length) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void readBytes(byte[] dst) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void readBytes(byte[] dst, int dstIndex, int length) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void readBytes(ByteBuffer dst) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void readBytes(OutputStream out, int length) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public int readBytes(GatheringByteChannel out, int length)
            throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void skipBytes(int length) {
        throw new UnsupportedOperationException();
    }

    @Override
    @Deprecated
    public int skipBytes(ChannelBufferIndexFinder indexFinder) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeByte(int value) {
        writerIndex++;
    }

    @Override
    public void writeShort(int value) {
    writerIndex += 2;
}

@Override
public void writeMedium(int value) {
    writerIndex += 3;
}

@Override
public void writeInt(int value) {
    writerIndex += 4;
}

@Override
public void writeLong(long value) {
    writerIndex += 8;
}


    @Override
    public void writeChar(int value) {
        writeShort(value);
    }

    @Override
    public void writeFloat(float value) {
        writeInt(Float.floatToIntBits(value));
    }

    @Override
    public void writeDouble(double value) {
        writeLong(Double.doubleToLongBits(value));

    }

    @Override
    public void writeBytes(ChannelBuffer src) {
        writerIndex += src.readableBytes();

    }

    @Override
    public void writeBytes(ChannelBuffer src, int length) {
        writerIndex += src.readableBytes();

    }

    @Override
    public void writeBytes(ChannelBuffer src, int srcIndex, int length) {
        writerIndex += length;
    }

    @Override
    public void writeBytes(byte[] src) {
        writerIndex += src.length;

    }

    @Override
    public void writeBytes(byte[] src, int srcIndex, int length) {
        writerIndex += length;
    }

    @Override
    public void writeBytes(ByteBuffer src) {
        writerIndex += src.remaining();

    }

    @Override
    public int writeBytes(InputStream in, int length) throws IOException {
        writerIndex += length;
        return length;
    }

    @Override
    public int writeBytes(ScatteringByteChannel in, int length)
            throws IOException {
        writerIndex += length;
        return length;
    }

    @Override
    public void writeZero(int length) {
        writerIndex += length;

    }

    @Override
    public int indexOf(int fromIndex, int toIndex, byte value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int indexOf(int fromIndex, int toIndex,
            ChannelBufferIndexFinder indexFinder) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int bytesBefore(byte value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int bytesBefore(ChannelBufferIndexFinder indexFinder) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int bytesBefore(int length, byte value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int bytesBefore(int length, ChannelBufferIndexFinder indexFinder) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int bytesBefore(int index, int length, byte value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int bytesBefore(int index, int length,
            ChannelBufferIndexFinder indexFinder) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ChannelBuffer copy() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ChannelBuffer copy(int index, int length) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ChannelBuffer slice() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ChannelBuffer slice(int index, int length) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ChannelBuffer duplicate() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ByteBuffer toByteBuffer() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ByteBuffer toByteBuffer(int index, int length) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ByteBuffer[] toByteBuffers() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ByteBuffer[] toByteBuffers(int index, int length) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean hasArray() {
        throw new UnsupportedOperationException();
    }

    @Override
    public byte[] array() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int arrayOffset() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString(Charset charset) {
        return "LengthCountingPseudoChannelBuffer(length="+writerIndex+")";
    }

    @Override
    public String toString(int index, int length, Charset charset) {
        return toString();
    }

    @Override
    @Deprecated
    public String toString(String charsetName) {
        return toString();
    }

    @Override
    @Deprecated
    public String toString(String charsetName,
            ChannelBufferIndexFinder terminatorFinder) {
        return toString();
    }

    @Override
    @Deprecated
    public String toString(int index, int length, String charsetName) {
        return toString();
    }

    @Override
    @Deprecated
    public
    String toString(int index, int length, String charsetName,
            ChannelBufferIndexFinder terminatorFinder) {
        return toString();
    }

    @Override
    public int compareTo(ChannelBuffer buffer) {
        throw new UnsupportedOperationException();

    }

}
