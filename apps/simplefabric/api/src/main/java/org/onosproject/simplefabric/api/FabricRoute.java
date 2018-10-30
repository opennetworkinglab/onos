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
package org.onosproject.simplefabric.api;

import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onosproject.cluster.NodeId;

/**
 * Interface of fabric route.
 */
public interface FabricRoute {

    /**
     * Source of the route.
     */
    enum Source {
        /**
         * Route came from the iBGP route source.
         */
        BGP,

        /**
         * Route came from the FPM route source.
         */
        FPM,

        /**
         * Route can from the static route source.
         */
        STATIC,

        /**
         * Route source was not defined.
         */
        UNDEFINED
    }

    /**
     * Returns the route source.
     *
     * @return route source
     */
    Source source();

    /**
     * Returns the IP prefix of the route.
     *
     * @return IP prefix
     */
    IpPrefix prefix();

    /**
     * Returns the next hop IP address.
     *
     * @return next hop
     */
    IpAddress nextHop();

    /**
     * Returns the ONOS node the route was sourced from.
     *
     * @return ONOS node ID
     */
    NodeId sourceNode();

    /**
     * Builder of FabricRoute.
     */
    interface Builder {

        /**
         * Returns FabricRoute builder with supplied source.
         *
         * @param source source of route
         * @return FabricRoute instance builder
         */
        Builder source(Source source);

        /**
         * Returns FabricRoute builder with supplied IP prefix.
         *
         * @param prefix IP prefix
         * @return FabricRoute instance builder
         */
        Builder prefix(IpPrefix prefix);

        /**
         * Returns Fabric builder with supplied next hop.
         *
         * @param nextHop next hop
         * @return FabricRoute instance builder
         */
        Builder nextHop(IpAddress nextHop);

        /**
         * Returns Fabric builder with supplied source node identifier.
         *
         * @param sourceNode source node identifier
         * @return FabricRoute instance builder
         */
        Builder sourceNode(NodeId sourceNode);

        /**
         * Builds an immutable FabricRoute instance.
         *
         * @return FabricRoute instance
         */
        FabricRoute build();
    }
}
