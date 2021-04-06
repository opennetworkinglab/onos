/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.ui.impl;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.ElementId;
import org.onosproject.net.HostId;
import org.onosproject.net.Link;
import org.onosproject.net.flow.criteria.Criterion;
import org.onosproject.net.flow.criteria.PortCriterion;
import org.onosproject.net.flow.instructions.Instructions.OutputInstruction;
import org.onosproject.net.intent.FlowRuleIntent;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.OpticalConnectivityIntent;
import org.onosproject.net.intent.ProtectionEndpointIntent;
import org.onosproject.net.link.LinkService;
import org.onosproject.ui.impl.topo.util.ServicesBundle;
import org.onosproject.ui.impl.topo.util.TrafficLink;
import org.onosproject.ui.impl.topo.util.TrafficLink.StatsType;
import org.onosproject.ui.impl.topo.util.TrafficLinkMap;
import org.onosproject.ui.topo.AbstractTopoMonitor;
import org.onosproject.ui.topo.DeviceHighlight;
import org.onosproject.ui.topo.Highlights;
import org.onosproject.ui.topo.HostHighlight;
import org.onosproject.ui.topo.LinkHighlight.Flavor;
import org.onosproject.ui.topo.Mod;
import org.onosproject.ui.topo.NodeHighlight;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

import static org.onosproject.net.MarkerResource.marker;
import static org.onosproject.ui.impl.ProtectedIntentMonitor.ProtectedMode.IDLE;
import static org.onosproject.ui.impl.ProtectedIntentMonitor.ProtectedMode.SELECTED_INTENT;

/**
 * Encapsulates the behavior of monitoring protected intents.
 */
//TODO refactor duplicated methods from here and the TrafficMonitor to AbstractTopoMonitor
public class ProtectedIntentMonitor extends AbstractTopoMonitor {

    private static final Logger log =
            LoggerFactory.getLogger(ProtectedIntentMonitor.class);
    private static final String PRIMARY_PATH_TAG = "protection1";

    private static final String PROT_PRIMARY = "protPrimary";
    private static final String PROT_BACKUP = "protBackup";


    private static final Mod MOD_PROT_PRIMARY = new Mod(PROT_PRIMARY);
    private static final Set<Mod> PROTECTED_MOD_PRIMARY_SET =
            ImmutableSet.of(MOD_PROT_PRIMARY);

    private static final Mod MOD_PROT_BACKUP = new Mod(PROT_BACKUP);
    private static final Set<Mod> PROTECTED_MOD_BACKUP_SET =
            ImmutableSet.of(MOD_PROT_BACKUP);


    /**
     * Designates the different modes of operation.
     */
    public enum ProtectedMode {
        IDLE,
        SELECTED_INTENT
    }

    private final ServicesBundle services;
    private final TopologyViewMessageHandler msgHandler;

    private final Timer timer = new Timer("topo-protected-intents");

    private TimerTask trafficTask = null;
    private ProtectedMode mode = ProtectedMode.IDLE;
    private Intent selectedIntent = null;


    /**
     * Constructs a protected intent monitor.
     *
     * @param services      bundle of services
     * @param msgHandler    our message handler
     */
    public ProtectedIntentMonitor(ServicesBundle services,
                                  TopologyViewMessageHandler msgHandler) {
        this.services = services;
        this.msgHandler = msgHandler;
    }

    // =======================================================================
    // === API ===

    // TODO: move this out to the "h2h/multi-intent app"

    /**
     * Monitor for protected intent data to be sent back to the web client,
     * for the given intent.
     *
     * @param intent the intent to monitor
     */
    public synchronized void monitor(Intent intent) {
        log.debug("monitor intent: {}", intent.id());
        selectedIntent = intent;
        mode = SELECTED_INTENT;
        scheduleTask();
        sendSelectedIntents();
    }

    /**
     * Stop all traffic monitoring.
     */
    public synchronized void stopMonitoring() {
        log.debug("STOP monitoring");
        if (mode != IDLE) {
            sendClearAll();
        }
    }


