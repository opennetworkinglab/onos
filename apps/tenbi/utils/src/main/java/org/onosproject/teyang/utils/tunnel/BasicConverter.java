/*
 * Copyright 2016 Open Networking Laboratory
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

package org.onosproject.teyang.utils.tunnel;

import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev20130715.ietfinettypes.IpAddress;

/**
 * Basic converter tools for ietf NBI &amp; SBI.
 */
public abstract class BasicConverter {

    //no instantiation
    private BasicConverter() {

    }

    /**
     * Converts a long value to IpAddress.
     *
     * @param value long value
     * @return ip address
     */
    static IpAddress longToIp(long value) {
        StringBuilder sb = new StringBuilder();
        sb.append((value >> 24) & 0xFF).append(".");
        sb.append((value >> 16) & 0xFF).append(".");
        sb.append((value >> 8) & 0xFF).append(".");
        sb.append(value & 0xFF);
        return IpAddress.fromString(sb.toString());
    }

    /**
     * Converts a long value to byte array.
     *
     * @param value long value
     * @return byte array
     */
    static byte[] longToByte(long value) {
        long temp = value;
        byte[] b = new byte[8];
        for (int i = 0; i < b.length; i++) {
            b[i] = new Long(temp & 0xff).byteValue();
            temp = temp >> 8;
        }
        return b;
    }

    /**
     * Converts a IP address to long value.
     *
     * @param ipAddress IP address
     * @return long value
     */
    static long ipToLong(IpAddress ipAddress) {
        long[] ip = new long[4];
        String strIp = ipAddress.toString();
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
     * Converts byte array to long value.
     *
     * @param bytes byte array
     * @return long value
     */
    static long bytesToLong(byte[] bytes) {
        return ((long) bytes[7] & 255L) << 56 |
                ((long) bytes[6] & 255L) << 48 |
                ((long) bytes[5] & 255L) << 40 |
                ((long) bytes[4] & 255L) << 32 |
                ((long) bytes[3] & 255L) << 24 |
                ((long) bytes[2] & 255L) << 16 |
                ((long) bytes[1] & 255L) << 8 |
                (long) bytes[0] & 255L;
    }
}
