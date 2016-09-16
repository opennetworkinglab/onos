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
package org.onosproject.graphitemetrics;


import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.graphite.Graphite;
import com.codahale.metrics.graphite.GraphiteReporter;
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
import org.onosproject.core.CoreService;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;

import java.net.InetSocketAddress;
import java.util.Dictionary;
import java.util.concurrent.TimeUnit;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * A metric report that reports all metrics value to graphite monitoring server.
 */
@Component(immediate = true)
public class DefaultGraphiteMetricsReporter implements GraphiteMetricsReporter {

    private final Logger log = getLogger(getClass());

    private static final TimeUnit REPORT_TIME_UNIT = TimeUnit.MINUTES;
    private static final int DEFAULT_REPORT_PERIOD = 1;

    private static final String DEFAULT_METRIC_NAMES = "default";
    private static final String DEFAULT_ADDRESS = "localhost";
    private static final int DEFAULT_PORT = 2003;
    private static final String DEFAULT_METRIC_NAME_PREFIX = "onos";


    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MetricsService metricsService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ComponentConfigService cfgService;

    @Property(name = "monitorAll", boolValue = true,
            label = "Enable to monitor all of metrics stored in metric registry default is true")
    protected boolean monitorAll = true;

    @Property(name = "metricNames", value = DEFAULT_METRIC_NAMES,
            label = "Names of metric to be monitored; default metric names are 'default'")
    protected String metricNames = DEFAULT_METRIC_NAMES;

    @Property(name = "address", value = DEFAULT_ADDRESS,
            label = "IP address of graphite monitoring server; default is localhost")
    protected String address = DEFAULT_ADDRESS;

    @Property(name = "port", intValue = DEFAULT_PORT,
            label = "Port number of graphite monitoring server; default is 2003")
    protected int port = DEFAULT_PORT;

    @Property(name = "reportPeriod", intValue = DEFAULT_REPORT_PERIOD,
            label = "Reporting period of graphite monitoring server; default is 1")
    protected int reportPeriod = DEFAULT_REPORT_PERIOD;

    @Property(name = "metricNamePrefix", value = DEFAULT_METRIC_NAME_PREFIX,
            label = "Prefix of metric name for graphite back-end server; default is 'onos'")
    protected String metricNamePrefix = DEFAULT_METRIC_NAME_PREFIX;

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

        Boolean newMonitorAll = Tools.isPropertyEnabled(properties, "monitorAll");
        if (newMonitorAll == null) {
            log.info("Monitor all metrics is not configured, " +
                    "using current value of {}", monitorAll);
        } else {
            monitorAll = newMonitorAll;
            log.info("Configured. Monitor all metrics is {}, ",
                    monitorAll ? "enabled" : "disabled");
        }

        String newMetricNames = Tools.get(properties, "metricNames");
        metricNames = newMetricNames != null ? newMetricNames : DEFAULT_METRIC_NAMES;
        log.info("Configured. Metric name is {}", metricNames);

        String newAddress = Tools.get(properties, "address");
        address = newAddress != null ? newAddress : DEFAULT_ADDRESS;
        log.info("Configured. Graphite monitoring server address is {}", address);

        Integer newPort = Tools.getIntegerProperty(properties, "port");
        if (newPort == null) {
            port = DEFAULT_PORT;
            log.info("Graphite port is not configured, default value is {}", port);
        } else {
            port = newPort;
            log.info("Configured. Graphite port is configured to {}", port);
        }

        Integer newReportPeriod = Tools.getIntegerProperty(properties, "reportPeriod");
        if (newReportPeriod == null) {
            reportPeriod = DEFAULT_REPORT_PERIOD;
            log.info("Report period of graphite server is not configured, " +
                    "default value is {}", reportPeriod);
        } else {
            reportPeriod = newReportPeriod;
            log.info("Configured. Report period of graphite server" +
                    " is configured to {}", reportPeriod);
        }

        String newMetricNamePrefix = Tools.get(properties, "metricNamePrefix");
        metricNamePrefix = newMetricNamePrefix != null ?
                newMetricNamePrefix : DEFAULT_METRIC_NAME_PREFIX;

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
