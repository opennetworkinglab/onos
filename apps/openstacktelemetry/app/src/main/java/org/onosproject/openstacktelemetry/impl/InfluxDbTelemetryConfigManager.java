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
package org.onosproject.openstacktelemetry.impl;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.util.Tools;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.openstacktelemetry.api.InfluxDbTelemetryAdminService;
import org.onosproject.openstacktelemetry.api.InfluxDbTelemetryConfigService;
import org.onosproject.openstacktelemetry.api.config.TelemetryConfig;
import org.onosproject.openstacktelemetry.config.DefaultInfluxDbTelemetryConfig;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;

import static org.onosproject.openstacktelemetry.api.Constants.DEFAULT_DISABLE;
import static org.onosproject.openstacktelemetry.api.Constants.DEFAULT_INFLUXDB_DATABASE;
import static org.onosproject.openstacktelemetry.api.Constants.DEFAULT_INFLUXDB_ENABLE_BATCH;
import static org.onosproject.openstacktelemetry.api.Constants.DEFAULT_INFLUXDB_PASSWORD;
import static org.onosproject.openstacktelemetry.api.Constants.DEFAULT_INFLUXDB_SERVER_IP;
import static org.onosproject.openstacktelemetry.api.Constants.DEFAULT_INFLUXDB_SERVER_PORT;
import static org.onosproject.openstacktelemetry.api.Constants.DEFAULT_INFLUXDB_USERNAME;
import static org.onosproject.openstacktelemetry.util.OpenstackTelemetryUtil.getBooleanProperty;

/**
 * InfluxDB server configuration manager for publishing openstack telemetry.
 */
@Component(immediate = true)
@Service
public class InfluxDbTelemetryConfigManager implements InfluxDbTelemetryConfigService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String ENABLE_SERVICE = "enableService";
    private static final String ADDRESS = "address";
    private static final String PORT = "port";
    private static final String USERNAME = "username";
    private static final String PASSWORD = "password";
    private static final String DATABASE = "database";
    private static final String ENABLE_BATCH = "enableBatch";

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ComponentConfigService componentConfigService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected InfluxDbTelemetryAdminService influxDbTelemetryAdminService;

    @Property(name = ADDRESS, value = DEFAULT_INFLUXDB_SERVER_IP,
            label = "Default IP address to establish initial connection to InfluxDB server")
    protected String address = DEFAULT_INFLUXDB_SERVER_IP;

    @Property(name = PORT, intValue = DEFAULT_INFLUXDB_SERVER_PORT,
            label = "Default port number to establish initial connection to InfluxDB server")
    protected Integer port = DEFAULT_INFLUXDB_SERVER_PORT;

    @Property(name = USERNAME, value = DEFAULT_INFLUXDB_USERNAME,
            label = "Username used for authenticating against InfluxDB server")
    protected String username = DEFAULT_INFLUXDB_USERNAME;

    @Property(name = PASSWORD, value = DEFAULT_INFLUXDB_PASSWORD,
            label = "Password used for authenticating against InfluxDB server")
    protected String password = DEFAULT_INFLUXDB_PASSWORD;

    @Property(name = DATABASE, value = DEFAULT_INFLUXDB_DATABASE,
            label = "Database of InfluxDB server")
    protected String database = DEFAULT_INFLUXDB_DATABASE;

    @Property(name = ENABLE_BATCH, boolValue = DEFAULT_INFLUXDB_ENABLE_BATCH,
            label = "Flag value of enabling batch mode of InfluxDB server")
    protected Boolean enableBatch = DEFAULT_INFLUXDB_ENABLE_BATCH;

    @Property(name = ENABLE_SERVICE, boolValue = DEFAULT_DISABLE,
            label = "Specify the default behavior of telemetry service")
    protected Boolean enableService = DEFAULT_DISABLE;

    @Activate
    protected void activate(ComponentContext context) {
        componentConfigService.registerProperties(getClass());

        if (enableService) {
            influxDbTelemetryAdminService.start(getConfig());
        }
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        componentConfigService.unregisterProperties(getClass(), false);

        if (enableService) {
            influxDbTelemetryAdminService.stop();
        }
        log.info("Stopped");
    }

    @Modified
    private void modified(ComponentContext context) {
        readComponentConfiguration(context);

        if (enableService) {
            if (influxDbTelemetryAdminService.isRunning()) {
                influxDbTelemetryAdminService.restart(getConfig());
            } else {
                influxDbTelemetryAdminService.start(getConfig());
            }
        } else {
            if (influxDbTelemetryAdminService.isRunning()) {
                influxDbTelemetryAdminService.stop();
            }
        }
        log.info("Modified");
    }

    @Override
    public TelemetryConfig getConfig() {
        return new DefaultInfluxDbTelemetryConfig.DefaultBuilder()
                .withAddress(address)
                .withPort(port)
                .withUsername(username)
                .withPassword(password)
                .withDatabase(database)
                .withEnableBatch(enableBatch)
                .build();
    }

    /**
     * Extracts properties from the component configuration context.
     *
     * @param context the component context
     */
    private void readComponentConfiguration(ComponentContext context) {
        Dictionary<?, ?> properties = context.getProperties();

        String addressStr = Tools.get(properties, ADDRESS);
        address = addressStr != null ? addressStr : DEFAULT_INFLUXDB_SERVER_IP;
        log.info("Configured. InfluxDB server address is {}", address);

        Integer portConfigured = Tools.getIntegerProperty(properties, PORT);
        if (portConfigured == null) {
            port = DEFAULT_INFLUXDB_SERVER_PORT;
            log.info("InfluxDB server port is NOT configured, default value is {}", port);
        } else {
            port = portConfigured;
            log.info("Configured. InfluxDB server port is {}", port);
        }

        String usernameStr = Tools.get(properties, USERNAME);
        username = usernameStr != null ? usernameStr : DEFAULT_INFLUXDB_USERNAME;
        log.info("Configured. InfluxDB server username is {}", username);

        String passwordStr = Tools.get(properties, PASSWORD);
        password = passwordStr != null ? passwordStr : DEFAULT_INFLUXDB_PASSWORD;
        log.info("Configured. InfluxDB server password is {}", password);

        String databaseStr = Tools.get(properties, DATABASE);
        database = databaseStr != null ? databaseStr : DEFAULT_INFLUXDB_DATABASE;
        log.info("Configured. InfluxDB server database is {}", database);

        Boolean enableBatchConfigured = getBooleanProperty(properties, ENABLE_BATCH);
        if (enableBatchConfigured == null) {
            enableBatch = DEFAULT_INFLUXDB_ENABLE_BATCH;
            log.info("InfluxDB server enable batch flag is " +
                    "NOT configured, default value is {}", enableBatch);
        } else {
            enableBatch = enableBatchConfigured;
            log.info("Configured. InfluxDB server enable batch is {}", enableBatch);
        }

        Boolean enableServiceConfigured =
                getBooleanProperty(properties, ENABLE_SERVICE);
        if (enableServiceConfigured == null) {
            enableService = DEFAULT_DISABLE;
            log.info("InfluxDB service enable flag is NOT " +
                    "configured, default value is {}", enableService);
        } else {
            enableService = enableServiceConfigured;
            log.info("Configured. InfluxDB service enable flag is {}", enableService);
        }
    }
}
