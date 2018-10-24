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

import org.onlab.util.Tools;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.openstacktelemetry.api.InfluxDbTelemetryAdminService;
import org.onosproject.openstacktelemetry.api.InfluxDbTelemetryConfigService;
import org.onosproject.openstacktelemetry.api.config.TelemetryConfig;
import org.onosproject.openstacktelemetry.config.DefaultInfluxDbTelemetryConfig;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;

import static org.onosproject.openstacktelemetry.impl.OsgiPropertyConstants.PROP_INFLUXDB_DATABASE;
import static org.onosproject.openstacktelemetry.impl.OsgiPropertyConstants.PROP_INFLUXDB_DATABASE_DEFAULT;
import static org.onosproject.openstacktelemetry.impl.OsgiPropertyConstants.PROP_INFLUXDB_ENABLE_BATCH;
import static org.onosproject.openstacktelemetry.impl.OsgiPropertyConstants.PROP_INFLUXDB_ENABLE_BATCH_DEFAULT;
import static org.onosproject.openstacktelemetry.impl.OsgiPropertyConstants.PROP_INFLUXDB_ENABLE_SERVICE;
import static org.onosproject.openstacktelemetry.impl.OsgiPropertyConstants.PROP_INFLUXDB_ENABLE_SERVICE_DEFAULT;
import static org.onosproject.openstacktelemetry.impl.OsgiPropertyConstants.PROP_INFLUXDB_MEASUREMENT;
import static org.onosproject.openstacktelemetry.impl.OsgiPropertyConstants.PROP_INFLUXDB_MEASUREMENT_DEFAULT;
import static org.onosproject.openstacktelemetry.impl.OsgiPropertyConstants.PROP_INFLUXDB_PASSWORD;
import static org.onosproject.openstacktelemetry.impl.OsgiPropertyConstants.PROP_INFLUXDB_PASSWORD_DEFAULT;
import static org.onosproject.openstacktelemetry.impl.OsgiPropertyConstants.PROP_INFLUXDB_SERVER_ADDRESS;
import static org.onosproject.openstacktelemetry.impl.OsgiPropertyConstants.PROP_INFLUXDB_SERVER_ADDRESS_DEFAULT;
import static org.onosproject.openstacktelemetry.impl.OsgiPropertyConstants.PROP_INFLUXDB_SERVER_PORT;
import static org.onosproject.openstacktelemetry.impl.OsgiPropertyConstants.PROP_INFLUXDB_SERVER_PORT_DEFAULT;
import static org.onosproject.openstacktelemetry.impl.OsgiPropertyConstants.PROP_INFLUXDB_USERNAME;
import static org.onosproject.openstacktelemetry.impl.OsgiPropertyConstants.PROP_INFLUXDB_USERNAME_DEFAULT;
import static org.onosproject.openstacktelemetry.util.OpenstackTelemetryUtil.getBooleanProperty;
import static org.onosproject.openstacktelemetry.util.OpenstackTelemetryUtil.initTelemetryService;

/**
 * InfluxDB server configuration manager for publishing openstack telemetry.
 */
