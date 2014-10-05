package org.onlab.onos.cli.net;

import static com.google.common.collect.Lists.newArrayList;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onlab.onos.cli.AbstractShellCommand;
import org.onlab.onos.cli.Comparators;
import org.onlab.onos.net.Device;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.device.DeviceService;
import org.onlab.onos.net.flow.FlowEntry;
import org.onlab.onos.net.flow.FlowEntry.FlowEntryState;
import org.onlab.onos.net.flow.FlowRuleService;

import com.google.common.collect.Maps;

/**
 * Lists all currently-known hosts.
 */
@Command(scope = "onos", name = "flows",
description = "Lists all currently-known flows.")
public class FlowsListCommand extends AbstractShellCommand {

    public static final String ANY = "any";

    private static final String FMT =
            "   id=%s, state=%s, bytes=%s, packets=%s, duration=%s, priority=%s";
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
        DeviceService deviceService = get(DeviceService.class);
        FlowRuleService service = get(FlowRuleService.class);
        Map<Device, List<FlowEntry>> flows = getSortedFlows(deviceService, service);
        for (Device d : flows.keySet()) {
            printFlows(d, flows.get(d));
        }
    }

    /**
     * Returns the list of devices sorted using the device ID URIs.
     *
     * @param service device service
     * @return sorted device list
     */
    protected Map<Device, List<FlowEntry>> getSortedFlows(DeviceService deviceService, FlowRuleService service) {
        Map<Device, List<FlowEntry>> flows = Maps.newHashMap();
        List<FlowEntry> rules;
        FlowEntryState s = null;
        if (state != null && !state.equals("any")) {
            s = FlowEntryState.valueOf(state.toUpperCase());
        }
        Iterable<Device> devices = uri == null ?  deviceService.getDevices() :
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
     * @param d the device
     * @param flows the set of flows for that device.
     */
    protected void printFlows(Device d, List<FlowEntry> flows) {
        print("Device: " + d.id());
        if (flows == null | flows.isEmpty()) {
            print(" %s", "No flows.");
            return;
        }
        for (FlowEntry f : flows) {
            print(FMT, Long.toHexString(f.id().value()), f.state(), f.bytes(),
                    f.packets(), f.life(), f.priority());
            print(SFMT, f.selector().criteria());
            print(TFMT, f.treatment().instructions());
        }

    }

}
