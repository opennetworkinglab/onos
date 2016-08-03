/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.net.behaviour;

import org.onosproject.net.Annotated;
import org.onosproject.net.Description;

import com.google.common.annotations.Beta;
import org.onosproject.net.SparseAnnotations;

import java.util.Optional;

/**
 * Describes a tunnel interface.
 */
@Beta
public interface TunnelDescription extends Description, Annotated {

    /**
     * Tunnel technology type.
     */
    enum Type {
        /**
         * Signifies that this is a MPLS tunnel.
         */
        MPLS,
        /**
         * Signifies that this is a L2 tunnel.
         */
        VLAN,
        /**
         * Signifies that this is a DC L2 extension tunnel.
         */
        VXLAN,
        /**
         * Signifies that this is a L3 tunnel.
         */
        GRE,
        /**
         * Signifies that this is a L1 OTN tunnel.
         */
        ODUK,
        /**
         * Signifies that this is a L0 OCH tunnel.
         */
        OCH
    }

    /**
     * Returns the identifier of the device where the interface is.
     *
     * @return device identifier
     */
    Optional<String> deviceId();

    /**
     * Return the name of the tunnel interface.
     *
     * @return tunnel interface name
     */
    String ifaceName();

    /**
     * Returns the tunnel type.
     *
     * @return tunnel type
     */
    Type type();

    /**
     * Returns the local connection point.
     *
     * @return tunnel source ConnectionPoint
     */
    Optional<TunnelEndPoint> local();

    /**
     * Returns the remote connection point.
     *
     * @return tunnel destination
     */
    Optional<TunnelEndPoint> remote();

    /**
     * Returns the tunnel key.
     *
     * @return tunnel key
     */
    Optional<TunnelKey> key();

    /**
     * Returns the connection point source.
     *
     * @deprecated version 1.7.0 - Hummingbird; use local instead
     * @return tunnel source ConnectionPoint
     */
    @Deprecated
    TunnelEndPoint src();

    /**
     * Returns the connection point destination.
     *
     * @deprecated version 1.7.0 - Hummingbird; use remote instead
     * @return tunnel destination
     */
    @Deprecated
    TunnelEndPoint dst();

    /**
     * Return the name of a tunnel.
     *
     * @deprecated version 1.7.0 - Hummingbird; use ifaceName instead
     * @return Tunnel Name
     */
    @Deprecated
    TunnelName tunnelName();

    /**
     * Builder of tunnel interface description entities.
     */
    interface Builder {

        /**
         * Returns new tunnel interface description.
         *
         * @return tunnel description
         */
        TunnelDescription build();

        /**
         * Returns tunnel interface description biulder with supplied device ID.
         *
         * @param deviceId device identifier
         * @return tunnel description builder
         */
        Builder deviceId(String deviceId);

        /**
         * Returns tunnel interface description builder with a given interface name.
         *
         * @param name tunnel interface name
         * @return tunnel description builder
         */
        Builder ifaceName(String name);

        /**
         * Returns tunnel interface description builder with a given tunnel type.
         *
         * @param type tunnel type
         * @return tunnel description builder
         */
        Builder type(Type type);

        /**
         * Returns tunnel interface description builder with a given local
         * tunnel endpoint.
         *
         * @param endpoint tunnel endpoint
         * @return tunnel description builder
         */
        Builder local(TunnelEndPoint endpoint);

        /**
         * Returns tunnel interface description builder with a given remote
         * tunnel endpoint.
         *
         * @param endpoint tunnel endpoint
         * @return tunnel description builder
         */
        Builder remote(TunnelEndPoint endpoint);

        /**
         * Returns tunnel interface description builder with a tunnel key.
         *
         * @param tunnelKey tunnel key
         * @return tunnel description builder
         */
        Builder key(TunnelKey tunnelKey);

        /**
         * Returns tunnel interface descriptions builder with other configurations.
         *
         * @param configs configurations
         * @return tunnel description builder
         */
        Builder otherConfigs(SparseAnnotations configs);
    }
}
