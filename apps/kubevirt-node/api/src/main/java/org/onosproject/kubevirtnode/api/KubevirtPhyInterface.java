/*
 * Copyright 2020-present Open Networking Foundation
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
package org.onosproject.kubevirtnode.api;

import org.onosproject.net.DeviceId;

/**
 * Representation of a KubeVirt physical interface used in KubeVirt networking service.
 */
public interface KubevirtPhyInterface {
    /**
     * Returns physical network name that this interface binds to.
     *
     * @return physical network name
     */
    String network();

    /**
     * Returns name of this physical interface.
     *
     * @return name of this physical interface
     */
    String intf();

    /**
     * Returns the device ID of the physical interface bridge at the node.
     *
     * @return device id
     */
    DeviceId physBridge();

    /**
     * Builder of kubevirt physical interface.
     */
    interface Builder {

        KubevirtPhyInterface build();

        /**
         * Returns physical network that this physical interface connects with.
         *
         * @param network network name
         * @return kubevirt physical interface builder
         */
        Builder network(String network);

        /**
         * Returns physical interface name.
         *
         * @param intf physical interface name of openstack node
         * @return kubevirt physical interface builder
         */
        Builder intf(String intf);

        /**
         * Returns kubevirt physical interface builder with supplied.
         *
         * @param physBridge device id of the physical bridge
         * @return kubevirt physical interface builder
         */
        Builder physBridge(DeviceId physBridge);
    }
}
