/*
 * Copyright 2014-present Open Networking Foundation
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

package org.onosproject.cli;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.Snapshot;
import com.codahale.metrics.Timer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import com.google.common.collect.Ordering;
import com.google.common.collect.TreeMultimap;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onlab.metrics.MetricsService;
import org.onlab.util.Tools;

import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;

import static java.lang.String.format;

/**
 * Prints metrics in the system.
 */
@Service
@Command(scope = "onos", name = "metrics",
         description = "Prints metrics in the system")
public class MetricsListCommand extends AbstractShellCommand {

    private static final String COUNTER = "counter";

    private static final String GAUGE = "gauge";
    private static final String VALUE = "value";

    private static final String METER = "meter";
    private static final String MEAN_RATE = "mean_rate";
    private static final String ONE_MIN_RATE = "1_min_rate";
    private static final String FIVE_MIN_RATE = "5_min_rate";
    private static final String FIFT_MIN_RATE = "15_min_rate";

    private static final String HISTOGRAM = "histogram";
    private static final String MIN = "min";
    private static final String MAX = "max";
    private static final String MEAN = "mean";
    private static final String STDDEV = "stddev";

    private static final String TIMER = "timer";

    @Argument(index = 0, name = "metricName", description = "Name of Metric",
            required = false, multiValued = false)
    @Completion(MetricNameCompleter.class)
    String metricName = null;

    @Override
    protected void doExecute() {
        MetricsService metricsService = get(MetricsService.class);

        MetricFilter filter = metricName != null ? (name, metric) -> name.equals(metricName) : MetricFilter.ALL;

        TreeMultimap<String, Metric> matched = listMetrics(metricsService, filter);
        matched.asMap().forEach((name, metrics) -> {
            if (outputJson()) {
                metrics.forEach(metric -> print("%s", json(metric)));
            } else {
                metrics.forEach(metric -> printMetric(name, metric));
            }
        });
    }

    /**
     * Print metric object.
     *
     * @param name metric name
     * @param metric metric object
     */
    private void printMetric(String name, Metric metric) {
        final String heading;

        if (metric instanceof Counter) {
            heading = format("-- %s : [%s] --", name, "Counter");
            print(heading);
            Counter counter = (Counter) metric;
            print("          count = %d", counter.getCount());

        } else if (metric instanceof Gauge) {
            heading = format("-- %s : [%s] --", name, "Gauge");
            print(heading);
            @SuppressWarnings("rawtypes")
            Gauge gauge = (Gauge) metric;
            final Object value = gauge.getValue();
            if (name.endsWith("EpochMs") && value instanceof Long) {
                print("          value = %s (%s)", value, Tools.defaultOffsetDataTime((Long) value));
            } else {
                print("          value = %s", value);
            }

        } else if (metric instanceof Histogram) {
            heading = format("-- %s : [%s] --", name, "Histogram");
            print(heading);
            final Histogram histogram = (Histogram) metric;
            final Snapshot snapshot = histogram.getSnapshot();
            print("          count = %d", histogram.getCount());
            print("            min = %d", snapshot.getMin());
            print("            max = %d", snapshot.getMax());
            print("           mean = %f", snapshot.getMean());
            print("         stddev = %f", snapshot.getStdDev());

        } else if (metric instanceof Meter) {
            heading = format("-- %s : [%s] --", name, "Meter");
            print(heading);
            final Meter meter = (Meter) metric;
            print("          count = %d", meter.getCount());
            print("      mean rate = %f", meter.getMeanRate());
            print("  1-minute rate = %f", meter.getOneMinuteRate());
            print("  5-minute rate = %f", meter.getFiveMinuteRate());
            print(" 15-minute rate = %f", meter.getFifteenMinuteRate());

        } else if (metric instanceof Timer) {
            heading = format("-- %s : [%s] --", name, "Timer");
            print(heading);
            final Timer timer = (Timer) metric;
            final Snapshot snapshot = timer.getSnapshot();
            print("          count = %d", timer.getCount());
            print("      mean rate = %f per second", timer.getMeanRate());
            print("  1-minute rate = %f per second", timer.getOneMinuteRate());
            print("  5-minute rate = %f per second", timer.getFiveMinuteRate());
            print(" 15-minute rate = %f per second", timer.getFifteenMinuteRate());
            print("            min = %f ms", nanoToMs(snapshot.getMin()));
            print("            max = %f ms", nanoToMs(snapshot.getMax()));
            print("           mean = %f ms", nanoToMs(snapshot.getMean()));
            print("         stddev = %f ms", nanoToMs(snapshot.getStdDev()));
        } else {
            heading = format("-- %s : [%s] --", name, metric.getClass().getCanonicalName());
            print(heading);
            print("Unknown Metric type:{}", metric.getClass().getCanonicalName());
        }
        print(Strings.repeat("-", heading.length()));
    }

