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

import com.google.common.testing.EqualsTester;
import org.apache.commons.lang3.RandomUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Random;

import static java.lang.Integer.max;
import static java.lang.String.format;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

public class ImmutableByteSequenceTest {
    public static final int MIN_RAND_FIT_VALUE = 0xf;
    public static final int MAX_RAND_FIT_VALUE = 0x7fffffff;
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void testCopy() throws Exception {

        byte byteValue = (byte) 1;
        short shortValue = byteValue;
        int intValue = byteValue;
        long longValue = byteValue;
        byte[] arrayValue = new byte[64];
        arrayValue[63] = byteValue;
        ByteBuffer bufferValue = ByteBuffer.allocate(64).put(arrayValue);

        ImmutableByteSequence bsByte = ImmutableByteSequence.copyFrom(byteValue);
        ImmutableByteSequence bsShort = ImmutableByteSequence.copyFrom(shortValue);
        ImmutableByteSequence bsInt = ImmutableByteSequence.copyFrom(intValue);
        ImmutableByteSequence bsLong = ImmutableByteSequence.copyFrom(longValue);
        ImmutableByteSequence bsArray = ImmutableByteSequence.copyFrom(arrayValue);
        ImmutableByteSequence bsBuffer = ImmutableByteSequence.copyFrom(bufferValue);

        assertThat("byte sequence of a byte value must have size 1",
                   bsByte.size(), is(equalTo(1)));
        assertThat("byte sequence of a short value must have size 2",
                   bsShort.size(), is(equalTo(2)));
        assertThat("byte sequence of an int value must have size 4",
                   bsInt.size(), is(equalTo(4)));
        assertThat("byte sequence of a long value must have size 8",
                   bsLong.size(), is(equalTo(8)));
        assertThat("byte sequence of a byte array value must have same size of the array",
                   bsArray.size(), is(equalTo(arrayValue.length)));
        assertThat("byte sequence of a byte buffer value must have same size of the buffer",
                   bsBuffer.size(), is(equalTo(bufferValue.capacity())));

        String errStr = "incorrect byte sequence value";

        assertThat(errStr, bsByte.asArray()[0], is(equalTo(byteValue)));
        assertThat(errStr, bsShort.asArray()[1], is(equalTo(byteValue)));
        assertThat(errStr, bsInt.asArray()[3], is(equalTo(byteValue)));
        assertThat(errStr, bsLong.asArray()[7], is(equalTo(byteValue)));
        assertThat(errStr, bsArray.asArray()[63], is(equalTo(byteValue)));
        assertThat(errStr, bsBuffer.asArray()[63], is(equalTo(byteValue)));
    }

    @Test
    public void testCopyAndFit() throws Exception {
        int originalByteWidth = 3;
        int paddedByteWidth = 4;
        int trimmedByteWidth = 2;
        int indexFirstNonZeroByte = 1;

        byte byteValue = (byte) 1;
        byte[] arrayValue = new byte[originalByteWidth];
        arrayValue[indexFirstNonZeroByte] = byteValue;
        ByteBuffer bufferValue = ByteBuffer.allocate(originalByteWidth).put(arrayValue);

        ImmutableByteSequence bsBuffer = ImmutableByteSequence.copyAndFit(
                bufferValue, originalByteWidth * 8);
        ImmutableByteSequence bsBufferTrimmed = ImmutableByteSequence.copyAndFit(
                bufferValue, trimmedByteWidth * 8);
        ImmutableByteSequence bsBufferPadded = ImmutableByteSequence.copyAndFit(
                bufferValue, paddedByteWidth * 8);

        assertThat("byte sequence of the byte buffer must be 3 bytes long",
                   bsBuffer.size(), is(equalTo(originalByteWidth)));
        assertThat("byte sequence of the byte buffer must be 3 bytes long",
                   bsBufferTrimmed.size(), is(equalTo(trimmedByteWidth)));
        assertThat("byte sequence of the byte buffer must be 3 bytes long",
                   bsBufferPadded.size(), is(equalTo(paddedByteWidth)));

        String errStr = "incorrect byte sequence value";

        assertThat(errStr, bsBuffer.asArray()[indexFirstNonZeroByte], is(equalTo(byteValue)));
        assertThat(errStr, bsBufferTrimmed.asArray()[indexFirstNonZeroByte - 1], is(equalTo(byteValue)));
        assertThat(errStr, bsBufferPadded.asArray()[indexFirstNonZeroByte + 1], is(equalTo(byteValue)));
        assertThat(errStr, bsBufferPadded.asArray()[paddedByteWidth - 1], is(equalTo((byte) 0x00)));
    }

