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
package org.onosproject.openstacktelemetry.config;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.onosproject.openstacktelemetry.api.config.GrpcTelemetryConfig;
import org.onosproject.openstacktelemetry.api.config.TelemetryConfig;

import java.util.Map;
import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A configuration file contains gRPC telemetry parameters.
 */
public final class DefaultGrpcTelemetryConfig implements GrpcTelemetryConfig {

    private final String address;
    private final int port;
    private final boolean usePlaintext;
    private final int maxInboundMsgSize;
    private final Map<String, Object> configMap;

    private DefaultGrpcTelemetryConfig(String address, int port,
                                       boolean usePlaintext, int maxInboundMsgSize,
                                       Map<String, Object> configMap) {
        this.address = address;
        this.port = port;
        this.usePlaintext = usePlaintext;
        this.maxInboundMsgSize = maxInboundMsgSize;
        this.configMap = configMap;
    }

    @Override
    public String address() {
        return address;
    }

    @Override
    public int port() {
        return port;
    }

    @Override
    public boolean usePlaintext() {
        return usePlaintext;
    }

    @Override
    public int maxInboundMsgSize() {
        return maxInboundMsgSize;
    }

    @Override
    public Map<String, Object> configMap() {
        if (configMap != null) {
            return ImmutableMap.copyOf(configMap);
        } else {
            return Maps.newConcurrentMap();
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof DefaultGrpcTelemetryConfig) {
            final DefaultGrpcTelemetryConfig other = (DefaultGrpcTelemetryConfig) obj;
            return Objects.equals(this.address, other.address) &&
                    Objects.equals(this.port, other.port) &&
                    Objects.equals(this.usePlaintext, other.usePlaintext) &&
                    Objects.equals(this.maxInboundMsgSize, other.maxInboundMsgSize) &&
                    Objects.equals(this.configMap, other.configMap);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(address, port, usePlaintext, maxInboundMsgSize, configMap);
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("address", address)
                .add("port", port)
                .add("usePlaintext", usePlaintext)
                .add("maxInboundMsgSize", maxInboundMsgSize)
                .add("configMap", configMap)
                .toString();
    }

    @Override
    public TelemetryConfig.Builder createBuilder() {
        return new DefaultBuilder();
    }

    /**
     * Builder class of DefaultKafkaTelemetryConfig.
     */
    public static final class DefaultBuilder implements Builder {

        private String address;
        private int port;
        private boolean usePlaintext;
        private int maxInboundMsgSize;
        private Map<String, Object> configMap;

        @Override
        public Builder withAddress(String address) {
            this.address = address;
            return this;
        }

        @Override
        public Builder withPort(int port) {
            this.port = port;
            return this;
        }

        @Override
        public Builder withUsePlaintext(boolean usePlaintext) {
            this.usePlaintext = usePlaintext;
            return this;
        }

        @Override
        public Builder withMaxInboundMsgSize(int maxInboundMsgSize) {
            this.maxInboundMsgSize = maxInboundMsgSize;
            return this;
        }

        @Override
        public Builder withConfigMap(Map<String, Object> configMap) {
            this.configMap = configMap;
            return this;
        }

        @Override
        public GrpcTelemetryConfig build() {
            checkNotNull(address, "gRPC server address cannot be null");

            return new DefaultGrpcTelemetryConfig(address, port, usePlaintext,
                    maxInboundMsgSize, configMap);
        }
    }
}
