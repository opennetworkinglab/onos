/*
 * Copyright 2014-present Open Networking Foundation
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

    private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();

    private HexString() {
    }

    /**
     * Convert a byte array to a colon-separated hex string.
     *
     * @param bytes byte array to be converted
     * @return converted colon-separated hex string, e.g. "0f:ca:fe:de:ad:be:ef",
     *         or "(null)" if given byte array is null
     */
    public static String toHexString(final byte[] bytes) {
        return toHexString(bytes, ":");
    }

    /**
     * Convert a byte array to a hex string separated by given separator.
     *
     * @param bytes byte array to be converted
     * @param separator the string use to separate each byte
     * @return converted hex string, or "(null)" if given byte array is null
     */
    public static String toHexString(final byte[] bytes, String separator) {
        if (bytes == null) {
            return "(null)";
        }
        if (separator == null) {
            separator = "";
        }
        int slen = bytes.length * (2 + separator.length()) - separator.length();
        StringBuilder ret = new StringBuilder(slen);
        boolean addSeparator = !separator.isEmpty();
        for (int i = 0; i < bytes.length; i++) {
            if (i > 0 && addSeparator) {
                ret.append(separator);
            }
            ret.append(HEX_CHARS[(bytes[i] >> 4) & 0xF]);
            ret.append(HEX_CHARS[(bytes[i] & 0xF)]);
        }
        return ret.toString();
    }

    /**
     * Convert a long number to colon-separated hex string.
     * Prepend zero padding until given length.
     *
     * @param val long number to be converted
     * @param padTo prepend zeros until this length
     * @return converted colon-separated hex string, e.g. "0f:ca:fe:de:ad:be:ef"
     */
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

    /**
     * Convert a long number to colon-separated hex string.
     * Prepend zero padding until 8 bytes.
     *
     * @param val long number to be converted
     * @return converted colon-separated hex string, e.g. "0f:ca:fe:de:ad:be:ef"
     */
    public static String toHexString(final long val) {
        return toHexString(val, 8);
    }

    /**
     * Convert a colon-separated hex string to byte array.
     *
     * @param values colon-separated hex string to be converted,
     *               e.g. "0f:ca:fe:de:ad:be:ef"
     * @return converted byte array
     * @throws NumberFormatException if input hex string cannot be parsed
     */
    public static byte[] fromHexString(final String values) {
        return fromHexString(values, ":");
    }

    /**
     * Convert a hex-string with arbitrary separator to byte array.
     * If separator is the empty string or null, then no separator will be considered.
     *
     * @param values hex string to be converted
     * @param separator regex for separator
     * @return converted byte array
     * @throws NumberFormatException if input hex string cannot be parsed
     */
    public static byte[] fromHexString(final String values, String separator) {
        String regexSeparator;
        if (separator == null || separator.length() == 0) {
            regexSeparator = "(?<=\\G.{2})"; // Split string into several two character strings
        } else {
            regexSeparator = separator;
        }
        String[] octets = values.split(regexSeparator);
        byte[] ret = new byte[octets.length];

        for (int i = 0; i < octets.length; i++) {
            if (octets[i].length() > 2) {
                throw new NumberFormatException("Invalid octet length");
            }
            ret[i] = Integer.valueOf(octets[i], 16).byteValue();
        }
        return ret;
    }

    /**
     * Convert a colon-separated hex string to long.
     *
     * @param value colon-separated hex string to be converted,
     *              e.g. "00:0f:ca:fe:de:ad:be:ef"
     * @return converted long number
     * @throws NumberFormatException if input hex string cannot be parsed
     */
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
