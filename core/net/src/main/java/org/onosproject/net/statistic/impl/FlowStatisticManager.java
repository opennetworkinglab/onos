/*
 * Copyright 2015-present Open Networking Laboratory
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

package org.onosproject.net.statistic.impl;

import com.google.common.base.MoreObjects;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;

import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.ElementId;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.DefaultTypedFlowEntry;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.FlowRuleEvent;
import org.onosproject.net.flow.FlowRuleListener;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.TypedStoredFlowEntry;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.statistic.DefaultLoad;
import org.onosproject.net.statistic.FlowStatisticService;
import org.onosproject.net.statistic.Load;
import org.onosproject.net.statistic.FlowStatisticStore;
import org.onosproject.net.statistic.SummaryFlowEntryWithLoad;
import org.onosproject.net.statistic.TypedFlowEntryWithLoad;

import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.security.AppGuard.checkPermission;
import static org.slf4j.LoggerFactory.getLogger;
import static org.onosproject.security.AppPermission.Type.*;

/**
 * Provides an implementation of the Flow Statistic Service.
 */
@Component(immediate = true)
@Service
public class FlowStatisticManager implements FlowStatisticService {
    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowRuleService flowRuleService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected FlowStatisticStore flowStatisticStore;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    private final InternalFlowRuleStatsListener frListener = new InternalFlowRuleStatsListener();

    // FIXME: refactor these comparators to be shared with the CLI implmentations
    public static final Comparator<ElementId> ELEMENT_ID_COMPARATOR = new Comparator<ElementId>() {
        @Override
        public int compare(ElementId id1, ElementId id2) {
            return id1.toString().compareTo(id2.toString());
        }
    };

    public static final Comparator<ConnectPoint> CONNECT_POINT_COMPARATOR = new Comparator<ConnectPoint>() {
        @Override
        public int compare(ConnectPoint o1, ConnectPoint o2) {
            int compareId = ELEMENT_ID_COMPARATOR.compare(o1.elementId(), o2.elementId());
            return (compareId != 0) ?
                    compareId :
                    Long.signum(o1.port().toLong() - o2.port().toLong());
        }
    };

    public static final Comparator<TypedFlowEntryWithLoad> TYPEFLOWENTRY_WITHLOAD_COMPARATOR =
            new Comparator<TypedFlowEntryWithLoad>() {
                @Override
                public int compare(TypedFlowEntryWithLoad fe1, TypedFlowEntryWithLoad fe2) {
                    long delta = fe1.load().rate() - fe2.load().rate();
                    return delta == 0 ? 0 : (delta > 0 ? -1 : +1);
                }
            };

    @Activate
    public void activate() {
        flowRuleService.addListener(frListener);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        flowRuleService.removeListener(frListener);
        log.info("Stopped");
    }

    @Override
    public Map<ConnectPoint, SummaryFlowEntryWithLoad> loadSummary(Device device) {
        checkPermission(STATISTIC_READ);

        Map<ConnectPoint, SummaryFlowEntryWithLoad> summaryLoad = new TreeMap<>(CONNECT_POINT_COMPARATOR);

        if (device == null) {
            return summaryLoad;
        }

        List<Port> ports = new ArrayList<>(deviceService.getPorts(device.id()));

        for (Port port : ports) {
            ConnectPoint cp = new ConnectPoint(device.id(), port.number());
            SummaryFlowEntryWithLoad sfe = loadSummaryPortInternal(cp);
            summaryLoad.put(cp, sfe);
        }

        return summaryLoad;
    }

    @Override
    public SummaryFlowEntryWithLoad loadSummary(Device device, PortNumber pNumber) {
        checkPermission(STATISTIC_READ);

        ConnectPoint cp = new ConnectPoint(device.id(), pNumber);
        return loadSummaryPortInternal(cp);
    }

