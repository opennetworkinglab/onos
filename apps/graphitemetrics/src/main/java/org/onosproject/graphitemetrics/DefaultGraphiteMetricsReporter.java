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
package org.onosproject.graphitemetrics;


import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.graphite.Graphite;
import com.codahale.metrics.graphite.GraphiteReporter;
import org.apache.commons.lang.StringUtils;
import org.onlab.metrics.MetricsService;
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

import java.net.InetSocketAddress;
import java.util.Dictionary;
import java.util.concurrent.TimeUnit;

import static org.onosproject.graphitemetrics.OsgiPropertyConstants.ADDRESS;
import static org.onosproject.graphitemetrics.OsgiPropertyConstants.ADDRESS_DEFAULT;
import static org.onosproject.graphitemetrics.OsgiPropertyConstants.METRIC_NAMES;
import static org.onosproject.graphitemetrics.OsgiPropertyConstants.METRIC_NAMES_DEFAULT;
import static org.onosproject.graphitemetrics.OsgiPropertyConstants.METRIC_NAME_PREFIX;
import static org.onosproject.graphitemetrics.OsgiPropertyConstants.METRIC_NAME_PREFIX_DEFAULT;
import static org.onosproject.graphitemetrics.OsgiPropertyConstants.MONITOR_ALL;
import static org.onosproject.graphitemetrics.OsgiPropertyConstants.MONITOR_ALL_DEFAULT;
import static org.onosproject.graphitemetrics.OsgiPropertyConstants.PORT;
import static org.onosproject.graphitemetrics.OsgiPropertyConstants.PORT_DEFAULT;
import static org.onosproject.graphitemetrics.OsgiPropertyConstants.REPORT_PERIOD;
import static org.onosproject.graphitemetrics.OsgiPropertyConstants.REPORT_PERIOD_DEFAULT;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * A metric report that reports all metrics value to graphite monitoring server.
 */
@Component(
    immediate = true,
    property = {
        MONITOR_ALL + ":Boolean=" + MONITOR_ALL_DEFAULT,
        METRIC_NAMES + "=" + METRIC_NAMES_DEFAULT,
        ADDRESS + "=" + ADDRESS_DEFAULT,
        PORT + ":Integer=" + PORT_DEFAULT,
        REPORT_PERIOD + ":Integer=" + REPORT_PERIOD_DEFAULT,
        METRIC_NAME_PREFIX + "=" + METRIC_NAME_PREFIX_DEFAULT
    }
)
public class DefaultGraphiteMetricsReporter implements GraphiteMetricsReporter {

    private final Logger log = getLogger(getClass());

    private static final TimeUnit REPORT_TIME_UNIT = TimeUnit.MINUTES;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected MetricsService metricsService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ComponentConfigService cfgService;

    /** Enable to monitor all of metrics stored in metric registry default is true. */
    protected boolean monitorAll = MONITOR_ALL_DEFAULT;

    /** Names of metric to be monitored; default metric names are 'default'. */
    protected String metricNames = METRIC_NAMES_DEFAULT;

    /** IP address of graphite monitoring server; default is localhost. */
    protected String address = ADDRESS_DEFAULT;

    /** Port number of graphite monitoring server; default is 2003. */
    protected int port = PORT_DEFAULT;

    /** Reporting period of graphite monitoring server; default is 1. */
    protected int reportPeriod = REPORT_PERIOD_DEFAULT;

    /** Prefix of metric name for graphite back-end server; default is 'onos'. */
    protected String metricNamePrefix = METRIC_NAME_PREFIX_DEFAULT;

    private Graphite graphite;
    private GraphiteReporter graphiteReporter;

    @Activate
    public void activate() {
        cfgService.registerProperties(getClass());
        coreService.registerApplication("org.onosproject.graphitemetrics");
        metricsService.registerReporter(this);

        startReport();
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        cfgService.unregisterProperties(getClass(), false);

        stopReport();
        metricsService.unregisterReporter(this);

        log.info("Stopped");
    }

