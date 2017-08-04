/*
 * Copyright 2015-present Open Networking Foundation
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

import java.nio.ByteBuffer;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertTrue;

/**
 * Utilities for testing packet methods.
 */
public final class PacketTestUtils {

    private PacketTestUtils() {
    }

    /**
     * Tests that the Deserializer function is resilient to bad input parameters
     * such as null input, negative offset and length, etc.
     *
     * @param deserializer deserializer function to test
     */
    public static void testDeserializeBadInput(Deserializer deserializer) {
        byte[] bytes = ByteBuffer.allocate(4).array();

        try {
            deserializer.deserialize(null, 0, 4);
            fail("NullPointerException was not thrown");
        } catch (NullPointerException e) {
            assertTrue(true);
        } catch (DeserializationException e) {
            fail("NullPointerException was not thrown");
        }

        // input byte array length, offset and length don't make sense
        expectDeserializationException(deserializer, bytes, -1, 0);
        expectDeserializationException(deserializer, bytes, 0, -1);
        expectDeserializationException(deserializer, bytes, 0, 5);
        expectDeserializationException(deserializer, bytes, 2, 3);
        expectDeserializationException(deserializer, bytes, 5, 0);
    }

    /**
     * Tests that the Deserializer function is resilient to truncated input, or
     * cases where the input byte array does not contain enough bytes to
     * deserialize the packet.
     *
     * @param deserializer deserializer function to test
     * @param header       byte array of a full-size packet
     */
    public static void testDeserializeTruncated(Deserializer deserializer,
                                                byte[] header) {
        byte[] truncated;

        for (int i = 0; i < header.length; i++) {
            truncated = new byte[i];

            ByteBuffer.wrap(header).get(truncated);

            expectDeserializationException(deserializer, truncated, 0, truncated.length);
        }
    }

    /**
     * Run the given deserializer function against the given inputs and verify
     * that a DeserializationException is thrown. The the test will fail if a
     * DeserializationException is not thrown by the deserializer function.
     *
     * @param deserializer deserializer function to test
     * @param bytes        input byte array
     * @param offset       input offset
     * @param length       input length
     */
    public static void expectDeserializationException(Deserializer deserializer,
                                                      byte[] bytes, int offset, int length) {
        try {
            deserializer.deserialize(bytes, offset, length);
            fail("DeserializationException was not thrown");
        } catch (DeserializationException e) {
            assertTrue(true);
        }
    }
}
