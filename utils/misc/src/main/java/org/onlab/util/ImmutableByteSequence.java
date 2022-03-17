/*
 * Copyright 2016-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onlab.util;

import com.google.common.base.Objects;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static org.apache.commons.lang3.ArrayUtils.reverse;

/**
 * Immutable sequence of bytes, assumed to represent a value in {@link
 * ByteOrder#BIG_ENDIAN BIG_ENDIAN} order.
 * <p>
 * Sequences can be created copying from an already existing representation of a
 * sequence of bytes, such as {@link ByteBuffer} or {@code byte[]}; or by
 * copying bytes from a primitive data type, such as {@code long}, {@code int}
 * or {@code short}. In the first case, bytes are assumed to be already given in
 * big-endian order, while in the second case big-endianness is enforced by this
 * class.
 */
public final class ImmutableByteSequence {

    private enum BitwiseOp {
        AND,
        OR,
        XOR
    }

    /*
    Actual bytes are backed by a byte buffer.
    The order of a newly-created byte buffer is always BIG_ENDIAN.
     */
    private ByteBuffer value;
    private boolean isAscii = false;

    /**
     * Private constructor. Creates a new byte sequence object backed by the
     * passed ByteBuffer.
     *
     * @param value a byte buffer
     */
    private ImmutableByteSequence(ByteBuffer value) {
        this.value = value;
        // Rewind buffer so it's ready to be read.
        // No write operation should be performed on it from now on.
        this.value.rewind();
    }

    private ImmutableByteSequence(ByteBuffer value, boolean isAscii) {
        this(value);
        this.isAscii = isAscii;
    }

    /**
     * Creates a new immutable byte sequence with the same content and order of
     * the passed byte array.
     *
     * @param original a byte array value
     * @return a new immutable byte sequence
     */
    public static ImmutableByteSequence copyFrom(byte[] original) {
        checkArgument(original != null && original.length > 0,
                      "Cannot copy from an empty or null array");
        return new ImmutableByteSequence(
                ByteBuffer.allocate(original.length).put(original));
    }

    /**
     * Creates a new immutable byte sequence with the same content and order of
     * the passed byte array, from/to the given indexes (inclusive).
     *
     * @param original a byte array value
     * @param fromIdx  starting index
     * @param toIdx    ending index
     * @return a new immutable byte sequence
     */
    public static ImmutableByteSequence copyFrom(byte[] original, int fromIdx, int toIdx) {
        checkArgument(original != null && original.length > 0,
                      "Cannot copy from an empty or null array");
        checkArgument(toIdx >= fromIdx && toIdx < original.length, "invalid indexes");
        ByteBuffer buffer = ByteBuffer.allocate((toIdx - fromIdx) + 1);
        for (int i = fromIdx; i <= toIdx; i++) {
            buffer.put(original[i]);
        }
        return new ImmutableByteSequence(buffer);
    }

    /**
     * Creates a new immutable byte sequence copying bytes from the given
     * ByteBuffer {@link ByteBuffer}. If the byte buffer order is not big-endian
     * bytes will be copied in reverse order.
     *
     * @param original a byte buffer
     * @return a new byte buffer object
     */
    public static ImmutableByteSequence copyFrom(ByteBuffer original) {
        checkArgument(original != null && original.capacity() > 0,
                      "Cannot copy from an empty or null byte buffer");

        byte[] bytes = new byte[original.capacity()];

        // copy bytes from original buffer
        original.rewind();
        original.get(bytes);

        if (original.order() == ByteOrder.LITTLE_ENDIAN) {
            // FIXME: this can be improved, e.g. read bytes in reverse order from original
            reverse(bytes);
        }

        return new ImmutableByteSequence(ByteBuffer.wrap(bytes));
    }

    /**
     * Creates a new immutable byte sequence from the given string.
     *
     * @param original a string
     * @return a new byte buffer object
     */
    public static ImmutableByteSequence copyFrom(String original) {
        checkArgument(original != null && original.length() > 0,
                      "Cannot copy from an empty or null string");
        return new ImmutableByteSequence(ByteBuffer.allocate(original.length())
                                                 .put(original.getBytes()),
                                         true);
    }

    /**
     * Creates a new byte sequence of 8 bytes containing the given long value.
     *
     * @param original a long value
     * @return a new immutable byte sequence
     */
    public static ImmutableByteSequence copyFrom(long original) {
        return new ImmutableByteSequence(
                ByteBuffer.allocate(Long.BYTES).putLong(original));
    }

    /**
     * Creates a new byte sequence of 4 bytes containing the given int value.
     *
     * @param original an int value
     * @return a new immutable byte sequence
     */
    public static ImmutableByteSequence copyFrom(int original) {
        return new ImmutableByteSequence(
                ByteBuffer.allocate(Integer.BYTES).putInt(original));
    }

