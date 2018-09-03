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
 * A vtap criterion used for mirroring traffic.
 */
public interface OpenstackVtapCriterion {

    /**
     * Returns IP prefix of source.
     *
     * @return source IP prefix of vtap criterion
     */
    IpPrefix srcIpPrefix();

    /**
     * Returns IP prefix of destination.
     *
     * @return destination IP prefix of vtap criterion
     */
    IpPrefix dstIpPrefix();

    /**
     * Returns IP protocol.
     *
     * @return IP protocol of vtap criterion
     */
    byte ipProtocol();

    /**
     * Returns source transport port.
     *
     * @return source transport port of vtap criterion
     */

    TpPort srcTpPort();

    /**
     * Returns destination transport port.
     *
     * @return destination transport port of vtap criterion
     */
    TpPort dstTpPort();

    /**
     * Builder of new OpenstackVtapCriterion instance.
     */
    interface Builder {
        /**
         * Returns openstack vtap criterion builder with supplied source IP prefix.
         *
         * @param srcIpPrefix Source IP prefix
         * @return openstack vtap criterion builder
         */
        Builder srcIpPrefix(IpPrefix srcIpPrefix);

        /**
         * Returns openstack vtap criterion builder with supplied destination IP prefix.
         *
         * @param dstIpPrefix Destination IP prefix
         * @return openstack vtap criterion builder
         */
        Builder dstIpPrefix(IpPrefix dstIpPrefix);

        /**
         * Returns openstack vtap criterion builder with supplied ipProtocol.
         *
         * @param ipProtocol IP protocol
         * @return openstack vtap criterion builder
         */
        Builder ipProtocol(byte ipProtocol);

        /**
         * Returns openstack vtap criterion builder with supplied source port.
         *
         * @param srcTpPort Source transport port
         * @return openstack vtap criterion builder
         */
        Builder srcTpPort(TpPort srcTpPort);

        /**
         * Returns openstack vtap criterion builder with supplied destination port.
         *
         * @param dstTpPort Destination transport port
         * @return openstack vtap criterion builder
         */
        Builder dstTpPort(TpPort dstTpPort);

        /**
         * Builds an immutable OpenstackVtapCriterion instance.
         *
         * @return OpenstackVtapCriterion criterion
         */
        OpenstackVtapCriterion build();
    }
}
