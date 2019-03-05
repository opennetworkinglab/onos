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
package org.onosproject.cli.net;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.action.Option;
import org.onlab.util.StringFilter;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.cli.PlaceholderCompleter;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowEntry.FlowEntryState;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.utils.Comparators;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.google.common.collect.Lists.newArrayList;


/**
 * Lists all currently-known flows.
 */
@Service
@Command(scope = "onos", name = "flows",
         description = "Lists all currently-known flows.")
public class FlowsListCommand extends AbstractShellCommand {

    private static final Predicate<FlowEntry> TRUE_PREDICATE = f -> true;

    public static final String ANY = "any";

    private static final String LONG_FORMAT = "    id=%s, state=%s, bytes=%s, "
            + "packets=%s, duration=%s, liveType=%s, priority=%s, tableId=%s, appId=%s, "
            + "selector=%s, treatment=%s";

    private static final String SHORT_FORMAT = "    %s, bytes=%s, packets=%s, "
            + "table=%s, priority=%s, selector=%s, treatment=%s";

    @Argument(index = 0, name = "state", description = "Flow Rule state",
            required = false, multiValued = false)
    @Completion(FlowRuleStatusCompleter.class)
    String state = null;

    @Argument(index = 1, name = "uri", description = "Device ID",
              required = false, multiValued = false)
    @Completion(DeviceIdCompleter.class)
    String uri = null;

    @Argument(index = 2, name = "table", description = "Table ID",
            required = false, multiValued = false)
    @Completion(PlaceholderCompleter.class)
    String table = null;

    @Option(name = "-s", aliases = "--short",
            description = "Print more succinct output for each flow",
            required = false, multiValued = false)
    private boolean shortOutput = false;

    @Option(name = "-n", aliases = "--no-core-flows",
            description = "Suppress core flows from output",
            required = false, multiValued = false)
    private boolean suppressCoreOutput = false;

    @Option(name = "-c", aliases = "--count",
            description = "Print flow count only",
            required = false, multiValued = false)
    private boolean countOnly = false;

    @Option(name = "-f", aliases = "--filter",
            description = "Filter flows by specific keyword",
            required = false, multiValued = true)
    private List<String> filter = new ArrayList<>();

    @Option(name = "-r", aliases = "--remove",
            description = "Remove flows by specific keyword",
            required = false, multiValued = false)
    private String remove = null;

    private Predicate<FlowEntry> predicate = TRUE_PREDICATE;

    private StringFilter contentFilter;

    @Override
    protected void doExecute() {
        CoreService coreService = get(CoreService.class);
        DeviceService deviceService = get(DeviceService.class);
        FlowRuleService service = get(FlowRuleService.class);
        contentFilter = new StringFilter(filter, StringFilter.Strategy.AND);

        compilePredicate();

        if (countOnly && !suppressCoreOutput && filter.isEmpty() && remove == null) {
            if (state == null && uri == null) {
                deviceService.getDevices().forEach(device -> printCount(device, service));
            } else if (uri == null) {
                deviceService.getDevices()
                        .forEach(device -> printCount(device, FlowEntryState.valueOf(state.toUpperCase()), service));
            } else {
                Device device = deviceService.getDevice(DeviceId.deviceId(uri));
                if (device != null) {
                    printCount(device, FlowEntryState.valueOf(state.toUpperCase()), service);
                }
            }
            return;
        }

        SortedMap<Device, List<FlowEntry>> flows = getSortedFlows(deviceService, service, coreService);

        // Remove flows
        if (remove != null) {
            flows.values().forEach(flowList -> {
                if (!remove.isEmpty()) {
                    filter.add(remove);
                    contentFilter = new StringFilter(filter, StringFilter.Strategy.AND);
                }
                if (!filter.isEmpty() || (remove != null && !remove.isEmpty())) {
                    flowList = filterFlows(flowList);
                    this.removeFlowsInteractive(flowList, service, coreService);
                }
            });
            return;
        }

        // Show flows
        if (outputJson()) {
            print("%s", json(flows.keySet(), flows));
        } else {
            flows.forEach((device, flow) -> printFlows(device, flow, coreService));
        }
    }

    /**
     * Removes the flows passed as argument after confirmation is provided
     * for each of them.
     * If no explicit confirmation is provided, the flow is not removed.
     *
     * @param flows       list of flows to remove
     * @param flowService FlowRuleService object
     * @param coreService CoreService object
     */
    public void removeFlowsInteractive(Iterable<FlowEntry> flows,
                                       FlowRuleService flowService, CoreService coreService) {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        flows.forEach(flow -> {
            ApplicationId appId = coreService.getAppId(flow.appId());
            System.out.print(String.format("Id=%s, AppId=%s. Remove? [y/N]: ",
                                           flow.id(), appId != null ? appId.name() : "<none>"));
            String response;
            try {
                response = br.readLine();
                response = response.trim().replace("\n", "");
                if ("y".equals(response)) {
                    flowService.removeFlowRules(flow);
                }
            } catch (IOException e) {
                response = "";
            }
            print(response);
        });
    }

    /**
     * Produces a JSON array of flows grouped by the each device.
     *
     * @param devices     collection of devices to group flow by
     * @param flows       collection of flows per each device
     * @return JSON array
     */
    private JsonNode json(Iterable<Device> devices,
                          Map<Device, List<FlowEntry>> flows) {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode result = mapper.createArrayNode();
        for (Device device : devices) {
            result.add(json(mapper, device, flows.get(device)));
        }
        return result;
    }