    @Test
    public void testCopyAndFitEndianness() throws Exception {
        int originalByteWidth = 4;
        int indexByteNonZeroBig = 1;
        int indexByteNonZeroLittle = 2;
        byte byteValue = (byte) 1;

        ByteBuffer bbBigEndian = ByteBuffer
                .allocate(originalByteWidth)
                .order(ByteOrder.BIG_ENDIAN);
        bbBigEndian.put(indexByteNonZeroBig, byteValue);
        ImmutableByteSequence bsBufferCopyBigEndian =
                ImmutableByteSequence.copyAndFit(bbBigEndian, originalByteWidth * 8);

        ByteBuffer bbLittleEndian = ByteBuffer
                .allocate(originalByteWidth)
                .order(ByteOrder.LITTLE_ENDIAN);
        bbLittleEndian.put(indexByteNonZeroLittle, byteValue);
        ImmutableByteSequence bsBufferCopyLittleEndian =
                ImmutableByteSequence.copyAndFit(bbLittleEndian, originalByteWidth * 8);

        // creates a new sequence from primitive type
        byte[] arrayValue = new byte[originalByteWidth];
        arrayValue[indexByteNonZeroBig] = byteValue;
        ImmutableByteSequence bsArrayCopy =
                ImmutableByteSequence.copyFrom(arrayValue);

        new EqualsTester()
                // big-endian byte array cannot be equal to little-endian array
                .addEqualityGroup(bbBigEndian.array())
                .addEqualityGroup(bbLittleEndian.array())
                // all byte sequences must be equal
                .addEqualityGroup(bsBufferCopyBigEndian,
                                  bsBufferCopyLittleEndian,
                                  bsArrayCopy)
                // byte buffer views of all sequences must be equal
                .addEqualityGroup(bsBufferCopyBigEndian.asReadOnlyBuffer(),
                                  bsBufferCopyLittleEndian.asReadOnlyBuffer(),
                                  bsArrayCopy.asReadOnlyBuffer())
                // byte buffer orders of all sequences must be ByteOrder.BIG_ENDIAN
                .addEqualityGroup(bsBufferCopyBigEndian.asReadOnlyBuffer().order(),
                                  bsBufferCopyLittleEndian.asReadOnlyBuffer().order(),
                                  bsArrayCopy.asReadOnlyBuffer().order(),
                                  ByteOrder.BIG_ENDIAN)
                .testEquals();
    }

    @Test
    public void testIllegalCopyAndFit() throws Exception {
        int originalByteWidth = 3;
        int trimmedByteWidth = 1;
        int indexFirstNonZeroByte = 1;

        byte byteValue = (byte) 1;
        byte[] arrayValue = new byte[originalByteWidth];
        arrayValue[indexFirstNonZeroByte] = byteValue;
        ByteBuffer bufferValue = ByteBuffer.allocate(originalByteWidth).put(arrayValue);

        try {
            ImmutableByteSequence.copyAndFit(bufferValue, trimmedByteWidth * 8);
            Assert.fail(format("Expect ByteSequenceTrimException due to value = %s and bitWidth %d",
                               Arrays.toString(arrayValue), trimmedByteWidth * 8));
        } catch (ImmutableByteSequence.ByteSequenceTrimException e) {
            // We expect this.
        }
    }