    // =======================================================================
    // === Helper methods ===
    private void sendClearAll() {
        clearAll();
        sendClearHighlights();
    }

    private void clearAll() {
        this.mode = IDLE;
        clearSelection();
        cancelTask();
    }

    private void clearSelection() {
        selectedIntent = null;
    }

    //TODO duplicate and can be brought in abstract upper class.
    private synchronized void scheduleTask() {
        if (trafficTask == null) {
            log.debug("Starting up background protected intent task...");
            trafficTask = new TrafficUpdateTask();
            timer.schedule(trafficTask, trafficPeriod, trafficPeriod);
        } else {
            log.debug("(protected intent task already running)");
        }
    }

    private synchronized void cancelTask() {
        if (trafficTask != null) {
            trafficTask.cancel();
            trafficTask = null;
        }
    }

    private void sendSelectedIntents() {
        log.debug("sendSelectedIntents: {}", selectedIntent);
        msgHandler.sendHighlights(protectedIntentHighlights());
    }

    private void sendClearHighlights() {
        log.debug("sendClearHighlights");
        msgHandler.sendHighlights(new Highlights());
    }

    // =======================================================================
    // === Generate messages in JSON object node format
    private Highlights protectedIntentHighlights() {
        Highlights highlights = new Highlights();
        TrafficLinkMap linkMap = new TrafficLinkMap();
        IntentService intentService = services.intent();
        if (selectedIntent != null) {
            List<Intent> installables = intentService.getInstallableIntents(selectedIntent.key());

            if (installables != null) {
                ProtectionEndpointIntent ep1 = installables.stream()
                        .filter(ProtectionEndpointIntent.class::isInstance)
                        .map(ProtectionEndpointIntent.class::cast)
                        .findFirst().orElse(null);
                ProtectionEndpointIntent ep2 = installables.stream()
                        .filter(ii -> !ii.equals(ep1))
                        .filter(ProtectionEndpointIntent.class::isInstance)
                        .map(ProtectionEndpointIntent.class::cast)
                        .findFirst().orElse(null);
                if (ep1 == null || ep2 == null) {
                    log.warn("Selected Intent {} didn't have 2 protection endpoints",
                             selectedIntent.key());
                    stopMonitoring();
                    return highlights;
                }
                Set<Link> primary = new LinkedHashSet<>();
                Set<Link> backup = new LinkedHashSet<>();

                Map<Boolean, List<FlowRuleIntent>> transits = installables.stream()
                        .filter(FlowRuleIntent.class::isInstance)
                        .map(FlowRuleIntent.class::cast)
                        // only consider fwd links so that ants march in one direction
                        // TODO: didn't help need further investigation.
                        //.filter(i -> !i.resources().contains(marker("rev")))
                        .collect(Collectors.groupingBy(this::isPrimary));

                // walk primary
                ConnectPoint primHead = ep1.description().paths().get(0).output().connectPoint();
                ConnectPoint primTail = ep2.description().paths().get(0).output().connectPoint();
                List<FlowRuleIntent> primTransit = transits.getOrDefault(true, ImmutableList.of());
                populateLinks(primary, primHead, primTail, primTransit);

                // walk backup
                ConnectPoint backHead = ep1.description().paths().get(1).output().connectPoint();
                ConnectPoint backTail = ep2.description().paths().get(1).output().connectPoint();
                List<FlowRuleIntent> backTransit = transits.getOrDefault(false, ImmutableList.of());
                populateLinks(backup, backHead, backTail, backTransit);

                // Add packet to optical links
                if (!usingBackup(primary)) {
                    primary.addAll(protectedIntentMultiLayer(primHead, primTail));
                }
                backup.addAll(protectedIntentMultiLayer(backHead, backTail));

                boolean isOptical = selectedIntent instanceof OpticalConnectivityIntent;
                //last parameter (traffic) signals if the link is highlighted with ants or solid line
                //Flavor is swapped so green is primary path.
                if (usingBackup(primary)) {
                    //the backup becomes in use so we have a dotted line
                    processLinks(linkMap, backup, Flavor.PRIMARY_HIGHLIGHT,
                                 isOptical, true, PROTECTED_MOD_BACKUP_SET);
                } else {
                    processLinks(linkMap, primary, Flavor.PRIMARY_HIGHLIGHT,
                                 isOptical, true, PROTECTED_MOD_PRIMARY_SET);
                    processLinks(linkMap, backup, Flavor.SECONDARY_HIGHLIGHT,
                                 isOptical, false, PROTECTED_MOD_BACKUP_SET);
                }

                updateHighlights(highlights, primary);
                updateHighlights(highlights, backup);
                colorLinks(highlights, linkMap);
                highlights.subdueAllElse(Highlights.Amount.MINIMALLY);
            } else {
                log.debug("Selected Intent has no installable intents");
            }
        } else {
            log.debug("Selected Intent is null");
        }
        return highlights;
    }

