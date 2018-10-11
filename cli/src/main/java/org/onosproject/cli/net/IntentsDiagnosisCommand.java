/*
 * Copyright 2015-present Open Networking Foundation
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

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Streams;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.action.Option;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultDevice;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.LinkKey;
import org.onosproject.net.Port;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.device.PortStatistics;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.intent.FlowRuleIntent;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.Key;
import org.onosproject.net.intent.ObjectiveTrackerService;
import org.onosproject.net.intent.PointToPointIntent;
import org.onosproject.net.intent.WorkPartitionService;
import org.onosproject.net.statistic.FlowStatisticService;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

@Service
@Command(scope = "onos", name = "intents-diagnosis",
        description = "Diagnosis intents")
public class IntentsDiagnosisCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "key",
            description = "Intent key",
            required = false, multiValued = false)
    @Completion(IntentKeyCompleter.class)
    String key = null;

    @Option(name = "-d", aliases = "--details", description = "printing intent details",
            required = false, multiValued = false)
    private boolean dump = false;

    @Option(name = "-l", aliases = "--link", description = "printing local intentsByLink",
            required = false, multiValued = false)
    private boolean dumpIntentByLink = false;

    private static final int MAX_INTENT_PATH = 100;
    private static final String FIELD_INTENTS_BY_LINK = "intentsByLink";

    @Override
    protected void doExecute() {

        print("intents-diagnosis");
        ServiceRefs svcRefs = buildServiceRefs();
        if (svcRefs == null) {
            return;
        }
        try {
            for (Intent intent : svcRefs.intentsService().getIntents()) {
                if (key != null && !intent.key().toString().equals(key)) {
                    continue;
                }
                print("");
                printIntentHdr(intent, svcRefs);
                if (intent instanceof PointToPointIntent) {
                    diagnosisP2Pintent((PointToPointIntent) intent, svcRefs);
                } else {
                    // TODO : it needs to implement other types of intent
                    print(" It doesn't support %s intent.", intent.getClass().getSimpleName());
                }
            }
            if (dumpIntentByLink) {
                dumpIntentsByLink(svcRefs);
            }
        } catch (Exception e) {
            print("error: " + e);
        }

    }

    private void printIntentHdr(Intent intent, ServiceRefs svcRefs) {
        print("* intent key: %s", intent.key());
        print(" - state: %s", svcRefs.intentsService().getIntentState(intent.key()));
        dump(" - leader: %s %s", svcRefs.getWorkPartitionService().getLeader(intent.key(), Key::hash),
                svcRefs.workPartitionService.isMine(intent.key(), Key::hash) ? "(Mine)" : "");
    }

    private void dumpIntentsByLink(ServiceRefs svcRefs) {
        Set<Map.Entry<LinkKey, Key>> intentsByLink = getIntentsByLinkSet(svcRefs);

        print("* intentsbylink:");
        for (Map.Entry<LinkKey, Key> entry : intentsByLink) {
            print(" - %s, Intents: %s ", entry.getKey(), entry.getValue());
        }
    }

    private Set<Map.Entry<LinkKey, Key>> getIntentsByLinkSet(ServiceRefs svcRefs) {

        try {

            ObjectiveTrackerService objTracker = svcRefs.getObjectiveTrackerService();

            // Utilizing reflection instead of adding new interface for getting intentsByLink
            Field f = objTracker.getClass().getDeclaredField(FIELD_INTENTS_BY_LINK);
            f.setAccessible(true);
            SetMultimap<LinkKey, Key> intentsByLink = (SetMultimap<LinkKey, Key>) f.get(objTracker);

            return ImmutableSet.copyOf(intentsByLink.entries());
        } catch (NoSuchFieldException | IllegalAccessException ex) {
            error("error: " + ex);
            return ImmutableSet.of();
        }
    }

    private void diagnosisP2Pintent(PointToPointIntent intent, ServiceRefs svcRefs) {

        List<Intent> installableIntents = svcRefs.intentsService().getInstallableIntents(intent.key());

        if (installableIntents.size() == 0) {
            error("NO INSTALLABLE INTENTS");
            return;
        }

        Set<String> notSupport = new HashSet<>();
        for (Intent installable: installableIntents) {
            if (installable instanceof FlowRuleIntent) {
                checkP2PFlowRuleIntent(intent, (FlowRuleIntent) installable, svcRefs);
            } else {
                // TODO : it needs to implement other types of installables
                notSupport.add(installable.getClass().getSimpleName());
            }
        }

        if (notSupport.size() > 0) {
            print(" It doesn't support %s.", notSupport);
        }
    }

    private void checkP2PFlowRuleIntent(PointToPointIntent intent, FlowRuleIntent installable, ServiceRefs svcRefs) {

        final Map<DeviceId, DeviceOnIntent> devs = createDevicesOnP2PIntent(intent, installable);

        boolean errorOccurred = false;
        // checking the number of links & CPs in P2P intent
        for (DeviceOnIntent dev: devs.values()) {
            if (dev.getIngressLinks().size() > 1) {
                error("MULTIPLE NUMBER OF INGRESS LINKs on " + dev.deviceId()
                        + ": " + dev.getIngressLinks());
                errorOccurred = true;
            }
            if (dev.getIngressCps().size() > 1) {
                error("MULTIPLE NUMBER OF INGRESS CONNECT POINTs on " + dev.deviceId()
                        + ": " + dev.getIngressCps());
                errorOccurred = true;
            }
            if (dev.getEgressLinks().size() > 1) {
                error("MULTIPLE NUMBER OF EGRESS LINKs: on " + dev.deviceId()
                        + ": " + dev.getEgressLinks());
                errorOccurred = true;
            }
            if (dev.getEgressCps().size() > 1) {
                error("MULTIPLE NUMBER OF EGRESS CONNECT POINTs: on " + dev.deviceId()
                        + ": " + dev.getEgressCps());
                errorOccurred = true;
            }
        }

        ConnectPoint startCp = intent.filteredIngressPoint().connectPoint();
        DeviceOnIntent startDev = devs.get(startCp.deviceId());
        if (startDev == null) {
            error("STARTING CONNECT POINT DEVICE: " + startCp.deviceId() + " is not on intent");
            errorOccurred = true;
        }

        ConnectPoint endCp = intent.filteredEgressPoint().connectPoint();
        DeviceOnIntent endDev = devs.get(endCp.deviceId());
        if (endDev == null) {
            error("END CONNECT POINT DEVICE: " + endCp.deviceId() + " is not on intent");
            errorOccurred = true;
        }

        if (!errorOccurred) {
            // Per device checking with path-order
            DeviceOnIntent dev = startDev;
            int i = 0;
            for (; i < MAX_INTENT_PATH; i++) {
                perDeviceChecking(dev, svcRefs);

                // P2P intent has only 1 egress CP
                ConnectPoint egressCp = dev.getEgressCps().stream().findFirst().orElse(null);
                if (egressCp != null && Objects.equals(endCp, egressCp)) {
                    break;
                }

                // P2P intent has only 1 egress link
                Link egressLink = dev.getEgressLinks().stream().findFirst().orElse(null);
                if (egressLink == null) {
                    error("INVALID EGRESS LINK & CONNECT POINT for: " + dev);
                    errorOccurred = true;
                    break;
                }
                if (Objects.equals(egressLink.dst(), endCp)) {
                    break;
                }

                // P2P intent only 1 ingress link
                dev = devs.values().stream()
                        .filter(nextDev -> Objects.equals(
                            egressLink, nextDev.getIngressLinks().stream().findFirst().orElse(null)))
                        .findAny().orElse(null);
                if (dev == null) {
                    error("FAILED TO FIND NEXT DEV for: " + dev + ", LINK: " + egressLink);
                    errorOccurred = true;
                    break;
                }
            }
            if (i == MAX_INTENT_PATH) {
                error("MAX INTENT PATH WAS EXCEEDED");
                errorOccurred = true;
            }
        }

        if (errorOccurred) {
            // Installable checking
            dump("");
            dump("ERROR OCCURRED. DO PER FLOW CHECKING");
            perFlowRuleChecking(installable, svcRefs);
        }

        if (svcRefs.workPartitionService.isMine(intent.key(), Key::hash)) {
            checkIntentsByLink(installable, svcRefs);
        }
    }

    private void checkIntentsByLink(FlowRuleIntent installable, ServiceRefs svcRefs) {

        Set<Map.Entry<LinkKey, Key>> intentsByLink = getIntentsByLinkSet(svcRefs);

        installable.resources().forEach(
                rsrc -> {
                    if (rsrc instanceof Link) {
                        Link link = (Link) rsrc;
                        LinkKey linkKey = LinkKey.linkKey(link);
                        intentsByLink.stream()
                                .filter(entry -> Objects.equals(entry.getKey(), linkKey)
                                        && Objects.equals(entry.getValue(), installable.key()))
                                .findAny()
                                .orElseGet(() -> {
                                    error("FAILED TO FIND LINK(" + link + ") for intents: " + installable.key());
                                    return null;
                                });
                    }
                }
        );
    }

    // TODO: It needs to consider FLowObjectiveIntent case
    private void perDeviceChecking(DeviceOnIntent devOnIntent, ServiceRefs svcRefs) {

        Collection<PortStatistics> portStats =
                svcRefs.deviceService().getPortStatistics(devOnIntent.deviceId());
        Collection<PortStatistics> portDeltaStats =
                svcRefs.deviceService().getPortDeltaStatistics(devOnIntent.deviceId());

        dump("");
        dump(" ------------------------------------------------------------------------------------------");

        Device device = svcRefs.deviceService.getDevice(devOnIntent.deviceId());
        if (device == null) {
            error("INVALID DEVICE for " + devOnIntent.deviceId());
            return;
        }

        dump(" %s", getDeviceString(device));
        dump("  %s", device.annotations());

        devOnIntent.getIngressCps().stream()
                .forEach(cp -> dumpCpStatistics(cp, portStats, portDeltaStats, "INGRESS", svcRefs));

        Stream<FlowEntry> flowEntries = Streams.stream(svcRefs.flowService.getFlowEntries(devOnIntent.deviceId()));

        devOnIntent.getFlowRules().stream()
                .forEach(
                        intentFlowRule -> {
                            FlowEntry matchedEntry = flowEntries
                                    .filter(entry -> Objects.equals(intentFlowRule.id(), entry.id()))
                                    .findFirst().orElse(null);

                            if (matchedEntry == null) {
                                error("FAILED TO FIND FLOW ENTRY: for " + intentFlowRule);
                                return;
                            }

                            if (Objects.equals(intentFlowRule.selector(), matchedEntry.selector()) &&
                                    Objects.equals(intentFlowRule.treatment(), matchedEntry.treatment())) {
                                dumpFlowEntry(matchedEntry, "FLOW ENTRY");
                                return;
                            }

                            error("INSTALLABLE-FLOW ENTRY mismatch");
                            dumpFlowRule(intentFlowRule, "INSTALLABLE");
                            dumpFlowEntry(matchedEntry, "FLOW ENTRY");
                        }
                );

        devOnIntent.getEgressCps().stream()
                .forEach(
                        cp -> dumpCpStatistics(cp, portStats, portDeltaStats, "EGRESS", svcRefs)
                );
    }

    // TODO: It needs to consider FLowObjectiveIntent case
    private void perFlowRuleChecking(FlowRuleIntent installable, ServiceRefs svcRefs) {

        installable.flowRules().forEach(
                flowrule -> {
                    DeviceId devId = flowrule.deviceId();
                    if (devId == null) {
                        error("INVALID DEVICE ID for " + flowrule);
                        return;
                    }

                    Device dev = svcRefs.deviceService.getDevice(devId);
                    if (dev == null) {
                        error("INVALID DEVICE for " + flowrule);
                        return;
                    }

                    dump("");
                    dump(
                    " ------------------------------------------------------------------------------------------");
                    dump(" %s", getDeviceString(dev));
                    dump("  %s", dev.annotations());

                    svcRefs.flowService().getFlowEntries(devId)
                            .forEach(
                                    entry -> {
                                        dumpFlowRule(flowrule, "INSTALLABLE");
                                        dumpFlowEntry(entry, "FLOW ENTRY");

                                        if (!flowrule.selector().equals(entry.selector())) {
                                            return;
                                        }
                                        if (flowrule.id().equals(entry.id()) &&
                                                flowrule.treatment().equals(entry.treatment())) {
                                            dumpFlowEntry(entry, "FLOW ENTRY");
                                            return;
                                        }
                                        error("INSTALLABLE-FLOW ENTRY mismatch");
                                    }
                            );
                }
        );
    }

    private Map<DeviceId, DeviceOnIntent> createDevicesOnP2PIntent(
            PointToPointIntent intent, FlowRuleIntent flowRuleIntent) {

        final Map<DeviceId, DeviceOnIntent> devMap = new HashMap<>();

        flowRuleIntent.resources().forEach(
                rsrc -> {
                    if (rsrc instanceof Link) {
                        Link link = (Link) rsrc;
                        ConnectPoint srcCp = link.src();
                        ConnectPoint dstCp = link.dst();
                        try {
                            DeviceOnIntent dev = devMap.computeIfAbsent(srcCp.deviceId(), DeviceOnIntent::new);
                            dev.addEgressLink(link);

                            dev = devMap.computeIfAbsent(dstCp.deviceId(), DeviceOnIntent::new);
                            dev.addIngressLink(link);
                        } catch (IllegalStateException e) {
                            print("error: " + e);
                        }
                    }
                }
        );

        ConnectPoint startCp = intent.filteredIngressPoint().connectPoint();
        DeviceOnIntent startDev = devMap.computeIfAbsent(startCp.deviceId(), DeviceOnIntent::new);
        if (!startDev.hasIngressCp(startCp)) {
            startDev.addIngressCp(startCp);
        }

        ConnectPoint endCp = intent.filteredEgressPoint().connectPoint();
        DeviceOnIntent endDev = devMap.computeIfAbsent(endCp.deviceId(), DeviceOnIntent::new);
        if (!endDev.hasEgressCp(endCp)) {
            endDev.addEgessCp(endCp);
        }

        flowRuleIntent.flowRules().forEach(
                flowRule -> {
                    DeviceId devId = flowRule.deviceId();
                    if (devId == null) {
                        error("INVALID DEVICE ID for " + flowRule);
                        return;
                    }
                    DeviceOnIntent dev = devMap.get(devId);
                    if (dev == null) {
                        error("DEVICE(" + devId + ") IS NOT ON INTENTS LINKS");
                        return;
                    }

                    dev.addFlowRule(flowRule);
                }
        );

        return devMap;
    }

    private String getDeviceString(Device dev) {

        StringBuilder buf = new StringBuilder();
        if (dev != null) {
            buf.append(String.format("Device: %s, ", dev.id()));
            buf.append(String.format("%s, ", dev.type()));
            buf.append(String.format("%s, ", dev.manufacturer()));
            buf.append(String.format("%s, ", dev.hwVersion()));
            buf.append(String.format("%s, ", dev.swVersion()));
            if (dev instanceof DefaultDevice) {
                DefaultDevice dfltDev = (DefaultDevice) dev;
                if (dfltDev.driver() != null) {
                    buf.append(String.format("%s, ", dfltDev.driver().name()));
                }
                String channelId = dfltDev.annotations().value("channelId");
                if (channelId != null) {
                    buf.append(String.format("%s, ", channelId));
                }
            }
        }

        return buf.toString();
    }

    private void dumpFlowRule(FlowRule rule, String hdr) {
        dump("  " + hdr + ":");
        dump("   - id=%s, priority=%d", rule.id(), rule.priority());
        dump("   - %s", rule.selector());
        dump("   - %s", rule.treatment());
    }

    private void dumpFlowEntry(FlowEntry entry, String hdr) {
        dumpFlowRule(entry, hdr);
        dump("   - packets=%d", entry.packets());
    }


    private void dumpCpStatistics(ConnectPoint cp, Collection<PortStatistics> devPortStats,
                                  Collection<PortStatistics> devPortDeltaStats, String direction, ServiceRefs svcs) {
        if (cp == null) {
            return;
        }

        dump("  %s:", direction);

        if (cp.port().isLogical()) {
            dump("   - logical: device: %s, port: %s", cp.deviceId(), cp.port());
            return;
        }

        Port port =  svcs.deviceService.getPort(cp.deviceId(), cp.port());
        if (port == null) {
            return;
        }

        try {
            devPortStats.stream()
                    .filter(stat -> stat.portNumber().equals(cp.port()))
                    .forEach(stat -> dump("   - stat   : %s:", getPortStatStr(stat, port)));
        } catch (IllegalStateException e) {
            error("error: " + e);
            return;
        }

        try {
            devPortDeltaStats.stream()
                    .filter(stat -> stat.portNumber().equals(cp.port()))
                    .forEach(stat -> dump("   - delta  : %s:", getPortStatStr(stat, port)));
        } catch (IllegalStateException e) {
            error("error: " + e);
        }
    }

    private void dump(String format, Object... args) {
        if (dump) {
            print(format, args);
        }
    }

    private String getPortStatStr(PortStatistics stat, Port port) {

        final String portName = port.annotations().value(AnnotationKeys.PORT_NAME);

        return String.format("port: %s(%s), ", stat.portNumber(), portName) +
                String.format("enabled: %b, ", port.isEnabled()) +
                String.format("pktRx: %d, ", stat.packetsReceived()) +
                String.format("pktTx: %d, ", stat.packetsSent()) +
                String.format("pktRxErr: %d, ", stat.packetsRxErrors()) +
                String.format("pktTxErr: %d, ", stat.packetsTxErrors()) +
                String.format("pktRxDrp: %d, ", stat.packetsRxDropped()) +
                String.format("pktTxDrp: %d", stat.packetsTxDropped());
    }

    private static class DeviceOnIntent {

        private final DeviceId devId;

        private Collection<Link> ingressLinks = new ArrayList<>();

        private Collection<Link> egressLinks = new ArrayList<>();

        private Collection<ConnectPoint> ingressCps = new ArrayList<>();

        private Collection<ConnectPoint> egressCps = new ArrayList<>();

        private Collection<FlowRule> flowRules = new ArrayList<>();

        public DeviceOnIntent(DeviceId devId) {
            this.devId = devId;
        }

        public DeviceId deviceId() {
            return devId;
        }

        public Collection<Link> getIngressLinks() {
            return ingressLinks;
        }

        public Collection<Link> getEgressLinks() {
            return egressLinks;
        }

        public void addIngressLink(Link link) {
            ingressLinks.add(link);
            addIngressCp(link.dst());
        }

        public void addEgressLink(Link link) {
            egressLinks.add(link);
            addEgessCp(link.src());
        }

        public void addIngressCp(ConnectPoint cp) {
            ingressCps.add(cp);
        }

        public void addEgessCp(ConnectPoint cp) {
            egressCps.add(cp);
        }

        public boolean hasIngressCp(final ConnectPoint cp) {
            return ingressCps.stream().anyMatch(icp -> Objects.equals(icp, cp));
        }

        public boolean hasEgressCp(ConnectPoint cp) {
            return egressCps.stream().anyMatch(ecp -> Objects.equals(ecp, cp));
        }

        public Collection<ConnectPoint> getIngressCps() {
            return ingressCps;
        }

        public Collection<ConnectPoint> getEgressCps() {
            return egressCps;
        }

        public Collection<FlowRule> getFlowRules() {
            return flowRules;
        }

        public void addFlowRule(FlowRule flowRule) {
            flowRules.add(flowRule);
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(getClass())
                    .omitNullValues()
                    .add("devId", devId)
                    .add("ingressLinks", ingressLinks)
                    .add("egressLinks", egressLinks)
                    .add("flowRules", flowRules)
                    .toString();
        }
    }


    private ServiceRefs buildServiceRefs() {
        IntentService intentsService = get(IntentService.class);
        if (intentsService == null) {
            return null;
        }
        DeviceService deviceService = get(DeviceService.class);
        if (deviceService == null) {
            return null;
        }
        FlowStatisticService flowStatsService = get(FlowStatisticService.class);
        if (flowStatsService == null) {
            return null;
        }
        FlowRuleService flowService = get(FlowRuleService.class);
        if (flowService == null) {
            return null;
        }
        WorkPartitionService workPartitionService = get(WorkPartitionService.class);
        if (workPartitionService == null) {
            return null;
        }
        ObjectiveTrackerService objectiveTrackerService = get(ObjectiveTrackerService.class);
        if (objectiveTrackerService == null) {
            return null;
        }

        return new ServiceRefs(
                intentsService,
                deviceService,
                flowService,
                workPartitionService,
                objectiveTrackerService
        );
    }

    private static final class ServiceRefs {

        private IntentService intentsService;
        private DeviceService deviceService;
        private FlowRuleService flowService;
        private WorkPartitionService workPartitionService;
        private ObjectiveTrackerService objectiveTrackerService;

        private ServiceRefs(
                IntentService intentsService,
                DeviceService deviceService,
                FlowRuleService flowService,
                WorkPartitionService workPartitionService,
                ObjectiveTrackerService objectiveTrackerService
        ) {
            this.intentsService = intentsService;
            this.deviceService = deviceService;
            this.flowService = flowService;
            this.workPartitionService = workPartitionService;
            this.objectiveTrackerService = objectiveTrackerService;
        }

        public IntentService intentsService() {
            return intentsService;
        }

        public DeviceService deviceService() {
            return deviceService;
        }

        public FlowRuleService flowService() {
            return flowService;
        }

        public WorkPartitionService getWorkPartitionService() {
            return workPartitionService;
        }

        public ObjectiveTrackerService getObjectiveTrackerService() {
            return objectiveTrackerService;
        }
    }

}
