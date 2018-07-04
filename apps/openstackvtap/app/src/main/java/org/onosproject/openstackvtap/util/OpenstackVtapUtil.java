/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.openstackvtap.util;

import org.onlab.packet.IPv4;
import org.onosproject.net.group.DefaultGroupKey;
import org.onosproject.net.group.GroupKey;
import org.onosproject.openstackvtap.api.OpenstackVtap;

/**
 * An utility that used in openstack vTap app.
 */
public final class OpenstackVtapUtil {

    private static final String VTAP_GROUP_KEY = "VTAP_GROUP_KEY";

    /**
     * Prevents object instantiation from external.
     */
    private OpenstackVtapUtil() {
    }

    /**
     * Obtains vTap type from the given string.
     *
     * @param str string
     * @return vTap type
     */
    public static OpenstackVtap.Type getVtapTypeFromString(String str) {
        switch (str) {
            case "all":
                return OpenstackVtap.Type.VTAP_ALL;
            case "tx":
                return OpenstackVtap.Type.VTAP_TX;
            case "rx":
                return OpenstackVtap.Type.VTAP_RX;
            case "none":
                return OpenstackVtap.Type.VTAP_NONE;
            default:
                throw new IllegalArgumentException("Invalid vTap type string");
        }
    }

    /**
     * Obtains IP protocol type from the given string.
     *
     * @param str string
     * @return vTap type
     */
    public static byte getProtocolTypeFromString(String str) {
        switch (str) {
            case "tcp":
                return IPv4.PROTOCOL_TCP;
            case "udp":
                return IPv4.PROTOCOL_UDP;
            case "icmp":
                return IPv4.PROTOCOL_ICMP;
            default:
                return 0;
        }
    }

    public static GroupKey getGroupKey(int groupId) {
        return new DefaultGroupKey((VTAP_GROUP_KEY + Integer.toString(groupId)).getBytes());
    }
}
