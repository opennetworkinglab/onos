/*
 * Copyright 2014-present Open Networking Laboratory
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

import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.Test;

/**
 * Test cases for byte[] pretty printer.
 */
public class ByteArraySizeHashPrinterTest {

    /**
     * Test method for {@link org.onlab.util.ByteArraySizeHashPrinter#toString()}.
     */
    @Test
    public void testToStringNull() {
        final byte[] none = null;

        assertEquals("byte[]{null}", String.valueOf(ByteArraySizeHashPrinter.of(none)));
        assertNull(ByteArraySizeHashPrinter.orNull(none));
    }

    /**
     * Test method for {@link org.onlab.util.ByteArraySizeHashPrinter#toString()}.
     */
    @Test
    public void testToString() {
        final byte[] some = new byte[] {2, 5, 0, 1 };
        final String expected = "byte[]{length=" + some.length + ", hash=" + Arrays.hashCode(some) + "}";

        assertEquals(expected, String.valueOf(ByteArraySizeHashPrinter.of(some)));
        assertNotNull(ByteArraySizeHashPrinter.orNull(some));
    }

}