    @Override
    public Map<ConnectPoint, List<TypedFlowEntryWithLoad>> loadAllByType(Device device,
                                                                  TypedStoredFlowEntry.FlowLiveType liveType,
                                                                  Instruction.Type instType) {
        checkPermission(STATISTIC_READ);

        Map<ConnectPoint, List<TypedFlowEntryWithLoad>> allLoad = new TreeMap<>(CONNECT_POINT_COMPARATOR);

        if (device == null) {
            return allLoad;
        }

        List<Port> ports = new ArrayList<>(deviceService.getPorts(device.id()));

        for (Port port : ports) {
            ConnectPoint cp = new ConnectPoint(device.id(), port.number());
            List<TypedFlowEntryWithLoad> tfel = loadAllPortInternal(cp, liveType, instType);
            allLoad.put(cp, tfel);
        }

        return allLoad;
    }

    @Override
    public List<TypedFlowEntryWithLoad> loadAllByType(Device device, PortNumber pNumber,
                                               TypedStoredFlowEntry.FlowLiveType liveType,
                                               Instruction.Type instType) {
        checkPermission(STATISTIC_READ);

        ConnectPoint cp = new ConnectPoint(device.id(), pNumber);
        return loadAllPortInternal(cp, liveType, instType);
    }

    @Override
    public Map<ConnectPoint, List<TypedFlowEntryWithLoad>> loadTopnByType(Device device,
                                                                   TypedStoredFlowEntry.FlowLiveType liveType,
                                                                   Instruction.Type instType,
                                                                   int topn) {
        checkPermission(STATISTIC_READ);

        Map<ConnectPoint, List<TypedFlowEntryWithLoad>> allLoad = new TreeMap<>(CONNECT_POINT_COMPARATOR);

        if (device == null) {
            return allLoad;
        }

        List<Port> ports = new ArrayList<>(deviceService.getPorts(device.id()));

        for (Port port : ports) {
            ConnectPoint cp = new ConnectPoint(device.id(), port.number());
            List<TypedFlowEntryWithLoad> tfel = loadTopnPortInternal(cp, liveType, instType, topn);
            allLoad.put(cp, tfel);
        }

        return allLoad;
    }

    @Override
    public List<TypedFlowEntryWithLoad> loadTopnByType(Device device, PortNumber pNumber,
                                                TypedStoredFlowEntry.FlowLiveType liveType,
                                                Instruction.Type instType,
                                                int topn) {
        checkPermission(STATISTIC_READ);

        ConnectPoint cp = new ConnectPoint(device.id(), pNumber);
        return loadTopnPortInternal(cp, liveType, instType, topn);
    }

