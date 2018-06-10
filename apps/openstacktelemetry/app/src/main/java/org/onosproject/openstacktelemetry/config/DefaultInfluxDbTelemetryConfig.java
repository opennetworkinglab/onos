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
import org.onosproject.openstacktelemetry.api.config.InfluxDbTelemetryConfig;
import org.onosproject.openstacktelemetry.api.config.TelemetryConfig;

import java.util.Map;
import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A configuration file contains InfluxDB telemetry parameters.
 */
public final class DefaultInfluxDbTelemetryConfig implements InfluxDbTelemetryConfig {

    private final String address;
    private final int port;
    private final String username;
    private final String password;
    private final String database;
    private final boolean enableBatch;
    private final Map<String, Object> configMap;

    private DefaultInfluxDbTelemetryConfig(String address, int port,
                                           String username, String password,
                                           String database, boolean enableBatch,
                                           Map<String, Object> configMap) {
        this.address = address;
        this.port = port;
        this.username = username;
        this.password = password;
        this.database = database;
        this.enableBatch = enableBatch;
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
    public String username() {
        return username;
    }

    @Override
    public String password() {
        return password;
    }

    @Override
    public String database() {
        return database;
    }

    @Override
    public boolean enableBatch() {
        return enableBatch;
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

        if (obj instanceof DefaultInfluxDbTelemetryConfig) {
            final DefaultInfluxDbTelemetryConfig other = (DefaultInfluxDbTelemetryConfig) obj;
            return Objects.equals(this.address, other.address) &&
                    Objects.equals(this.port, other.port) &&
                    Objects.equals(this.username, other.username) &&
                    Objects.equals(this.password, other.password) &&
                    Objects.equals(this.database, other.database) &&
                    Objects.equals(this.enableBatch, other.enableBatch) &&
                    Objects.equals(this.configMap, other.configMap);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(address, port, username, password, database, enableBatch, configMap);
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("address", address)
                .add("port", port)
                .add("username", username)
                .add("password", password)
                .add("database", database)
                .add("enableBatch", enableBatch)
                .add("configMap", configMap)
                .toString();
    }

    @Override
    public TelemetryConfig.Builder createBuilder() {
        return new DefaultBuilder();
    }

    /**
     * Builder class of DefaultInfluxDbTelemetryConfig.
     */
    public static final class DefaultBuilder implements Builder {

        private String address;
        private int port;
        private String username;
        private String password;
        private String database;
        private boolean enableBatch;
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
        public Builder withUsername(String username) {
            this.username = username;
            return this;
        }

        @Override
        public Builder withPassword(String password) {
            this.password = password;
            return this;
        }

        @Override
        public Builder withDatabase(String database) {
            this.database = database;
            return this;
        }

        @Override
        public Builder withEnableBatch(boolean enableBatch) {
            this.enableBatch = enableBatch;
            return this;
        }

        @Override
        public Builder withConfigMap(Map<String, Object> configMap) {
            this.configMap = configMap;
            return this;
        }

        @Override
        public InfluxDbTelemetryConfig build() {
            checkNotNull(address, "InfluxDB server address cannot be null");
            checkNotNull(username, "InfluxDB server username cannot be null");
            checkNotNull(password, "InfluxDB server password cannot be null");
            checkNotNull(database, "InfluxDB server database cannot be null");

            return new DefaultInfluxDbTelemetryConfig(address, port, username,
                    password, database, enableBatch, configMap);
        }
    }
}