    @Modified
    public void modified(ComponentContext context) {
        readComponentConfiguration(context);

        // Restarts reporting
        stopReport();
        startReport();
    }

    public void startReport() {
        configGraphite();
        graphiteReporter = buildReporter(graphite);

        try {
            graphiteReporter.start(reportPeriod, REPORT_TIME_UNIT);
        } catch (Exception e) {
            log.error("Errors during reporting to graphite, msg: {}" + e.getMessage());
        }

        log.info("Start to report metrics to graphite server.");
    }

    public void stopReport() {
        graphiteReporter.stop();
        graphite = null;
        graphiteReporter = null;
        log.info("Stop reporting metrics to graphite server.");
    }

    @Override
    public void restartReport() {
        stopReport();
        startReport();
    }

    @Override
    public void notifyMetricsChange() {
        graphiteReporter.stop();
        graphiteReporter = buildReporter(graphite);

        try {
            graphiteReporter.start(reportPeriod, REPORT_TIME_UNIT);
        } catch (Exception e) {
            log.error("Errors during reporting to graphite, msg: {}" + e.getMessage());
        }

        log.info("Metric registry has been changed, apply changes.");
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
     * Looks up whether the metric name contains the given prefix keywords.
     * Note that the keywords are separated with comma as delimiter
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

        Boolean newMonitorAll = Tools.isPropertyEnabled(properties, MONITOR_ALL);
        if (newMonitorAll == null) {
            log.info("Monitor all metrics is not configured, " +
                    "using current value of {}", monitorAll);
        } else {
            monitorAll = newMonitorAll;
            log.info("Configured. Monitor all metrics is {}, ",
                    monitorAll ? "enabled" : "disabled");
        }

        String newMetricNames = Tools.get(properties, METRIC_NAMES);
        metricNames = newMetricNames != null ? newMetricNames : METRIC_NAMES_DEFAULT;
        log.info("Configured. Metric name is {}", metricNames);

        String newAddress = Tools.get(properties, ADDRESS);
        address = newAddress != null ? newAddress : ADDRESS_DEFAULT;
        log.info("Configured. Graphite monitoring server address is {}", address);

        Integer newPort = Tools.getIntegerProperty(properties, PORT);
        if (newPort == null) {
            port = PORT_DEFAULT;
            log.info("Graphite port is not configured, default value is {}", port);
        } else {
            port = newPort;
            log.info("Configured. Graphite port is configured to {}", port);
        }

        Integer newReportPeriod = Tools.getIntegerProperty(properties, REPORT_PERIOD);
        if (newReportPeriod == null) {
            reportPeriod = REPORT_PERIOD_DEFAULT;
            log.info("Report period of graphite server is not configured, " +
                    "default value is {}", reportPeriod);
        } else {
            reportPeriod = newReportPeriod;
            log.info("Configured. Report period of graphite server" +
                    " is configured to {}", reportPeriod);
        }

        String newMetricNamePrefix = Tools.get(properties, METRIC_NAME_PREFIX);
        metricNamePrefix = newMetricNamePrefix != null ?
                newMetricNamePrefix : METRIC_NAME_PREFIX_DEFAULT;

    }

    /**
     * Configures parameters for graphite config.
     */
    private void configGraphite() {
        try {
            graphite = new Graphite(new InetSocketAddress(address, port));
        } catch (Exception e) {
            log.error("Fail to connect to given graphite server! : " + e.getMessage());
        }
    }

    /**
     * Builds reporter with the given graphite config.
     *
     * @param graphiteCfg graphite config
     * @return reporter
     */
    private GraphiteReporter buildReporter(Graphite graphiteCfg) {
        MetricRegistry metricRegistry = metricsService.getMetricRegistry();
        return GraphiteReporter.forRegistry(filter(metricRegistry))
                .prefixedWith(metricNamePrefix)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .build(graphiteCfg);
    }
}
