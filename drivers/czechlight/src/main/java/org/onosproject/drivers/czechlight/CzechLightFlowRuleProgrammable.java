/*
 * Copyright 2019-2020 Jan Kundrát, CESNET, <jan.kundrat@cesnet.cz> and Open Networking Foundation
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

package org.onosproject.drivers.czechlight;

import org.apache.commons.configuration.HierarchicalConfiguration;
import org.onlab.util.Spectrum;
import org.onosproject.core.CoreService;
import org.onosproject.drivers.odtn.impl.DeviceConnectionCache;
import org.onosproject.net.ChannelSpacing;
import org.onosproject.net.GridType;
import org.onosproject.net.Lambda;
import org.onosproject.net.OchSignal;
import org.onosproject.net.OchSignalType;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.flow.DefaultFlowEntry;
import org.onosproject.net.flow.DefaultFlowRule;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleProgrammable;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.criteria.Criteria;
import org.onosproject.net.flow.criteria.OchSignalCriterion;
import org.onosproject.net.flow.criteria.PortCriterion;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.flow.instructions.L0ModificationInstruction;
import org.onosproject.netconf.DatastoreId;
import org.onosproject.netconf.NetconfController;
import org.onosproject.netconf.NetconfException;
import org.onosproject.netconf.NetconfSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

/** Modification of the MC by a ROADM device.
 * The signal might be either attenuated by a specified amount of dB, or its target power can be set
 * to a specified power in dBm.
 */
class MCManipulation {
    Double attenuation;
    Double targetPower;

    public MCManipulation(final Double attenuation, final Double targetPower) {
        this.attenuation = attenuation;
        this.targetPower = targetPower;
    }

    public String toString() {
        if (attenuation != null) {
            return "attenuation: " + String.valueOf(attenuation);
        }

        if (targetPower != null) {
            return "targetPower: " + String.valueOf(targetPower);
        }

        return "none";
    }
};

/** Representation of a ROADM configuration for a given Media Channel.
 * This contains frequency (`channel`), routing (`leafPort`) and attenuation or power set point (`manipulation`).
 * */
class CzechLightRouting {
    MediaChannelDefinition channel;
    int leafPort;
    MCManipulation manipulation;

    public CzechLightRouting(final MediaChannelDefinition channel, final int leafPort, final MCManipulation manip) {
        this.channel = channel;
        this.leafPort = leafPort;
        this.manipulation = manip;
    }

    public String toString() {
        return channel.toString() + " -> " + String.valueOf(leafPort) + " (" + manipulation.toString() + ")";
    }
};

/**
 * Implementation of FlowRuleProgrammable interface for CzechLight SDN ROADMs.
 */
public class CzechLightFlowRuleProgrammable extends AbstractHandlerBehaviour implements FlowRuleProgrammable {

    private final Logger log =
            LoggerFactory.getLogger(getClass());

    private static final String NETCONF_OP_MERGE = "merge";
    private static final String NETCONF_OP_NONE = "none";
    private static final String ELEMENT_ADD = "add";
    private static final String ELEMENT_DROP = "drop";

    private enum Direction {
        ADD,
        DROP,
    };
    // FIXME: can we get this programmaticaly?
    private static final String DEFAULT_APP = "org.onosproject.drivers.czechlight";

    @Override
    public Collection<FlowEntry> getFlowEntries() {
        if (deviceType() == CzechLightDiscovery.DeviceType.INLINE_AMP
                || deviceType() == CzechLightDiscovery.DeviceType.COHERENT_ADD_DROP) {
            final var data = getConnectionCache().get(data().deviceId());
            if (data == null) {
                return new ArrayList<>();
            }
            return data.stream()
                    .map(rule -> new DefaultFlowEntry(rule))
                    .collect(Collectors.toList());
        }

        HierarchicalConfiguration xml;
        try {
            xml = doGetSubtree(CzechLightDiscovery.CHANNEL_DEFS_FILTER + CzechLightDiscovery.MC_ROUTING_FILTER);
        } catch (NetconfException e) {
            log.error("Cannot read data from NETCONF: {}", e);
            return new ArrayList<>();
        }
        final var allChannels = MediaChannelDefinition.parseChannelDefinitions(xml);

        Collection<FlowEntry> list = new ArrayList<>();

        final var allMCs = xml.configurationsAt("data.media-channels");
        allMCs.stream()
                .map(cfg -> confToMCRouting(ELEMENT_ADD, allChannels, cfg))
                .filter(Objects::nonNull)
                .forEach(flow -> {
                    log.debug("{}: found ADD: {}", data().deviceId(), flow.toString());
                    list.add(new DefaultFlowEntry(asFlowRule(Direction.ADD, flow), FlowEntry.FlowEntryState.ADDED));
                });
        allMCs.stream()
                .map(cfg -> confToMCRouting(ELEMENT_DROP, allChannels, cfg))
                .filter(Objects::nonNull)
                .forEach(flow -> {
                    log.debug("{}: found DROP: {}", data().deviceId(), flow.toString());
                    list.add(new DefaultFlowEntry(asFlowRule(Direction.DROP, flow), FlowEntry.FlowEntryState.ADDED));
                });
        return list;
    }