    /**
     * Creates a new byte sequence of 2 bytes containing the given short value.
     *
     * @param original a short value
     * @return a new immutable byte sequence
     */
    public static ImmutableByteSequence copyFrom(short original) {
        return new ImmutableByteSequence(
                ByteBuffer.allocate(Short.BYTES).putShort(original));
    }

    /**
     * Creates a new byte sequence of 1 byte containing the given value.
     *
     * @param original a byte value
     * @return a new immutable byte sequence
     */
    public static ImmutableByteSequence copyFrom(byte original) {
        return new ImmutableByteSequence(
                ByteBuffer.allocate(Byte.BYTES).put(original));
    }

    /**
     * Creates a new immutable byte sequence while trimming or expanding the
     * content of the given byte buffer to fit the given bit-width. Calling this
     * method has the same behavior as
     * {@code ImmutableByteSequence.copyFrom(original).fit(bitWidth)}.
     *
     * @param original a byte buffer value
     * @param bitWidth a non-zero positive integer
     * @return a new immutable byte sequence
     * @throws ByteSequenceTrimException if the byte buffer cannot be fitted
     */
    public static ImmutableByteSequence copyAndFit(ByteBuffer original, int bitWidth)
            throws ByteSequenceTrimException {
        checkArgument(original != null && original.capacity() > 0,
                      "Cannot copy from an empty or null byte buffer");
        checkArgument(bitWidth > 0,
                      "bit-width must be a non-zero positive integer");
        if (original.order() == ByteOrder.LITTLE_ENDIAN) {
            // FIXME: this can be improved, e.g. read bytes in reverse order from original
            byte[] newBytes = new byte[original.capacity()];
            original.get(newBytes);
            reverse(newBytes);
            return internalCopyAndFit(ByteBuffer.wrap(newBytes), bitWidth);
        } else {
            return internalCopyAndFit(original.duplicate(), bitWidth);
        }
    }

    private static ImmutableByteSequence internalCopyAndFit(ByteBuffer byteBuf, int bitWidth)
            throws ByteSequenceTrimException {
        final int byteWidth = (bitWidth + 7) / 8;
        final ByteBuffer newByteBuffer = ByteBuffer.allocate(byteWidth);
        byteBuf.rewind();
        if (byteWidth >= byteBuf.capacity()) {
            newByteBuffer.position(byteWidth - byteBuf.capacity());
            newByteBuffer.put(byteBuf);
        } else {
            for (int i = 0; i < byteBuf.capacity() - byteWidth; i++) {
                if (byteBuf.get(i) != 0) {
                    throw new ByteSequenceTrimException(byteBuf, bitWidth);
                }
            }
            newByteBuffer.put(byteBuf.position(byteBuf.capacity() - byteWidth));
        }
        return new ImmutableByteSequence(newByteBuffer);
    }

    /**
     * Creates a new byte sequence of the given size where all bits are 0.
     *
     * @param size number of bytes
     * @return a new immutable byte sequence
     */
    public static ImmutableByteSequence ofZeros(int size) {
        // array is initialized to all 0's by default
        return new ImmutableByteSequence(ByteBuffer.wrap(new byte[size]));
    }

    /**
     * Creates a new byte sequence of the given size where all bits are 1.
     *
     * @param size number of bytes
     * @return a new immutable byte sequence
     */
    public static ImmutableByteSequence ofOnes(int size) {
        byte[] bytes = new byte[size];
        Arrays.fill(bytes, (byte) 0xFF);
        return new ImmutableByteSequence(ByteBuffer.wrap(bytes));
    }

    /**
     * Creates a new byte sequence that is prefixed with specified number of
     * zeros if val = 0 or ones if val = 0xff.
     *
     * @param size       number of total bytes
     * @param prefixBits number of bits in prefix
     * @param val        0 for prefix of zeros; 0xff for prefix of ones
     * @return new immutable byte sequence
     */
    static ImmutableByteSequence prefix(int size, long prefixBits, byte val) {
        checkArgument(val == 0 || val == (byte) 0xff, "Val must be 0 or 0xff");
        byte[] bytes = new byte[size];
        int prefixBytes = (int) (prefixBits / Byte.SIZE);
        Arrays.fill(bytes, 0, prefixBytes, val);
        Arrays.fill(bytes, prefixBytes, bytes.length, (byte) ~val);
        int partialBits = (int) (prefixBits % Byte.SIZE);
        if (partialBits != 0) {
            bytes[prefixBytes] = val == 0 ?
                    (byte) (0xff >> partialBits) : (byte) (0xff << Byte.SIZE - partialBits);
        }
        return new ImmutableByteSequence(ByteBuffer.wrap(bytes));
    }