    @Test
    public void testEndianness() throws Exception {

        long longValue = RandomUtils.nextLong();

        // creates a new sequence from a big-endian buffer
        ByteBuffer bbBigEndian = ByteBuffer
                .allocate(8)
                .order(ByteOrder.BIG_ENDIAN)
                .putLong(longValue);
        ImmutableByteSequence bsBufferCopyBigEndian =
                ImmutableByteSequence.copyFrom(bbBigEndian);

        // creates a new sequence from a little-endian buffer
        ByteBuffer bbLittleEndian = ByteBuffer
                .allocate(8)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putLong(longValue);
        ImmutableByteSequence bsBufferCopyLittleEndian =
                ImmutableByteSequence.copyFrom(bbLittleEndian);

        // creates a new sequence from primitive type
        ImmutableByteSequence bsLongCopy =
                ImmutableByteSequence.copyFrom(longValue);


        new EqualsTester()
                // big-endian byte array cannot be equal to little-endian array
                .addEqualityGroup(bbBigEndian.array())
                .addEqualityGroup(bbLittleEndian.array())
                // all byte sequences must be equal
                .addEqualityGroup(bsBufferCopyBigEndian,
                                  bsBufferCopyLittleEndian,
                                  bsLongCopy)
                // byte buffer views of all sequences must be equal
                .addEqualityGroup(bsBufferCopyBigEndian.asReadOnlyBuffer(),
                                  bsBufferCopyLittleEndian.asReadOnlyBuffer(),
                                  bsLongCopy.asReadOnlyBuffer())
                // byte buffer orders of all sequences must be ByteOrder.BIG_ENDIAN
                .addEqualityGroup(bsBufferCopyBigEndian.asReadOnlyBuffer().order(),
                                  bsBufferCopyLittleEndian.asReadOnlyBuffer().order(),
                                  bsLongCopy.asReadOnlyBuffer().order(),
                                  ByteOrder.BIG_ENDIAN)
                .testEquals();
    }

    @Test
    public void testBitSetMethods() throws Exception {
        // All zeros tests
        assertThat("3 bytes, all 0's",
                   ImmutableByteSequence.ofZeros(3),
                   is(equalTo(ImmutableByteSequence.copyFrom(
                           new byte[]{0, 0, 0}))));
        assertThat("3 bytes, all 0's via prefix",
                   ImmutableByteSequence.prefixZeros(3, 3 * Byte.SIZE),
                   is(equalTo(ImmutableByteSequence.copyFrom(
                           new byte[]{0, 0, 0}))));

        // All ones tests
        assertThat("3 bytes, all 1's",
                   ImmutableByteSequence.ofZeros(3),
                   is(equalTo(ImmutableByteSequence.copyFrom(
                           new byte[]{0, 0, 0}))));
        assertThat("3 bytes, all 1's via prefix",
                   ImmutableByteSequence.prefixOnes(3, 3 * Byte.SIZE),
                   is(equalTo(ImmutableByteSequence.copyFrom(
                           new byte[]{(byte) 0xff, (byte) 0xff, (byte) 0xff}))));

        // Zero prefix tests
        assertThat("2 bytes, prefixed with 5 0's",
                   ImmutableByteSequence.prefix(2, 5, (byte) 0),
                   is(equalTo(ImmutableByteSequence.copyFrom(
                           new byte[]{(byte) 0x7, (byte) 0xff}))));
        assertThat("4 bytes, prefixed with 16 0's",
                   ImmutableByteSequence.prefix(4, 16, (byte) 0),
                   is(equalTo(ImmutableByteSequence.copyFrom(
                           new byte[]{0, 0, (byte) 0xff, (byte) 0xff}))));
        assertThat("4 bytes, prefixed with 20 0's",
                   ImmutableByteSequence.prefix(4, 20, (byte) 0),
                   is(equalTo(ImmutableByteSequence.copyFrom(
                           new byte[]{0, 0, (byte) 0x0f, (byte) 0xff}))));
        assertThat("8 bytes, prefixed with 36 0's",
                   ImmutableByteSequence.prefixZeros(8, 38),
                   is(equalTo(ImmutableByteSequence.copyFrom(
                           new byte[]{0, 0, 0, 0, (byte) 0x03, (byte) 0xff, (byte) 0xff, (byte) 0xff}))));

        // Ones prefix tests
        assertThat("2 bytes, prefixed with 5 1's",
                   ImmutableByteSequence.prefix(2, 5, (byte) 0xff),
                   is(equalTo(ImmutableByteSequence.copyFrom(
                           new byte[]{(byte) 0xf8, 0}))));
        assertThat("4 bytes, prefixed with 16 1's",
                   ImmutableByteSequence.prefix(4, 16, (byte) 0xff),
                   is(equalTo(ImmutableByteSequence.copyFrom(
                           new byte[]{(byte) 0xff, (byte) 0xff, 0, 0}))));
        assertThat("4 bytes, prefixed with 20 1's",
                   ImmutableByteSequence.prefix(4, 20, (byte) 0xff),
                   is(equalTo(ImmutableByteSequence.copyFrom(
                           new byte[]{(byte) 0xff, (byte) 0xff, (byte) 0xf0, 0}))));
        assertThat("8 bytes, prefixed with 10 1's",
                   ImmutableByteSequence.prefixOnes(8, 10),
                   is(equalTo(ImmutableByteSequence.copyFrom(
                           new byte[]{(byte) 0xff, (byte) 0xc0, 0, 0, 0, 0, 0, 0}))));
    }