    private SummaryFlowEntryWithLoad loadSummaryPortInternal(ConnectPoint cp) {
        checkPermission(STATISTIC_READ);

        Set<FlowEntry> currentStats;
        Set<FlowEntry> previousStats;

        TypedStatistics typedStatistics;
        synchronized (flowStatisticStore) {
             currentStats = flowStatisticStore.getCurrentFlowStatistic(cp);
            if (currentStats == null) {
                return new SummaryFlowEntryWithLoad(cp, new DefaultLoad());
            }
            previousStats = flowStatisticStore.getPreviousFlowStatistic(cp);
            if (previousStats == null) {
                return new SummaryFlowEntryWithLoad(cp, new DefaultLoad());
            }
            // copy to local flow entry
            typedStatistics = new TypedStatistics(currentStats, previousStats);

            // Check for validity of this stats data
            checkLoadValidity(currentStats, previousStats);
        }

        // current and previous set is not empty!
        Set<FlowEntry> currentSet = typedStatistics.current();
        Set<FlowEntry> previousSet = typedStatistics.previous();
        Load totalLoad = new DefaultLoad(aggregateBytesSet(currentSet), aggregateBytesSet(previousSet),
                TypedFlowEntryWithLoad.avgPollInterval());

        Map<FlowRule, TypedStoredFlowEntry> currentMap;
        Map<FlowRule, TypedStoredFlowEntry> previousMap;

        currentMap = typedStatistics.currentImmediate();
        previousMap = typedStatistics.previousImmediate();
        Load immediateLoad = new DefaultLoad(aggregateBytesMap(currentMap), aggregateBytesMap(previousMap),
                TypedFlowEntryWithLoad.shortPollInterval());

        currentMap = typedStatistics.currentShort();
        previousMap = typedStatistics.previousShort();
        Load shortLoad = new DefaultLoad(aggregateBytesMap(currentMap), aggregateBytesMap(previousMap),
                TypedFlowEntryWithLoad.shortPollInterval());

        currentMap = typedStatistics.currentMid();
        previousMap = typedStatistics.previousMid();
        Load midLoad = new DefaultLoad(aggregateBytesMap(currentMap), aggregateBytesMap(previousMap),
                TypedFlowEntryWithLoad.midPollInterval());

        currentMap = typedStatistics.currentLong();
        previousMap = typedStatistics.previousLong();
        Load longLoad = new DefaultLoad(aggregateBytesMap(currentMap), aggregateBytesMap(previousMap),
                TypedFlowEntryWithLoad.longPollInterval());

        currentMap = typedStatistics.currentUnknown();
        previousMap = typedStatistics.previousUnknown();
        Load unknownLoad = new DefaultLoad(aggregateBytesMap(currentMap), aggregateBytesMap(previousMap),
                TypedFlowEntryWithLoad.avgPollInterval());

        return new SummaryFlowEntryWithLoad(cp, totalLoad, immediateLoad, shortLoad, midLoad, longLoad, unknownLoad);
    }

    private List<TypedFlowEntryWithLoad> loadAllPortInternal(ConnectPoint cp,
                                                             TypedStoredFlowEntry.FlowLiveType liveType,
                                                             Instruction.Type instType) {
        checkPermission(STATISTIC_READ);

        List<TypedFlowEntryWithLoad> retTfel = new ArrayList<>();

        Set<FlowEntry> currentStats;
        Set<FlowEntry> previousStats;

        TypedStatistics typedStatistics;
        synchronized (flowStatisticStore) {
            currentStats = flowStatisticStore.getCurrentFlowStatistic(cp);
            if (currentStats == null) {
                return retTfel;
            }
            previousStats = flowStatisticStore.getPreviousFlowStatistic(cp);
            if (previousStats == null) {
                return retTfel;
            }
            // copy to local flow entry set
            typedStatistics = new TypedStatistics(currentStats, previousStats);

            // Check for validity of this stats data
            checkLoadValidity(currentStats, previousStats);
        }

        // current and previous set is not empty!
        boolean isAllLiveType = (liveType == null ? true : false); // null is all live type
        boolean isAllInstType = (instType == null ? true : false); // null is all inst type

        Map<FlowRule, TypedStoredFlowEntry> currentMap;
        Map<FlowRule, TypedStoredFlowEntry> previousMap;

        if (isAllLiveType || liveType == TypedStoredFlowEntry.FlowLiveType.IMMEDIATE_FLOW) {
            currentMap = typedStatistics.currentImmediate();
            previousMap = typedStatistics.previousImmediate();

            List<TypedFlowEntryWithLoad> fel = typedFlowEntryLoadByInstInternal(cp, currentMap, previousMap,
                    isAllInstType, instType, TypedFlowEntryWithLoad.shortPollInterval());
            if (fel.size() > 0) {
                retTfel.addAll(fel);
            }
        }

        if (isAllLiveType || liveType == TypedStoredFlowEntry.FlowLiveType.SHORT_FLOW) {
            currentMap = typedStatistics.currentShort();
            previousMap = typedStatistics.previousShort();

            List<TypedFlowEntryWithLoad> fel = typedFlowEntryLoadByInstInternal(cp, currentMap, previousMap,
                    isAllInstType, instType, TypedFlowEntryWithLoad.shortPollInterval());
            if (fel.size() > 0) {
                retTfel.addAll(fel);
            }
        }

        if (isAllLiveType || liveType == TypedStoredFlowEntry.FlowLiveType.MID_FLOW) {
            currentMap = typedStatistics.currentMid();
            previousMap = typedStatistics.previousMid();

            List<TypedFlowEntryWithLoad> fel = typedFlowEntryLoadByInstInternal(cp, currentMap, previousMap,
                    isAllInstType, instType, TypedFlowEntryWithLoad.midPollInterval());
            if (fel.size() > 0) {
                retTfel.addAll(fel);
            }
        }

        if (isAllLiveType || liveType == TypedStoredFlowEntry.FlowLiveType.LONG_FLOW) {
            currentMap = typedStatistics.currentLong();
            previousMap = typedStatistics.previousLong();

            List<TypedFlowEntryWithLoad> fel = typedFlowEntryLoadByInstInternal(cp, currentMap, previousMap,
                    isAllInstType, instType, TypedFlowEntryWithLoad.longPollInterval());
            if (fel.size() > 0) {
                retTfel.addAll(fel);
            }
        }

        if (isAllLiveType || liveType == TypedStoredFlowEntry.FlowLiveType.UNKNOWN_FLOW) {
            currentMap = typedStatistics.currentUnknown();
            previousMap = typedStatistics.previousUnknown();

            List<TypedFlowEntryWithLoad> fel = typedFlowEntryLoadByInstInternal(cp, currentMap, previousMap,
                    isAllInstType, instType, TypedFlowEntryWithLoad.avgPollInterval());
            if (fel.size() > 0) {
                retTfel.addAll(fel);
            }
        }

        return retTfel;
    }

