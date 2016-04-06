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
package org.onosproject.influxdbmetrics;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.util.Tools;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.core.CoreService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;

import java.util.Dictionary;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * A configuration service for InfluxDB metrics.
 * Both InfluxDbMetrics Reporter and Retriever rely on this configuration service.
 */
@Component(immediate = true)
public class InfluxDbMetricsConfig {

    private final Logger log = getLogger(getClass());

    private static final String DEFAULT_ADDRESS = "localhost";
    private static final int DEFAULT_PORT = 8086;
    private static final String DEFAULT_DATABASE = "onos";
    private static final String DEFAULT_USERNAME = "onos";
    private static final String DEFAULT_PASSWORD = "onos.password";

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected InfluxDbMetricsReporter influxDbMetricsReporter;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected InfluxDbMetricsRetriever influxDbMetricsRetriever;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ComponentConfigService cfgService;

    @Property(name = "address", value = DEFAULT_ADDRESS,
            label = "IP address of influxDB server; default is localhost")
    protected String address = DEFAULT_ADDRESS;

    @Property(name = "port", intValue = DEFAULT_PORT,
            label = "Port number of influxDB server; default is 8086")
    protected int port = DEFAULT_PORT;

    @Property(name = "database", value = DEFAULT_DATABASE,
            label = "Database name of influxDB server; default is onos")
    protected String database = DEFAULT_DATABASE;

    @Property(name = "username", value = DEFAULT_USERNAME,
            label = "Username of influxDB server; default is onos")
    protected String username = DEFAULT_USERNAME;

    @Property(name = "password", value = DEFAULT_PASSWORD,
            label = "Password of influxDB server; default is onos.password")
    protected String password = DEFAULT_PASSWORD;

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

        String addressStr = Tools.get(properties, "address");
        address = addressStr != null ? addressStr : DEFAULT_ADDRESS;
        log.info("Configured. InfluxDB server address is {}", address);

        String databaseStr = Tools.get(properties, "database");
        database = databaseStr != null ? databaseStr : DEFAULT_DATABASE;
        log.info("Configured. InfluxDB server database is {}", database);

        String usernameStr = Tools.get(properties, "username");
        username = usernameStr != null ? usernameStr : DEFAULT_USERNAME;
        log.info("Configured. InfluxDB server username is {}", username);

        String passwordStr = Tools.get(properties, "password");
        password = passwordStr != null ? passwordStr : DEFAULT_PASSWORD;
        log.info("Configured. InfluxDB server password is {}", password);

        Integer portConfigured = Tools.getIntegerProperty(properties, "port");
        if (portConfigured == null) {
            port = DEFAULT_PORT;
            log.info("InfluxDB port is not configured, default value is {}", port);
        } else {
            port = portConfigured;
            log.info("Configured. InfluxDB port is configured to {}", port);
        }
    }
}
