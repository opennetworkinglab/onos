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

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.onosproject.net.DeviceId;
import org.onosproject.net.SparseAnnotations;

import java.util.List;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * The default implementation of bridge.
 */
public final class DefaultBridgeDescription implements BridgeDescription {

    private final String name;

    /* Optional OpenFlow configurations */
    private final List<ControllerInfo> controllers;
    private final boolean enableLocalController;
    private final Optional<FailMode> failMode;
    private final Optional<String> datapathId;
    private final Optional<Boolean> disableInBand;

    /* Adds more configurations */

    private DefaultBridgeDescription(String name,
                                     List<ControllerInfo> controllers,
                                     boolean enableLocalController,
                                     Optional<FailMode> failMode,
                                     Optional<String> datapathId,
                                     Optional<Boolean> disableInBand) {
        this.name = checkNotNull(name);
        this.controllers = controllers;
        this.enableLocalController = enableLocalController;
        this.failMode = failMode;
        this.datapathId = datapathId;
        this.disableInBand = disableInBand;
    }

    @Override
    public SparseAnnotations annotations() {
        return null;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public List<ControllerInfo> controllers() {
        return controllers;
    }

    @Override
    public boolean enableLocalController() {
        return enableLocalController;
    }

    @Override
    public Optional<FailMode> failMode() {
        return failMode;
    }

    @Override
    public Optional<String> datapathId() {
        return datapathId;
    }

    @Override
    public Optional<DeviceId> deviceId() {
        if (datapathId.isPresent()) {
            return Optional.of(DeviceId.deviceId("of:" + datapathId.get()));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Optional<Boolean> disableInBand() {
        return disableInBand;
    }

    /**
     * Creates and returns a new builder instance.
     *
     * @return new builder
     */
    public static BridgeDescription.Builder builder() {
        return new Builder();
    }

    public static final class Builder implements BridgeDescription.Builder {

        private String name;
        private List<ControllerInfo> controllers = Lists.newArrayList();
        private boolean enableLocalController = false;
        private Optional<FailMode> failMode = Optional.empty();
        private Optional<String> datapathId = Optional.empty();
        private Optional<Boolean> disableInBand = Optional.empty();

        private Builder() {
        }

        @Override
        public BridgeDescription build() {
            return new DefaultBridgeDescription(name, controllers,
                                                enableLocalController,
                                                failMode,
                                                datapathId,
                                                disableInBand);
        }

        @Override
        public Builder name(String name) {
            checkArgument(!Strings.isNullOrEmpty(name));
            this.name = name;
            return this;
        }

        @Override
        public Builder controllers(List<ControllerInfo> controllers) {
            if (controllers != null) {
                this.controllers = Lists.newArrayList(controllers);
            }
            return this;
        }

        @Override
        public Builder enableLocalController() {
            this.enableLocalController = true;
            return this;
        }

        @Override
        public Builder failMode(FailMode failMode) {
            this.failMode = Optional.ofNullable(failMode);
            return this;
        }

        @Override
        public Builder datapathId(String datapathId) {
            this.datapathId = Optional.ofNullable(datapathId);
            return this;
        }

        @Override
        public Builder disableInBand() {
            this.disableInBand = Optional.of(Boolean.TRUE);
            return this;
        }
    }
}
