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

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.onosproject.openstacktelemetry.api.config.InfluxDbTelemetryConfig;
import org.onosproject.openstacktelemetry.api.config.TelemetryConfig;
import org.onosproject.openstacktelemetry.api.config.TelemetryConfigProperties;

import java.util.Map;
import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A configuration file contains InfluxDB telemetry parameters.
 */
public final class DefaultInfluxDbTelemetryConfig implements InfluxDbTelemetryConfig {

    protected static final String ADDRESS = "address";
    protected static final String PORT = "port";
    protected static final String USERNAME = "username";
    protected static final String PASSWORD = "password";
    protected static final String DATABASE = "database";
    protected static final String MEASUREMENT = "measurement";
    protected static final String ENABLE_BATCH = "enableBatch";
    protected static final String CONFIG_MAP = "configMap";

    private final String address;
    private final int port;
    private final String username;
    private final String password;
    private final String database;
    private final String measurement;
    private final boolean enableBatch;
    private final Map<String, Object> configMap;

    private DefaultInfluxDbTelemetryConfig(String address, int port,
                                           String username, String password,
                                           String database, String measurement,
                                           boolean enableBatch,
                                           Map<String, Object> configMap) {
        this.address = address;
        this.port = port;
        this.username = username;
        this.password = password;
        this.database = database;
        this.measurement = measurement;
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
    public String measurement() {
        return measurement;
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
                    Objects.equals(this.measurement, other.measurement) &&
                    Objects.equals(this.enableBatch, other.enableBatch) &&
                    Objects.equals(this.configMap, other.configMap);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(address, port, username, password, database,
                measurement, enableBatch, configMap);
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add(ADDRESS, address)
                .add(PORT, port)
                .add(USERNAME, username)
                .add(PASSWORD, password)
                .add(DATABASE, database)
                .add(MEASUREMENT, measurement)
                .add(ENABLE_BATCH, enableBatch)
                .add(CONFIG_MAP, configMap)
                .toString();
    }

    @Override
    public TelemetryConfigProperties.Builder createBuilder() {
        return new DefaultBuilder();
    }

    /**
     * Builds an influxDB telemetry config from telemetry config instance.
     *
     * @param config telemetry config
     * @return influxDB telemetry config
     */
    public static InfluxDbTelemetryConfig fromTelemetryConfig(TelemetryConfig config) {
        if (config.type() != TelemetryConfig.ConfigType.INFLUXDB) {
            return null;
        }

        boolean enableBatch = Strings.isNullOrEmpty(config.getProperty(ENABLE_BATCH)) ? false :
                Boolean.valueOf(config.getProperty(ENABLE_BATCH));

        return new DefaultBuilder()
                .withAddress(config.getProperty(ADDRESS))
                .withPort(Integer.valueOf(config.getProperty(PORT)))
                .withUsername(config.getProperty(USERNAME))
                .withPassword(config.getProperty(PASSWORD))
                .withDatabase(config.getProperty(DATABASE))
                .withMeasurement(config.getProperty(MEASUREMENT))
                .withEnableBatch(enableBatch)
                .build();
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
        private String measurement;
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
        public Builder withMeasurement(String measurement) {
            this.measurement = measurement;
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

            return new DefaultInfluxDbTelemetryConfig(address, port, username,
                    password, database, measurement, enableBatch, configMap);
        }

    }
}