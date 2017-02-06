/*
 * Copyright 2017-present Open Networking Laboratory
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
import com.google.common.collect.Sets;
import org.onosproject.net.DeviceId;
import org.onosproject.net.ElementId;
import org.onosproject.net.HostId;
import org.onosproject.net.Link;
import org.onosproject.net.MarkerResource;
import org.onosproject.net.NetworkResource;
import org.onosproject.net.behaviour.protection.TransportEndpointDescription;
import org.onosproject.net.intent.FlowRuleIntent;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.OpticalConnectivityIntent;
import org.onosproject.net.intent.ProtectionEndpointIntent;
import org.onosproject.ui.impl.topo.util.ServicesBundle;
import org.onosproject.ui.impl.topo.util.TrafficLink;
import org.onosproject.ui.impl.topo.util.TrafficLink.StatsType;
import org.onosproject.ui.impl.topo.util.TrafficLinkMap;
import org.onosproject.ui.topo.AbstractTopoMonitor;
import org.onosproject.ui.topo.DeviceHighlight;
import org.onosproject.ui.topo.Highlights;
import org.onosproject.ui.topo.HostHighlight;
import org.onosproject.ui.topo.LinkHighlight.Flavor;
import org.onosproject.ui.topo.NodeHighlight;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import static org.onosproject.ui.impl.ProtectedIntentMonitor.ProtectedMode.IDLE;
import static org.onosproject.ui.impl.ProtectedIntentMonitor.ProtectedMode.SELECTED_INTENT;

/**
 * Encapsulates the behavior of monitoring protected intents.
 */
//TODO refactor duplicated methods from here and the TrafficMonitor to AbstractTopoMonitor
public class ProtectedIntentMonitor extends AbstractTopoMonitor {

    private static final Logger log =
            LoggerFactory.getLogger(ProtectedIntentMonitor.class);
    public static final String PRIMARY_PATH_TAG = "protection1";

    /**
     * Designates the different modes of operation.
     */
    public enum ProtectedMode {
        IDLE,
        SELECTED_INTENT
    }

    private final long trafficPeriod;
    private final ServicesBundle servicesBundle;
    private final TopologyViewMessageHandler msgHandler;

    private final Timer timer = new Timer("topo-protected-intents");

    private TimerTask trafficTask = null;
    private ProtectedMode mode = ProtectedMode.IDLE;
    private Intent selectedIntent = null;


    /**
     * Constructs a protected intent monitor.
     *
     * @param trafficPeriod  traffic task period in ms
     * @param servicesBundle bundle of services
     * @param msgHandler     our message handler
     */
    public ProtectedIntentMonitor(long trafficPeriod, ServicesBundle servicesBundle,
                                  TopologyViewMessageHandler msgHandler) {
        this.trafficPeriod = trafficPeriod;
        this.servicesBundle = servicesBundle;
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
        if (selectedIntent != null) {
            List<Intent> installables = servicesBundle.intentService()
                    .getInstallableIntents(selectedIntent.key());
            Set<Link> primary = new HashSet<>();
            Set<Link> backup = new HashSet<>();
            if (installables != null) {
                //There should be exactly two FlowRuleIntent and four
                //ProtectionEndpointIntent for each ProtectedTransportIntent.
                for (Intent installable : installables) {
                    if (installable instanceof FlowRuleIntent) {
                        handleFlowRuleIntent(primary, backup, (FlowRuleIntent) installable);
                    } else if (installable instanceof ProtectionEndpointIntent) {
                        handleProtectionEndpointIntent(primary, backup,
                                                       (ProtectionEndpointIntent) installable);
                    } else {
                        log.warn("Intent {} is not an expected installable type {} " +
                                         "related to ProtectedTransportIntent",
                                 installable.id(), installable.getClass().getSimpleName());
                        stopMonitoring();
                    }
                }
                boolean isOptical = selectedIntent instanceof OpticalConnectivityIntent;
                //last parameter (traffic) signals if the link is highlited with ants or solid line
                //Flavor is swapped so green is primary path.
                if (usingBackup(primary)) {
                    //the backup becomes in use so we have a dotted line
                    processLinks(linkMap, backup, Flavor.PRIMARY_HIGHLIGHT, isOptical, true);
                } else {
                    processLinks(linkMap, primary, Flavor.SECONDARY_HIGHLIGHT, isOptical, true);
                    processLinks(linkMap, backup, Flavor.PRIMARY_HIGHLIGHT, isOptical, false);
                }
                updateHighlights(highlights, primary);
                updateHighlights(highlights, backup);
                colorLinks(highlights, linkMap);
                highlights.subdueAllElse(Highlights.Amount.MINIMALLY);
            } else {
                log.debug("Selected Intent has no installables intents");
            }
        } else {
            log.debug("Selected Intent is null");
        }
        return highlights;
    }

    // - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -

    private void handleProtectionEndpointIntent(Set<Link> primary, Set<Link> backup,
                                                ProtectionEndpointIntent peIntent) {
        TransportEndpointDescription primaryDesc = peIntent
                .description().paths().get(0);
        TransportEndpointDescription secondaryDesc = peIntent
                .description().paths().get(1);
        primary.addAll(servicesBundle.linkService()
                               .getLinks(primaryDesc.output()
                                                 .connectPoint()));
        backup.addAll(servicesBundle.linkService()
                              .getLinks(secondaryDesc.output()
                                                .connectPoint()));
    }

    private void handleFlowRuleIntent(Set<Link> primary, Set<Link> backup,
                                      FlowRuleIntent frIntent) {
        boolean protection1 = frIntent.resources().stream()
                .filter(r -> r instanceof MarkerResource)
                .map(NetworkResource::toString)
                .anyMatch(rstring -> rstring.equals(PRIMARY_PATH_TAG));
        if (protection1) {
            primary.addAll(linkResources(frIntent));
        } else {
            backup.addAll(linkResources(frIntent));
        }
    }

    // returns true if the backup path is the one where the traffic is currently flowing
    private boolean usingBackup(Set<Link> primary) {
        Set<Link> activeLinks = Sets.newHashSet(servicesBundle.linkService().getActiveLinks());
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
                              boolean showTraffic) {
        if (links != null) {
            for (Link link : links) {
                TrafficLink tlink = linkMap.add(link);
                tlink.tagFlavor(flavor);
                tlink.optical(isOptical);
                if (showTraffic) {
                    tlink.antMarch(true);
                }
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
