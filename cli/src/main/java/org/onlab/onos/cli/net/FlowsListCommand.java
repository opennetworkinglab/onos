package org.onlab.onos.cli.net;

import static com.google.common.collect.Lists.newArrayList;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.apache.karaf.shell.commands.Command;
import org.onlab.onos.cli.AbstractShellCommand;
import org.onlab.onos.net.Device;
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
            "   id=%s, selector=%s, treatment=%s, state=%s";

    protected static final Comparator<FlowRule> ID_COMPARATOR = new Comparator<FlowRule>() {
        @Override
        public int compare(FlowRule f1, FlowRule f2) {
            return Long.valueOf(f1.id().value()).compareTo(f2.id().value());
        }
    };

    @Override
    protected void execute() {
        DeviceService deviceService = getService(DeviceService.class);
        FlowRuleService service = getService(FlowRuleService.class);
        Map<Device, List<FlowRule>> flows = getSortedFlows(deviceService, service);
        for (Device d : deviceService.getDevices()) {
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
        List<FlowRule> rules;
        for (Device d : deviceService.getDevices()) {
            rules = newArrayList(service.getFlowEntries(d.id()));
            Collections.sort(rules, ID_COMPARATOR);
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
        for (FlowRule f : flows) {
            print(FMT, f.id().value(), f.selector(), f.treatment(), f.state());
        }

    }

}