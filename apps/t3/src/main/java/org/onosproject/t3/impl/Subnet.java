/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.t3.impl;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Utility class to test if an Ip is in a given subnet.
 */
public class Subnet {
    private final int bytesSubnetCount;
    private final BigInteger bigMask;
    private final BigInteger bigSubnetMasked;

    /**
     * Constructor for use via format "192.168.0.0/24" or "2001:db8:85a3:880:0:0:0:0/57".
     * @param subnetAddress the address
     * @param bits the mask
     */
    public Subnet(InetAddress subnetAddress, int bits) {
        bytesSubnetCount = subnetAddress.getAddress().length; // 4 or 16
        bigMask = BigInteger.valueOf(-1).shiftLeft(bytesSubnetCount * 8 - bits); // mask = -1 << 32 - bits
        bigSubnetMasked = new BigInteger(subnetAddress.getAddress()).and(bigMask);
    }

    /**
     * Constructor for use via format "192.168.0.0/255.255.255.0" or single address.
     * @param subnetAddress the address
     * @param mask the mask
     */
    public Subnet(InetAddress subnetAddress, InetAddress mask) {
        bytesSubnetCount = subnetAddress.getAddress().length;
        // no mask given case is handled here.
        bigMask = null == mask ? BigInteger.valueOf(-1) : new BigInteger(mask.getAddress());
        bigSubnetMasked = new BigInteger(subnetAddress.getAddress()).and(bigMask);
    }

    /**
     * Subnet factory method.
     *
     * @param subnetMask format: "192.168.0.0/24" or "192.168.0.0/255.255.255.0"
     *                   or single address or "2001:db8:85a3:880:0:0:0:0/57"
     * @return a new instance
     * @throws UnknownHostException thrown if unsupported subnet mask.
     */
    public static Subnet createInstance(String subnetMask)
            throws UnknownHostException {
        final String[] stringArr = subnetMask.split("/");
        if (2 > stringArr.length) {
            return new Subnet(InetAddress.getByName(stringArr[0]), (InetAddress) null);
        } else if (stringArr[1].contains(".") || stringArr[1].contains(":")) {
            return new Subnet(InetAddress.getByName(stringArr[0]), InetAddress.getByName(stringArr[1]));
        } else {
            return new Subnet(InetAddress.getByName(stringArr[0]), Integer.parseInt(stringArr[1]));
        }
    }

    /**
     * Tests if the address is in the given subnet.
     * @param address the address to test.
     * @return true if inside the subnet
     */
    public boolean isInSubnet(InetAddress address) {
        byte[] bytesAddress = address.getAddress();
        if (this.bytesSubnetCount != bytesAddress.length) {
            return false;
        }
        BigInteger bigAddress = new BigInteger(bytesAddress);
        return bigAddress.and(this.bigMask).equals(this.bigSubnetMasked);
    }

    @Override
    public final boolean equals(Object obj) {
        if (!(obj instanceof Subnet)) {
            return false;
        }
        final Subnet other = (Subnet) obj;
        return bigSubnetMasked.equals(other.bigSubnetMasked) &&
                bigMask.equals(other.bigMask) &&
                bytesSubnetCount == other.bytesSubnetCount;
    }

    @Override
    public final int hashCode() {
        return bytesSubnetCount;
    }

    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder();
        bigInteger2IpString(buf, bigSubnetMasked, bytesSubnetCount);
        buf.append('/');
        bigInteger2IpString(buf, bigMask, bytesSubnetCount);
        return buf.toString();
    }

    private void bigInteger2IpString(StringBuilder buf, BigInteger bigInteger, int displayBytes) {
        boolean isIPv4 = 4 == displayBytes;
        byte[] bytes = bigInteger.toByteArray();
        int diffLen = displayBytes - bytes.length;
        byte fillByte = 0 > (int) bytes[0] ? (byte) 0xFF : (byte) 0x00;

        int integer;
        for (int i = 0; i < displayBytes; i++) {
            if (0 < i && !isIPv4 && i % 2 == 0) {
                buf.append(':');
            } else if (0 < i && isIPv4) {
                buf.append('.');
            }
            integer = 0xFF & (i < diffLen ? fillByte : bytes[i - diffLen]);
            if (!isIPv4 && 0x10 > integer) {
                buf.append('0');
            }
            buf.append(isIPv4 ? integer : Integer.toHexString(integer));
        }
    }
}