    private List<TypedFlowEntryWithLoad> typedFlowEntryLoadByInstInternal(ConnectPoint cp,
                                                                      Map<FlowRule, TypedStoredFlowEntry> currentMap,
                                                                      Map<FlowRule, TypedStoredFlowEntry> previousMap,
                                                                      boolean isAllInstType,
                                                                      Instruction.Type instType,
                                                                      int liveTypePollInterval) {
        List<TypedFlowEntryWithLoad> fel = new ArrayList<>();

        for (TypedStoredFlowEntry tfe : currentMap.values()) {
            if (isAllInstType ||
                    tfe.treatment().allInstructions().stream().
                            filter(i -> i.type() == instType).
                            findAny().isPresent()) {
                long currentBytes = tfe.bytes();
                long previousBytes = previousMap.getOrDefault(tfe, new DefaultTypedFlowEntry((FlowRule) tfe)).bytes();
                Load fLoad = new DefaultLoad(currentBytes, previousBytes, liveTypePollInterval);
                fel.add(new TypedFlowEntryWithLoad(cp, tfe, fLoad));
            }
        }

        return fel;
    }

    private List<TypedFlowEntryWithLoad> loadTopnPortInternal(ConnectPoint cp,
                                                             TypedStoredFlowEntry.FlowLiveType liveType,
                                                             Instruction.Type instType,
                                                             int topn) {
        List<TypedFlowEntryWithLoad> fel = loadAllPortInternal(cp, liveType, instType);

        // Sort with descending order of load
        List<TypedFlowEntryWithLoad> tfel =
                fel.stream().sorted(TYPEFLOWENTRY_WITHLOAD_COMPARATOR).
                        limit(topn).collect(Collectors.toList());

        return tfel;
    }

    private long aggregateBytesSet(Set<FlowEntry> setFE) {
        return setFE.stream().mapToLong(FlowEntry::bytes).sum();
    }

    private long aggregateBytesMap(Map<FlowRule, TypedStoredFlowEntry> mapFE) {
        return mapFE.values().stream().mapToLong(FlowEntry::bytes).sum();
    }

    /**
     * Internal data class holding two set of typed flow entries.
     */
    private static class TypedStatistics {
        private final ImmutableSet<FlowEntry> currentAll;
        private final ImmutableSet<FlowEntry> previousAll;