    /**
     * Creates a new byte sequence that is prefixed with specified number of
     * zeros.
     *
     * @param size       number of total bytes
     * @param prefixBits number of bits in prefix
     * @return new immutable byte sequence
     */
    public static ImmutableByteSequence prefixZeros(int size, long prefixBits) {
        return prefix(size, prefixBits, (byte) 0);
    }

    /**
     * Creates a new byte sequence that is prefixed with specified number of
     * ones.
     *
     * @param size       number of total bytes
     * @param prefixBits number of bits in prefix
     * @return new immutable byte sequence
     */
    public static ImmutableByteSequence prefixOnes(int size, long prefixBits) {
        return prefix(size, prefixBits, (byte) 0xff);
    }

    /**
     * Returns a view of this sequence as a read-only {@link ByteBuffer}.
     * <p>
     * The returned buffer will have position 0, while limit and capacity will
     * be set to this sequence {@link #size()}. The buffer order will be
     * big-endian.
     *
     * @return a read-only byte buffer
     */
    public ByteBuffer asReadOnlyBuffer() {
        // position, limit and capacity set rewind at constructor
        return value.asReadOnlyBuffer();
    }

    /**
     * Gets the number of bytes in this sequence.
     *
     * @return an integer value
     */
    public int size() {
        return this.value.limit() - this.value.position();
    }

    /**
     * Creates a new byte array view of this sequence.
     *
     * @return a new byte array
     */
    public byte[] asArray() {
        ByteBuffer bb = asReadOnlyBuffer();
        byte[] bytes = new byte[size()];
        bb.get(bytes);
        return bytes;
    }

    private ImmutableByteSequence doBitwiseOp(ImmutableByteSequence other, BitwiseOp op) {
        checkArgument(other != null && this.size() == other.size(),
                      "Other sequence must be non null and with same size as this");
        byte[] newBytes = new byte[this.size()];
        byte[] thisBytes = this.asArray();
        byte[] otherBytes = other.asArray();
        for (int i = 0; i < this.size(); i++) {
            switch (op) {
                case AND:
                    newBytes[i] = (byte) (thisBytes[i] & otherBytes[i]);
                    break;
                case OR:
                    newBytes[i] = (byte) (thisBytes[i] | otherBytes[i]);
                    break;
                case XOR:
                    newBytes[i] = (byte) (thisBytes[i] ^ otherBytes[i]);
                    break;
                default:
                    throw new IllegalArgumentException(
                            "Unknown bitwise operator " + op.name());
            }
        }
        return ImmutableByteSequence.copyFrom(newBytes);
    }

    /**
     * Returns a new byte sequence corresponding to the result of a bitwise AND
     * operation between this sequence and the given other, i.e. {@code this &
     * other}.
     *
     * @param other other byte sequence
     * @return new byte sequence
     * @throws IllegalArgumentException if other sequence is null or its size is
     *                                  different than this sequence size
     */
    public ImmutableByteSequence bitwiseAnd(ImmutableByteSequence other) {
        return doBitwiseOp(other, BitwiseOp.AND);
    }

    /**
     * Returns a new byte sequence corresponding to the result of a bitwise OR
     * operation between this sequence and the given other, i.e. {@code this |
     * other}.
     *
     * @param other other byte sequence
     * @return new byte sequence
     * @throws IllegalArgumentException if other sequence is null or its size is
     *                                  different than this sequence size
     */
    public ImmutableByteSequence bitwiseOr(ImmutableByteSequence other) {
        return doBitwiseOp(other, BitwiseOp.OR);
    }

    /**
     * Returns a new byte sequence corresponding to the result of a bitwise XOR
     * operation between this sequence and the given other, i.e. {@code this ^
     * other}.
     *
     * @param other other byte sequence
     * @return new byte sequence
     * @throws IllegalArgumentException if other sequence is null or its size is
     *                                  different than this sequence size
     */
    public ImmutableByteSequence bitwiseXor(ImmutableByteSequence other) {
        return doBitwiseOp(other, BitwiseOp.XOR);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final ImmutableByteSequence other = (ImmutableByteSequence) obj;
        return Objects.equal(this.value, other.value);
    }

