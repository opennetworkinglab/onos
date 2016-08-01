/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.drivers.lumentum;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.tuple.Pair;
import org.onosproject.net.ChannelSpacing;
import org.onosproject.net.GridType;
import org.onosproject.net.OchSignal;
import org.onosproject.net.OchSignalType;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.flow.DefaultFlowEntry;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowId;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleProgrammable;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criteria;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snmp4j.PDU;
import org.snmp4j.event.ResponseEvent;
import org.snmp4j.smi.Integer32;
import org.snmp4j.smi.OID;
import org.snmp4j.smi.UnsignedInteger32;
import org.snmp4j.smi.VariableBinding;
import org.snmp4j.util.TreeEvent;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkArgument;

// TODO: need to convert between OChSignal and XC channel number
public class LumentumFlowRuleProgrammable extends AbstractHandlerBehaviour implements FlowRuleProgrammable {

    private static final Logger log =
            LoggerFactory.getLogger(LumentumFlowRuleProgrammable.class);

    // Default values
    private static final int DEFAULT_TARGET_GAIN_PREAMP = 150;
    private static final int DEFAULT_TARGET_GAIN_BOOSTER = 200;
    private static final int DISABLE_CHANNEL_TARGET_POWER = -650;
    private static final int DEFAULT_CHANNEL_TARGET_POWER = -30;
    private static final int DISABLE_CHANNEL_ABSOLUTE_ATTENUATION = 160;
    private static final int DEFAULT_CHANNEL_ABSOLUTE_ATTENUATION = 50;
    private static final int DISABLE_CHANNEL_ADD_DROP_PORT_INDEX = 1;
    private static final int OUT_OF_SERVICE = 1;
    private static final int IN_SERVICE = 2;
    private static final int OPEN_LOOP = 1;
    private static final int CLOSED_LOOP = 2;
    // First 20 ports are add/mux ports, next 20 are drop/demux
    private static final int DROP_PORT_OFFSET = 20;

    // OIDs
    private static final String CTRL_AMP_MODULE_SERVICE_STATE_PREAMP = ".1.3.6.1.4.1.46184.1.4.4.1.2.1";
    private static final String CTRL_AMP_MODULE_SERVICE_STATE_BOOSTER = ".1.3.6.1.4.1.46184.1.4.4.1.2.2";
    private static final String CTRL_AMP_MODULE_TARGET_GAIN_PREAMP = ".1.3.6.1.4.1.46184.1.4.4.1.8.1";
    private static final String CTRL_AMP_MODULE_TARGET_GAIN_BOOSTER = ".1.3.6.1.4.1.46184.1.4.4.1.8.2";
    private static final String CTRL_CHANNEL_STATE = ".1.3.6.1.4.1.46184.1.4.2.1.3.";
    private static final String CTRL_CHANNEL_MODE = ".1.3.6.1.4.1.46184.1.4.2.1.4.";
    private static final String CTRL_CHANNEL_TARGET_POWER = ".1.3.6.1.4.1.46184.1.4.2.1.6.";
    private static final String CTRL_CHANNEL_ADD_DROP_PORT_INDEX = ".1.3.6.1.4.1.46184.1.4.2.1.13.";
    private static final String CTRL_CHANNEL_ABSOLUTE_ATTENUATION = ".1.3.6.1.4.1.46184.1.4.2.1.5.";

    private LumentumSnmpDevice snmp;

    @Override
    public Collection<FlowEntry> getFlowEntries() {
        try {
            snmp = new LumentumSnmpDevice(handler().data().deviceId());
        } catch (IOException e) {
            log.error("Failed to connect to device: ", e);
            return Collections.emptyList();
        }

        // Line in is last but one port, line out is last
        DeviceService deviceService = this.handler().get(DeviceService.class);
        List<Port> ports = deviceService.getPorts(data().deviceId());
        if (ports.size() < 2) {
            return Collections.emptyList();
        }
        PortNumber lineIn = ports.get(ports.size() - 2).number();
        PortNumber lineOut = ports.get(ports.size() - 1).number();

        Collection<FlowEntry> entries = Lists.newLinkedList();

        // Add rules
        OID addOid = new OID(CTRL_CHANNEL_STATE + "1");
        entries.addAll(
                fetchRules(addOid, true, lineOut).stream()
                        .map(fr -> new DefaultFlowEntry(fr, FlowEntry.FlowEntryState.ADDED, 0, 0, 0))
                        .collect(Collectors.toList())
        );

        // Drop rules
        OID dropOid = new OID(CTRL_CHANNEL_STATE + "2");
        entries.addAll(
                fetchRules(dropOid, false, lineIn).stream()
                        .map(fr -> new DefaultFlowEntry(fr, FlowEntry.FlowEntryState.ADDED, 0, 0, 0))
                        .collect(Collectors.toList())
        );

        return entries;
    }