    /**
     * Returns the packet to optical mapping given a head and tail of a protection path.
     *
     * @param head head of path
     * @param tail tail of path
     */
    private Set<Link> protectedIntentMultiLayer(ConnectPoint head, ConnectPoint tail) {
        List<Link> links = new LinkedList<>();
        LinkService linkService = services.link();
        IntentService intentService = services.intent();

        // Ingress cross connect link
        links.addAll(
                linkService.getEgressLinks(head).stream()
                        .filter(l -> l.type() == Link.Type.OPTICAL)
                        .collect(Collectors.toList())
        );

        // Egress cross connect link
        links.addAll(
                linkService.getIngressLinks(tail).stream()
                        .filter(l -> l.type() == Link.Type.OPTICAL)
                        .collect(Collectors.toList())
        );

        // The protected intent does not rely on a multi-layer mapping
        if (links.size() != 2) {
            return Collections.emptySet();
        }

        // Expected head and tail of optical circuit (not connectivity!) intent
        ConnectPoint ocHead = links.get(0).dst();
        ConnectPoint ocTail = links.get(1).src();

        // Optical connectivity
        // FIXME: assumes that transponder (OTN device) is a one-to-one mapping
        // We need to track the multi-layer aspects better
        intentService.getIntents().forEach(intent -> {
            if (intent instanceof OpticalConnectivityIntent) {
                OpticalConnectivityIntent ocIntent = (OpticalConnectivityIntent) intent;
                if (ocHead.deviceId().equals(ocIntent.getSrc().deviceId()) &&
                        ocTail.deviceId().equals(ocIntent.getDst().deviceId())) {
                    intentService.getInstallableIntents(ocIntent.key()).forEach(i -> {
                        if (i instanceof FlowRuleIntent) {
                            FlowRuleIntent fr = (FlowRuleIntent) i;
                            links.addAll(linkResources(fr));
                        }
                    });
                }
            }
        });

        return new LinkedHashSet<>(links);
    }

    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

    /**
     * Populate Links along the primary/backup path.
     *
     * @param links   link collection to populate [output]
     * @param head    head-end of primary/backup path
     * @param tail    tail-end of primary/backup path
     * @param transit Intents if any
     */
    private void populateLinks(Set<Link> links,
                               ConnectPoint head,
                               ConnectPoint tail,
                               List<FlowRuleIntent> transit) {
        // find first hop link
        Link first = transit.stream()
                // search for Link head -> transit Intent head
                // as first candidate of 1st hop Link
                .flatMap(fri -> fri.flowRules().stream())
                .map(fr ->
                    // find first input port from FlowRule
                    Optional.ofNullable(fr.selector().getCriterion(Criterion.Type.IN_PORT))
                        .filter(PortCriterion.class::isInstance)
                        .map(PortCriterion.class::cast)
                        .map(PortCriterion::port)
                        .map(pn -> new ConnectPoint(fr.deviceId(), pn))
                        .orElse(null)
                ).filter(Objects::nonNull)
                .map(dst -> services.link().getLink(head, dst))
                .filter(Objects::nonNull)
                .findFirst()
                // if there isn't one probably 1 hop to the tail
                .orElse(services.link().getLink(head, tail));

        // add first link
        if (first != null) {
            links.add(first);
        }

        // add links in the middle if any
        transit.forEach(fri -> links.addAll(linkResources(fri)));

        // add last hop if any
        Lists.reverse(transit).stream()
                // search for Link transit Intent tail -> tail
                // as candidate of last hop Link
                .flatMap(fri -> ImmutableList.copyOf(fri.flowRules()).reverse().stream())
                .map(fr ->
                    // find first output port from FlowRule
                    fr.treatment().allInstructions().stream()
                            .filter(OutputInstruction.class::isInstance).findFirst()
                        .map(OutputInstruction.class::cast)
                        .map(OutputInstruction::port)
                        .map(pn -> new ConnectPoint(fr.deviceId(), pn))
                        .orElse(null)
                ).filter(Objects::nonNull)
                .map(src -> services.link().getLink(src, tail))
                .filter(Objects::nonNull)
                .findFirst()
                .ifPresent(links::add);
    }