    @Override
    public Collection<FlowRule> applyFlowRules(Collection<FlowRule> rules) {
        if (deviceType() == CzechLightDiscovery.DeviceType.INLINE_AMP
                || deviceType() == CzechLightDiscovery.DeviceType.COHERENT_ADD_DROP) {
            rules.forEach(
                    rule -> {
                        log.debug("{}: asked for {} (whole C-band is always forwarded by the HW)",
                                data().deviceId(), rule);
                        getConnectionCache().add(data().deviceId(), rule.toString(), rule);
                    }
            );
            return rules;
        }

        HierarchicalConfiguration xml;
        try {
            xml = doGetSubtree(CzechLightDiscovery.CHANNEL_DEFS_FILTER + CzechLightDiscovery.MC_ROUTING_FILTER);
        } catch (NetconfException e) {
            log.error("Cannot read data from NETCONF: {}", e);
            return new ArrayList<>();
        }
        final var allChannels = MediaChannelDefinition.parseChannelDefinitions(xml);
        var hopefullyAdded = new ArrayList<FlowRule>();

        // temporary store because both ADD and DROP must go into the same <media-channel> list item
        var changes = new TreeMap<String, String>();
        rules.forEach(
                rule -> {
                    log.debug("{}: asked to INSERT rule for:", data().deviceId());
                    rule.selector().criteria().forEach(
                            criteria -> log.debug("  criteria {}", criteria.toString())
                    );
                    rule.treatment().allInstructions().forEach(
                            instruction -> log.debug("  instruction {}", instruction.toString())
                    );

                    String element;
                    long leafPort;
                    if (inputPortFromFlow(rule).toLong() == CzechLightDiscovery.PORT_COMMON) {
                        element = ELEMENT_DROP;
                        leafPort = outputPortFromFlow(rule).toLong();
                    } else {
                        element = ELEMENT_ADD;
                        leafPort = inputPortFromFlow(rule).toLong();
                    }
                    final var och = ochSignalFromFlow(rule);
                    final var channel = allChannels.entrySet().stream()
                            .filter(entry -> MediaChannelDefinition.mcMatches(entry, och))
                            .findAny()
                            .orElse(null);
                    if (channel == null) {
                        log.error("No matching channel definition available for the following rule at {}:",
                                data().deviceId());
                        rule.selector().criteria().forEach(
                                criteria -> log.error("  criteria {}", criteria.toString())
                        );
                        rule.treatment().allInstructions().forEach(
                                instruction -> log.error("  instruction {}", instruction.toString())
                        );
                    } else {
                        log.info("{}: Creating \"{}\" MC {}: leaf {}", data().deviceId(),
                                element, channel.getKey(), leafPort);
                        var sb = new StringBuilder();
                        sb.append("<");
                        sb.append(element);
                        sb.append(">");
                        sb.append("<port>");
                        if (deviceType() == CzechLightDiscovery.DeviceType.LINE_DEGREE) {
                            sb.append(CzechLightDiscovery.LINE_EXPRESS_PREFIX);
                        }
                        sb.append(String.valueOf(leafPort));
                        sb.append("</port>");
                        // FIXME: propagate attenuation or power target
                        if (deviceType() == CzechLightDiscovery.DeviceType.LINE_DEGREE) {
                            if (outputPortFromFlow(rule).toLong() == CzechLightDiscovery.PORT_COMMON) {
                                sb.append("<power>-5.0</power>");
                            } else {
                                sb.append("<power>-12.0</power>");
                            }
                        } else {
                            if (outputPortFromFlow(rule).toLong() == CzechLightDiscovery.PORT_COMMON) {
                                sb.append("<power>-12.0</power>");
                            } else {
                                sb.append("<power>-5.0</power>");
                            }
                        }
                        sb.append("</");
                        sb.append(element);
                        sb.append(">");
                        changes.put(channel.getKey(),
                                changes.getOrDefault(channel.getKey(), "") + sb.toString());
                        hopefullyAdded.add(rule);
                    }
                });

        if (!hopefullyAdded.isEmpty()) {
            var sb = new StringBuilder();
            changes.forEach(
                    (channel, data) -> {
                        sb.append(CzechLightDiscovery.XML_MC_OPEN);
                        sb.append("<channel>");
                        sb.append(channel);
                        sb.append("</channel>");
                        sb.append(data);
                        sb.append(CzechLightDiscovery.XML_MC_CLOSE);
                    });
            doEditConfig(NETCONF_OP_MERGE, sb.toString());
        }
        return hopefullyAdded;
    }