    @Override
    public Collection<FlowRule> applyFlowRules(Collection<FlowRule> rules) {
        try {
            snmp = new LumentumSnmpDevice(data().deviceId());
        } catch (IOException e) {
            log.error("Failed to connect to device: ", e);
        }

        // Line ports
        DeviceService deviceService = this.handler().get(DeviceService.class);
        List<Port> ports = deviceService.getPorts(data().deviceId());
        List<PortNumber> linePorts = ports.subList(ports.size() - 2, ports.size()).stream()
                .map(p -> p.number())
                .collect(Collectors.toList());

        // Apply the valid rules on the device
        Collection<FlowRule> added = rules.stream()
                .map(r -> new CrossConnectFlowRule(r, linePorts))
                .filter(xc -> installCrossConnect(xc))
                .collect(Collectors.toList());

        // Cache the cookie/priority
        CrossConnectCache cache = this.handler().get(CrossConnectCache.class);
        added.forEach(xc -> cache.set(
                Objects.hash(data().deviceId(), xc.selector(), xc.treatment()),
                xc.id(),
                xc.priority()));

        return added;
    }

    @Override
    public Collection<FlowRule> removeFlowRules(Collection<FlowRule> rules) {
        try {
            snmp = new LumentumSnmpDevice(data().deviceId());
        } catch (IOException e) {
            log.error("Failed to connect to device: ", e);
        }

        // Line ports
        DeviceService deviceService = this.handler().get(DeviceService.class);
        List<Port> ports = deviceService.getPorts(data().deviceId());
        List<PortNumber> linePorts = ports.subList(ports.size() - 2, ports.size()).stream()
                .map(p -> p.number())
                .collect(Collectors.toList());

        // Apply the valid rules on the device
        Collection<FlowRule> removed = rules.stream()
                .map(r -> new CrossConnectFlowRule(r, linePorts))
                .filter(xc -> removeCrossConnect(xc))
                .collect(Collectors.toList());

        // Remove flow rule from cache
        CrossConnectCache cache = this.handler().get(CrossConnectCache.class);
        removed.forEach(xc -> cache.remove(
                Objects.hash(data().deviceId(), xc.selector(), xc.treatment())));

        return removed;
    }