        private final Map<FlowRule, TypedStoredFlowEntry> currentImmediate = new HashMap<>();
        private final Map<FlowRule, TypedStoredFlowEntry> previousImmediate = new HashMap<>();

        private final Map<FlowRule, TypedStoredFlowEntry> currentShort = new HashMap<>();
        private final Map<FlowRule, TypedStoredFlowEntry> previousShort = new HashMap<>();

        private final Map<FlowRule, TypedStoredFlowEntry> currentMid = new HashMap<>();
        private final Map<FlowRule, TypedStoredFlowEntry> previousMid = new HashMap<>();

        private final Map<FlowRule, TypedStoredFlowEntry> currentLong = new HashMap<>();
        private final Map<FlowRule, TypedStoredFlowEntry> previousLong = new HashMap<>();

        private final Map<FlowRule, TypedStoredFlowEntry> currentUnknown = new HashMap<>();
        private final Map<FlowRule, TypedStoredFlowEntry> previousUnknown = new HashMap<>();

        public TypedStatistics(Set<FlowEntry> current, Set<FlowEntry> previous) {
            this.currentAll = ImmutableSet.copyOf(checkNotNull(current));
            this.previousAll = ImmutableSet.copyOf(checkNotNull(previous));

            currentAll.forEach(fe -> {
                TypedStoredFlowEntry tfe = TypedFlowEntryWithLoad.newTypedStoredFlowEntry(fe);

                switch (tfe.flowLiveType()) {
                    case IMMEDIATE_FLOW:
                        currentImmediate.put(fe, tfe);
                        break;
                    case SHORT_FLOW:
                        currentShort.put(fe, tfe);
                        break;
                    case MID_FLOW:
                        currentMid.put(fe, tfe);
                        break;
                    case LONG_FLOW:
                        currentLong.put(fe, tfe);
                        break;
                    default:
                        currentUnknown.put(fe, tfe);
                        break;
                }
            });

            previousAll.forEach(fe -> {
                TypedStoredFlowEntry tfe = TypedFlowEntryWithLoad.newTypedStoredFlowEntry(fe);

                switch (tfe.flowLiveType()) {
                    case IMMEDIATE_FLOW:
                        if (currentImmediate.containsKey(fe)) {
                            previousImmediate.put(fe, tfe);
                        } else if (currentShort.containsKey(fe)) {
                            previousShort.put(fe, tfe);
                        } else if (currentMid.containsKey(fe)) {
                            previousMid.put(fe, tfe);
                        } else if (currentLong.containsKey(fe)) {
                            previousLong.put(fe, tfe);
                        } else {
                            previousUnknown.put(fe, tfe);
                        }
                        break;
                    case SHORT_FLOW:
                        if (currentShort.containsKey(fe)) {
                            previousShort.put(fe, tfe);
                        } else if (currentMid.containsKey(fe)) {
                            previousMid.put(fe, tfe);
                        } else if (currentLong.containsKey(fe)) {
                            previousLong.put(fe, tfe);
                        } else {
                            previousUnknown.put(fe, tfe);
                        }
                        break;
                    case MID_FLOW:
                        if (currentMid.containsKey(fe)) {
                            previousMid.put(fe, tfe);
                        } else if (currentLong.containsKey(fe)) {
                            previousLong.put(fe, tfe);
                        } else {
                            previousUnknown.put(fe, tfe);
                        }
                        break;
                    case LONG_FLOW:
                        if (currentLong.containsKey(fe)) {
                            previousLong.put(fe, tfe);
                        } else {
                            previousUnknown.put(fe, tfe);
                        }
                        break;
                    default:
                        previousUnknown.put(fe, tfe);
                        break;
                }
            });
        }

        /**
         * Returns flow entries as the current value.
         *
         * @return flow entries as the current value
         */
        public ImmutableSet<FlowEntry> current() {
            return currentAll;
        }