    @Test
    public void testBadPrefixVal() {
        thrown.expect(IllegalArgumentException.class);
        thrown.reportMissingExceptionWithMessage(
                "Expect IllegalArgumentException due to val = 0x7");
        ImmutableByteSequence.prefix(5, 10, (byte) 0x7);
    }

    @Test
    public void testMsbIndex() {
        assertThat("Value 0 should have MSB index -1",
                   ImmutableByteSequence.copyFrom(0).msbIndex(), is(-1));
        for (int i = 0; i < 63; i++) {
            long value = (long) Math.pow(2, i);
            assertThat(format("Value %d should have MSB index %d", value, i),
                       ImmutableByteSequence.copyFrom(value).msbIndex(), is(i));
        }
    }

    private void checkIllegalFit(ImmutableByteSequence bytes, int bitWidth) {
        try {
            bytes.fit(bitWidth);
            Assert.fail(format("Except ByteSequenceTrimException due to value = %s and bitWidth %d",
                               bytes.toString(), bitWidth));
        } catch (ImmutableByteSequence.ByteSequenceTrimException e) {
            // We expect this.
        }
    }

    private void checkLegalFit(ImmutableByteSequence bytes, int bitWidth)
            throws ImmutableByteSequence.ByteSequenceTrimException {
        ImmutableByteSequence fitBytes = bytes.fit(bitWidth);
        ImmutableByteSequence sameBytes = fitBytes.fit(bytes.size() * 8);
        assertThat(format("Fitted value %s (re-extended to %s) not equal to original value %s",
                          fitBytes, sameBytes, bytes),
                   sameBytes,
                   is(equalTo(bytes)));
    }

    @Test
    public void testFit() throws ImmutableByteSequence.ByteSequenceTrimException {
        // Test fit by forcing a given MSB index.
        for (int msbIndex = 0; msbIndex < 32; msbIndex++) {
            long value = (long) Math.pow(2, msbIndex);
            ImmutableByteSequence bytes = ImmutableByteSequence.copyFrom(value);
            checkLegalFit(bytes, msbIndex + 1);
            if (msbIndex != 0) {
                checkIllegalFit(bytes, msbIndex);
            }
        }
    }

