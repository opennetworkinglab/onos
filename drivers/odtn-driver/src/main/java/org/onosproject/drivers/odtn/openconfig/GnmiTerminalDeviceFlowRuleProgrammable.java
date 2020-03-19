/*
 * Copyright 2020-present Open Networking Foundation
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

package org.onosproject.drivers.odtn.openconfig;

import com.google.common.collect.ImmutableList;
import gnmi.Gnmi;
import org.onlab.util.Frequency;
import org.onosproject.drivers.odtn.impl.DeviceConnectionCache;
import org.onosproject.drivers.odtn.impl.FlowRuleParser;
import org.onosproject.gnmi.api.GnmiClient;
import org.onosproject.gnmi.api.GnmiController;
import org.onosproject.gnmi.api.GnmiUtils.GnmiPathBuilder;
import org.onosproject.grpc.utils.AbstractGrpcHandlerBehaviour;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.DefaultFlowEntry;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleProgrammable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static org.onosproject.odtn.behaviour.OdtnDeviceDescriptionDiscovery.OC_NAME;

/**
 * A FlowRuleProgrammable behavior which converts flow rules to gNMI calls that sets
 * frequency for the optical component.
 */
public class GnmiTerminalDeviceFlowRuleProgrammable
        extends AbstractGrpcHandlerBehaviour<GnmiClient, GnmiController>
        implements FlowRuleProgrammable {

    private static final Logger log =
            LoggerFactory.getLogger(GnmiTerminalDeviceFlowRuleProgrammable.class);

    public GnmiTerminalDeviceFlowRuleProgrammable() {
        super(GnmiController.class);
    }

    @Override
    public Collection<FlowEntry> getFlowEntries() {
        // TODO: currently, we store flow rules in a cluster store. Should check if rule/config exists via gNMI.
        if (!setupBehaviour("getFlowEntries")) {
            return Collections.emptyList();
        }
        DeviceConnectionCache cache = getConnectionCache();
        Set<FlowRule> cachedRules = cache.get(deviceId);
        if (cachedRules == null) {
            return ImmutableList.of();
        }

        return cachedRules.stream()
                .filter(Objects::nonNull)
                .map(r -> new DefaultFlowEntry(r, FlowEntry.FlowEntryState.ADDED, 0, 0, 0))
                .collect(Collectors.toList());
    }

    @Override
    public Collection<FlowRule> applyFlowRules(Collection<FlowRule> rules) {
        if (!setupBehaviour("applyFlowRules")) {
            return Collections.emptyList();
        }
        List<FlowRule> added = new ArrayList<>();
        for (FlowRule r : rules) {
            String connectionId = applyFlowRule(r);
            if (connectionId != null) {
                getConnectionCache().add(deviceId, connectionId, r);
                added.add(r);
            }
        }
        return added;
    }

    @Override
    public Collection<FlowRule> removeFlowRules(Collection<FlowRule> rules) {
        if (!setupBehaviour("removeFlowRules")) {
            return Collections.emptyList();
        }
        List<FlowRule> removed = new ArrayList<>();
        for (FlowRule r : rules) {
            String connectionId = removeFlowRule(r);
            if (connectionId != null) {
                getConnectionCache().remove(deviceId, connectionId);
                removed.add(r);
            }
        }
        return removed;
    }

    private String applyFlowRule(FlowRule r) {
        FlowRuleParser frp = new FlowRuleParser(r);
        if (!frp.isReceiver()) {
            String opticalPortName = getOpticalPortName(frp.getPortNumber());
            if (opticalPortName == null) {
                log.warn("[Apply] No optical port name found from port {}, skipped",
                        frp.getPortNumber());
                return null;
            }
            if (!setOpticalPortFrequency(opticalPortName, frp.getCentralFrequency())) {
                // Already logged in setOpticalChannelFrequency function
                return null;
            }
            return opticalPortName + ":" + frp.getCentralFrequency().asGHz();
        }
        return String.valueOf(frp.getCentralFrequency().asGHz());

    }

    private String removeFlowRule(FlowRule r) {
        FlowRuleParser frp = new FlowRuleParser(r);
        if (!frp.isReceiver()) {
            String opticalPortName = getOpticalPortName(frp.getPortNumber());
            if (opticalPortName == null) {
                log.warn("[Remove] No optical port name found from port {}, skipped",
                         frp.getPortNumber());
                return null;
            }
            if (!setOpticalPortFrequency(opticalPortName, Frequency.ofMHz(0))) {
                // Already logged in setOpticalChannelFrequency function
                return null;
            }
            return opticalPortName + ":" + frp.getCentralFrequency().asGHz();
        }
        return String.valueOf(frp.getCentralFrequency().asGHz());
    }

    private boolean setOpticalPortFrequency(String opticalPortName, Frequency freq) {
        // gNMI set
        // /components/component[name=opticalPortName]/optical-channel/config/frequency
        Gnmi.Path path = GnmiPathBuilder.newBuilder()
                .addElem("components")
                .addElem("component").withKeyValue("name", opticalPortName)
                .addElem("optical-channel")
                .addElem("config")
                .addElem("frequency")
                .build();
        Gnmi.TypedValue val = Gnmi.TypedValue.newBuilder()
                .setUintVal((long) freq.asMHz())
                .build();
        Gnmi.Update update = Gnmi.Update.newBuilder()
                .setPath(path)
                .setVal(val)
                .build();
        Gnmi.SetRequest req = Gnmi.SetRequest.newBuilder()
                .addUpdate(update)
                .build();
        try {
            client.set(req).get();
            return true;
        } catch (ExecutionException | InterruptedException e) {
            log.warn("Got exception when performing gNMI set operation: {}", e.getMessage());
            log.warn("{}", req);
        }
        return false;
    }

    private String getOpticalPortName(PortNumber portNumber) {
        Port clientPort = handler().get(DeviceService.class).getPort(deviceId, portNumber);
        if (clientPort == null) {
            log.warn("Unable to get port from device {}, port {}", deviceId, portNumber);
            return null;
        }
        return clientPort.annotations().value(OC_NAME);
    }

    private DeviceConnectionCache getConnectionCache() {
        return DeviceConnectionCache.init();
    }
}
