/*
 * Copyright 2016 Open Networking Laboratory
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

import com.codahale.metrics.MetricRegistry;
import com.izettle.metrics.influxdb.InfluxDbHttpSender;
import com.izettle.metrics.influxdb.InfluxDbReporter;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.metrics.MetricsService;
import org.onlab.util.Tools;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;

import java.util.Dictionary;
import java.util.concurrent.TimeUnit;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * A Metric reporter that reports all metrics value to influxDB server.
 */
@Component(immediate = true)
public class InfluxDbMetricsReporter {
    private final Logger log = getLogger(getClass());

    private static final int REPORT_PERIOD = 1;
    private static final TimeUnit REPORT_TIME_UNIT = TimeUnit.MINUTES;

    private static final String DEFAULT_PROTOCOL = "http";
    private static final String DEFAULT_ADDRESS = "localhost";
    private static final int DEFAULT_PORT = 8086;
    private static final String DEFAULT_METRIC_NAMES = "default";
    private static final String DEFAULT_DATABASE = "onos";
    private static final String DEFAULT_USERNAME = "onos";
    private static final String DEFAULT_PASSWORD = "onos.password";
    private static final String SEPARATOR = ":";
    private static final int DEFAULT_CONN_TIMEOUT = 1000;
    private static final int DEFAULT_READ_TIMEOUT = 1000;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MetricsService metricsService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ComponentConfigService cfgService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    @Property(name = "monitorAll", boolValue = true,
            label = "Enable to monitor all of metrics stored in metric registry " +
                    "default is true")
    protected boolean monitorAll = true;

    @Property(name = "metricNames", value = DEFAULT_METRIC_NAMES,
            label = "Names of metric to be monitored in third party monitoring " +
                    "server; default metric names are 'default'")
    protected String metricNames = DEFAULT_METRIC_NAMES;

    @Property(name = "address", value = DEFAULT_ADDRESS,
            label = "IP address of influxDB server; " +
                    "default is localhost")
    protected String address = DEFAULT_ADDRESS;

    @Property(name = "port", intValue = DEFAULT_PORT,
            label = "Port number of influxDB server; " +
                    "default is 8086")
    protected int port = DEFAULT_PORT;

    @Property(name = "database", value = DEFAULT_DATABASE,
            label = "Database name of influxDB server; " +
                    "default is onos")
    protected String database = DEFAULT_DATABASE;

    @Property(name = "username", value = DEFAULT_USERNAME,
            label = "Username of influxDB server; default is onos")
    protected String username = DEFAULT_USERNAME;

    @Property(name = "password", value = DEFAULT_PASSWORD,
            label = "Password of influxDB server; default is onos.password")
    protected String password = DEFAULT_PASSWORD;

    private ApplicationId appId;
    private InfluxDbReporter influxDbReporter;
    private InfluxDbHttpSender influxDbHttpSender;

    @Activate
    public void activate() {
        cfgService.registerProperties(getClass());
        appId = coreService.registerApplication("org.onosproject.influxdbmetrics");

        startReport();
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        cfgService.unregisterProperties(getClass(), false);

        stopReport();
        log.info("Stopped");
    }

    @Modified
    public void modified(ComponentContext context) {
        readComponentConfiguration(context);
        restartReport();
    }

    protected void startReport() {
        try {
            influxDbHttpSender = new InfluxDbHttpSender(DEFAULT_PROTOCOL, address,
                    port, database, username + SEPARATOR + password, REPORT_TIME_UNIT,
                    DEFAULT_CONN_TIMEOUT, DEFAULT_READ_TIMEOUT);
            MetricRegistry mr = metricsService.getMetricRegistry();
            influxDbReporter = InfluxDbReporter.forRegistry(addHostPrefix(filter(mr)))
                    .convertRatesTo(TimeUnit.SECONDS)
                    .convertDurationsTo(TimeUnit.MILLISECONDS)
                    .build(influxDbHttpSender);
            influxDbReporter.start(REPORT_PERIOD, REPORT_TIME_UNIT);
            log.info("Start to report metrics to influxDB.");
        } catch (Exception e) {
            log.error("Fail to connect to given influxDB server!");
        }
    }

    protected void stopReport() {
        influxDbReporter.stop();
        influxDbHttpSender = null;
        influxDbReporter = null;
        log.info("Stop reporting metrics to influxDB.");
    }

    protected void restartReport() {
        stopReport();
        startReport();
    }

    /**
     * Filters the metrics to only include a set of the given metrics.
     *
     * @param metricRegistry original metric registry
     * @return filtered metric registry
     */
    protected MetricRegistry filter(MetricRegistry metricRegistry) {
        if (!monitorAll) {
            final MetricRegistry filtered = new MetricRegistry();
            metricRegistry.getNames().stream().filter(name ->
                    containsName(name, metricNames)).forEach(name ->
                    filtered.register(name, metricRegistry.getMetrics().get(name)));
            return filtered;
        } else {
            return metricRegistry;
        }
    }

    /**
     * Appends node IP prefix to all performance metrics.
     *
     * @param metricRegistry original metric registry
     * @return prefix added metric registry
     */
    protected MetricRegistry addHostPrefix(MetricRegistry metricRegistry) {
        MetricRegistry moddedRegistry = new MetricRegistry();
        ControllerNode node = clusterService.getLocalNode();
        String prefix = node.id().id() + ".";
        metricRegistry.getNames().stream().forEach(name ->
                moddedRegistry.register(prefix + name, metricRegistry.getMetrics().get(name)));

        return moddedRegistry;
    }

    /**
     * Looks up whether the metric name contains the given prefix keywords.
     * Note that the keywords are separated with comma as delimiter.
     *
     * @param full the original metric name that to be compared with
     * @param prefixes the prefix keywords that are matched against with the metric name
     * @return boolean value that denotes whether the metric name starts with the given prefix
     */
    protected boolean containsName(String full, String prefixes) {
        String[] prefixArray = StringUtils.split(prefixes, ",");
        for (String prefix : prefixArray) {
            if (StringUtils.startsWith(full, StringUtils.trimToEmpty(prefix))) {
                return true;
            }
        }
        return false;
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

        String metricNameStr = Tools.get(properties, "metricNames");
        metricNames = metricNameStr != null ? metricNameStr : DEFAULT_METRIC_NAMES;
        log.info("Configured. Metric name is {}", metricNames);

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

        Boolean monitorAllEnabled = Tools.isPropertyEnabled(properties, "monitorAll");
        if (monitorAllEnabled == null) {
            log.info("Monitor all metrics is not configured, " +
                    "using current value of {}", monitorAll);
        } else {
            monitorAll = monitorAllEnabled;
            log.info("Configured. Monitor all metrics is {}",
                    monitorAll ? "enabled" : "disabled");
        }
    }
}
