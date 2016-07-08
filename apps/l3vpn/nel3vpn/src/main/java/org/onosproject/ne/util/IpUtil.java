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
package org.onosproject.ne.util;

import java.net.InetAddress;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * IpUtil ip util.
 */
public final class IpUtil {
    private static final Logger log = LoggerFactory.getLogger(IpUtil.class);

    private static final int NUMBER_2 = 2;
    private static final int NUMBER_4 = 4;
    private static final int NUMBER_8 = 8;
    private static final int NUMBER_16 = 16;
    private static final int NUMBER_24 = 24;
    private static final int NUMBER_31 = 31;
    private static final int NUMBER_32 = 32;

    private static final Long NUMBER_0XFF = (long) 0xff;
    private static final Long NUMBER_0XFF000000L = 0xff000000L;
    private static final Long NUMBER_0X00FF0000L = 0x00ff0000L;
    private static final Long NUMBER_0X0000FF00L = 0x0000ff00L;
    private static final Long NUMBER_0X000000FFL = 0x000000ffL;

    /**
     * Constructs a IpUtil object. Utility classes should not have a public or
     * default constructor, otherwise IDE will compile unsuccessfully. This
     * class should not be instantiated.
     */
    private IpUtil() {
    }

    /**
     * Convert number to mask.
     *
     * @param num
     * @return mask
     */
    public static String getMask(String num) {
        StringBuffer sb = new StringBuffer();
        int numInt = Integer.parseInt(num);
        if (numInt < 0 || numInt >= NUMBER_32) {
            return "255.255.255.255";
        }
        for (int i = 0; i < numInt; i++) {
            sb.append(1);
        }
        for (int i = numInt; i < NUMBER_32; i++) {
            sb.append(0);
        }
        String mask = sb.toString();
        sb = new StringBuffer();
        for (int i = 0; i < NUMBER_4; i++) {
            sb.append(Integer.parseInt(
                                       mask.substring(NUMBER_8 * i,
                                                      NUMBER_8 * (i + 1)),
                                       NUMBER_2)
                    + ".");
        }
        mask = sb.substring(0, sb.length() - 1);
        return mask;
    }

    /**
     * Convert Ip Address to Ip Prefix.
     *
     * @param ipAddress
     * @param netMask
     * @return Ip Prefix
     */
    public static String getIpPrefix(String ipAddress, String netMask) {
        if (0 == getNetMaskLen(netMask)) {
            throw new IllegalArgumentException("invalid netmask input");
        }

        InetAddress ip;
        InetAddress nm;
        try {
            ip = InetAddress.getByName(ipAddress);
            nm = InetAddress.getByName(netMask);
        } catch (Exception e) {
            throw new IllegalArgumentException("Ip or Mask is not valid");
        }

        byte[] ipBytes = ip.getAddress();
        byte[] nmBytes = nm.getAddress();
        if (ipBytes.length != nmBytes.length) {
            throw new IllegalArgumentException("Cannot compare IPv4 and IPv6 addresses");
        }

        if (ipBytes.length != NUMBER_4) {
            throw new IllegalArgumentException("only IPv4 is supported");
        }

        long ipLong = 0;
        long nmLong = 0;
        int j = 0;
        for (int i = ipBytes.length - 1; i >= 0; i--) {
            ipLong = ipLong | ((ipBytes[j] & NUMBER_0XFF) << (i * NUMBER_8));
            nmLong = nmLong | ((nmBytes[j] & NUMBER_0XFF) << (i * NUMBER_8));
            j++;
        }

        // Apply Network mask to get the IP prefix
        long ipPrefix = ipLong & nmLong;
        String ipStr = getIpString(ipPrefix);
        return ipStr;
    }

    /**
     * Get length of mask.
     *
     * @param netMask
     * @return length of mask
     */
    public static int getNetMaskLen(String netMask) {
        if (netMask == null) {
            return 0;
        }

        long nmLong = getLongIp(netMask);

        int maskLen;
        for (maskLen = 0; maskLen < NUMBER_32; maskLen++) {
            if (0 == (nmLong & (1 << (NUMBER_31 - maskLen)))) {
                break;
            }
        }

        if ((maskLen == 0) || (maskLen == NUMBER_32)) {
            return maskLen;
        }

        /* Net mask should only have trailing zeros */
        for (int i = maskLen + 1; i < NUMBER_32; i++) {
            if (1 == (nmLong & (1 << (NUMBER_31 - i)))) {
                throw new IllegalArgumentException("net Mask is not valid, it is not continous mask"
                        + netMask);
            }
        }
        return maskLen;
    }

    /**
     * Convert Ip from String to Long.
     *
     * @param ipString
     * @return ip
     */
    public static Long getLongIp(String ipString) {
        if (ipString == null) {
            throw new IllegalArgumentException("null input");
        }

        InetAddress ip;
        try {
            ip = InetAddress.getByName(ipString);
        } catch (Exception e) {
            throw new IllegalArgumentException("Ip is not valid" + ipString);
        }

        byte[] ipBytes = ip.getAddress();
        if (ipBytes.length != NUMBER_4) {
            throw new IllegalArgumentException("only IPv4 is supported"
                    + ipString);
        }

        long ipLong = 0;
        int j = 0;
        for (int i = ipBytes.length - 1; i >= 0; i--) {
            ipLong = ipLong | ((ipBytes[j] & NUMBER_0XFF) << (i * NUMBER_8));
            j++;
        }
        return ipLong;
    }

    /**
     * Get Ip from long to String.
     *
     * @param ipAddr
     * @return ip
     */
    public static String getIpString(long ipAddr) {
        String ipStr = String.format("%d.%d.%d.%d",
                                     (ipAddr & NUMBER_0XFF000000L) >> NUMBER_24,
                                     (ipAddr & NUMBER_0X00FF0000L) >> NUMBER_16,
                                     (ipAddr & NUMBER_0X0000FF00L) >> NUMBER_8,
                                     ipAddr & NUMBER_0X000000FFL);
        return ipStr;
    }
}
