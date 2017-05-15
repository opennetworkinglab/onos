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
package org.onosproject.ovsdb.controller;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.onosproject.net.behaviour.BridgeDescription;
import org.onosproject.net.behaviour.BridgeDescription.FailMode;
import org.onosproject.net.behaviour.ControllerInfo;

import static com.google.common.base.MoreObjects.toStringHelper;
import static org.onosproject.ovsdb.controller.OvsdbConstant.DATAPATH_ID;
import static org.onosproject.ovsdb.controller.OvsdbConstant.DISABLE_INBAND;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The class representing an OVSDB bridge.
 * This class is immutable.
 */
public final class OvsdbBridge {

    private final String name;

    /* OpenFlow properties */
    private final Optional<FailMode> failMode;
    private final List<ControllerInfo> controllers;

    /* Adds more properties */
    private final Optional<String> datapathType;

    /* other optional configs */
    private final Map<String, String> otherConfigs;

    /**
     * Default constructor.
     *
     * @param name name of the bridge
     * @param failMode openflow controller fail mode policy
     * @param controllers list of openflow controllers
     * @param datapathType ovs datapath_type
     * @param otherConfigs other configs
     */
    private OvsdbBridge(String name, Optional<FailMode> failMode,
                       List<ControllerInfo> controllers,
                       Optional<String> datapathType,
                       Map<String, String> otherConfigs) {
        this.name = checkNotNull(name);
        this.failMode = failMode;
        this.controllers = controllers;
        this.datapathType = datapathType;
        this.otherConfigs = otherConfigs;
    }

    /**
     * Gets the bridge name of bridge.
     *
     * @return the bridge name of bridge
     */
    public String name() {
        return name;
    }

    /**
     * Returns the controllers of the bridge.
     *
     * @return list of controllers
     */
    public List<ControllerInfo> controllers() {
        return controllers;
    }

    /**
     * Returns fail mode of the bridge.
     *
     * @return fail mode
     */
    public Optional<FailMode> failMode() {
        return failMode;
    }

    /**
     * Returns OVSDB datapath Type of the bridge.
     *
     * @return datapath type
     */
    public Optional<String> datapathType() {
        return datapathType;
    }

    /**
     * Returns other configurations of the bridge.
     *
     * @return map of configurations
     */
    public Map<String, String> otherConfigs() {
        return otherConfigs;
    }

    /**
     * Gets the datapathId of bridge.
     *
     * @return datapath id; null if not used
     */
    public Optional<String> datapathId() {
        return Optional.ofNullable(otherConfigs.get(DATAPATH_ID));
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof OvsdbBridge) {
            final OvsdbBridge otherOvsdbBridge = (OvsdbBridge) obj;
            return Objects.equals(this.name, otherOvsdbBridge.name);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("bridgeName", name)
                .add("failMode", failMode)
                .add("controllers", controllers)
                .add("datapathType", datapathType)
                .add("otherConfigs", otherConfigs)
                .toString();
    }

    /**
     * Returns a new builder instance.
     *
     * @return ovsdb bridge builder
     */
    public static OvsdbBridge.Builder builder() {
        return new Builder();
    }

    /**
     * Returns OVSDB bridge object with a given bridge description.
     *
     * @param bridgeDesc bridge description
     * @return ovsdb bridge
     */
    public static OvsdbBridge.Builder builder(BridgeDescription bridgeDesc) {
        return new Builder(bridgeDesc);
    }

    /**
     * Builder of OVSDB bridge entities.
     */
    public static final class Builder {
        private String name;
        private Optional<FailMode> failMode = Optional.empty();
        private List<ControllerInfo> controllers = Lists.newArrayList();
        private Optional<String> datapathType = Optional.empty();
        private Map<String, String> otherConfigs = Maps.newHashMap();

        private Builder() {
        }

        /**
         * Constructs OVSDB bridge builder with a given bridge description.
         *
         * @param bridgeDesc bridge description
         */
        private Builder(BridgeDescription bridgeDesc) {
            if (bridgeDesc.datapathId().isPresent()) {
                otherConfigs.put(DATAPATH_ID, bridgeDesc.datapathId().get());
            }
            if (bridgeDesc.disableInBand().isPresent()) {
                otherConfigs.put(DISABLE_INBAND,
                                 bridgeDesc.disableInBand().get().toString());
            }
            if (bridgeDesc.datapathType().isPresent()) {
                this.datapathType = bridgeDesc.datapathType();
            }
            this.name = bridgeDesc.name();
            this.failMode = bridgeDesc.failMode();
            this.controllers = Lists.newArrayList(bridgeDesc.controllers());
        }

        /**
         * Builds an immutable OVSDB bridge.
         *
         * @return ovsdb bridge
         */
        public OvsdbBridge build() {
            return new OvsdbBridge(name, failMode, controllers, datapathType, otherConfigs);
        }

        /**
         * Returns OVSDB bridge builder with a given name.
         *
         * @param name name of the bridge
         * @return ovsdb bridge builder
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Returns OVSDB bridge builder with a given fail mode.
         *
         * @param failMode fail mode
         * @return ovsdb bridge builder
         */
        public Builder failMode(FailMode failMode) {
            this.failMode = Optional.ofNullable(failMode);
            return this;
        }

        /**
         * Returns OVSDB bridge builder with given controllers.
         *
         * @param controllers list of controllers
         * @return ovsdb bridge builder
         */
        public Builder controllers(List<ControllerInfo> controllers) {
            this.controllers = Lists.newArrayList(controllers);
            return this;
        }

        /**
         * Returns OVSDB bridge builder with a given controller.
         *
         * @param controller controller
         * @return ovsdb bridge builder
         */
        public Builder controller(ControllerInfo controller) {
            this.controllers = Lists.newArrayList(controller);
            return this;
        }

        /**
         * Returns OVSDB bridge builder with given configs.
         *
         * @param otherConfigs other configs
         * @return ovsdb bridge builder
         */
        public Builder otherConfigs(Map<String, String> otherConfigs) {
            this.otherConfigs = Maps.newHashMap(otherConfigs);
            return this;
        }

        /**
         * Returns OVSDB bridge builder with a given datapath ID.
         *
         * @param datapathId datapath id
         * @return ovsdb bridge builder
         */
        public Builder datapathId(String datapathId) {
            otherConfigs.put(DATAPATH_ID, datapathId);
            return this;
        }

        /**
         * Returns OVSDB bridge builder with a given datapath type.
         *
         * @param datapathType datapath Type
         * @return ovsdb bridge builder
         */
        public Builder datapathType(String datapathType) {
            this.datapathType = Optional.ofNullable(datapathType);
            return this;
        }

        /**
         * Returns OVSDB bridge builder with a given disable in-band config.
         *
         * @return ovsdb bridge builder
         */
        public Builder disableInBand() {
            otherConfigs.put(DATAPATH_ID, Boolean.TRUE.toString());
            return this;
        }
    }
}
