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

import org.onlab.packet.IpAddress;
import org.onosproject.net.Annotated;
import org.onosproject.net.SparseAnnotations;

/**
 * Abstraction of an openstack vtap network.
 */
public interface OpenstackVtapNetwork extends Annotated {

    /**
     * List of valid openstack vtap tunneling modes.
     */
    enum Mode {
        /**
         * Indicates GRE tunneling.
         */
        GRE,

        /**
         * Indicates VXLAN tunneling.
         */
        VXLAN
    }

    /**
     * Returns the OpenstackVtapNetwork mode.
     *
     * @return mode of vtap tunneling
     */
    Mode mode();

    /**
     * Returns the network id of the vtap tunneling.
     *
     * @return networkId (e.g., gre key, vxlan vni)
     */
    Integer networkId();

    /**
     * Returns the vtap server IP address used for tunneling.
     *
     * @return ip address for vtap server
     */
    IpAddress serverIp();

    /**
     * Builder of new OpenstackVtapNetwork instance.
     */
    interface Builder {
        /**
         * Returns openstack vtap network builder with supplied OpenstackVtapNetwork mode.
         *
         * @param mode mode of vtap tunneling
         * @return openstack vtap network builder
         */
        Builder mode(Mode mode);

        /**
         * Returns openstack vtap network builder with supplied networkId for tunneling.
         *
         * @param networkId (e.g., gre key, vxlan vni)
         * @return openstack vtap network builder
         */
        Builder networkId(Integer networkId);

        /**
         * Returns openstack vtap network builder with supplied server IP address.
         *
         * @param serverIp ip address for vtap server
         * @return openstack vtap network builder
         */
        Builder serverIp(IpAddress serverIp);

        /**
         * Returns openstack vtap network builder with supplied annotations.
         *
         * @param annotations a set of annotations
         * @return openstack vtap network builder
         */
        Builder annotations(SparseAnnotations annotations);

        /**
         * Builds an immutable OpenstackVtapNetwork instance.
         *
         * @return OpenstackVtapNetwork instance
         */
        OpenstackVtapNetwork build();
    }
}
