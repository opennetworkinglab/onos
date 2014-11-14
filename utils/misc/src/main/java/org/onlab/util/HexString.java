/*
 * Copyright 2014 Open Networking Laboratory
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

public final class HexString {

    private HexString() {

    }

    /**
     * Convert a string of bytes to a ':' separated hex string.
     *
     * @param bytes string of bytes to convert
     * @return "0f:ca:fe:de:ad:be:ef"
     */
    public static String toHexString(final byte[] bytes) {
        if (bytes == null) {
            return "(null)";
        }
        int i;
        StringBuilder ret = new StringBuilder(bytes.length * 3 - 1);
        String tmp;
        for (i = 0; i < bytes.length; i++) {
            if (i > 0) {
                ret.append(':');
            }
            tmp = Integer.toHexString((bytes[i] & 0xff));
            if (tmp.length() == 1) {
                ret.append('0');
            }
            ret.append(tmp);
        }
        return ret.toString();
    }

    public static String toHexString(final long val, final int padTo) {
        char[] arr = Long.toHexString(val).toCharArray();
        StringBuilder ret = new StringBuilder(padTo * 3 - 1);
        // prepend the right number of leading zeros
        int i = 0;
        for (; i < (padTo * 2 - arr.length); i++) {
            ret.append('0');
            if ((i % 2) != 0) {
                ret.append(':');
            }
        }
        for (int j = 0; j < arr.length; j++) {
            ret.append(arr[j]);
            if ((((i + j) % 2) != 0) && (j < (arr.length - 1))) {
                ret.append(':');
            }
        }
        return ret.toString();
    }

    public static String toHexString(final long val) {
        return toHexString(val, 8);
    }

    /**
     * Convert a string of hex values into a string of bytes.
     *
     * @param values
     *            "0f:ca:fe:de:ad:be:ef"
     * @return [15, 5 ,2, 5, 17]
     * @throws NumberFormatException
     *             If the string can not be parsed
     */
    public static byte[] fromHexString(final String values) {
        String[] octets = values.split(":");
        byte[] ret = new byte[octets.length];

        for (int i = 0; i < octets.length; i++) {
            if (octets[i].length() > 2) {
                throw new NumberFormatException("Invalid octet length");
            }
            ret[i] = Integer.valueOf(octets[i], 16).byteValue();
        }
        return ret;
    }

    public static long toLong(String value) {
        String[] octets = value.split(":");
        if (octets.length > 8) {
            throw new NumberFormatException("Input string is too big to fit in long: " + value);
        }
        long l = 0;
        for (String octet: octets) {
            if (octet.length() > 2) {
                throw new NumberFormatException(
                        "Each colon-separated byte component must consist of 1 or 2 hex digits: " + value);
            }
            short s = Short.parseShort(octet, 16);
            l = (l << 8) + s;
        }
        return l;
    }
}