    @SuppressWarnings("rawtypes")
    private TreeMultimap<String, Metric> listMetrics(MetricsService metricsService, MetricFilter filter) {
        TreeMultimap<String, Metric> metrics = TreeMultimap.create(Comparator.naturalOrder(), Ordering.arbitrary());

        Map<String, Counter> counters = metricsService.getCounters(filter);
        for (Entry<String, Counter> entry : counters.entrySet()) {
            metrics.put(entry.getKey(), entry.getValue());
        }
        Map<String, Gauge> gauges = metricsService.getGauges(filter);
        for (Entry<String, Gauge> entry : gauges.entrySet()) {
            metrics.put(entry.getKey(), entry.getValue());
        }
        Map<String, Histogram> histograms = metricsService.getHistograms(filter);
        for (Entry<String, Histogram> entry : histograms.entrySet()) {
            metrics.put(entry.getKey(), entry.getValue());
        }
        Map<String, Meter> meters = metricsService.getMeters(filter);
        for (Entry<String, Meter> entry : meters.entrySet()) {
            metrics.put(entry.getKey(), entry.getValue());
        }
        Map<String, Timer> timers = metricsService.getTimers(filter);
        for (Entry<String, Timer> entry : timers.entrySet()) {
            metrics.put(entry.getKey(), entry.getValue());
        }

        return metrics;
    }

    /**
     * Creates a json object for a certain metric.
     *
     * @param metric metric object
     * @return json object
     */
    private ObjectNode json(Metric metric) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode objectNode = mapper.createObjectNode();
        ObjectNode dataNode = mapper.createObjectNode();

        if (metric instanceof Counter) {
            dataNode.put(COUNTER, ((Counter) metric).getCount());
            objectNode.set(COUNTER, dataNode);
        } else if (metric instanceof Gauge) {
            objectNode.put(VALUE, ((Gauge) metric).getValue().toString());
            objectNode.set(GAUGE, dataNode);
        } else if (metric instanceof Meter) {
            dataNode.put(COUNTER, ((Meter) metric).getCount());
            dataNode.put(MEAN_RATE, ((Meter) metric).getMeanRate());
            dataNode.put(ONE_MIN_RATE, ((Meter) metric).getOneMinuteRate());
            dataNode.put(FIVE_MIN_RATE, ((Meter) metric).getFiveMinuteRate());
            dataNode.put(FIFT_MIN_RATE, ((Meter) metric).getFifteenMinuteRate());
            objectNode.set(METER, dataNode);
        } else if (metric instanceof Histogram) {
            dataNode.put(COUNTER, ((Histogram) metric).getCount());
            dataNode.put(MEAN, ((Histogram) metric).getSnapshot().getMean());
            dataNode.put(MIN, ((Histogram) metric).getSnapshot().getMin());
            dataNode.put(MAX, ((Histogram) metric).getSnapshot().getMax());
            dataNode.put(STDDEV, ((Histogram) metric).getSnapshot().getStdDev());
            objectNode.set(HISTOGRAM, dataNode);
        } else if (metric instanceof Timer) {
            dataNode.put(COUNTER, ((Timer) metric).getCount());
            dataNode.put(MEAN_RATE, ((Timer) metric).getMeanRate());
            dataNode.put(ONE_MIN_RATE, ((Timer) metric).getOneMinuteRate());
            dataNode.put(FIVE_MIN_RATE, ((Timer) metric).getFiveMinuteRate());
            dataNode.put(FIFT_MIN_RATE, ((Timer) metric).getFifteenMinuteRate());
            dataNode.put(MEAN, nanoToMs(((Timer) metric).getSnapshot().getMean()));
            dataNode.put(MIN, nanoToMs(((Timer) metric).getSnapshot().getMin()));
            dataNode.put(MAX, nanoToMs(((Timer) metric).getSnapshot().getMax()));
            dataNode.put(STDDEV, nanoToMs(((Timer) metric).getSnapshot().getStdDev()));
            objectNode.set(TIMER, dataNode);
        }
        return objectNode;
    }

    private double nanoToMs(double nano) {
        return nano / 1_000_000D;
    }


}
