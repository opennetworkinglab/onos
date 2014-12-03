/*
 * Copyright 2014 Open Networking Laboratory
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
import com.google.common.collect.Maps;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.core.CoreService;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.cli.Comparators;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowEntry.FlowEntryState;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.instructions.Instruction;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Lists.newArrayList;
import static org.onosproject.cli.net.DevicesListCommand.getSortedDevices;

/**
 * Lists all currently-known hosts.
 */
@Command(scope = "onos", name = "flows",
         description = "Lists all currently-known flows.")
public class FlowsListCommand extends AbstractShellCommand {

    public static final String ANY = "any";

    private static final String FMT =
            "   id=%s, state=%s, bytes=%s, packets=%s, duration=%s, priority=%s, appId=%s";
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
        Map<Device, List<FlowEntry>> flows = getSortedFlows(deviceService, service);

        if (outputJson()) {
            print("%s", json(coreService, getSortedDevices(deviceService), flows));
        } else {
            for (Device d : getSortedDevices(deviceService)) {
                printFlows(d, flows.get(d), coreService);
            }
        }
    }

    /**
     * Produces a JSON array of flows grouped by the each device.
     *
     * @param coreService core service
     * @param devices     collection of devices to group flow by
     * @param flows       collection of flows per each device
     * @return JSON array
     */
    private JsonNode json(CoreService coreService, Iterable<Device> devices,
                          Map<Device, List<FlowEntry>> flows) {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode result = mapper.createArrayNode();
        for (Device device : devices) {
            result.add(json(coreService, mapper, device, flows.get(device)));
        }
        return result;
    }

    // Produces JSON object with the flows of the given device.
    private ObjectNode json(CoreService coreService, ObjectMapper mapper,
                            Device device, List<FlowEntry> flows) {
        ObjectNode result = mapper.createObjectNode();
        ArrayNode array = mapper.createArrayNode();

        for (FlowEntry flow : flows) {
            array.add(json(coreService, mapper, flow));
        }

        result.put("device", device.id().toString())
                .put("flowCount", flows.size())
                .set("flows", array);
        return result;
    }

    // Produces JSON structure with the specified flow data.
    private ObjectNode json(CoreService coreService, ObjectMapper mapper,
                            FlowEntry flow) {
        ObjectNode result = mapper.createObjectNode();
        ArrayNode crit = mapper.createArrayNode();
        for (Criterion c : flow.selector().criteria()) {
            crit.add(c.toString());
        }

        ArrayNode instr = mapper.createArrayNode();
        for (Instruction i : flow.treatment().instructions()) {
            instr.add(i.toString());
        }

        result.put("flowId", Long.toHexString(flow.id().value()))
                .put("state", flow.state().toString())
                .put("bytes", flow.bytes())
                .put("packets", flow.packets())
                .put("life", flow.life())
                .put("appId", coreService.getAppId(flow.appId()).name());
        result.set("selector", crit);
        result.set("treatment", instr);
        return result;
    }

    /**
     * Returns the list of devices sorted using the device ID URIs.
     *
     * @param deviceService device service
     * @param service flow rule service
     * @return sorted device list
     */
    protected Map<Device, List<FlowEntry>> getSortedFlows(DeviceService deviceService,
                                                          FlowRuleService service) {
        Map<Device, List<FlowEntry>> flows = Maps.newHashMap();
        List<FlowEntry> rules;
        FlowEntryState s = null;
        if (state != null && !state.equals("any")) {
            s = FlowEntryState.valueOf(state.toUpperCase());
        }
        Iterable<Device> devices = uri == null ? deviceService.getDevices() :
                Collections.singletonList(deviceService.getDevice(DeviceId.deviceId(uri)));
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
            Collections.sort(rules, Comparators.FLOW_RULE_COMPARATOR);
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
                print(FMT, Long.toHexString(f.id().value()), f.state(),
                      f.bytes(), f.packets(), f.life(), f.priority(),
                      coreService.getAppId(f.appId()).name());
                print(SFMT, f.selector().criteria());
                print(TFMT, f.treatment().instructions());
            }
        }
    }

}