    // Installs cross connect on device
    private boolean installCrossConnect(CrossConnectFlowRule xc) {

        int channel = toChannel(xc.ochSignal());
        long addDrop = xc.addDrop().toLong();
        if (!xc.isAddRule()) {
            addDrop -= DROP_PORT_OFFSET;
        }

        // Create the PDU object
        PDU pdu = new PDU();
        pdu.setType(PDU.SET);

        // Enable preamp & booster
        List<OID> oids = Arrays.asList(new OID(CTRL_AMP_MODULE_SERVICE_STATE_PREAMP),
                new OID(CTRL_AMP_MODULE_SERVICE_STATE_BOOSTER));
        oids.forEach(
                oid -> pdu.add(new VariableBinding(oid, new Integer32(IN_SERVICE)))
        );

        // Set target gain on preamp & booster
        OID ctrlAmpModuleTargetGainPreamp = new OID(CTRL_AMP_MODULE_TARGET_GAIN_PREAMP);
        pdu.add(new VariableBinding(ctrlAmpModuleTargetGainPreamp, new Integer32(DEFAULT_TARGET_GAIN_PREAMP)));
        OID ctrlAmpModuleTargetGainBooster = new OID(CTRL_AMP_MODULE_TARGET_GAIN_BOOSTER);
        pdu.add(new VariableBinding(ctrlAmpModuleTargetGainBooster, new Integer32(DEFAULT_TARGET_GAIN_BOOSTER)));

        // Make cross connect
        OID ctrlChannelAddDropPortIndex = new OID(CTRL_CHANNEL_ADD_DROP_PORT_INDEX +
                (xc.isAddRule() ? "1." : "2.") + channel);
        pdu.add(new VariableBinding(ctrlChannelAddDropPortIndex, new UnsignedInteger32(addDrop)));

        // Add rules use closed loop, drop rules open loop
        // Add rules are set to target power, drop rules are attenuated
        if (xc.isAddRule()) {
            OID ctrlChannelMode = new OID(CTRL_CHANNEL_MODE + "1." + channel);
            pdu.add(new VariableBinding(ctrlChannelMode, new Integer32(CLOSED_LOOP)));

            OID ctrlChannelTargetPower = new OID(CTRL_CHANNEL_TARGET_POWER + "1." + channel);
            pdu.add(new VariableBinding(ctrlChannelTargetPower, new Integer32(DEFAULT_CHANNEL_TARGET_POWER)));
        } else {
            OID ctrlChannelMode = new OID(CTRL_CHANNEL_MODE + "2." + channel);
            pdu.add(new VariableBinding(ctrlChannelMode, new Integer32(OPEN_LOOP)));

            OID ctrlChannelAbsoluteAttenuation = new OID(CTRL_CHANNEL_ABSOLUTE_ATTENUATION + "2." + channel);
            pdu.add(new VariableBinding(
                    ctrlChannelAbsoluteAttenuation, new UnsignedInteger32(DEFAULT_CHANNEL_ABSOLUTE_ATTENUATION)));
        }

        // Final step is to enable the channel
        OID ctrlChannelState = new OID(CTRL_CHANNEL_STATE + (xc.isAddRule() ? "1." : "2.") + channel);
        pdu.add(new VariableBinding(ctrlChannelState, new Integer32(IN_SERVICE)));

        try {
            ResponseEvent response = snmp.set(pdu);

             // TODO: parse response
        } catch (IOException e) {
            log.error("Failed to create cross connect, unable to connect to device: ", e);
        }

        return true;
    }

    // Removes cross connect on device
    private boolean removeCrossConnect(CrossConnectFlowRule xc) {

        int channel = toChannel(xc.ochSignal());

        // Create the PDU object
        PDU pdu = new PDU();
        pdu.setType(PDU.SET);

        // Disable the channel
        OID ctrlChannelState = new OID(CTRL_CHANNEL_STATE + (xc.isAddRule() ? "1." : "2.") + channel);
        pdu.add(new VariableBinding(ctrlChannelState, new Integer32(OUT_OF_SERVICE)));

        // Put cross connect back into default port 1
        OID ctrlChannelAddDropPortIndex = new OID(CTRL_CHANNEL_ADD_DROP_PORT_INDEX +
                (xc.isAddRule() ? "1." : "2.") + channel);
        pdu.add(new VariableBinding(ctrlChannelAddDropPortIndex,
                new UnsignedInteger32(DISABLE_CHANNEL_ADD_DROP_PORT_INDEX)));

        // Put port/channel back to open loop
        OID ctrlChannelMode = new OID(CTRL_CHANNEL_MODE + (xc.isAddRule() ? "1." : "2.") + channel);
        pdu.add(new VariableBinding(ctrlChannelMode, new Integer32(OPEN_LOOP)));

        // Add rules are set to target power, drop rules are attenuated
        if (xc.isAddRule()) {
            OID ctrlChannelTargetPower = new OID(CTRL_CHANNEL_TARGET_POWER + "1." + channel);
            pdu.add(new VariableBinding(ctrlChannelTargetPower, new Integer32(DISABLE_CHANNEL_TARGET_POWER)));
        } else {
            OID ctrlChannelAbsoluteAttenuation = new OID(CTRL_CHANNEL_ABSOLUTE_ATTENUATION + "2." + channel);
            pdu.add(new VariableBinding(
                    ctrlChannelAbsoluteAttenuation, new UnsignedInteger32(DISABLE_CHANNEL_ABSOLUTE_ATTENUATION)));
        }

        try {
            ResponseEvent response = snmp.set(pdu);

            // TODO: parse response
        } catch (IOException e) {
            log.error("Failed to remove cross connect, unable to connect to device: ", e);
            return false;
        }

        return true;
    }

