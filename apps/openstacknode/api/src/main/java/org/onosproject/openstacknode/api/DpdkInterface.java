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
package org.onosproject.openstacknode.api;

/**
 * Representation of dpdk interface information.
 */
public interface DpdkInterface {
    Long DEFAULT_MTU_SIZE = 1500L;

    /**
     * Dpdk interface type.
     */
    enum Type {
        /**
         * A DPDK net device.
         */
        DPDK,
        /**
         * A DPDK_VHOST_USER net device.
         */
        DPDK_VHOST_USER,
        /**
         * A DPDK_VHOST_USER_CLIENT net device.
         */
        DPDK_VHOST_USER_CLIENT
    }


    /**
     * Returns the name of the device where the dpdk interface is.
     *
     * @return device name
     */
    String deviceName();

    /**
     * Returns the name of the dpdk interface.
     *
     * @return dpdk interface name
     */
    String intf();

    /**
     * Returns the dpdk device arguments of this dpdk port.
     * ex) "0000:85:00.1"
     *
     * @return pci address
     */
    String pciAddress();

    /**
     * Returns the dpdk interface type.
     *
     * @return type
     */
    Type type();

    /**
     * Returns the mtu size.
     *
     * @return mtu
     */
    Long mtu();

    /**
     * Builder of dpdk interface description entities.
     */
    interface Builder {
        /**
         * Returns new dpdk interface.
         *
         * @return dpdk interface description
         */
        DpdkInterface build();

        /**
         * Returns dpdk interface builder with supplied device name.
         *
         * @param  deviceName device name
         * @return dpdk interface
         */
        Builder deviceName(String deviceName);

        /**
         * Returns dpdk interface builder with supplied interface name.
         *
         * @param name interface name
         * @return dpdk interface
         */
        Builder intf(String name);

        /**
         * Returns dpdk interface builder with supplied pci address.
         *
         * @param pciAddress pci address
         * @return dpdk interface
         */
        Builder pciAddress(String pciAddress);

        /**
         * Returns dpdk interface builder with supplied type.
         *
         * @param type type
         * @return dpdk interface
         */
        Builder type(Type type);

        /**
         * Returns dpdk interface builder with supplied mtu size.
         *
         * @param mtu mtu
         * @return dpdk interface
         */
        Builder mtu(Long mtu);
    }
}
