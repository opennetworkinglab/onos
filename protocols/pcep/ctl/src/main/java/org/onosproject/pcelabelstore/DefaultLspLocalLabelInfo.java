/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.pcelabelstore;

import com.google.common.base.MoreObjects;

import java.util.Objects;

import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.pcelabelstore.api.LspLocalLabelInfo;
import org.onosproject.incubator.net.resource.label.LabelResourceId;

/**
 * Local node details including IN and OUT labels as well as IN and OUT port details.
 */
public final class DefaultLspLocalLabelInfo implements LspLocalLabelInfo {

    private final DeviceId deviceId;
    private final LabelResourceId inLabelId;
    private final LabelResourceId outLabelId;
    private final PortNumber inPort;
    private final PortNumber outPort;

    /**
     * Initialization of member variables.
     *
     * @param deviceId device id
     * @param inLabelId in label id of a node
     * @param outLabelId out label id of a node
     * @param inPort input port
     * @param outPort remote port
     */
    private DefaultLspLocalLabelInfo(DeviceId deviceId,
                                     LabelResourceId inLabelId,
                                     LabelResourceId outLabelId,
                                     PortNumber inPort,
                                     PortNumber outPort) {
       this.deviceId = deviceId;
       this.inLabelId = inLabelId;
       this.outLabelId = outLabelId;
       this.inPort = inPort;
       this.outPort = outPort;
    }

    /**
     * Initialization of member variables for serialization.
     */
    private DefaultLspLocalLabelInfo() {
       this.deviceId = null;
       this.inLabelId = null;
       this.outLabelId = null;
       this.inPort = null;
       this.outPort = null;
    }

    @Override
    public DeviceId deviceId() {
       return deviceId;
    }

    @Override
    public LabelResourceId inLabelId() {
       return inLabelId;
    }

    @Override
    public LabelResourceId outLabelId() {
       return outLabelId;
    }

    @Override
    public PortNumber inPort() {
       return inPort;
    }

    @Override
    public PortNumber outPort() {
       return outPort;
    }

    @Override
    public int hashCode() {
        return Objects.hash(deviceId, inLabelId, outLabelId, inPort, outPort);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof LspLocalLabelInfo) {
            final DefaultLspLocalLabelInfo other = (DefaultLspLocalLabelInfo) obj;
            return Objects.equals(this.deviceId, other.deviceId) &&
                    Objects.equals(this.inLabelId, other.inLabelId) &&
                    Objects.equals(this.outLabelId, other.outLabelId) &&
                    Objects.equals(this.inPort, other.inPort) &&
                    Objects.equals(this.outPort, other.outPort);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .omitNullValues()
                .add("DeviceId", deviceId)
                .add("InLabelId", inLabelId)
                .add("OutLabelId", outLabelId)
                .add("InPort", inPort)
                .add("OutPort", outPort)
                .toString();
    }

    /**
     * Creates and returns a new builder instance that clones an existing object.
     *
     * @param deviceLabelInfo device label information
     * @return new builder
     */
    public static Builder builder(LspLocalLabelInfo deviceLabelInfo) {
        return new Builder(deviceLabelInfo);
    }

    /**
     * Creates and returns a new builder instance.
     *
     * @return new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder.
     */
    public static final class Builder implements LspLocalLabelInfo.Builder {
        private DeviceId deviceId;
        private LabelResourceId inLabelId;
        private LabelResourceId outLabelId;
        private PortNumber inPort;
        private PortNumber outPort;

        /**
         * Constructs default builder.
         */
        private Builder() {
        }

        /**
         * Initializes member variables with existing object.
         */
        private Builder(LspLocalLabelInfo deviceLabelInfo) {
            this.deviceId = deviceLabelInfo.deviceId();
            this.inLabelId = deviceLabelInfo.inLabelId();
            this.outLabelId = deviceLabelInfo.outLabelId();
            this.inPort = deviceLabelInfo.inPort();
            this.outPort = deviceLabelInfo.outPort();
        }

        @Override
        public Builder deviceId(DeviceId id) {
            this.deviceId = id;
            return this;
        }

        @Override
        public Builder inLabelId(LabelResourceId id) {
            this.inLabelId = id;
            return this;
        }

        @Override
        public Builder outLabelId(LabelResourceId id) {
            this.outLabelId = id;
            return this;
        }

        @Override
        public Builder inPort(PortNumber port) {
            this.inPort = port;
            return this;
        }

        @Override
        public Builder outPort(PortNumber port) {
            this.outPort = port;
            return this;
        }

        @Override
        public LspLocalLabelInfo build() {
            return new DefaultLspLocalLabelInfo(deviceId, inLabelId, outLabelId, inPort, outPort);
        }
    }
}