        /**
         * Returns flow entries as the previous value.
         *
         * @return flow entries as the previous value
         */
        public ImmutableSet<FlowEntry> previous() {
            return previousAll;
        }

        public Map<FlowRule, TypedStoredFlowEntry> currentImmediate() {
            return currentImmediate;
        }
        public Map<FlowRule, TypedStoredFlowEntry> previousImmediate() {
            return previousImmediate;
        }
        public Map<FlowRule, TypedStoredFlowEntry> currentShort() {
            return currentShort;
        }
        public Map<FlowRule, TypedStoredFlowEntry> previousShort() {
            return previousShort;
        }
        public Map<FlowRule, TypedStoredFlowEntry> currentMid() {
            return currentMid;
        }
        public Map<FlowRule, TypedStoredFlowEntry> previousMid() {
            return previousMid;
        }
        public Map<FlowRule, TypedStoredFlowEntry> currentLong() {
            return currentLong;
        }
        public Map<FlowRule, TypedStoredFlowEntry> previousLong() {
            return previousLong;
        }
        public Map<FlowRule, TypedStoredFlowEntry> currentUnknown() {
            return currentUnknown;
        }
        public Map<FlowRule, TypedStoredFlowEntry> previousUnknown() {
            return previousUnknown;
        }

        /**
         * Validates values are not empty.
         *
         * @return false if either of the sets is empty. Otherwise, true.
         */
        public boolean isValid() {
            return !(currentAll.isEmpty() || previousAll.isEmpty());
        }

        @Override
        public int hashCode() {
            return Objects.hash(currentAll, previousAll);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof TypedStatistics)) {
                return false;
            }
            final TypedStatistics other = (TypedStatistics) obj;
            return Objects.equals(this.currentAll, other.currentAll) &&
                    Objects.equals(this.previousAll, other.previousAll);
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("current", currentAll)
                    .add("previous", previousAll)
                    .toString();
        }
    }

    private void checkLoadValidity(Set<FlowEntry> current, Set<FlowEntry> previous) {
        current.forEach(c -> {
            FlowEntry f = previous.stream().filter(p -> c.equals(p)).
                    findAny().orElse(null);
            if (f != null && c.bytes() < f.bytes()) {
                log.debug("FlowStatisticManager:checkLoadValidity():" +
                        "Error: " + c + " :Previous bytes=" + f.bytes() +
                        " is larger than current bytes=" + c.bytes() + " !!!");
            }
        });

    }

    /**
     * Creates a predicate that checks the instruction type of a flow entry is the same as
     * the specified instruction type.
     *
     * @param instType instruction type to be checked
     * @return predicate
     */
    private static Predicate<FlowEntry> hasInstructionType(Instruction.Type instType) {
        return new Predicate<FlowEntry>() {
            @Override
            public boolean apply(FlowEntry flowEntry) {
                List<Instruction> allInstructions = flowEntry.treatment().allInstructions();

                return allInstructions.stream().filter(i -> i.type() == instType).findAny().isPresent();
            }
        };
    }

    /**
     * Internal flow rule event listener for FlowStatisticManager.
     */
    private class InternalFlowRuleStatsListener implements FlowRuleListener {

        @Override
        public void event(FlowRuleEvent event) {
            FlowRule rule = event.subject();
            switch (event.type()) {
                case RULE_ADDED:
                    if (rule instanceof FlowEntry) {
                        flowStatisticStore.addFlowStatistic((FlowEntry) rule);
                    }
                    break;
                case RULE_UPDATED:
                    flowStatisticStore.updateFlowStatistic((FlowEntry) rule);
                    break;
                case RULE_ADD_REQUESTED:
                    break;
                case RULE_REMOVE_REQUESTED:
                    break;
                case RULE_REMOVED:
                    flowStatisticStore.removeFlowStatistic(rule);
                    break;
                default:
                    log.warn("Unknown flow rule event {}", event);
            }
        }
    }
}
