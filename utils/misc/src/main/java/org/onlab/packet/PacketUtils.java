/*
 * Copyright 2015-present Open Networking Laboratory
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

package org.onlab.packet;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Utilities for working with packet headers.
 */
public final class PacketUtils {

    private PacketUtils() {
    }

    /**
     * Check the length of the input buffer is appropriate given the offset and
     * length parameters.
     *
     * @param byteLength length of the input buffer array
     * @param offset offset given to begin reading bytes from
     * @param length length given to read up until
     * @throws DeserializationException if the input parameters don't match up (i.e
     * we can't read that many bytes from the buffer at the given offest)
     */
    public static void checkBufferLength(int byteLength, int offset, int length)
            throws DeserializationException {
        boolean ok = (offset >= 0 && offset < byteLength);
        ok = ok & (length >= 0 && offset + length <= byteLength);

        if (!ok) {
            throw new DeserializationException("Unable to read " + length + " bytes from a "
                + byteLength + " byte array starting at offset " + offset);
        }
    }

    /**
     * Check that there are enough bytes in the buffer to read some number of
     * bytes that we need to read a full header.
     *
     * @param givenLength given size of the buffer
     * @param requiredLength number of bytes we need to read some header fully
     * @throws DeserializationException if there aren't enough bytes
     */
    public static void checkHeaderLength(int givenLength, int requiredLength)
            throws DeserializationException {
        if (requiredLength > givenLength) {
            throw new DeserializationException(requiredLength
                + " bytes are needed to continue deserialization, however only "
                + givenLength + " remain in buffer");
        }
    }

    /**
     * Check the input parameters are sane and there's enough bytes to read
     * the required length.
     *
     * @param data input byte buffer
     * @param offset offset of the start of the header
     * @param length length given to deserialize the header
     * @param requiredLength length needed to deserialize header
     * @throws DeserializationException if we're unable to deserialize the
     * packet based on the input parameters
     */
    public static void checkInput(byte[] data, int offset, int length, int requiredLength)
            throws DeserializationException {
        checkNotNull(data);
        checkBufferLength(data.length, offset, length);
        checkHeaderLength(length, requiredLength);
    }
}
