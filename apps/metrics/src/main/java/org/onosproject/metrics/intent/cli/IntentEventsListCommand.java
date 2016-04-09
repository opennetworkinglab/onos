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
package org.onosproject.metrics.intent.cli;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.metrics.intent.IntentMetricsService;
import org.onosproject.net.intent.IntentEvent;

/**
 * Command to show the list of last intent events.
 */
@Command(scope = "onos", name = "intents-events",
         description = "Lists the last intent events")
public class IntentEventsListCommand extends AbstractShellCommand {

    private static final String FORMAT_EVENT = "Event=%s";

    @Override
    protected void execute() {
        IntentMetricsService service = get(IntentMetricsService.class);

        if (outputJson()) {
            print("%s", json(service.getEvents()));
        } else {
            for (IntentEvent event : service.getEvents()) {
                print(FORMAT_EVENT, event);
                print("");          // Extra empty line for clarity
            }
        }
    }

    /**
     * Produces a JSON array of intent events.
     *
     * @param intentEvents the intent events with the data
     * @return JSON array with the intent events
     */
    private JsonNode json(List<IntentEvent> intentEvents) {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode result = mapper.createArrayNode();

        for (IntentEvent event : intentEvents) {
            result.add(json(mapper, event));
        }
        return result;
    }

    /**
     * Produces JSON object for a intent event.
     *
     * @param mapper the JSON object mapper to use
     * @param intentEvent the intent event with the data
     * @return JSON object for the intent event
     */
    private ObjectNode json(ObjectMapper mapper, IntentEvent intentEvent) {
        ObjectNode result = mapper.createObjectNode();

        result.put("time", intentEvent.time())
            .put("type", intentEvent.type().toString())
            .put("event", intentEvent.toString());
        return result;
    }
}