    @Override
    public Collection<FlowRule> removeFlowRules(Collection<FlowRule> rules) {
        if (deviceType() == CzechLightDiscovery.DeviceType.INLINE_AMP
                || deviceType() == CzechLightDiscovery.DeviceType.COHERENT_ADD_DROP) {
            rules.forEach(
                    rule -> {
                        log.debug("{}: asked to remove {} (whole C-band is always forwarded by the HW)",
                                data().deviceId(), rule);
                        getConnectionCache().remove(data().deviceId(), rule);
                    }
            );
            return rules;
        }
        HierarchicalConfiguration xml;
        try {
            xml = doGetSubtree(CzechLightDiscovery.CHANNEL_DEFS_FILTER + CzechLightDiscovery.MC_ROUTING_FILTER);
        } catch (NetconfException e) {
            log.error("Cannot read data from NETCONF: {}", e);
            return new ArrayList<>();
        }
        final var allChannels = MediaChannelDefinition.parseChannelDefinitions(xml);

        var hopefullyRemoved = new ArrayList<FlowRule>();

        // temporary store because both ADD and DROP must go into the same <media-channel> list item
        var changes = new TreeMap<String, String>();

        rules.forEach(
                rule -> {
                    final String element = inputPortFromFlow(rule).toLong() == CzechLightDiscovery.PORT_COMMON ?
                            ELEMENT_DROP : ELEMENT_ADD;
                    final var och = ochSignalFromFlow(rule);
                    final var channel = allChannels.entrySet().stream()
                            .filter(entry -> MediaChannelDefinition.mcMatches(entry, och))
                            .findAny()
                            .orElse(null);
                    if (channel == null) {
                        log.error("Cannot find what channel to remove for the following flow rule at {}:",
                                data().deviceId());
                        rule.selector().criteria().forEach(
                                criteria -> log.error("  criteria {}", criteria.toString())
                        );
                        rule.treatment().allInstructions().forEach(
                                instruction -> log.error("  instruction {}", instruction.toString())
                        );
                    } else {
                        log.info("{}: Removing {} MC {}", data().deviceId(), element, channel.getKey());
                        changes.put(channel.getKey(),
                                changes.getOrDefault(channel.getKey(), "")
                                        + "<" + element + " nc:operation=\"remove\"/>");
                        hopefullyRemoved.add(rule);
                    }
                });

        if (!hopefullyRemoved.isEmpty()) {
            var sb = new StringBuilder();
            changes.forEach(
                    (channel, data) -> {
                        sb.append(CzechLightDiscovery.XML_MC_OPEN);
                        sb.append("<channel>");
                        sb.append(channel);
                        sb.append("</channel>");
                        sb.append(data);
                        sb.append(CzechLightDiscovery.XML_MC_CLOSE);
                    });
            doEditConfig(NETCONF_OP_NONE, sb.toString());
        }
        return hopefullyRemoved;
    }

    private static CzechLightRouting confToMCRouting(final String keyPrefix,
                                                     final Map<String, MediaChannelDefinition> allChannels,
                                                     final HierarchicalConfiguration item) {
        if (!item.containsKey(keyPrefix + ".port")) {
            return null;
        }
        // the leaf port is either just a number, or a number prefixed by "E"
        final var portStr = item.getString(keyPrefix + ".port");
        final int leafPort = Integer.parseInt(portStr.startsWith(CzechLightDiscovery.LINE_EXPRESS_PREFIX) ?
                portStr.substring(1) : portStr);
        return new CzechLightRouting(
                allChannels.get(item.getString("channel")),
                leafPort,
                new MCManipulation(
                        item.getDouble(keyPrefix + ".attenuation", null),
                        item.getDouble(keyPrefix + ".power", null)
                )
        );
    }

