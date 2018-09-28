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
package org.onosproject.events;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Stream;

import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onlab.util.Tools;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.cluster.ClusterEvent;
import org.onosproject.event.Event;
import org.onosproject.mastership.MastershipEvent;
import org.onosproject.net.Link;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.host.HostEvent;
import org.onosproject.net.intent.IntentEvent;
import org.onosproject.net.link.LinkEvent;
import org.onosproject.net.topology.Topology;
import org.onosproject.net.topology.TopologyEvent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;

import static java.util.stream.Collectors.toList;

/**
 * Command to print history of instance local ONOS Events.
 */
@Service
@Command(scope = "onos", name = "events",
         description = "Command to print history of instance local ONOS Events")
public class EventsCommand
    extends AbstractShellCommand {

    @Option(name = "--all", aliases = "-a",
            description = "Include all Events (default behavior)",
            required = false)
    private boolean all = false;

    @Option(name = "--mastership", aliases = "-m",
            description = "Include MastershipEvent",
            required = false)
    private boolean mastership = false;

    @Option(name = "--device", aliases = "-d",
            description = "Include DeviceEvent",
            required = false)
    private boolean device = false;

    @Option(name = "--link", aliases = "-l",
            description = "Include LinkEvent",
            required = false)
    private boolean link = false;

    @Option(name = "--topology", aliases = "-t",
            description = "Include TopologyEvent",
            required = false)
    private boolean topology = false;

    @Option(name = "--host", aliases = "-h",
            description = "Include HostEvent",
            required = false)
    private boolean host = false;

    @Option(name = "--cluster", aliases = "-c",
            description = "Include ClusterEvent",
            required = false)
    private boolean cluster = false;

    @Option(name = "--intent", aliases = "-i",
            description = "Include IntentEvent",
            required = false)
    private boolean intent = false;

    @Option(name = "--max-events", aliases = "-n",
            description = "Maximum number of events to print",
            required = false,
            valueToShowInHelp = "-1 [no limit]")
    private long maxSize = -1;

    @Override
    protected void doExecute() {
        EventHistoryService eventHistoryService = get(EventHistoryService.class);

        Stream<Event<?, ?>> events = eventHistoryService.history().stream();

        boolean dumpAll = all || !(mastership || device || link || topology || host || cluster || intent);

        if (!dumpAll) {
            Predicate<Event<?, ?>> filter = (defaultIs) -> false;

            if (mastership) {
                filter = filter.or(evt -> evt instanceof MastershipEvent);
            }
            if (device) {
                filter = filter.or(evt -> evt instanceof DeviceEvent);
            }
            if (link) {
                filter = filter.or(evt -> evt instanceof LinkEvent);
            }
            if (topology) {
                filter = filter.or(evt -> evt instanceof TopologyEvent);
            }
            if (host) {
                filter = filter.or(evt -> evt instanceof HostEvent);
            }
            if (cluster) {
                filter = filter.or(evt -> evt instanceof ClusterEvent);
            }
            if (intent) {
                filter = filter.or(evt -> evt instanceof IntentEvent);
            }

            events = events.filter(filter);
        }

        if (maxSize > 0) {
            events = events.limit(maxSize);
        }

        if (outputJson()) {
            ArrayNode jsonEvents = events.map(this::json).collect(toArrayNode());
            printJson(jsonEvents);
        } else {
            events.forEach(this::printEvent);
        }

    }

    private Collector<JsonNode, ArrayNode, ArrayNode> toArrayNode() {
        return Collector.of(() -> mapper().createArrayNode(),
                            ArrayNode::add,
                            ArrayNode::addAll);
    }

    private ObjectNode json(Event<?, ?> event) {
        ObjectNode result = mapper().createObjectNode();

        result.put("time", event.time())
              .put("type", event.type().toString())
              .put("event", event.toString());

        return result;
    }

    /**
     * Print JsonNode using default pretty printer.
     *
     * @param json JSON node to print
     */
    @java.lang.SuppressWarnings("squid:S1148")
    private void printJson(JsonNode json) {
        try {
            print("%s", mapper().writerWithDefaultPrettyPrinter().writeValueAsString(json));
        } catch (JsonProcessingException e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            print("[ERROR] %s\n%s", e.getMessage(), sw.toString());
        }
    }

    private void printEvent(Event<?, ?> event) {
        if (event instanceof DeviceEvent) {
            DeviceEvent deviceEvent = (DeviceEvent) event;
            if (event.type().toString().startsWith("PORT")) {
                // Port event
                print("%s %s\t%s/%s [%s]",
                      Tools.defaultOffsetDataTime(event.time()),
                      event.type(),
                      deviceEvent.subject().id(), deviceEvent.port().number(),
                      deviceEvent.port()
                  );
            } else {
                // Device event
                print("%s %s\t%s [%s]",
                      Tools.defaultOffsetDataTime(event.time()),
                      event.type(),
                      deviceEvent.subject().id(),
                      deviceEvent.subject()
                  );
            }

        } else if (event instanceof MastershipEvent) {
            print("%s %s\t%s [%s]",
                  Tools.defaultOffsetDataTime(event.time()),
                  event.type(),
                  event.subject(),
                  ((MastershipEvent) event).roleInfo());

        } else if (event instanceof LinkEvent) {
            LinkEvent linkEvent = (LinkEvent) event;
            Link link = linkEvent.subject();
            print("%s %s\t%s/%s-%s/%s [%s]",
                  Tools.defaultOffsetDataTime(event.time()),
                  event.type(),
                  link.src().deviceId(), link.src().port(), link.dst().deviceId(), link.dst().port(),
                  link);

        } else if (event instanceof HostEvent) {
            HostEvent hostEvent = (HostEvent) event;
            print("%s %s\t%s [%s->%s]",
                  Tools.defaultOffsetDataTime(event.time()),
                  event.type(),
                  hostEvent.subject().id(),
                  hostEvent.prevSubject(), hostEvent.subject());

        } else if (event instanceof TopologyEvent) {
            TopologyEvent topoEvent = (TopologyEvent) event;
            List<Event> reasons = MoreObjects.firstNonNull(topoEvent.reasons(),
                                                           ImmutableList.<Event>of());
            Topology topo = topoEvent.subject();
            String summary = String.format("(d=%d,l=%d,c=%d)",
                                           topo.deviceCount(),
                                           topo.linkCount(),
                                           topo.clusterCount());
            print("%s %s%s [%s]",
                  Tools.defaultOffsetDataTime(event.time()),
                  event.type(),
                  summary,
                  reasons.stream().map(e -> e.type()).collect(toList()));

        } else if (event instanceof ClusterEvent) {
            print("%s %s\t%s [%s]",
                  Tools.defaultOffsetDataTime(event.time()),
                  event.type(),
                  ((ClusterEvent) event).subject().id(),
                  event.subject());

        } else {
            // Unknown Event?
            print("%s %s\t%s [%s]",
                  Tools.defaultOffsetDataTime(event.time()),
                  event.type(),
                  event.subject(),
                  event);
        }
    }

}
