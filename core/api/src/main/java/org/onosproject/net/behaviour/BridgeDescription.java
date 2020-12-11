/*
 * Copyright 2015-present Open Networking Foundation
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

import org.onosproject.net.Description;
import org.onosproject.net.DeviceId;

import java.util.List;
import java.util.Optional;

/**
 * The abstraction of a bridge. Bridge represents an Ethernet switch with no or
 * multiple OpenFlow controllers. Only OVSDB device provides bridge config behavior
 * now and the bridge description is based on OVSDB schema.
 */
public interface BridgeDescription extends Description {

    enum FailMode {
        /**
         * The bridge will not set up flows on its own when the controller
         * connection fails or no controllers are defined.
         */
        SECURE,
        /**
         * The bridge will take over responsibility of setting up flows.
         */
        STANDALONE
    }

    /**
     * Returns bridge name.
     *
     * @return bridge name
     */
    String name();

    /**
     * Returns OpenFlow controllers of the bridge.
     * If it's empty, then no OpenFlow controllers are used for the bridge.
     *
     * @return set of controllers
     */
    List<ControllerInfo> controllers();

    /**
     * Returns whether to use local controller as an OpenFlow controller of the
     * bridge if no controllers are specified.
     *
     * @return true to set local controller, false otherwise
     */
    boolean enableLocalController();

    /**
     * Returns fail mode of the bridge.
     * If it's not set, the default setting of the bridge is used.
     *
     * @return fail mode
     */
    Optional<FailMode> failMode();

    /**
     * Returns OpenFlow datapath ID of the bridge. Valid only if OpenFlow controller
     * is configured for the bridge.
     *
     * @return datapath id
     */
    Optional<String> datapathId();

    /**
     * Returns OVSDB datapath Type of the bridge.
     *
     * @return datapath type
     */
    Optional<String> datapathType();

    /**
     * Returns OpenFlow device ID. Valid only if OpenFlow controller is configured
     * for the bridge.
     *
     * @return device id
     */
    Optional<DeviceId> deviceId();

    /**
     * Returns in band control is enabled or not. If set to true, disable in-band
     * control on the bridge regardless of controller and manager settings.
     * If it's not set, the default setting of the bridge is used.
     *
     * @return true if in-band is disabled, false if in-band is enabled
     */
    Optional<Boolean> disableInBand();

    /**
     * Returns multicast snooping is enabled or not. If set to true, enable multicast
     * snooping on the bridge.
     * If it is not set, the multicast snooping is disabled.
     *
     * @return true if the multicast snooping is enabled, false otherwise
     */
    Optional<Boolean> mcastSnoopingEnable();

    /**
     * Returns list of Control Protocol Versions supported on device.
     * @return List of Control Protocol Versions enabled on bridge
     */
    Optional<List<ControlProtocolVersion>> controlProtocols();

    /**

    /**
     * Builder of bridge description entities.
     */
    interface Builder {

        /**
         * Returns bridge description builder with a given name.
         *
         * @param name bridge name
         * @return bridge description builder
         */
        Builder name(String name);

        /**
         * Returns bridge description builder with given controllers.
         *
         * @param controllers set of controllers
         * @return bridge description builder
         */
        Builder controllers(List<ControllerInfo> controllers);

        /**
         * Returns bridge description builder with local controller enabled.
         *
         * @return bridge description builder
         */
        Builder enableLocalController();

        /**
         * Returns bridge description builder with a given fail mode.
         *
         * @param failMode fail mode
         * @return bridge description builder
         */
        Builder failMode(FailMode failMode);

        /**
         * Returns bridge description builder with a given datapath ID.
         *
         * @param datapathId datapath id
         * @return bridge description builder
         */
        Builder datapathId(String datapathId);

        /**
         * Returns bridge description builder with a given datapath type.
         *
         * @param datapathType datapath type
         * @return bridge description builder
         */
        Builder datapathType(String datapathType);

        /**
         * Returns bridge description builder with given control protocol versions.
         * @param controlProtocols List of control protocol
         * @return bridge description builder
         */
        Builder controlProtocols(List<ControlProtocolVersion> controlProtocols);

        /**
         * Returns bridge description builder with in-band control disabled.
         *
         * @return bridge description builder
         */
        Builder disableInBand();

        /**
         * Returns bridge description builder with mcast snooping enabled.
         *
         * @return bridge description builder
         */
        Builder mcastSnoopingEnable();

        /**
         * Builds an immutable bridge description.
         *
         * @return bridge description
         */
        BridgeDescription build();
    }
}
