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
package org.onosproject.openstackvtap.api;

import org.onlab.packet.IpPrefix;
import org.onlab.packet.TpPort;

/**
 * A vTAP criterion used for mirroring traffic.
 */
public interface OpenstackVtapCriterion {

    /**
     * Returns IP Prefix of Source VM.
     *
     * @return source IP prefix
     */
    IpPrefix srcIpPrefix();

    /**
     * Returns IP Prefix of Destination VM.
     *
     * @return destination IP prefix
     */
    IpPrefix dstIpPrefix();

    /**
     * Returns IP protocol.
     *
     * @return IP protocol
     */
    byte ipProtocol();

    /**
     * Returns source transport port.
     *
     * @return source transport port number
     */

    TpPort srcTpPort();

    /**
     * Returns destination transport port.
     *
     * @return destination transport port number
     */
    TpPort dstTpPort();

    /**
     * Builder of new openstack vTap criteria.
     */
    interface Builder {

        /**
         * Builds an immutable openstack vTap criterion instance.
         *
         * @return openstack vTap criterion
         */
        OpenstackVtapCriterion build();

        /**
         * Returns openstack vTap criterion builder with supplied srcIpPrefix.
         *
         * @param srcIpPrefix Source IP address
         * @return openstack vTap criterion builder
         */
        Builder srcIpPrefix(IpPrefix srcIpPrefix);

        /**
         * Returns openstack vTap criterion builder with supplied srcIpPrefix.
         *
         * @param dstIpPrefix Destination IP Prefix
         * @return openstack vTap criterion builder
         */
        Builder dstIpPrefix(IpPrefix dstIpPrefix);

        /**
         * Returns openstack vTap criterion builder with supplied ipProtocol.
         *
         * @param ipProtocol IP protocol number
         * @return openstack vTap criterion builder
         */
        Builder ipProtocol(byte ipProtocol);

        /**
         * Returns openstack vTap criterion builder with supplied srcTpPort.
         *
         * @param srcTpPort Source transport port number
         * @return openstack vTap criterion builder
         */
        Builder srcTpPort(TpPort srcTpPort);

        /**
         * Returns openstack vTap criterion builder with supplied dstTpPort.
         *
         * @param dstTpPort Destination transport port number
         * @return openstack vTap criterion builder
         */
        Builder dstTpPort(TpPort dstTpPort);
    }
}
