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

package org.onosproject.imr;

import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang3.tuple.Pair;
import org.onlab.util.KryoNamespace;
import org.onosproject.core.ApplicationId;
import org.onosproject.imr.data.Path;
import org.onosproject.imr.data.Route;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.ElementId;
import org.onosproject.net.FilteredConnectPoint;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.Link;
import org.onosproject.net.PortNumber;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleEvent;
import org.onosproject.net.flow.FlowRuleListener;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.host.HostService;
import org.onosproject.net.intent.ConnectivityIntent;
import org.onosproject.net.intent.FlowRuleIntent;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentEvent;
import org.onosproject.net.intent.IntentListener;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.intent.IntentState;
import org.onosproject.net.intent.Key;
import org.onosproject.net.intent.LinkCollectionIntent;
import org.onosproject.net.intent.PointToPointIntent;
import org.onosproject.net.link.LinkService;
import org.onosproject.net.statistic.FlowStatisticStore;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.DistributedSet;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Manager of Intent Monitor and Reroute.
 */
@Component(immediate = true, service = IntentMonitorAndRerouteService.class)
public class IntentMonitorAndRerouteManager implements IntentMonitorAndRerouteService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private ConsistentMap<ApplicationId, Map<Key, ConnectivityIntent>> monitoredIntentsDistr;
    private Map<ApplicationId, Map<Key, ConnectivityIntent>> monitoredIntents;

    private DistributedSet<Key> toBeMonitoredIntents;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected IntentService intentService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected FlowRuleService flowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected LinkService linkService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected FlowStatisticStore statsStore;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected StorageService storageService;

    private InternalIntentListener intentListener = new InternalIntentListener();

    private InternalFlowRuleListener flowRuleListener = new InternalFlowRuleListener();

    @Activate
    protected void activate() {
        intentService.addListener(intentListener);
        flowRuleService.addListener(flowRuleListener);

        monitoredIntentsDistr = storageService
                .<ApplicationId, Map<Key, ConnectivityIntent>>consistentMapBuilder()
                .withSerializer(Serializer.using(KryoNamespaces.API))
                .withName("IMR-monitoredIntents")
                .build();
        monitoredIntents = monitoredIntentsDistr.asJavaMap();

        toBeMonitoredIntents = storageService.<Key>setBuilder()
                .withSerializer(Serializer.using(
                        new KryoNamespace.Builder()
                                .register(KryoNamespaces.API)
                                .register(Key.class)
                                .build()))
                .withName("IMR-toMonitorIntents")
                .build()
                .asDistributedSet();
        log.info("IntentMonitorAndReroute activated");
    }

    @Deactivate
    protected void deactivate() {
        intentService.removeListener(intentListener);
        flowRuleService.removeListener(flowRuleListener);
        monitoredIntents
                .forEach(((applicationId, keyConnectivityIntentMap) ->
                        keyConnectivityIntentMap.keySet()
                                .forEach(this::removeIntent)));
        log.info("IntentMonitorAndReroute deactivated");
    }


    private synchronized void storeMonitoredIntent(ConnectivityIntent intent) {
        log.debug("Store Monitored Intent {}", intent.key());
        Map<Key, ConnectivityIntent> temp = monitoredIntents.getOrDefault(intent.appId(), new ConcurrentHashMap<>());
        temp.put(intent.key(), intent);
        monitoredIntents.put(intent.appId(), temp);
    }

    @Override
    public synchronized boolean startMonitorIntent(Key intentKey) {
        checkNotNull(intentKey, "Intent Key must not be null");
        log.debug("Start Monitor Intent: {}", intentKey.toString());
        toBeMonitoredIntents.add(intentKey);

        //Check if the requested intent is already present in the intent manager
        Intent installedIntent = intentService.getIntent(intentKey);
        if (!allowedIntent(installedIntent)) {
            return false;
        }
        //Check if the intent that is present in the intent subsystem is already installed
        if (intentService.getIntentState(intentKey) == IntentState.INSTALLED) {
            storeMonitoredIntent((ConnectivityIntent) installedIntent);
        }
        return true;
    }


    /**
     * Returns whether the intent can be monitored or not.
     * @param intent The intent you want to check if it is allowed to be monitored.
     * @return true if the intent's type is of one of the allowed types
     * ({@link LinkCollectionIntent}, {@link PointToPointIntent}).
     */
    public boolean allowedIntent(Intent intent) {
        return intent instanceof LinkCollectionIntent || intent instanceof PointToPointIntent;
    }

    @Override
    public synchronized boolean stopMonitorIntent(Key intentKey) {
        checkNotNull(intentKey, "Intent key must not be null");
        log.debug("Stop Monitor Intent: ", intentKey.toString());
        if (!toBeMonitoredIntents.contains(intentKey)) {
            return false;
        }
        removeIntent(intentKey);
        toBeMonitoredIntents.remove(intentKey);
        return true;
    }

    /**
     * Removes the intent from the internal structure.
     * @param intentKey Key of the intent to be removed.
     * @return true if the intent is found and removed, false otherwise.
     */
    private synchronized boolean removeIntent(Key intentKey) {
        for (Map.Entry<ApplicationId, Map<Key, ConnectivityIntent>> appIntents
                : monitoredIntents.entrySet()) {
            if (appIntents.getValue().containsKey(intentKey)) {
                appIntents.getValue().remove(intentKey);
                //TODO: check if it works without reputting the map
                flushIntentStatStore(intentKey);
                monitoredIntents.put(appIntents.getKey(), appIntents.getValue());
                if (appIntents.getValue().isEmpty()) {
                    monitoredIntents.remove(appIntents.getKey());
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Flushes the statistics (from the statistics store) of an intent.
     * @param intentKey Key of the intent which statistics has to be cleaned.
     */
    private synchronized void flushIntentStatStore(Key intentKey) {
        checkNotNull(intentKey);
        //Remove all the flow rule on the stats store related to the passed intentKey
        intentService.getInstallableIntents(intentKey)
                .stream()
                .map(intent -> (FlowRuleIntent) intent)
                .forEach(intent -> intent.flowRules()
                        .forEach(flowRule -> statsStore.removeFlowStatistic(flowRule))
                );
    }


    /**
     * Generates a new {@Link LinkCollectionIntent} applying the new path.
     * @param links List of links of the new path.
     * @param intentKey Key of the intent you want to re-route.
     * @param appId Application id that submits initially the intent.
     * @return The new intent, if not possibile it will return the old intent already installed.
     */
    private ConnectivityIntent generateLinkCollectionIntent(
            List<Link> links,
            Key intentKey,
            ApplicationId appId) {
        checkNotNull(links);
        checkNotNull(appId);

        // Gets the oldIntent already installed
        ConnectivityIntent oldIntent = monitoredIntents.get(appId).get(intentKey);

        //Flush the statistics of the currently installed intent
        flushIntentStatStore(intentKey);

        //get the connect point of the old intent
        // Left element of the Pair is the ingress, right one is the egress
        Pair<Set<FilteredConnectPoint>, Set<FilteredConnectPoint>> cpPair = extractEndConnectPoints(oldIntent);
        if (cpPair == null) {
            return oldIntent;
        }

        // Now generate the new intent
        LinkCollectionIntent newIntent = LinkCollectionIntent.builder()
                .appId(oldIntent.appId())
                .key(intentKey)
                .selector(oldIntent.selector())
                .filteredIngressPoints(ImmutableSet.copyOf(cpPair.getLeft()))
                .filteredEgressPoints(ImmutableSet.copyOf(cpPair.getRight()))
                .treatment(oldIntent.treatment())
                .priority(oldIntent.priority())
                .constraints(oldIntent.constraints())
                .links(ImmutableSet.copyOf(links))
                //TODO: is there a way to get from the old intent?
                .applyTreatmentOnEgress(true)
                .build();

        return newIntent;
    }

    @Override
    public boolean applyPath(Route route) {
        checkNotNull(route, "Route to apply must be not null");
        checkNotNull(route.appId(), "Application id must be not null");
        checkNotNull(route.key(), "Intent key to apply must be not null");
        checkNotNull(route.paths(), "New path must be not null");
        checkArgument(route.paths().size() >= 1);

        ApplicationId appId = route.appId();
        Key key = route.key();

        // check if the app and the intent key are monitored
        if (!monitoredIntents.containsKey(appId)) {
            return false;
        }
        if (!monitoredIntents.get(appId).containsKey(key)) {
            return false;
        }

        // TODO: now we manage only the unsplittable routing
        Path currentPath = route.paths()
                .stream()
                .max(Comparator.comparing(Path::weight))
                .get();

        // Check if the last and first element of the path are HostId
        // in this case remove them from the list
        if (currentPath.path().get(0) instanceof HostId) {
            currentPath.path().remove(0);
        }
        if (currentPath.path().get(currentPath.path().size() - 1) instanceof HostId) {
            currentPath.path().remove(currentPath.path().size() - 1);
        }

        List<Link> links = createPathFromDeviceList(currentPath.path());

        // Generate the new Link collection intent, if not possible it will return the old intent
        ConnectivityIntent intent = generateLinkCollectionIntent(links, key, appId);
        storeMonitoredIntent(intent);
        intentService.submit(intent);
        return true;
    }

    @Override
    public Map<ApplicationId, Map<Key, List<FlowEntry>>> getStats() {
        //TODO: check if there is a better way to get the statistics
        Map<ApplicationId, Map<Key, List<FlowEntry>>> currentStatistics = new HashMap<>();
        monitoredIntents.forEach((appId, mapIntentKey) ->
                                         currentStatistics.putAll(getStats(appId))
        );
        return currentStatistics;
    }

    @Override
    public Map<ApplicationId, Map<Key, List<FlowEntry>>> getStats(ApplicationId appId) {
        checkNotNull(appId);

        //TODO: is there a better way to get statistics?
        Map<ApplicationId, Map<Key, List<FlowEntry>>> currentStatistics = new HashMap<>();
        currentStatistics.put(appId, new HashMap<>());
        if (monitoredIntents.containsKey(appId)) {
            Set<Key> keySet = monitoredIntents.get(appId).keySet();
            for (Key intentKey : keySet) {

                List<FlowEntry> flowEntries = getStats(intentKey);
                currentStatistics.get(appId).put(intentKey, flowEntries);
            }
        }
        return currentStatistics;
    }

    @Override
    public Map<ApplicationId, Map<Key, List<FlowEntry>>> getStats(ApplicationId appId, Key intentKey) {
        checkNotNull(appId);
        checkNotNull(intentKey);
        checkArgument(monitoredIntents.containsKey(appId));
        checkArgument(monitoredIntents.get(appId).containsKey(intentKey));

        Map<ApplicationId, Map<Key, List<FlowEntry>>> currentStatistics = new HashMap<>();
        currentStatistics.put(appId, new HashMap<>());
        List<FlowEntry> flowEntries = getStats(intentKey);
        currentStatistics.get(appId).put(intentKey, flowEntries);
        return currentStatistics;
    }

    /**
     * Returns the list of flow entries of a particular intent.
     * @param intentKey
     * @return List of the flow entries of the specified intent,
     * it contains all the statistics of that intent.
     */
    private List<FlowEntry> getStats(Key intentKey) {
        List<FlowEntry> currentStatistics = new LinkedList<>();
        intentService.getInstallableIntents(intentKey)
                .forEach(intent -> ((FlowRuleIntent) intent).flowRules()
                         .forEach(flowRule -> {
                             ConnectPoint cp = buildConnectPoint(flowRule);
                             currentStatistics.addAll(getStats(cp, flowRule));
                         })
                );
        return currentStatistics;
    }

    /**
     * Returns a list of flow entry related to the connect point and flow rule passed.
     * @param cp ConnectPoint we want to retrieve the flow entry from.
     * @param flowRule FlowRule.
     * @return List of flow entries.
     */
    private List<FlowEntry> getStats(ConnectPoint cp, FlowRule flowRule) {
        return statsStore.getCurrentFlowStatistic(cp)
                .stream()
                .filter(flowEntry -> flowEntry
                        .id()
                        .equals(flowRule.id()))
                .collect(Collectors.toList());
    }

    /**
     * Returns a list of links starting from a list of devices.
     * @param deviceList List of devices.
     * @return A path in terms of list of links.
     */
    private List<Link> createPathFromDeviceList(List<ElementId> deviceList) {
        List<Link> path = new ArrayList<>();
        if (deviceList.size() == 1) {
            return path;
        }

        // Left element represents the input and right the output
        List<Pair<DeviceId, DeviceId>> devicePairs = IntStream.
                range(0, deviceList.size() - 1)
                .mapToObj(i -> Pair.of((DeviceId) deviceList.get(i), (DeviceId) deviceList.get(i + 1)))
                .collect(Collectors.toList());

        devicePairs.forEach(pair -> {
            //TODO use GetPath pair by pair?
            // The common Link between DevEgress and DevIngress is the intersection of their links
            Set<Link> commonLinks = new HashSet<>(linkService.getDeviceEgressLinks(pair.getLeft()));
            commonLinks.retainAll(linkService.getDeviceIngressLinks(pair.getRight()));
            if (commonLinks.size() == 0) {
                log.error("No link found between node {} and node {}!",
                          pair.getLeft(), pair.getRight());
            } else if (commonLinks.size() == 1) {
                path.add(commonLinks.iterator().next());
            } else {
                //TODO select the one with more bandwidth?
                log.warn("{} links found between node {} and node {}: taking the first one!",
                         commonLinks.size(), pair.getLeft(), pair.getRight());
                path.add(commonLinks.iterator().next());
            }
        });

        return path;
    }

    @Override
    public Map<ApplicationId, Map<Key, Pair<Set<ElementId>, Set<ElementId>>>> getMonitoredIntents() {
        Map<ApplicationId, Map<Key, Pair<Set<ElementId>, Set<ElementId>>>> currentMonitoredIntents
                = new ConcurrentHashMap<>();
        monitoredIntents.forEach((appId, appIntents) -> {
            currentMonitoredIntents.put(appId, new ConcurrentHashMap<>());
            appIntents.forEach((intentKey, intent) -> {
                Pair<Set<ElementId>, Set<ElementId>> endPair = extractEndPoints(intent);
                if (endPair != null) {
                    currentMonitoredIntents.get(appId).put(intentKey, endPair);
                }
            });
        });
        return currentMonitoredIntents;
    }

    @Override
    public Map<ApplicationId, Map<Key, Pair<Set<ElementId>, Set<ElementId>>>> getMonitoredIntents(
            ApplicationId appId) {
        Map<ApplicationId, Map<Key, Pair<Set<ElementId>, Set<ElementId>>>> currentMonitoredIntents
                = new ConcurrentHashMap<>();
        currentMonitoredIntents.put(appId, new ConcurrentHashMap<>());
        if (monitoredIntents.containsKey(appId)) {
            monitoredIntents.get(appId).forEach((intentKey, intent) -> {
                Pair<Set<ElementId>, Set<ElementId>> endPair = extractEndPoints(intent);
                if (endPair != null) {
                    currentMonitoredIntents.get(appId).put(intentKey, endPair);
                }
            });
        }
        return currentMonitoredIntents;
    }

    private Set<ElementId> connectedElements(Set<FilteredConnectPoint> cpSet) {
        Set<ElementId> connectedElem = new HashSet<>();
        cpSet.forEach(
            fcp -> {
                Set<Host> connectedHosts = hostService.getConnectedHosts(fcp.connectPoint());
                if (connectedHosts.size() == 0) {
                    // In this case the end point is an ELEMENT without host connected
                    connectedElem.add(fcp.connectPoint().elementId());
                } else {
                    // In this case we can have a set of hosts connected to that endpoint
                    connectedElem.addAll(connectedHosts.stream().map(Host::id)
                                   .collect(Collectors.toSet()));
                }
            }
        );
        return connectedElem;
    }

    /**
     * Extracts the endpoint from an intent.
     * @param intent
     * @return {@link Pair} containing in the Left element the set of input {@link ElementId},
     * in the Right element the set of output {@link ElementId}.
     */
    private Pair<Set<ElementId>, Set<ElementId>> extractEndPoints(Intent intent) {
        checkNotNull(intent, "intent must not be null");
        Pair<Set<FilteredConnectPoint>, Set<FilteredConnectPoint>> cpPair;
        cpPair = extractEndConnectPoints(intent);
        if (cpPair == null) {
            return null;
        }
        return Pair.of(connectedElements(cpPair.getLeft()), connectedElements(cpPair.getRight()));
    }

    /**
     * Returns the end connect points of an intent.
     * @param intent
     * @return {@link Pair} containing in the Left element the input end connect points,
     * in the Right element the output end connect points.
     */
    private Pair<Set<FilteredConnectPoint>, Set<FilteredConnectPoint>> extractEndConnectPoints(Intent intent) {
        checkNotNull(intent, "intent must not be null");

        Set<FilteredConnectPoint> inSet = new HashSet<>();
        Set<FilteredConnectPoint> outSet = new HashSet<>();
        if (intent instanceof PointToPointIntent) {
            inSet.add(((PointToPointIntent) intent).filteredIngressPoint());
            outSet.add(((PointToPointIntent) intent).filteredEgressPoint());
        } else if (intent instanceof LinkCollectionIntent) {
            inSet.addAll(((LinkCollectionIntent) intent).filteredIngressPoints());
            outSet.addAll(((LinkCollectionIntent) intent).filteredEgressPoints());
        }
        return Pair.of(inSet, outSet);
    }

    /**
     * Returns the connect point related to the output port of the rule.
     * @param rule
     * @return
     */
    private ConnectPoint buildConnectPoint(FlowRule rule) {
        PortNumber port = getOutput(rule);

        if (port == null) {
            return null;
        }
        return new ConnectPoint(rule.deviceId(), port);
    }

    /**
     * Returns the output port related to the rule.
     * @param rule
     * @return
     */
    private PortNumber getOutput(FlowRule rule) {
        for (Instruction i : rule.treatment().allInstructions()) {
            if (i.type() == Instruction.Type.OUTPUT) {
                Instructions.OutputInstruction out = (Instructions.OutputInstruction) i;
                return out.port();
            }
        }
        return null;
    }


    private class InternalIntentListener implements IntentListener {

        @Override
        public void event(IntentEvent event) {
            // It receives only events related to ConnectivityIntent to be monitored
            Key intentKey = event.subject().key();
            switch (event.type()) {
                case INSTALLED:
                    // When an intent is installed and it need to be monitored
                    // it will pass from the "toBeMonitored" state to the "monitored" state
                    log.info("Monitored intent INSTALLED");
                    storeMonitoredIntent((ConnectivityIntent) event.subject());
                    break;

                case WITHDRAWN:
                    // When an intent is withdrawn
                    // it will go back from the "monitored" state to the "toBeMonitored"
                    log.info("Monitored intent WITHDWRAWN");
                    removeIntent(intentKey);
                    break;

                case FAILED:
                    log.warn("FAILED event not handled");
                    break;
                default:
                    log.warn("Unknown intent event");
            }
        }

        @Override
        public boolean isRelevant(IntentEvent event) {
            /*
             * Check if the Intent event is relevant.
             * An intent event is relevant if it is of one of the allowed types
             * and if it is one of the monitored ones.
             */
            Key intentKey = event.subject().key();
            return allowedIntent(event.subject())
                    && toBeMonitoredIntents.contains(intentKey);
        }
    }

    private class InternalFlowRuleListener implements FlowRuleListener {
        @Override
        public void event(FlowRuleEvent event) {
            FlowRule rule = event.subject();
            switch (event.type()) {
                case RULE_ADDED:
                case RULE_UPDATED:
                    // In case of rule update, flow statistics are updated
                    if (rule instanceof FlowEntry) {
                        statsStore.updateFlowStatistic((FlowEntry) rule);
                    }
                    break;
                case RULE_REMOVED:
                    // In case of rule removal, flow statistics are removed from the store
                    log.info("Rule removed: {}", rule.id());
                    statsStore.removeFlowStatistic(rule);
                    break;
                default:
                    log.warn("Unknown flow rule event");
            }
        }

        @Override
        public boolean isRelevant(FlowRuleEvent event) {
            /*
            *  Check if the rule event is relevant and it needs to be managed
             * A Rule event is relevant if the flow rule it refers to is
             * part of one of the monitored intents
             */
            FlowRule rule = event.subject();
            for (Map.Entry<ApplicationId, Map<Key, ConnectivityIntent>> entry : monitoredIntents.entrySet()) {
                for (Key key : entry.getValue().keySet()) {
                    List<Intent> ints =  intentService.getInstallableIntents(key);
                    for (Intent i : ints) {
                        if (i instanceof FlowRuleIntent
                                && ((FlowRuleIntent) i).flowRules().contains(rule)) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }
    }
}