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
import org.onosproject.openstacktelemetry.api.config.PrometheusTelemetryConfig;
import org.onosproject.openstacktelemetry.api.config.TelemetryConfig;
import org.onosproject.openstacktelemetry.api.config.TelemetryConfigProperties;

import java.util.Map;
import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.openstacktelemetry.api.config.TelemetryConfig.ConfigType.PROMETHEUS;

/**
 * A configuration file contains Prometheus telemetry parameters.
 */
public final class DefaultPrometheusTelemetryConfig implements PrometheusTelemetryConfig {

    protected static final String ADDRESS = "address";
    protected static final String PORT = "port";
    protected static final String CONFIG_MAP = "configMap";

    private final String address;
    private final int port;
    private final Map<String, Object> configMap;

    private DefaultPrometheusTelemetryConfig(String address,
                                             int port,
                                             Map<String, Object> configMap) {
        this.address = address;
        this.port = port;
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

        if (obj instanceof DefaultPrometheusTelemetryConfig) {
            final DefaultPrometheusTelemetryConfig other = (DefaultPrometheusTelemetryConfig) obj;
            return Objects.equals(this.address, other.address) &&
                    Objects.equals(this.port, other.port) &&
                    Objects.equals(this.configMap, other.configMap);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(address, port, configMap);
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add(ADDRESS, address)
                .add(PORT, port)
                .add(CONFIG_MAP, configMap)
                .toString();
    }

    @Override
    public TelemetryConfigProperties.Builder createBuilder() {
        return new DefaultBuilder();
    }

    /**
     * Builds a prometheus telemetry config from telemetry config instance.
     *
     * @param config telemetry config
     * @return prometheus telemetry config
     */
    public static PrometheusTelemetryConfig fromTelemetryConfig(TelemetryConfig config) {
        if (config.type() != PROMETHEUS) {
            return null;
        }

        return new DefaultBuilder()
                .withAddress(config.getProperty(ADDRESS))
                .withPort(Integer.valueOf(config.getProperty(PORT)))
                .build();
    }

    /**
     * Builder class of DefaultPrometheusTelemetryConfig.
     */
    public static final class DefaultBuilder implements Builder {
        private String address;
        private int port;
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
        public Builder withConfigMap(Map<String, Object> configMap) {
            this.configMap = configMap;
            return this;
        }

        @Override
        public PrometheusTelemetryConfig build() {
            checkNotNull(address, "Prometheus exporter binding address cannot be null");
            return new DefaultPrometheusTelemetryConfig(address, port, configMap);
        }
    }
}