    private FlowRule asFlowRule(final Direction direction, final CzechLightRouting routing) {
        FlowRuleService service = handler().get(FlowRuleService.class);
        Iterable<FlowEntry> entries = service.getFlowEntries(data().deviceId());

        final var portIn = PortNumber.portNumber(direction == Direction.DROP ?
                CzechLightDiscovery.PORT_COMMON : routing.leafPort);
        final var portOut = PortNumber.portNumber(direction == Direction.ADD ?
                CzechLightDiscovery.PORT_COMMON : routing.leafPort);

        final var channelWidth = routing.channel.highMHz - routing.channel.lowMHz;
        final var channelCentralFreq = (int) (routing.channel.lowMHz + channelWidth / 2);

        for (FlowEntry entry : entries) {
            final var och = ochSignalFromFlow(entry);
            if (och.centralFrequency().asMHz() == channelCentralFreq
                    && och.slotWidth().asMHz() == channelWidth
                    && portIn.equals(inputPortFromFlow(entry))
                    && portOut.equals(outputPortFromFlow(entry))) {
                return entry;
            }
        }

        final var channelSlotWidth = (int) (channelWidth / ChannelSpacing.CHL_12P5GHZ.frequency().asMHz());
        final var channelMultiplier = (int) ((channelCentralFreq - Spectrum.CENTER_FREQUENCY.asMHz())
                / ChannelSpacing.CHL_6P25GHZ.frequency().asMHz());

        TrafficSelector selector = DefaultTrafficSelector.builder()
                .matchInPort(portIn)
                .add(Criteria.matchOchSignalType(OchSignalType.FLEX_GRID))
                .add(Criteria.matchLambda(Lambda.ochSignal(GridType.FLEX, ChannelSpacing.CHL_6P25GHZ,
                        channelMultiplier, channelSlotWidth)))
                .build();
        TrafficTreatment treatment = DefaultTrafficTreatment.builder()
                .setOutput(portOut)
                .build();
        return DefaultFlowRule.builder()
                .forDevice(data().deviceId())
                .withSelector(selector)
                .withTreatment(treatment)
                // the concept of priorities does not make sense for a ROADM MC configuration,
                // but it's mandatory nonetheless
                .withPriority(666)
                .makePermanent()
                .fromApp(handler().get(CoreService.class).getAppId(DEFAULT_APP))
                .build();
    }

    private static PortNumber inputPortFromFlow(final Object flow) {
        return ((flow instanceof FlowEntry) ?
                ((FlowEntry) flow).selector() : ((FlowRule) flow).selector()).criteria().stream()
                .filter(c -> c instanceof PortCriterion)
                .map(c -> ((PortCriterion) c).port())
                .findAny()
                .orElse(null);
    }

    private static PortNumber outputPortFromFlow(final Object flow) {
        return ((flow instanceof FlowEntry) ?
                ((FlowEntry) flow).treatment() : ((FlowRule) flow).treatment()).immediate().stream()
                .filter(c -> c instanceof Instructions.OutputInstruction)
                .map(c -> ((Instructions.OutputInstruction) c).port())
                .findAny()
                .orElse(null);
    }

    private static OchSignal ochSignalFromFlow(final Object flow) {
        final var fromCriteria = ((flow instanceof FlowEntry) ?
                ((FlowEntry) flow).selector() : ((FlowRule) flow).selector()).criteria().stream()
                .filter(c -> c instanceof OchSignalCriterion)
                .map(c -> ((OchSignalCriterion) c).lambda())
                .findAny()
                .orElse(null);
        if (fromCriteria != null) {
            return fromCriteria;
        }
        return ((flow instanceof FlowEntry) ?
                ((FlowEntry) flow).treatment() : ((FlowRule) flow).treatment()).immediate().stream()
                .filter(c -> c instanceof L0ModificationInstruction.ModOchSignalInstruction)
                .map(c -> ((L0ModificationInstruction.ModOchSignalInstruction) c).lambda())
                .findAny()
                .orElse(null);
    }

    private CzechLightDiscovery.DeviceType deviceType() {
        var annotations = this.handler().get(DeviceService.class).getDevice(handler().data().deviceId()).annotations();
        return CzechLightDiscovery.DeviceType.valueOf(annotations.value(CzechLightDiscovery.DEVICE_TYPE_ANNOTATION));
    }

    private DeviceConnectionCache getConnectionCache() {
        return DeviceConnectionCache.init();
    }

    private HierarchicalConfiguration doGetSubtree(final String subtreeXml) throws NetconfException {
        NetconfSession session = getNetconfSession();
        if (session == null) {
            log.error("Cannot request NETCONF session for {}", data().deviceId());
            return null;
        }
        return CzechLightDiscovery.doGetSubtree(session, subtreeXml);
    }

    private HierarchicalConfiguration doGetXPath(final String prefix, final String namespace, final String xpathFilter)
            throws NetconfException {
        NetconfSession session = getNetconfSession();
        if (session == null) {
            log.error("Cannot request NETCONF session for {}", data().deviceId());
            return null;
        }
        return CzechLightDiscovery.doGetXPath(session, prefix, namespace, xpathFilter);
    }

    public boolean doEditConfig(String mode, String cfg) {
        NetconfSession session = getNetconfSession();
        if (session == null) {
            log.error("Cannot request NETCONF session for {}", data().deviceId());
            return false;
        }

        try {
            return session.editConfig(DatastoreId.RUNNING, mode, cfg);
        } catch (NetconfException e) {
            throw new IllegalStateException(new NetconfException("Failed to edit configuration.", e));
        }
    }

    private NetconfSession getNetconfSession() {
        NetconfController controller =
                checkNotNull(handler().get(NetconfController.class));
        return controller.getNetconfDevice(data().deviceId()).getSession();
    }
}
