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
package org.onosproject.openstacktelemetry.api;

import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.TpPort;
import org.onlab.packet.VlanId;
import org.onosproject.net.DeviceId;

/**
 * Flow info interface.
 */
public interface FlowInfo {

    /**
     * Obtains flow type.
     *
     * @return flow type
     */
    byte flowType();

    /**
     * Obtains device identifier.
     *
     * @return device identifier
     */
    DeviceId deviceId();

    /**
     * Obtains input interface identifier.
     *
     * @return input interface identifier
     */
    int inputInterfaceId();

    /**
     * Obtains output interface identifier.
     *
     * @return output interface identifier
     */
    int outputInterfaceId();

    /**
     * Obtains VLAN identifier.
     *
     * @return VLAN identifier
     */
    VlanId vlanId();

    /**
     * Obtains VxLAN identifier.
     *
     * @return VxLAN identifier
     */
    short vxlanId();

    /**
     * Obtains source IP address.
     *
     * @return source IP address
     */
    IpPrefix srcIp();

    /**
     * Obtains destination IP address.
     *
     * @return destination IP address
     */
    IpPrefix dstIp();

    /**
     * Obtains source port.
     *
     * @return source port
     */
    TpPort srcPort();

    /**
     * Obtains destination port.
     *
     * @return destination port
     */
    TpPort dstPort();

    /**
     * Obtains protocol type.
     *
     * @return protocol type
     */
    byte protocol();

    /**
     * Obtains source MAC address.
     *
     * @return source MAC address
     */
    MacAddress srcMac();

    /**
     * Obtains destination MAC address.
     *
     * @return destination MAC address
     */
    MacAddress dstMac();

    /**
     * Obtains flow level stats information.
     *
     * @return flow level stats information
     */
    StatsInfo statsInfo();

    /**
     * Checks the rough equality of old flow info and new flow info.
     * Note that we only test the equality for deviceId, srcIp, dstIP, srcPort,
     * dstPort, protocol
     *
     * @param flowInfo flow info object ot be compared
     * @return true if the two objects are identical, false otherwise
     */
    boolean roughEquals(FlowInfo flowInfo);

    /**
     * Make a key with IP Address and Transport Port information.
     *
     * @return unique flow info key in String
     */
    String uniqueFlowInfoKey();

    interface Builder {

        /**
         * Sets flow type.
         *
         * @param flowType flow type
         * @return builder instance
         */
        Builder withFlowType(byte flowType);

        /**
         * Sets device identifier.
         *
         * @param deviceId device identifier
         * @return builder instance
         */
        Builder withDeviceId(DeviceId deviceId);

        /**
         * Sets input interface identifier.
         *
         * @param inputInterfaceId input interface identifier
         * @return builder instance
         */
        Builder withInputInterfaceId(int inputInterfaceId);

        /**
         * Sets output interface identifier.
         *
         * @param outputInterfaceId output interface identifier
         * @return builder instance
         */
        Builder withOutputInterfaceId(int outputInterfaceId);

        /**
         * Sets VLAN identifier.
         *
         * @param vlanId VLAN identifier
         * @return builder instance
         */
        Builder withVlanId(VlanId vlanId);

        /**
         * Sets VxLAN identifier.
         *
         * @param vxlanId VxLAN identifier
         * @return builder instance
         */
        Builder withVxlanId(short vxlanId);

        /**
         * Sets source IP address.
         *
         * @param srcIp source IP address
         * @return builder instance
         */
        Builder withSrcIp(IpPrefix srcIp);

        /**
         * Sets destination IP address.
         *
         * @param dstIp destination IP address
         * @return builder instance
         */
        Builder withDstIp(IpPrefix dstIp);

        /**
         * Sets source port number.
         *
         * @param srcPort source port number
         * @return builder instance
         */
        Builder withSrcPort(TpPort srcPort);

        /**
         * Sets destination port number.
         *
         * @param dstPort destination port number
         * @return builder instance
         */
        Builder withDstPort(TpPort dstPort);

        /**
         * Sets protocol type.
         *
         * @param protocol protocol type
         * @return builder instance
         */
        Builder withProtocol(byte protocol);

        /**
         * Sets source MAC address.
         *
         * @param srcMac source MAC address
         * @return builder instance
         */
        Builder withSrcMac(MacAddress srcMac);

        /**
         * Sets destination MAC address.
         *
         * @param dstMac destination MAC address
         * @return builder instance
         */
        Builder withDstMac(MacAddress dstMac);

        /**
         * Sets flow level stats info.
         *
         * @param statsInfo flow level stats info
         * @return builder instance
         */
        Builder withStatsInfo(StatsInfo statsInfo);

        /**
         * Creates a FlowInfo instance.
         *
         * @return FlowInfo instance
         */
        FlowInfo build();
    }
}
