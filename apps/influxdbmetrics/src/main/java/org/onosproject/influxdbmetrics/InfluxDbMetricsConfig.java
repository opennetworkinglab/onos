/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onosproject.influxdbmetrics;

import org.onlab.util.Tools;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.core.CoreService;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.util.Dictionary;

import static org.onosproject.influxdbmetrics.OsgiPropertyConstants.ADDRESS;
import static org.onosproject.influxdbmetrics.OsgiPropertyConstants.ADDRESS_DEFAULT;
import static org.onosproject.influxdbmetrics.OsgiPropertyConstants.DATABASE;
import static org.onosproject.influxdbmetrics.OsgiPropertyConstants.DATABASE_DEFAULT;
import static org.onosproject.influxdbmetrics.OsgiPropertyConstants.PASSWORD;
import static org.onosproject.influxdbmetrics.OsgiPropertyConstants.PASSWORD_DEFAULT;
import static org.onosproject.influxdbmetrics.OsgiPropertyConstants.PORT;
import static org.onosproject.influxdbmetrics.OsgiPropertyConstants.PORT_DEFAULT;
import static org.onosproject.influxdbmetrics.OsgiPropertyConstants.USERNAME;
import static org.onosproject.influxdbmetrics.OsgiPropertyConstants.USERNAME_DEFAULT;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * A configuration service for InfluxDB metrics.
 * Both InfluxDbMetrics Reporter and Retriever rely on this configuration service.
 */
@Component(
    immediate = true,
    property = {
        ADDRESS + "=" + ADDRESS_DEFAULT,
        PORT + ":Integer=" + PORT_DEFAULT,
        DATABASE + "=" + DATABASE_DEFAULT,
        USERNAME + "=" + USERNAME_DEFAULT,
        PASSWORD + "=" + PASSWORD_DEFAULT
    }
)
public class InfluxDbMetricsConfig {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected InfluxDbMetricsReporter influxDbMetricsReporter;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected InfluxDbMetricsRetriever influxDbMetricsRetriever;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ComponentConfigService cfgService;

    /** IP address of influxDB server; default is localhost. */
    protected String address = ADDRESS_DEFAULT;

    /** Port number of influxDB server; default is 8086. */
    protected int port = PORT_DEFAULT;

    /** Database name of influxDB server; default is onos. */
    protected String database = DATABASE_DEFAULT;

    /** Username of influxDB server; default is onos. */
    protected String username = USERNAME_DEFAULT;

    /** Password of influxDB server; default is onos.password. */
    protected String password = PASSWORD_DEFAULT;

    @Activate
    public void activate() {
        cfgService.registerProperties(getClass());

        coreService.registerApplication("org.onosproject.influxdbmetrics");

        configReporter(influxDbMetricsReporter);
        configRetriever(influxDbMetricsRetriever);

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        cfgService.unregisterProperties(getClass(), false);

        log.info("Stopped");
    }

    @Modified
    public void modified(ComponentContext context) {
        readComponentConfiguration(context);

        configReporter(influxDbMetricsReporter);
        influxDbMetricsReporter.restartReport();

        configRetriever(influxDbMetricsRetriever);
    }

    private void configReporter(InfluxDbMetricsReporter reporter) {
        reporter.config(address, port, database, username, password);
    }

    private void configRetriever(InfluxDbMetricsRetriever retriever) {
        retriever.config(address, port, database, username, password);
    }

    /**
     * Extracts properties from the component configuration context.
     *
     * @param context the component context
     */
    private void readComponentConfiguration(ComponentContext context) {
        Dictionary<?, ?> properties = context.getProperties();

        String addressStr = Tools.get(properties, ADDRESS);
        address = addressStr != null ? addressStr : ADDRESS_DEFAULT;
        log.info("Configured. InfluxDB server address is {}", address);

        String databaseStr = Tools.get(properties, DATABASE);
        database = databaseStr != null ? databaseStr : DATABASE_DEFAULT;
        log.info("Configured. InfluxDB server database is {}", database);

        String usernameStr = Tools.get(properties, USERNAME);
        username = usernameStr != null ? usernameStr : USERNAME_DEFAULT;
        log.info("Configured. InfluxDB server username is {}", username);

        String passwordStr = Tools.get(properties, PASSWORD);
        password = passwordStr != null ? passwordStr : PASSWORD_DEFAULT;
        log.info("Configured. InfluxDB server password is {}", password);

        Integer portConfigured = Tools.getIntegerProperty(properties, PORT);
        if (portConfigured == null) {
            port = PORT_DEFAULT;
            log.info("InfluxDB port is not configured, default value is {}", port);
        } else {
            port = portConfigured;
            log.info("Configured. InfluxDB port is configured to {}", port);
        }
    }
}