    /**
     * Returns true if specified intent is marked with primary marker resource.
     *
     * @param intent to test
     * @return true if it is an Intent taking part of primary transit path
     */
    private boolean isPrimary(Intent intent) {
        return intent.resources()
                .contains(marker(PRIMARY_PATH_TAG));
    }

    // returns true if the backup path is the one where the traffic is currently flowing
    private boolean usingBackup(Set<Link> primary) {
        Set<Link> activeLinks = Sets.newHashSet(services.link().getActiveLinks());
        return primary.isEmpty() || !activeLinks.containsAll(primary);
    }

    private void updateHighlights(Highlights highlights, Iterable<Link> links) {
        for (Link link : links) {
            ensureNodePresent(highlights, link.src().elementId());
            ensureNodePresent(highlights, link.dst().elementId());
        }
    }

    //TODO duplicate and can be brought in abstract upper class.
    private void ensureNodePresent(Highlights highlights, ElementId eid) {
        String id = eid.toString();
        NodeHighlight nh = highlights.getNode(id);
        if (nh == null) {
            if (eid instanceof DeviceId) {
                nh = new DeviceHighlight(id);
                highlights.add((DeviceHighlight) nh);
            } else if (eid instanceof HostId) {
                nh = new HostHighlight(id);
                highlights.add((HostHighlight) nh);
            }
        }
    }

    private void processLinks(TrafficLinkMap linkMap, Iterable<Link> links,
                              Flavor flavor, boolean isOptical,
                              boolean showTraffic, Set<Mod> mods) {
        if (links != null) {
            for (Link link : links) {
                TrafficLink tlink = linkMap.add(link);
                tlink.tagFlavor(flavor);
                tlink.optical(isOptical);
                if (showTraffic) {
                    tlink.antMarch(true);
                }
                tlink.tagMods(mods);
            }
        }
    }

    //TODO duplicate and can be brought in abstract upper class.
    private void colorLinks(Highlights highlights, TrafficLinkMap linkMap) {
        for (TrafficLink tlink : linkMap.biLinks()) {
            highlights.add(tlink.highlight(StatsType.TAGGED));
        }
    }

    //TODO duplicate and can be brought in abstract upper class.
    // Extracts links from the specified flow rule intent resources
    private Collection<Link> linkResources(Intent installable) {
        ImmutableList.Builder<Link> builder = ImmutableList.builder();
        installable.resources().stream().filter(r -> r instanceof Link)
                .forEach(r -> builder.add((Link) r));
        return builder.build();
    }

    // =======================================================================
    // === Background Task

    // Provides periodic update of traffic information to the client
    private class TrafficUpdateTask extends TimerTask {
        @Override
        public void run() {
            try {
                switch (mode) {
                    case SELECTED_INTENT:
                        sendSelectedIntents();
                        break;

                    default:
                        // RELATED_INTENTS and IDLE modes should never invoke
                        // the background task, but if they do, they have
                        // nothing to do
                        break;
                }

            } catch (Exception e) {
                log.warn("Unable to process protected intent task due to {}", e.getMessage());
                log.warn("Boom!", e);
            }
        }
    }
}
