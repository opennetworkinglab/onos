package org.onlab.onos.metrics.topology.cli;

import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.karaf.shell.commands.Command;
import org.onlab.onos.cli.AbstractShellCommand;
import org.onlab.onos.event.Event;
import org.onlab.onos.metrics.topology.TopologyMetricsService;
import org.onlab.onos.net.topology.TopologyEvent;

/**
 * Command to show the list of last topology events.
 */
@Command(scope = "onos", name = "topology-events",
         description = "Lists the last topology events")
public class TopologyEventsListCommand extends AbstractShellCommand {

    private static final String FORMAT_EVENT =
        "Topology Event time=%d type=%s subject=%s";
    private static final String FORMAT_REASON =
        "    Reason time=%d type=%s subject=%s";

    @Override
    protected void execute() {
        TopologyMetricsService service = get(TopologyMetricsService.class);

        if (outputJson()) {
            print("%s", json(service.getEvents()));
        } else {
            for (TopologyEvent event : service.getEvents()) {
                print(FORMAT_EVENT, event.time(), event.type(),
                      event.subject());
                for (Event reason : event.reasons()) {
                    print(FORMAT_REASON, reason.time(), reason.type(),
                          reason.subject());
                }
                print("");          // Extra empty line for clarity
            }
        }
    }

    /**
     * Produces a JSON array of topology events.
     *
     * @param topologyEvents the topology events with the data
     * @return JSON array with the topology events
     */
    private JsonNode json(List<TopologyEvent> topologyEvents) {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode result = mapper.createArrayNode();

        for (TopologyEvent event : topologyEvents) {
            result.add(json(mapper, event));
        }
        return result;
    }

    /**
     * Produces JSON object for a topology event.
     *
     * @param mapper the JSON object mapper to use
     * @param topologyEvent the topology event with the data
     * @return JSON object for the topology event
     */
    private ObjectNode json(ObjectMapper mapper, TopologyEvent topologyEvent) {
        ObjectNode result = mapper.createObjectNode();
        ArrayNode reasons = mapper.createArrayNode();

        for (Event reason : topologyEvent.reasons()) {
            reasons.add(json(mapper, reason));
        }
        result.put("time", topologyEvent.time())
            .put("type", topologyEvent.type().toString())
            .put("subject", topologyEvent.subject().toString())
            .put("reasons", reasons);
        return result;
    }

    /**
     * Produces JSON object for a generic event.
     *
     * @param event the generic event with the data
     * @return JSON object for the generic event
     */
    private ObjectNode json(ObjectMapper mapper, Event event) {
        ObjectNode result = mapper.createObjectNode();

        result.put("time", event.time())
            .put("type", event.type().toString())
            .put("subject", event.subject().toString());
        return result;
    }
}
