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
package org.onosproject.gangliametrics;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ganglia.GangliaReporter;
import info.ganglia.gmetric4j.gmetric.GMetric;
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

import java.io.IOException;
import java.util.Dictionary;
import java.util.concurrent.TimeUnit;

import static org.onosproject.gangliametrics.OsgiPropertyConstants.ADDRESS;
import static org.onosproject.gangliametrics.OsgiPropertyConstants.ADDRESS_DEFAULT;
import static org.onosproject.gangliametrics.OsgiPropertyConstants.METRIC_NAMES;
import static org.onosproject.gangliametrics.OsgiPropertyConstants.METRIC_NAMES_DEFAULT;
import static org.onosproject.gangliametrics.OsgiPropertyConstants.MONITOR_ALL;
import static org.onosproject.gangliametrics.OsgiPropertyConstants.MONITOR_ALL_DEFAULT;
import static org.onosproject.gangliametrics.OsgiPropertyConstants.PORT;
import static org.onosproject.gangliametrics.OsgiPropertyConstants.PORT_DEFAULT;
import static org.onosproject.gangliametrics.OsgiPropertyConstants.TTL;
import static org.onosproject.gangliametrics.OsgiPropertyConstants.TTL_DEFAULT;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * A metric report that reports all metrics value to ganglia monitoring server.
 */
@Component(
    immediate = true,
    property = {
        MONITOR_ALL + ":Boolean=" + MONITOR_ALL_DEFAULT,
        METRIC_NAMES + "=" + METRIC_NAMES_DEFAULT,
        ADDRESS + "=" + ADDRESS_DEFAULT,
        PORT + ":Integer=" + PORT_DEFAULT,
        TTL + ":Integer=" + TTL_DEFAULT
    }
)
public class DefaultGangliaMetricsReporter implements GangliaMetricsReporter {
    private final Logger log = getLogger(getClass());

    // we will use uni-cast mode to transfer the metrics value by default
    private static final GMetric.UDPAddressingMode GANGLIA_MODE =
                         GMetric.UDPAddressingMode.UNICAST;
    private static final int REPORT_PERIOD = 1;
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

    /** IP address of ganglia monitoring server; default is localhost. */
    protected String address = ADDRESS_DEFAULT;

    /** Port number of ganglia monitoring server; default is 8649. */
    protected int port = PORT_DEFAULT;

    /** TTL value of ganglia monitoring server; default is 1. */
    protected int ttl = TTL_DEFAULT;

    private GMetric ganglia;
    private GangliaReporter gangliaReporter;

    @Activate
    public void activate() {
        cfgService.registerProperties(getClass());
        coreService.registerApplication("org.onosproject.gangliametrics");
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
        stopReport();
        startReport();
    }

    @Override
    public void startReport() {
        configGMetric();
        gangliaReporter = buildReporter(ganglia);

        try {
            gangliaReporter.start(REPORT_PERIOD, REPORT_TIME_UNIT);
        } catch (Exception e) {
            log.error("Errors during reporting to ganglia, msg: {}" + e.getMessage());
        }

        log.info("Start to report metrics to ganglia server.");
    }

    @Override
    public void stopReport() {
        gangliaReporter.stop();
        ganglia = null;
        gangliaReporter = null;
        log.info("Stop reporting metrics to ganglia server.");
    }

    @Override
    public void restartReport() {
        stopReport();
        startReport();
    }

    @Override
    public void notifyMetricsChange() {
        gangliaReporter.stop();
        gangliaReporter = buildReporter(ganglia);

        try {
            gangliaReporter.start(REPORT_PERIOD, REPORT_TIME_UNIT);
        } catch (Exception e) {
            log.error("Errors during reporting to ganglia, msg: {}" + e.getMessage());
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

        String addressStr = Tools.get(properties, ADDRESS);
        address = addressStr != null ? addressStr : ADDRESS_DEFAULT;
        log.info("Configured. Ganglia server address is {}", address);

        String metricNameStr = Tools.get(properties, METRIC_NAMES);
        metricNames = metricNameStr != null ? metricNameStr : METRIC_NAMES_DEFAULT;
        log.info("Configured. Metric name is {}", metricNames);

        Integer portConfigured = Tools.getIntegerProperty(properties, PORT);
        if (portConfigured == null) {
            port = PORT_DEFAULT;
            log.info("Ganglia port is not configured, default value is {}", port);
        } else {
            port = portConfigured;
            log.info("Configured. Ganglia port is configured to {}", port);
        }

        Integer ttlConfigured = Tools.getIntegerProperty(properties, TTL);
        if (ttlConfigured == null) {
            ttl = TTL_DEFAULT;
            log.info("Ganglia TTL is not configured, default value is {}", ttl);
        } else {
            ttl = ttlConfigured;
            log.info("Configured. Ganglia TTL is configured to {}", ttl);
        }

        Boolean monitorAllEnabled = Tools.isPropertyEnabled(properties, MONITOR_ALL);
        if (monitorAllEnabled == null) {
            log.info("Monitor all metrics is not configured, " +
                     "using current value of {}", monitorAll);
        } else {
            monitorAll = monitorAllEnabled;
            log.info("Configured. Monitor all metrics is {}",
                    monitorAll ? "enabled" : "disabled");
        }
    }

    /**
     * Configures parameters for GMetric.
     */
    private void configGMetric() {
        try {
            ganglia = new GMetric(address, port, GANGLIA_MODE, ttl);
        } catch (IOException e) {
            log.error("Fail to connect to given ganglia server!");
        }
    }

    /**
     * Builds reporter with the given ganglia metric.
     *
     * @param gMetric ganglia metric
     * @return reporter
     */
    private GangliaReporter buildReporter(GMetric gMetric) {
        MetricRegistry mr = metricsService.getMetricRegistry();

        return GangliaReporter.forRegistry(filter(mr))
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .build(gMetric);
    }
}
