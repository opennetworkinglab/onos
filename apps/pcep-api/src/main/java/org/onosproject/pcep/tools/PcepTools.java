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
package org.onosproject.pcep.tools;

import javax.xml.bind.DatatypeConverter;

/**
 * tools fo pcep app.
 */
public abstract class PcepTools {

    private PcepTools() {

    }

    /**
     * Converts decimal byte array to a hex string.
     *
     * @param byteArray byte array
     * @return a hex string
     */
    public static String toHexString(byte[] byteArray) {
        return DatatypeConverter.printHexBinary(byteArray);
    }

    /**
     * Converts a hex string to a decimal byte array.
     *
     * @param hexString a hex string
     * @return byte array
     */
    public static byte[] toByteArray(String hexString) {
        return DatatypeConverter.parseHexBinary(hexString);
    }

    /**
     * Converts a byte array to a decimal string.
     *
     * @param bytes a byte array
     * @return a decimal string
     */
    public static String toDecimalString(byte[] bytes) {
        String str = "";
        for (int i = 0; i < bytes.length; i++) {
            str += String.valueOf(bytes[i]);
        }
        return str;
    }

    /**
     * convert a string to the form of ip address.
     *
     * @param str a string
     * @return a string with ip format
     */
    public static String stringToIp(String str) {
        long ipInt = Long.parseLong(str, 16);
        return longToIp(ipInt);
    }

    /**
     * convert a long to ip format.
     *
     * @param ipLong a decimal number.
     * @return a ip format string
     */
    public static String longToIp(long ipLong) {
        StringBuilder sb = new StringBuilder();
        sb.append((ipLong >> 24) & 0xFF).append(".");
        sb.append((ipLong >> 16) & 0xFF).append(".");
        sb.append((ipLong >> 8) & 0xFF).append(".");
        sb.append(ipLong & 0xFF);
        return sb.toString();
    }

    /**
     * convert a string with ip format to a long.
     *
     * @param strIp a string with ip format
     * @return a long number
     */
    public static long ipToLong(String strIp) {
        long[] ip = new long[4];
        int position1 = strIp.indexOf(".");
        int position2 = strIp.indexOf(".", position1 + 1);
        int position3 = strIp.indexOf(".", position2 + 1);
        ip[0] = Long.parseLong(strIp.substring(0, position1));
        ip[1] = Long.parseLong(strIp.substring(position1 + 1, position2));
        ip[2] = Long.parseLong(strIp.substring(position2 + 1, position3));
        ip[3] = Long.parseLong(strIp.substring(position3 + 1));
        return (ip[0] << 24) + (ip[1] << 16) + (ip[2] << 8) + ip[3];
    }

    /**
     * get a integer value from a cut string.
     *
     * @param str a whole string
     * @param base cut the string from this index
     * @param offset the offset when execute the cut
     * @return a integer value
     */
    public static int tranferHexStringToInt(String str, int base, int offset) {
        return Integer.parseInt(str.substring(base, offset), 16);

    }
}
