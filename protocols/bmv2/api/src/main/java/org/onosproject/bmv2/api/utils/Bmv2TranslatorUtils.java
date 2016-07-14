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

package org.onosproject.bmv2.api.utils;

import com.google.common.annotations.Beta;
import org.onlab.util.HexString;
import org.onlab.util.ImmutableByteSequence;

import java.util.Arrays;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Collection of utility methods to deal with flow rule translation.
 */
@Beta
public final class Bmv2TranslatorUtils {

    private Bmv2TranslatorUtils() {
        // Ban constructor.
    }

    /**
     * Returns the number of bytes necessary to contain the given bit-width.
     *
     * @param bitWidth an integer value
     * @return an integer value
     */
    public static int roundToBytes(int bitWidth) {
        return (int) Math.ceil((double) bitWidth / 8);
    }

    /**
     * Trims or expands the given byte sequence so to fit a given bit-width.
     *
     * @param original a byte sequence
     * @param bitWidth an integer value
     * @return a new byte sequence
     * @throws ByteSequenceFitException if the byte sequence cannot be fitted in the given bit-width
     */
    public static ImmutableByteSequence fitByteSequence(ImmutableByteSequence original, int bitWidth)
            throws ByteSequenceFitException {

        checkNotNull(original, "byte sequence cannot be null");
        checkArgument(bitWidth > 0, "byte width must a non-zero positive integer");

        int newByteWidth = roundToBytes(bitWidth);

        if (original.size() == newByteWidth) {
            // nothing to do
            return original;
        }

        byte[] originalBytes = original.asArray();

        if (newByteWidth > original.size()) {
            // pad missing bytes with zeros
            return ImmutableByteSequence.copyFrom(Arrays.copyOf(originalBytes, newByteWidth));
        }

        byte[] newBytes = new byte[newByteWidth];
        // ImmutableByteSequence is always big-endian, hence check the array in reverse order
        int diff = originalBytes.length - newByteWidth;
        for (int i = originalBytes.length - 1; i > 0; i--) {
            byte ob = originalBytes[i]; // original byte
            byte nb; // new byte
            if (i > diff) {
                // no need to truncate, copy as is
                nb = ob;
            } else if (i == diff) {
                // truncate this byte, check if we're loosing something
                byte mask = (byte) ((1 >> ((bitWidth % 8) + 1)) - 1);
                if ((ob & ~mask) != 0) {
                    throw new ByteSequenceFitException(originalBytes, bitWidth);
                } else {
                    nb = (byte) (ob & mask);
                }
            } else {
                // drop this byte, check if we're loosing something
                if (originalBytes[i] != 0) {
                    throw new ByteSequenceFitException(originalBytes, bitWidth);
                } else {
                    continue;
                }
            }
            newBytes[i - diff] = nb;
        }

        return ImmutableByteSequence.copyFrom(newBytes);
    }

    /**
     * A byte sequence fit exception.
     */
    public static class ByteSequenceFitException extends Exception {
        public ByteSequenceFitException(byte[] bytes, int bitWidth) {
            super("cannot fit " + HexString.toHexString(bytes) + " into a " + bitWidth + " bits value");
        }
    }
}
