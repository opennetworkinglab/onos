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

/**
 * Provide a set of byte operations.
 */
public final class ByteOperator {

    /**
     * Private constructor which prevents from external instantiation.
     */
    private ByteOperator() {

    }

    /**
     * Obtains a specific bit value from a byte with given index number.
     *
     * @param value byte value
     * @param index index number
     * @return a specific bit value from a byte
     */
    public static boolean getBit(byte value, int index) {
        // the length of byte should always be positive whiles less than 8
        if (index > 7 || index < 0) {
            return false;
        }

        return (value & (0x1 << index)) != 0;
    }

    /**
     * Converts boolean value into bit.
     *
     * @param value boolean value
     * @param bit   bit value
     * @return converted bit value
     */
    public static byte toBit(boolean value, int bit) {
        return (byte) (value ? bit : 0x00);
    }
}
