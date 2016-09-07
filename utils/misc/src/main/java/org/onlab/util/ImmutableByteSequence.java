/*
 * Copyright 2016-present Open Networking Laboratory
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
import static org.apache.commons.lang3.ArrayUtils.reverse;

/**
 * Immutable sequence of bytes, assumed to represent a value in
 * {@link ByteOrder#BIG_ENDIAN BIG_ENDIAN} order.
 * <p>
 * Sequences can be created copying from an already existing representation of a
 * sequence of bytes, such as {@link ByteBuffer} or {@code byte[]}; or by
 * copying bytes from a primitive data type, such as {@code long}, {@code int}
 * or {@code short}. In the first case, bytes are assumed to be already given in
 * big-endian order, while in the second case big-endianness is enforced by this
 * class.
 */
public final class ImmutableByteSequence {

    /*
    Actual bytes are backed by a byte buffer.
    The order of a newly-created byte buffer is always BIG_ENDIAN.
     */
    private ByteBuffer value;

    /**
     * Private constructor.
     * Creates a new byte sequence object backed by the passed ByteBuffer.
     *
     * @param value a byte buffer
     */
    private ImmutableByteSequence(ByteBuffer value) {
        this.value = value;
        // Rewind buffer so it's ready to be read.
        // No write operation should be performed on it from now on.
        this.value.rewind();
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
     * @param fromIdx starting index
     * @param toIdx ending index
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
     * Creates a new byte sequence of the given size where alla bits are 0.
     *
     * @param size number of bytes
     * @return a new immutable byte sequence
     */
    public static ImmutableByteSequence ofZeros(int size) {
        byte[] bytes = new byte[size];
        Arrays.fill(bytes, (byte) 0);
        return new ImmutableByteSequence(ByteBuffer.wrap(bytes));
    }

    /**
     * Creates a new byte sequence of the given size where alla bits are 1.
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
        return this.value.capacity();
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

    @Override
    public String toString() {
        return HexString.toHexString(value.array());
    }
}