    /**
     * Convert OCh signal to Lumentum channel ID.
     *
     * @param ochSignal OCh signal
     * @return Lumentum channel ID
     */
    public static int toChannel(OchSignal ochSignal) {
        // FIXME: move to cross connect validation
        checkArgument(ochSignal.channelSpacing() == ChannelSpacing.CHL_50GHZ);
        checkArgument(LumentumSnmpDevice.START_CENTER_FREQ.compareTo(ochSignal.centralFrequency()) <= 0);
        checkArgument(LumentumSnmpDevice.END_CENTER_FREQ.compareTo(ochSignal.centralFrequency()) >= 0);

        return ochSignal.spacingMultiplier() + LumentumSnmpDevice.MULTIPLIER_SHIFT;
    }

    /**
     * Convert Lumentum channel ID to OCh signal.
     *
     * @param channel Lumentum channel ID
     * @return OCh signal
     */
    public static OchSignal toOchSignal(int channel) {
        checkArgument(1 <= channel);
        checkArgument(channel <= 96);

        return new OchSignal(GridType.DWDM, ChannelSpacing.CHL_50GHZ,
                channel - LumentumSnmpDevice.MULTIPLIER_SHIFT, 4);
    }

    // Returns the currently configured add/drop port for the given channel.
    private PortNumber getAddDropPort(int channel, boolean isAddPort) {
        OID oid = new OID(CTRL_CHANNEL_ADD_DROP_PORT_INDEX + (isAddPort ? "1" : "2"));

        for (TreeEvent event : snmp.get(oid)) {
            if (event == null) {
                return null;
            }

            VariableBinding[] varBindings = event.getVariableBindings();

            for (VariableBinding varBinding : varBindings) {
                if (varBinding.getOid().last() == channel) {
                    int port = varBinding.getVariable().toInt();
                    if (!isAddPort) {
                        port += DROP_PORT_OFFSET;
                    }
                    return PortNumber.portNumber(port);

                }
            }

        }

        return null;
    }

    // Returns the currently installed flow entries on the device.
    private List<FlowRule> fetchRules(OID oid, boolean isAdd, PortNumber linePort) {
        List<FlowRule> rules = new LinkedList<>();

        for (TreeEvent event : snmp.get(oid)) {
            if (event == null) {
                continue;
            }

            VariableBinding[] varBindings = event.getVariableBindings();
            for (VariableBinding varBinding : varBindings) {
                CrossConnectCache cache = this.handler().get(CrossConnectCache.class);

                if (varBinding.getVariable().toInt() == IN_SERVICE) {
                    int channel = varBinding.getOid().removeLast();

                    PortNumber addDropPort = getAddDropPort(channel, isAdd);
                    if (addDropPort == null) {
                        continue;
                    }

                    TrafficSelector selector = DefaultTrafficSelector.builder()
                            .matchInPort(isAdd ? addDropPort : linePort)
                            .add(Criteria.matchOchSignalType(OchSignalType.FIXED_GRID))
                            .add(Criteria.matchLambda(toOchSignal(channel)))
                            .build();
                    TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                            .setOutput(isAdd ? linePort : addDropPort)
                            .build();

                    // Lookup flow ID and priority
                    int hash = Objects.hash(data().deviceId(), selector, treatment);
                    Pair<FlowId, Integer> lookup = cache.get(hash);
                    if (lookup == null) {
                        continue;
                    }

                    FlowRule fr = DefaultFlowRule.builder()
                            .forDevice(data().deviceId())
                            .makePermanent()
                            .withSelector(selector)
                            .withTreatment(treatment)
                            .withPriority(lookup.getRight())
                            .withCookie(lookup.getLeft().value())
                            .build();
                    rules.add(fr);
                }
            }
        }

        return rules;
    }
}
