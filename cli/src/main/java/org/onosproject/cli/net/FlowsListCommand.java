/*
 * Copyright 2014-2015 Open Networking Laboratory
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

import static com.google.common.collect.Lists.newArrayList;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.cli.Comparators;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowEntry.FlowEntryState;
import org.onosproject.net.flow.FlowRuleService;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Lists all currently-known flows.
 */
@Command(scope = "onos", name = "flows",
         description = "Lists all currently-known flows.")
public class FlowsListCommand extends AbstractShellCommand {

    public static final String ANY = "any";

    private static final String FMT =
            "   id=%s, state=%s, bytes=%s, packets=%s, duration=%s, priority=%s, tableId=%s appId=%s, payLoad=%s";
    private static final String TFMT = "      treatment=%s";
    private static final String SFMT = "      selector=%s";

    @Argument(index = 1, name = "uri", description = "Device ID",
              required = false, multiValued = false)
    String uri = null;

    @Argument(index = 0, name = "state", description = "Flow Rule state",
              required = false, multiValued = false)
    String state = null;

    @Override
    protected void execute() {
        CoreService coreService = get(CoreService.class);
        DeviceService deviceService = get(DeviceService.class);
        FlowRuleService service = get(FlowRuleService.class);
        SortedMap<Device, List<FlowEntry>> flows = getSortedFlows(deviceService, service);

        if (outputJson()) {
            print("%s", json(flows.keySet(), flows));
        } else {
            flows.forEach((device, flow) -> printFlows(device, flow, coreService));
        }
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
     * @return sorted device list
     */
    protected SortedMap<Device, List<FlowEntry>> getSortedFlows(DeviceService deviceService,
                                                          FlowRuleService service) {
        SortedMap<Device, List<FlowEntry>> flows = new TreeMap<>(Comparators.ELEMENT_COMPARATOR);
        List<FlowEntry> rules;
        FlowEntryState s = null;
        if (state != null && !state.equals("any")) {
            s = FlowEntryState.valueOf(state.toUpperCase());
        }
        Iterable<Device> devices = null;
        if (uri == null) {
            devices = deviceService.getDevices();
        } else {
            Device dev = deviceService.getDevice(DeviceId.deviceId(uri));
            devices = (dev == null) ? deviceService.getDevices()
                                    : Collections.singletonList(dev);
        }
        for (Device d : devices) {
            if (s == null) {
                rules = newArrayList(service.getFlowEntries(d.id()));
            } else {
                rules = newArrayList();
                for (FlowEntry f : service.getFlowEntries(d.id())) {
                    if (f.state().equals(s)) {
                        rules.add(f);
                    }
                }
            }
            rules.sort(Comparators.FLOW_RULE_COMPARATOR);
            flows.put(d, rules);
        }
        return flows;
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
        boolean empty = flows == null || flows.isEmpty();
        print("deviceId=%s, flowRuleCount=%d", d.id(), empty ? 0 : flows.size());
        if (!empty) {
            for (FlowEntry f : flows) {
                ApplicationId appId = coreService.getAppId(f.appId());
                print(FMT, Long.toHexString(f.id().value()), f.state(),
                      f.bytes(), f.packets(), f.life(), f.priority(), f.tableId(),
                      appId != null ? appId.name() : "<none>",
                      f.payLoad() == null ? null : f.payLoad().payLoad().toString());
                print(SFMT, f.selector().criteria());
                print(TFMT, f.treatment());
            }
        }
    }

}
