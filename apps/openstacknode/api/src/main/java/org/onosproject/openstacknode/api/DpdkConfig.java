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

import java.util.Collection;

/**
 * Representation of dpdk config information.
 */
public interface DpdkConfig {

    /**
     * List of valid data path types.
     */
    enum DatapathType {
        NORMAL,
        NETDEV
    }

    /**
     * Returns the data path type.
     *
     * @return data path type; normal or netdev
     */
    DatapathType datapathType();

    /**
     * Returns socket directory which dpdk port bound to.
     *
     * @return socket directory
     */
    String socketDir();

    /**
     * Returns a collection of dpdk interfaces.
     *
     * @return dpdk interfaces
     */
    Collection<DpdkInterface> dpdkIntfs();

    interface Builder {
        /**
         * Returns new dpdk config.
         *
         * @return dpdk config
         */
        DpdkConfig build();

        /**
         * Returns dpdk config builder with supplied datapath type.
         *
         * @param datapathType datapath type
         * @return dpdk config builder
         */
        Builder datapathType(DatapathType datapathType);

        /**
         * Returns dpdk config builder with supplied socket directory.
         *
         * @param socketDir socket directory
         * @return dpdk config builder
         */
        Builder socketDir(String socketDir);

        /**
         * Returns dpdk config builder with supplied dpdk interfaces.
         *
         * @param dpdkIntfs a collection of dpdk interfaces
         * @return dpdk config builder
         */
        Builder dpdkIntfs(Collection<DpdkInterface> dpdkIntfs);
    }
}
