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
package org.onosproject.vtn.util;

import org.onlab.packet.IpAddress;

/**
 * IpUtil utility class.
 */
public final class IpUtil {

    private IpUtil() {
    }

    /**
     * check source Ip and destination Ip in same Subnet.
     *
     * @param srcIp source Ip
     * @param dstIp destination
     * @param mask netmask length
     * @return boolean
     */
    public static boolean checkSameSegment(IpAddress srcIp, IpAddress dstIp,
                                           int mask) {
        String[] ips = srcIp.toString().split("\\.");
        int ipAddr = (Integer.parseInt(ips[0]) << 24)
                | (Integer.parseInt(ips[1]) << 16)
                | (Integer.parseInt(ips[2]) << 8)
                | Integer.parseInt(ips[3]);
        int netmask = 0xFFFFFFFF << (32 - mask);
        String[] cidrIps = dstIp.toString().split("\\.");
        int cidrIpAddr = (Integer.parseInt(cidrIps[0]) << 24)
                | (Integer.parseInt(cidrIps[1]) << 16)
                | (Integer.parseInt(cidrIps[2]) << 8)
                | Integer.parseInt(cidrIps[3]);

        return (ipAddr & netmask) == (cidrIpAddr & netmask);
    }
}
