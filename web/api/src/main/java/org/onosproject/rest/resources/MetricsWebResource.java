/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.rest.resources;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Metric;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import com.codahale.metrics.MetricFilter;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Ordering;
import com.google.common.collect.TreeMultimap;
import org.onlab.metrics.MetricsService;
import org.onosproject.rest.AbstractWebResource;
import org.onlab.util.ItemNotFoundException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Comparator;
import java.util.Map;

/**
 * Query metrics.
 */
@Path("metrics")
public class MetricsWebResource extends AbstractWebResource {

    private static final String E_METRIC_NAME_NOT_FOUND = "Metric Name is not found";

    private final MetricsService service = get(MetricsService.class);
    private final ObjectNode root = mapper().createObjectNode();

    /**
     * Gets stats information of all metrics. Returns array of all information for
     * all metrics.
     *
     * @return 200 OK with metric information as array
     * @onos.rsModel Metrics
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAllMetrics() {
        ArrayNode metricsNode = root.putArray("metrics");
        service.getMetrics().forEach((name, metric) -> {
            ObjectNode item = mapper().createObjectNode();
            item.put("name", name);
            item.set("metric", codec(Metric.class).encode(metric, this));
            metricsNode.add(item);
        });

        return ok(root).build();
    }

    /**
     * Gets stats information of a metric. Returns array of all information for the
     * specified metric.
     *
     * @param metricName metric name
     * @return 200 OK with metric information as array
     * @onos.rsModel Metric
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("{metricName}")
    public Response getMetricByName(@PathParam("metricName") String metricName) {
        ObjectNode metricNode = root.putObject("metric");
        MetricFilter filter = metricName != null ? (name, metric) -> name.equals(metricName) : MetricFilter.ALL;
        TreeMultimap<String, Metric> matched = listMetrics(service, filter);

        if (matched.isEmpty()) {
            throw new ItemNotFoundException(E_METRIC_NAME_NOT_FOUND);
        }

        matched.asMap().get(metricName).forEach(m -> {
            metricNode.set(metricName, codec(Metric.class).encode(m, this));
        });

        return ok(root).build();
    }

    private TreeMultimap<String, Metric> listMetrics(MetricsService metricsService, MetricFilter filter) {
        TreeMultimap<String, Metric> metrics = TreeMultimap.create(Comparator.naturalOrder(), Ordering.arbitrary());

        Map<String, Counter> counters = metricsService.getCounters(filter);
        for (Map.Entry<String, Counter> entry : counters.entrySet()) {
            metrics.put(entry.getKey(), entry.getValue());
        }
        Map<String, Gauge> gauges = metricsService.getGauges(filter);
        for (Map.Entry<String, Gauge> entry : gauges.entrySet()) {
            metrics.put(entry.getKey(), entry.getValue());
        }
        Map<String, Histogram> histograms = metricsService.getHistograms(filter);
        for (Map.Entry<String, Histogram> entry : histograms.entrySet()) {
            metrics.put(entry.getKey(), entry.getValue());
        }
        Map<String, Meter> meters = metricsService.getMeters(filter);
        for (Map.Entry<String, Meter> entry : meters.entrySet()) {
            metrics.put(entry.getKey(), entry.getValue());
        }
        Map<String, Timer> timers = metricsService.getTimers(filter);
        for (Map.Entry<String, Timer> entry : timers.entrySet()) {
            metrics.put(entry.getKey(), entry.getValue());
        }

        return metrics;
    }
}
