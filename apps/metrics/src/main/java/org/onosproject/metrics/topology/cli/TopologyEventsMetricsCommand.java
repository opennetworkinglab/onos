/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.metrics.topology.cli;

import java.util.concurrent.TimeUnit;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Meter;
import com.codahale.metrics.json.MetricsModule;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onlab.metrics.EventMetric;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.metrics.topology.TopologyMetricsService;

/**
 * Command to show the topology events metrics.
 */
@Service
@Command(scope = "onos", name = "topology-events-metrics",
         description = "Lists topology events metrics")
public class TopologyEventsMetricsCommand extends AbstractShellCommand {

    private static final String FORMAT_GAUGE =
        "Topology %s Event Timestamp (ms from epoch)=%d";
    private static final String FORMAT_METER =
        "Topology %s Events count=%d rate(events/sec) mean=%f m1=%f m5=%f m15=%f";

    @Override
    protected void doExecute() {
        TopologyMetricsService service = get(TopologyMetricsService.class);

        if (outputJson()) {
            ObjectMapper mapper = new ObjectMapper()
                .registerModule(new MetricsModule(TimeUnit.SECONDS,
                                                  TimeUnit.MILLISECONDS,
                                                  false));
            ObjectNode result = mapper.createObjectNode();
            result = json(mapper, result, "topologyDeviceEvent",
                          service.topologyDeviceEventMetric());
            result = json(mapper, result, "topologyHostEvent",
                          service.topologyHostEventMetric());
            result = json(mapper, result, "topologyLinkEvent",
                          service.topologyLinkEventMetric());
            result = json(mapper, result, "topologyGraphEvent",
                          service.topologyGraphEventMetric());
            result = json(mapper, result, "topologyGraphReasonsEvent",
                          service.topologyGraphReasonsEventMetric());
            print("%s", result);
        } else {
            printEventMetric("Device", service.topologyDeviceEventMetric());
            printEventMetric("Host", service.topologyHostEventMetric());
            printEventMetric("Link", service.topologyLinkEventMetric());
            printEventMetric("Graph", service.topologyGraphEventMetric());
            printEventMetric("Graph Reasons",
                             service.topologyGraphReasonsEventMetric());
        }
    }

    /**
     * Produces JSON node for an Event Metric.
     *
     * @param mapper the JSON object mapper to use
     * @param objectNode the JSON object node to use
     * @param propertyPrefix the property prefix to use
     * @param eventMetric the Event Metric with the data
     * @return JSON object node for the Event Metric
     */
    private ObjectNode json(ObjectMapper mapper, ObjectNode objectNode,
                            String propertyPrefix, EventMetric eventMetric) {
        String gaugeName = propertyPrefix + "Timestamp";
        String meterName = propertyPrefix + "Rate";
        Gauge<Long> gauge = eventMetric.lastEventTimestampGauge();
        Meter meter = eventMetric.eventRateMeter();

        objectNode.set(gaugeName, json(mapper, gauge));
        objectNode.set(meterName, json(mapper, meter));
        return objectNode;
    }

    /**
     * Produces JSON node for an Object.
     *
     * @param mapper the JSON object mapper to use
     * @param object the Object with the data
     * @return JSON node for the Object
     */
    private JsonNode json(ObjectMapper mapper, Object object) {
        //
        // NOTE: The API for custom serializers is incomplete,
        // hence we have to parse the JSON string to create JsonNode.
        //
        try {
            final String objectJson = mapper.writeValueAsString(object);
            JsonNode jsonNode = mapper.readTree(objectJson);
            return jsonNode;
        } catch (JsonProcessingException e) {
            log.error("Error writing value as JSON string", e);
        }
        return null;
    }

    /**
     * Prints an Event Metric.
     *
     * @param operationStr the string with the intent operation to print
     * @param eventMetric the Event Metric to print
     */
    private void printEventMetric(String operationStr,
                                  EventMetric eventMetric) {
        Gauge<Long> gauge = eventMetric.lastEventTimestampGauge();
        Meter meter = eventMetric.eventRateMeter();
        TimeUnit rateUnit = TimeUnit.SECONDS;
        double rateFactor = rateUnit.toSeconds(1);

        // Print the Gauge
        print(FORMAT_GAUGE, operationStr, gauge.getValue());

        // Print the Meter
        print(FORMAT_METER, operationStr, meter.getCount(),
              meter.getMeanRate() * rateFactor,
              meter.getOneMinuteRate() * rateFactor,
              meter.getFiveMinuteRate() * rateFactor,
              meter.getFifteenMinuteRate() * rateFactor);
    }
}
