package org.onlab.onos.cli.net;

import static com.google.common.collect.Lists.newArrayList;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onlab.onos.cli.AbstractShellCommand;
import org.onlab.onos.net.Device;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.device.DeviceService;
import org.onlab.onos.net.flow.FlowRule;
import org.onlab.onos.net.flow.FlowRuleService;

import com.google.common.collect.Maps;

/**
 * Lists all currently-known hosts.
 */
@Command(scope = "onos", name = "flows",
description = "Lists all currently-known flows.")
public class FlowsListCommand extends AbstractShellCommand {

    private static final String FMT =
            "   id=%s, state=%s, bytes=%s, packets=%s, duration=%s, priority=%s";
    private static final String TFMT = "      treatment=%s";
    private static final String SFMT = "      selector=%s";

    @Argument(index = 0, name = "uri", description = "Device ID",
            required = false, multiValued = false)
    String uri = null;

    @Override
    protected void execute() {
        DeviceService deviceService = get(DeviceService.class);
        FlowRuleService service = get(FlowRuleService.class);
        Map<Device, List<FlowRule>> flows = getSortedFlows(deviceService, service);
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
    protected Map<Device, List<FlowRule>> getSortedFlows(DeviceService deviceService, FlowRuleService service) {
        Map<Device, List<FlowRule>> flows = Maps.newHashMap();
        List<FlowRule> rules = newArrayList();
        Iterable<Device> devices = uri == null ?  deviceService.getDevices() :
            Collections.singletonList(deviceService.getDevice(DeviceId.deviceId(uri)));
        for (Device d : devices) {
            rules = newArrayList(service.getFlowEntries(d.id()));
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
    protected void printFlows(Device d, List<FlowRule> flows) {
        print("Device: " + d.id());
        if (flows == null | flows.isEmpty()) {
            print(" %s", "No flows installed.");
            return;
        }
        for (FlowRule f : flows) {
            print(FMT, Long.toHexString(f.id().value()), f.state(), f.bytes(),
                    f.packets(), f.lifeMillis(), f.priority());
            print(SFMT, f.selector().criteria());
            print(TFMT, f.treatment().instructions());
        }

    }

}