    /**
     * Returns the index of the most significant bit (MSB), assuming a bit
     * numbering scheme of type "LSB 0", i.e. the bit numbering starts at zero
     * for the least significant bit (LSB). The MSB index of a byte sequence of
     * zeros will be -1.
     * <p>
     * As an example, the following conditions always hold true: {@code
     * ImmutableByteSequence.copyFrom(0).msbIndex() == -1
     * ImmutableByteSequence.copyFrom(1).msbIndex() == 0
     * ImmutableByteSequence.copyFrom(2).msbIndex() == 1
     * ImmutableByteSequence.copyFrom(3).msbIndex() == 1
     * ImmutableByteSequence.copyFrom(4).msbIndex() == 2
     * ImmutableByteSequence.copyFrom(512).msbIndex() == 9 }
     *
     * @return index of the MSB, -1 if the sequence has all bytes set to 0
     */
    public int msbIndex() {
        int index = (size() * 8) - 1;
        byteLoop:
        for (int i = 0; i < size(); i++) {
            byte b = value.get(i);
            if (b != 0) {
                for (int j = 7; j >= 0; j--) {
                    byte mask = (byte) ((1 << j) - 1);
                    if ((b & ~mask) != 0) {
                        break byteLoop;
                    }
                    index--;
                }
            }
            index -= 8;
        }
        return index;
    }

    /**
     * Returns the ASCII representation of the byte sequence if the content can
     * be interpreted as an ASCII string, otherwise returns the hexadecimal
     * representation of this byte sequence, e.g.0xbeef. The length of the
     * returned string is not representative of the length of the byte sequence,
     * as all padding zeros are removed.
     *
     * @return hexadecimal representation
     */
    @Override
    public String toString() {
        if (this.isAscii()) {
            return new String(value.array());
        } else {
            return "0x" + HexString
                    .toHexString(value.array(), "")
                    // Remove leading zeros, but leave one if string is all zeros.
                    .replaceFirst("^0+(?!$)", "");
        }
    }

    /**
     * Checks if the content can be interpreted as an ASCII printable string.
     *
     * @return True if the content can be interpreted as an ASCII printable
     *  string, false otherwise
     */
    public boolean isAscii() {
        return isAscii;
    }

    /**
     * Trims or expands a copy of this byte sequence so to fit the given
     * bit-width. When trimming, the operations is deemed to be safe only if the
     * trimmed bits are zero, i.e. it is safe to trim only when {@code bitWidth
     * > msbIndex()}, otherwise an exception will be thrown. When expanding, the
     * sequence will be padded with zeros. The returned byte sequence will have
     * minimum size to contain the given bit-width.
     *
     * @param bitWidth a non-zero positive integer
     * @return a new byte sequence
     * @throws ByteSequenceTrimException if the byte sequence cannot be fitted
     */
    public ImmutableByteSequence fit(int bitWidth) throws ByteSequenceTrimException {
        return doFit(this, bitWidth);
    }

    private static ImmutableByteSequence doFit(ImmutableByteSequence original,
                                               int bitWidth)
            throws ByteSequenceTrimException {

        checkNotNull(original, "byte sequence cannot be null");
        checkArgument(bitWidth > 0, "bit-width must be a non-zero positive integer");

        int newByteWidth = (bitWidth + 7) / 8;

        if (bitWidth == original.size() * 8) {
            // No need to fit.
            return original;
        }

        ByteBuffer newBuffer = ByteBuffer.allocate(newByteWidth);

        if (newByteWidth > original.size()) {
            // Pad extra bytes with 0's.
            int numPadBytes = newByteWidth - original.size();
            for (int i = 0; i < numPadBytes; i++) {
                newBuffer.put((byte) 0x00);
            }
            newBuffer.put(original.asReadOnlyBuffer());
        } else {
            // Trim sequence.
            if (bitWidth > original.msbIndex()) {
                int diff = original.size() - newByteWidth;
                ByteBuffer originalBuffer = original.asReadOnlyBuffer();
                for (int i = diff; i < original.size(); i++) {
                    newBuffer.put(originalBuffer.get(i));
                }
            } else {
                throw new ByteSequenceTrimException(original, bitWidth);
            }
        }

        return new ImmutableByteSequence(newBuffer);
    }

    /**
     * Returns a new ImmutableByteSequence with same content as this one, but with leading zero bytes stripped.
     *
     * @return new ImmutableByteSequence
     */
    public ImmutableByteSequence canonical() {
        ByteBuffer newByteBuffer = this.value.duplicate();
        ImmutableByteSequence canonicalBs = new ImmutableByteSequence(newByteBuffer);
        canonicalBs.value.rewind();
        while (canonicalBs.value.hasRemaining() && canonicalBs.value.get() == 0) {
            // Make style check happy
        }
        canonicalBs.value.position(canonicalBs.value.position() - 1);
        return canonicalBs;
    }

    /**
     * Signals a trim exception during byte sequence creation.
     */
    public static class ByteSequenceTrimException extends Exception {
        ByteSequenceTrimException(ImmutableByteSequence original, int bitWidth) {
            super(format("cannot trim %s into a %d bits long value",
                         original, bitWidth));
        }

        ByteSequenceTrimException(ByteBuffer original, int bitWidth) {
            super(format("cannot trim %s (ByteBuffer) into a %d bits long value",
                         original, bitWidth));
        }
    }
}
