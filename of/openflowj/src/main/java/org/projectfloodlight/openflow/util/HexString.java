/**
 *    Copyright (c) 2008 The Board of Trustees of The Leland Stanford Junior
 *    University
 *
 *    Licensed under the Apache License, Version 2.0 (the "License"); you may
 *    not use this file except in compliance with the License. You may obtain
 *    a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *    WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *    License for the specific language governing permissions and limitations
 *    under the License.
 **/

package org.projectfloodlight.openflow.util;

import org.projectfloodlight.openflow.types.U8;

public class HexString {
    /**
     * Convert a string of bytes to a ':' separated hex string
     *
     * @param bytes
     * @return "0f:ca:fe:de:ad:be:ef"
     */
    public static String toHexString(final byte[] bytes) {
        int i;
        String ret = "";
        String tmp;
        for (i = 0; i < bytes.length; i++) {
            if (i > 0)
                ret += ":";
            tmp = Integer.toHexString(U8.f(bytes[i]));
            if (tmp.length() == 1)
                ret += "0";
            ret += tmp;
        }
        return ret;
    }

    public static String toHexString(final long val, final int padTo) {
        char arr[] = Long.toHexString(val).toCharArray();
        String ret = "";
        // prepend the right number of leading zeros
        int i = 0;
        for (; i < (padTo * 2 - arr.length); i++) {
            ret += "0";
            if ((i % 2) != 0)
                ret += ":";
        }
        for (int j = 0; j < arr.length; j++) {
            ret += arr[j];
            if ((((i + j) % 2) != 0) && (j < (arr.length - 1)))
                ret += ":";
        }
        return ret;
    }

    public static String toHexString(final long val) {
        return toHexString(val, 8);
    }

    /**
     * Convert a string of hex values into a string of bytes
     *
     * @param values
     *            "0f:ca:fe:de:ad:be:ef"
     * @return [15, 5 ,2, 5, 17]
     * @throws NumberFormatException
     *             If the string can not be parsed
     */
    public static byte[] fromHexString(final String values) throws NumberFormatException {
        String[] octets = values.split(":");
        byte[] ret = new byte[octets.length];

        for (int i = 0; i < octets.length; i++) {
            if (octets[i].length() > 2)
                throw new NumberFormatException("Invalid octet length");
            ret[i] = Integer.valueOf(octets[i], 16).byteValue();
        }
        return ret;
    }

    public static long toLong(String value) throws NumberFormatException {
        String[] octets = value.split(":");
        if (octets.length > 8)
            throw new NumberFormatException("Input string is too big to fit in long: " + value);
        long l = 0;
        for (String octet: octets) {
            if (octet.length() > 2)
                throw new NumberFormatException("Each colon-separated byte component must consist of 1 or 2 hex digits: " + value);
            short s = Short.parseShort(octet, 16);
            l = (l << 8) + s;
        }
        return l;
    }
}
