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
import org.onlab.packet.TpPort;

/**
 * Flow Rule Interface for Statistics.
 */
public interface StatsFlowRule {
    /**
     * Returns IP Prefix of Source VM.
     *
     * @return srcIpPrefix
     */
    IpPrefix srcIpPrefix();

    /**
     * Returns IP Prefix of Destination VM.
     *
     * @return dstIpPrefix
     */
    IpPrefix dstIpPrefix();

    /**
     * Returns IP protocol.
     *
     * @return ipProtocol
     */
    byte ipProtocol();

    /**
     * Returns source transport port.
     *
     * @return srcTpPort
     */

    TpPort srcTpPort();

    /**
     * Returns destination transport port.
     *
     * @return dstTpPort
     */
    TpPort dstTpPort();

    /**
     * Builder of new flow rule entities.
     */
    interface Builder {

        /**
         * Builds an immutable openstack flow rule instance.
         *
         * @return openstack flow rule instance
         */
        StatsFlowRule build();

        /**
         * Returns openstack flow rule builder with supplied srcIpPrefix.
         *
         * @param srcIpPrefix Source IP address
         * @return openstack flow rule builder
         */
        Builder srcIpPrefix(IpPrefix srcIpPrefix);

        /**
         * Returns openstack flow rule builder with supplied srcIpPrefix.
         *
         * @param dstIpPrefix Destination IP Prefix
         * @return openstack flow rule builder
         */
        Builder dstIpPrefix(IpPrefix dstIpPrefix);

        /**
         * Returns openstack flow rule builder with supplied ipProtocol.
         *
         * @param ipProtocol IP protocol number
         * @return openstack flow rule builder
         */
        Builder ipProtocol(byte ipProtocol);

        /**
         * Returns openstack flow rule builder with supplied srcTpPort.
         *
         * @param srcTpPort Source transport port number
         * @return openstack flow rule builder
         */
        Builder srcTpPort(TpPort srcTpPort);

        /**
         * Returns openstack flow rule builder with supplied dstTpPort.
         *
         * @param dstTpPort Destination transport port number
         * @return openstack flow rule builder
         */
        Builder dstTpPort(TpPort dstTpPort);
    }
}