@Component(
    immediate = true,
    service = InfluxDbTelemetryConfigService.class,
    property = {
        PROP_INFLUXDB_ENABLE_SERVICE + ":Boolean=" + PROP_INFLUXDB_ENABLE_SERVICE_DEFAULT,
        PROP_INFLUXDB_SERVER_ADDRESS + "=" + PROP_INFLUXDB_SERVER_ADDRESS_DEFAULT,
        PROP_INFLUXDB_SERVER_PORT + ":Integer=" + PROP_INFLUXDB_SERVER_PORT_DEFAULT,
        PROP_INFLUXDB_USERNAME + "=" + PROP_INFLUXDB_USERNAME_DEFAULT,
        PROP_INFLUXDB_PASSWORD + "=" + PROP_INFLUXDB_PASSWORD_DEFAULT,
        PROP_INFLUXDB_DATABASE + "=" + PROP_INFLUXDB_DATABASE_DEFAULT,
        PROP_INFLUXDB_MEASUREMENT + "=" + PROP_INFLUXDB_MEASUREMENT_DEFAULT,
        PROP_INFLUXDB_ENABLE_BATCH + ":Boolean=" + PROP_INFLUXDB_ENABLE_BATCH_DEFAULT
    }
)
public class InfluxDbTelemetryConfigManager implements InfluxDbTelemetryConfigService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ComponentConfigService componentConfigService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected InfluxDbTelemetryAdminService influxDbTelemetryAdminService;

    /** Default IP address to establish initial connection to InfluxDB server. */
    protected String address = PROP_INFLUXDB_SERVER_ADDRESS_DEFAULT;

    /** Default port number to establish initial connection to InfluxDB server. */
    protected Integer port = PROP_INFLUXDB_SERVER_PORT_DEFAULT;

    /** Username used for authenticating against InfluxDB server. */
    protected String username = PROP_INFLUXDB_USERNAME_DEFAULT;

    /** Password used for authenticating against InfluxDB server. */
    protected String password = PROP_INFLUXDB_PASSWORD_DEFAULT;

    /** Database of InfluxDB server. */
    protected String database = PROP_INFLUXDB_DATABASE_DEFAULT;

    /** Measurement of InfluxDB server. */
    protected String measurement = PROP_INFLUXDB_MEASUREMENT_DEFAULT;

    /** Flag value of enabling batch mode of InfluxDB server. */
    protected Boolean enableBatch = PROP_INFLUXDB_ENABLE_SERVICE_DEFAULT;

    /** Specify the default behavior of telemetry service. */
    protected Boolean enableService = PROP_INFLUXDB_ENABLE_SERVICE_DEFAULT;

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
        initTelemetryService(influxDbTelemetryAdminService, getConfig(), enableService);
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
                .withMeasurement(measurement)
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

        String addressStr = Tools.get(properties, PROP_INFLUXDB_SERVER_ADDRESS);
        address = addressStr != null ? addressStr : PROP_INFLUXDB_SERVER_ADDRESS_DEFAULT;
        log.info("Configured. InfluxDB server address is {}", address);

        Integer portConfigured = Tools.getIntegerProperty(properties, PROP_INFLUXDB_SERVER_PORT);
        if (portConfigured == null) {
            port = PROP_INFLUXDB_SERVER_PORT_DEFAULT;
            log.info("InfluxDB server port is NOT configured, default value is {}", port);
        } else {
            port = portConfigured;
            log.info("Configured. InfluxDB server port is {}", port);
        }

        String usernameStr = Tools.get(properties, PROP_INFLUXDB_USERNAME);
        username = usernameStr != null ? usernameStr : PROP_INFLUXDB_USERNAME_DEFAULT;
        log.info("Configured. InfluxDB server username is {}", username);

        String passwordStr = Tools.get(properties, PROP_INFLUXDB_PASSWORD);
        password = passwordStr != null ? passwordStr : PROP_INFLUXDB_PASSWORD_DEFAULT;
        log.info("Configured. InfluxDB server password is {}", password);

        String databaseStr = Tools.get(properties, PROP_INFLUXDB_DATABASE);
        database = databaseStr != null ? databaseStr : PROP_INFLUXDB_DATABASE_DEFAULT;
        log.info("Configured. InfluxDB server database is {}", database);

        String measurementStr = Tools.get(properties, PROP_INFLUXDB_MEASUREMENT);
        measurement = measurementStr != null ? measurementStr : PROP_INFLUXDB_MEASUREMENT_DEFAULT;
        log.info("Configured. InfluxDB server measurement is {}", measurement);

        Boolean enableBatchConfigured = getBooleanProperty(properties, PROP_INFLUXDB_ENABLE_BATCH);
        if (enableBatchConfigured == null) {
            enableBatch = PROP_INFLUXDB_ENABLE_BATCH_DEFAULT;
            log.info("InfluxDB server enable batch flag is " +
                    "NOT configured, default value is {}", enableBatch);
        } else {
            enableBatch = enableBatchConfigured;
            log.info("Configured. InfluxDB server enable batch is {}", enableBatch);
        }

        Boolean enableServiceConfigured =
                getBooleanProperty(properties, PROP_INFLUXDB_ENABLE_SERVICE);
        if (enableServiceConfigured == null) {
            enableService = PROP_INFLUXDB_ENABLE_SERVICE_DEFAULT;
            log.info("InfluxDB service enable flag is NOT " +
                    "configured, default value is {}", enableService);
        } else {
            enableService = enableServiceConfigured;
            log.info("Configured. InfluxDB service enable flag is {}", enableService);
        }
    }
}