    @Test
    public void testRandomFit() throws ImmutableByteSequence.ByteSequenceTrimException {
        // Test fit against the computed MSB index.
        Random random = new Random();
        for (int i = 0; i < 1000; i++) {
            int randValue = random.nextInt((MAX_RAND_FIT_VALUE - MIN_RAND_FIT_VALUE) + 1) + MIN_RAND_FIT_VALUE;
            ImmutableByteSequence bytes = ImmutableByteSequence.copyFrom((long) randValue);
            int msbIndex = bytes.msbIndex();
            // Truncate.
            checkIllegalFit(bytes, max(msbIndex - random.nextInt(16), 1));
            // Expand.
            checkLegalFit(bytes, msbIndex + 2 + random.nextInt(128));
            // Fit to same bit-width of original value.
            checkLegalFit(bytes, msbIndex + 1);
        }
    }

    @Test
    public void testBitwiseOperations() {
        Random random = new Random();
        long long1 = random.nextLong();
        long long2 = random.nextLong();

        ImmutableByteSequence bs1 = ImmutableByteSequence.copyFrom(long1);
        ImmutableByteSequence bs2 = ImmutableByteSequence.copyFrom(long2);

        ImmutableByteSequence andBs = bs1.bitwiseAnd(bs2);
        ImmutableByteSequence orBs = bs1.bitwiseOr(bs2);
        ImmutableByteSequence xorBs = bs1.bitwiseXor(bs2);

        assertThat("Invalid bitwise AND result",
                   andBs.asReadOnlyBuffer().getLong(), is(long1 & long2));
        assertThat("Invalid bitwise OR result",
                   orBs.asReadOnlyBuffer().getLong(), is(long1 | long2));
        assertThat("Invalid bitwise XOR result",
                   xorBs.asReadOnlyBuffer().getLong(), is(long1 ^ long2));
    }

    @Test
    public void testCanonical() {
        ImmutableByteSequence bs = ImmutableByteSequence.copyFrom(0x000000ff);
        ImmutableByteSequence canonicalBs = bs.canonical();
        assertThat("Incorrect size", canonicalBs.size(), is(1));
        ByteBuffer bb = canonicalBs.asReadOnlyBuffer();
        assertThat("Incorrect byte buffer position", bb.position(), is(3));

        bs = ImmutableByteSequence.copyFrom(0x100000ff);
        canonicalBs = bs.canonical();
        assertThat("Incorrect size", canonicalBs.size(), is(4));
        bb = canonicalBs.asReadOnlyBuffer();
        assertThat("Incorrect byte buffer position", bb.position(), is(0));

        bs = ImmutableByteSequence.copyFrom(0x00000000ff0000ffL);
        canonicalBs = bs.canonical();
        assertThat("Incorrect size", canonicalBs.size(), is(4));
        bb = canonicalBs.asReadOnlyBuffer();
        assertThat("Incorrect byte buffer position", bb.position(), is(4));

        bs = ImmutableByteSequence.copyFrom(0);
        canonicalBs = bs.canonical();
        assertThat("Incorrect size", canonicalBs.size(), is(1));
        bb = canonicalBs.asReadOnlyBuffer();
        assertThat("Incorrect byte buffer position", bb.position(), is(3));

        bs = ImmutableByteSequence.copyFrom(0L);
        canonicalBs = bs.canonical();
        assertThat("Incorrect size", canonicalBs.size(), is(1));
        bb = canonicalBs.asReadOnlyBuffer();
        assertThat("Incorrect byte buffer position", bb.position(), is(7));

        new EqualsTester()
                .addEqualityGroup(
                        ImmutableByteSequence.copyFrom(0x000000ff).canonical(),
                        ImmutableByteSequence.copyFrom((short) 0x00ff).canonical())
                .addEqualityGroup(
                        ImmutableByteSequence.copyFrom(0x000001ff).canonical(),
                        ImmutableByteSequence.copyFrom(0x00000000000001ffL).canonical())
                .addEqualityGroup(
                        ImmutableByteSequence.copyFrom(0xc00001ff).canonical(),
                        ImmutableByteSequence.copyFrom(0x00000000c00001ffL).canonical())
                .addEqualityGroup(
                        ImmutableByteSequence.copyFrom(0).canonical(),
                        ImmutableByteSequence.copyFrom(0L).canonical())
                .testEquals();
    }
}
