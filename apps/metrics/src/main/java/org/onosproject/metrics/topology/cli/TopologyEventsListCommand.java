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
package org.onosproject.metrics.topology.cli;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.event.Event;
import org.onosproject.metrics.topology.TopologyMetricsService;
import org.onosproject.net.topology.TopologyEvent;

/**
 * Command to show the list of last topology events.
 */
@Command(scope = "onos", name = "topology-events",
         description = "Lists the last topology events")
public class TopologyEventsListCommand extends AbstractShellCommand {

    private static final String FORMAT_EVENT =  "Event=%s";
    private static final String FORMAT_REASON = "    Reason=%s";

    @Override
    protected void execute() {
        TopologyMetricsService service = get(TopologyMetricsService.class);

        if (outputJson()) {
            print("%s", json(service.getEvents()));
        } else {
            for (Event event : service.getEvents()) {
                print(FORMAT_EVENT, event);
                if (event instanceof TopologyEvent) {
                    TopologyEvent topologyEvent = (TopologyEvent) event;
                    for (Event reason : topologyEvent.reasons()) {
                        print(FORMAT_REASON, reason);
                    }
                }
                print("");          // Extra empty line for clarity
            }
        }
    }

    /**
     * Produces a JSON array of topology events.
     *
     * @param events the topology events with the data
     * @return JSON array with the topology events
     */
    private JsonNode json(List<Event> events) {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode result = mapper.createArrayNode();

        for (Event event : events) {
            result.add(json(mapper, event));
        }
        return result;
    }

    /**
     * Produces JSON object for a topology event.
     *
     * @param mapper the JSON object mapper to use
     * @param event the topology event with the data
     * @return JSON object for the topology event
     */
    private ObjectNode json(ObjectMapper mapper, Event event) {
        ObjectNode result = mapper.createObjectNode();

        result.put("time", event.time())
            .put("type", event.type().toString())
            .put("event", event.toString());

        // Add the reasons if a TopologyEvent
        if (event instanceof TopologyEvent) {
            TopologyEvent topologyEvent = (TopologyEvent) event;
            ArrayNode reasons = mapper.createArrayNode();
            for (Event reason : topologyEvent.reasons()) {
                reasons.add(json(mapper, reason));
            }
            result.set("reasons", reasons);
        }

        return result;
    }
}