    /**
     * Compiles a predicate to find matching flows based on the command
     * arguments.
     */
    private void compilePredicate() {
        if (state != null && !state.equals(ANY)) {
            final FlowEntryState feState = FlowEntryState.valueOf(state.toUpperCase());
            predicate = predicate.and(f -> f.state().equals(feState));
        }

        if (table != null) {
            final int tableId = Integer.parseInt(table);
            predicate = predicate.and(f -> f.tableId() == tableId);
        }
    }

    // Produces JSON object with the flows of the given device.
    private ObjectNode json(ObjectMapper mapper,
                            Device device, List<FlowEntry> flows) {
        ObjectNode result = mapper.createObjectNode();
        ArrayNode array = mapper.createArrayNode();

        flows.forEach(flow -> array.add(jsonForEntity(flow, FlowEntry.class)));

        result.put("device", device.id().toString())
                .put("flowCount", flows.size())
                .set("flows", array);
        return result;
    }

    /**
     * Returns the list of devices sorted using the device ID URIs.
     *
     * @param deviceService device service
     * @param service flow rule service
     * @param coreService core service
     * @return sorted device list
     */
    protected SortedMap<Device, List<FlowEntry>> getSortedFlows(DeviceService deviceService,
                                                          FlowRuleService service, CoreService coreService) {
        SortedMap<Device, List<FlowEntry>> flows = new TreeMap<>(Comparators.ELEMENT_COMPARATOR);
        List<FlowEntry> rules;

        Iterable<Device> devices = null;
        if (uri == null) {
            devices = deviceService.getDevices();
        } else {
            Device dev = deviceService.getDevice(DeviceId.deviceId(uri));
            devices = (dev == null) ? deviceService.getDevices()
                                    : Collections.singletonList(dev);
        }

        for (Device d : devices) {
            if (predicate.equals(TRUE_PREDICATE)) {
                rules = newArrayList(service.getFlowEntries(d.id()));
            } else {
                rules = newArrayList();
                for (FlowEntry f : service.getFlowEntries(d.id())) {
                    if (predicate.test(f)) {
                        rules.add(f);
                    }
                }
            }
            rules.sort(Comparators.FLOW_RULE_COMPARATOR);

            if (suppressCoreOutput) {
                short coreAppId = coreService.getAppId("org.onosproject.core").id();
                rules = rules.stream()
                        .filter(f -> f.appId() != coreAppId)
                        .collect(Collectors.toList());
            }
            flows.put(d, rules);
        }
        return flows;
    }

    /**
     * Filter a given list of flows based on the existing content filter.
     *
     * @param flows list of flows to filter
     * @return further filtered list of flows
     */
    private List<FlowEntry> filterFlows(List<FlowEntry> flows) {
        return flows.stream().
                filter(f -> contentFilter.filter(f)).collect(Collectors.toList());
    }

    private void printCount(Device device, FlowRuleService flowRuleService) {
        print("deviceId=%s, flowRuleCount=%d", device.id(), flowRuleService.getFlowRuleCount(device.id()));
    }

    private void printCount(Device device, FlowEntryState state, FlowRuleService flowRuleService) {
        print("deviceId=%s, flowRuleCount=%d", device.id(), flowRuleService.getFlowRuleCount(device.id(), state));
    }

    /**
     * Prints flows.
     *
     * @param d     the device
     * @param flows the set of flows for that device
     * @param coreService core system service
     */
    protected void printFlows(Device d, List<FlowEntry> flows,
                              CoreService coreService) {
        List<FlowEntry> filteredFlows = filterFlows(flows);
        boolean empty = filteredFlows == null || filteredFlows.isEmpty();
        print("deviceId=%s, flowRuleCount=%d", d.id(), empty ? 0 : filteredFlows.size());
        if (empty || countOnly) {
            return;
        }

        for (FlowEntry f : filteredFlows) {
            if (shortOutput) {
                print(SHORT_FORMAT, f.state(), f.bytes(), f.packets(),
                        f.table(), f.priority(), f.selector().criteria(),
                        printTreatment(f.treatment()));
            } else {
                ApplicationId appId = coreService.getAppId(f.appId());
                print(LONG_FORMAT, Long.toHexString(f.id().value()), f.state(),
                        f.bytes(), f.packets(), f.life(), f.liveType(), f.priority(), f.table(),
                        appId != null ? appId.name() : "<none>",
                        f.selector().criteria(), f.treatment());
            }
        }
    }

    private String printTreatment(TrafficTreatment treatment) {
        final String delimiter = ", ";
        StringBuilder builder = new StringBuilder("[");
        if (!treatment.immediate().isEmpty()) {
            builder.append("immediate=" + treatment.immediate() + delimiter);
        }
        if (!treatment.deferred().isEmpty()) {
            builder.append("deferred=" + treatment.deferred() + delimiter);
        }
        if (treatment.clearedDeferred()) {
            builder.append("clearDeferred" + delimiter);
        }
        if (treatment.tableTransition() != null) {
            builder.append("transition=" + treatment.tableTransition() + delimiter);
        }
        if (treatment.metered() != null) {
            builder.append("meter=" + treatment.metered() + delimiter);
        }
        if (treatment.writeMetadata() != null) {
            builder.append("metadata=" + treatment.writeMetadata() + delimiter);
        }
        // Chop off last delimiter
        builder.replace(builder.length() - delimiter.length(), builder.length(), "");
        builder.append("]");
        return builder.toString();
    }
}
