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

package org.onosproject.net.statistic.impl;

import com.google.common.base.MoreObjects;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.Device;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.DefaultFlowEntry;
import org.onosproject.net.flow.DefaultTypedFlowEntry;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.StoredFlowEntry;
import org.onosproject.net.flow.TypedStoredFlowEntry;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.statistic.DefaultLoad;
import org.onosproject.net.statistic.FlowEntryWithLoad;
import org.onosproject.net.statistic.FlowStatisticService;
import org.onosproject.net.statistic.Load;
import org.onosproject.net.statistic.PollInterval;
import org.onosproject.net.statistic.StatisticStore;
import org.onosproject.net.statistic.SummaryFlowEntryWithLoad;
import org.onosproject.net.statistic.TypedFlowEntryWithLoad;
import org.onosproject.utils.Comparators;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.security.AppGuard.checkPermission;
import static org.onosproject.security.AppPermission.Type.STATISTIC_READ;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Provides an implementation of the Flow Statistic Service.
 */
@Component(immediate = true, service = FlowStatisticService.class)
public class FlowStatisticManager implements FlowStatisticService {
    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected StatisticStore statisticStore;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected DeviceService deviceService;

    @Activate
    public void activate() {
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }

    @Override
    public Map<ConnectPoint, SummaryFlowEntryWithLoad> loadSummary(Device device) {
        checkPermission(STATISTIC_READ);

        Map<ConnectPoint, SummaryFlowEntryWithLoad> summaryLoad =
                                        new TreeMap<>(Comparators.CONNECT_POINT_COMPARATOR);

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
    public Map<ConnectPoint, List<FlowEntryWithLoad>> loadAllByType(Device device,
                                                                  FlowEntry.FlowLiveType liveType,
                                                                  Instruction.Type instType) {
        checkPermission(STATISTIC_READ);

        Map<ConnectPoint, List<FlowEntryWithLoad>> allLoad =
                                        new TreeMap<>(Comparators.CONNECT_POINT_COMPARATOR);

        if (device == null) {
            return allLoad;
        }

        List<Port> ports = new ArrayList<>(deviceService.getPorts(device.id()));

        for (Port port : ports) {
            ConnectPoint cp = new ConnectPoint(device.id(), port.number());
            List<FlowEntryWithLoad> fel = loadAllPortInternal(cp, liveType, instType);
            allLoad.put(cp, fel);
        }

        return allLoad;
    }

    @Override
    public List<FlowEntryWithLoad> loadAllByType(Device device, PortNumber pNumber,
                                               FlowEntry.FlowLiveType liveType,
                                               Instruction.Type instType) {
        checkPermission(STATISTIC_READ);

        ConnectPoint cp = new ConnectPoint(device.id(), pNumber);
        return loadAllPortInternal(cp, liveType, instType);
    }

    @Override
    public Map<ConnectPoint, List<FlowEntryWithLoad>> loadTopnByType(Device device,
                                                                   FlowEntry.FlowLiveType liveType,
                                                                   Instruction.Type instType,
                                                                   int topn) {
        checkPermission(STATISTIC_READ);

        Map<ConnectPoint, List<FlowEntryWithLoad>> allLoad =
                                        new TreeMap<>(Comparators.CONNECT_POINT_COMPARATOR);

        if (device == null) {
            return allLoad;
        }

        List<Port> ports = new ArrayList<>(deviceService.getPorts(device.id()));

        for (Port port : ports) {
            ConnectPoint cp = new ConnectPoint(device.id(), port.number());
            List<FlowEntryWithLoad> fel = loadTopnPortInternal(cp, liveType, instType, topn);
            allLoad.put(cp, fel);
        }

        return allLoad;
    }

    @Override
    public List<FlowEntryWithLoad> loadTopnByType(Device device, PortNumber pNumber,
                                                FlowEntry.FlowLiveType liveType,
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
        synchronized (statisticStore) {
             currentStats = statisticStore.getCurrentStatistic(cp);
            if (currentStats == null) {
                return new SummaryFlowEntryWithLoad(cp, new DefaultLoad());
            }
            previousStats = statisticStore.getPreviousStatistic(cp);
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
        PollInterval pollIntervalInstance = PollInterval.getInstance();

        // We assume that default pollInterval is flowPollFrequency in case adaptiveFlowSampling is true or false
        Load totalLoad = new DefaultLoad(aggregateBytesSet(currentSet), aggregateBytesSet(previousSet),
                                         pollIntervalInstance.getPollInterval());

        Map<FlowRule, FlowEntry> currentMap;
        Map<FlowRule, FlowEntry> previousMap;

        currentMap = typedStatistics.currentImmediate();
        previousMap = typedStatistics.previousImmediate();
        Load immediateLoad = new DefaultLoad(aggregateBytesMap(currentMap), aggregateBytesMap(previousMap),
                                             pollIntervalInstance.getPollInterval());

        currentMap = typedStatistics.currentShort();
        previousMap = typedStatistics.previousShort();
        Load shortLoad = new DefaultLoad(aggregateBytesMap(currentMap), aggregateBytesMap(previousMap),
                                         pollIntervalInstance.getPollInterval());

        currentMap = typedStatistics.currentMid();
        previousMap = typedStatistics.previousMid();
        Load midLoad = new DefaultLoad(aggregateBytesMap(currentMap), aggregateBytesMap(previousMap),
                                       pollIntervalInstance.getMidPollInterval());

        currentMap = typedStatistics.currentLong();
        previousMap = typedStatistics.previousLong();
        Load longLoad = new DefaultLoad(aggregateBytesMap(currentMap), aggregateBytesMap(previousMap),
                                        pollIntervalInstance.getLongPollInterval());

        currentMap = typedStatistics.currentUnknown();
        previousMap = typedStatistics.previousUnknown();
        Load unknownLoad = new DefaultLoad(aggregateBytesMap(currentMap), aggregateBytesMap(previousMap),
                                           pollIntervalInstance.getPollInterval());

        return new SummaryFlowEntryWithLoad(cp, totalLoad, immediateLoad, shortLoad, midLoad, longLoad, unknownLoad);
    }

    private List<FlowEntryWithLoad> loadAllPortInternal(ConnectPoint cp,
                                                             FlowEntry.FlowLiveType liveType,
                                                             Instruction.Type instType) {
        checkPermission(STATISTIC_READ);

        List<FlowEntryWithLoad> retFel = new ArrayList<>();

        Set<FlowEntry> currentStats;
        Set<FlowEntry> previousStats;

        TypedStatistics typedStatistics;
        synchronized (statisticStore) {
            currentStats = statisticStore.getCurrentStatistic(cp);
            if (currentStats == null) {
                return retFel;
            }
            previousStats = statisticStore.getPreviousStatistic(cp);
            if (previousStats == null) {
                return retFel;
            }
            // copy to local flow entry set
            typedStatistics = new TypedStatistics(currentStats, previousStats);

            // Check for validity of this stats data
            checkLoadValidity(currentStats, previousStats);
        }

        // current and previous set is not empty!
        boolean isAllInstType = (instType == null ? true : false); // null is all inst type
        boolean isAllLiveType = (liveType == null ? true : false); // null is all live type

        Map<FlowRule, FlowEntry> currentMap;
        Map<FlowRule, FlowEntry> previousMap;

        if (isAllLiveType) {
            currentMap = typedStatistics.currentAll();
            previousMap = typedStatistics.previousAll();
        } else {
            switch (liveType) {
                case IMMEDIATE:
                    currentMap = typedStatistics.currentImmediate();
                    previousMap = typedStatistics.previousImmediate();
                    break;
                case SHORT:
                    currentMap = typedStatistics.currentShort();
                    previousMap = typedStatistics.previousShort();
                    break;
                case MID:
                    currentMap = typedStatistics.currentMid();
                    previousMap = typedStatistics.previousMid();
                    break;
                case LONG:
                    currentMap = typedStatistics.currentLong();
                    previousMap = typedStatistics.previousLong();
                    break;
                case UNKNOWN:
                    currentMap = typedStatistics.currentUnknown();
                    previousMap = typedStatistics.previousUnknown();
                    break;
                default:
                    currentMap = new HashMap<>();
                    previousMap = new HashMap<>();
                    break;
            }
        }

        return typedFlowEntryLoadByInstInternal(cp, currentMap, previousMap, isAllInstType, instType);
    }

    private List<FlowEntryWithLoad> typedFlowEntryLoadByInstInternal(ConnectPoint cp,
                                                                      Map<FlowRule, FlowEntry> currentMap,
                                                                      Map<FlowRule, FlowEntry> previousMap,
                                                                      boolean isAllInstType,
                                                                      Instruction.Type instType) {
        List<FlowEntryWithLoad> fel = new ArrayList<>();

        currentMap.values().forEach(fe -> {
            if (isAllInstType ||
                    fe.treatment().allInstructions().stream().
                            filter(i -> i.type() == instType).
                            findAny().isPresent()) {
                long currentBytes = fe.bytes();
                long previousBytes = previousMap.getOrDefault(fe, new DefaultFlowEntry(fe)).bytes();
                long liveTypePollInterval = getLiveTypePollInterval(fe.liveType());
                Load fLoad = new DefaultLoad(currentBytes, previousBytes, liveTypePollInterval);
                fel.add(new FlowEntryWithLoad(cp, fe, fLoad));
            }
        });

        return fel;
    }

    private List<FlowEntryWithLoad> loadTopnPortInternal(ConnectPoint cp,
                                                             FlowEntry.FlowLiveType liveType,
                                                             Instruction.Type instType,
                                                             int topn) {
        List<FlowEntryWithLoad> fel = loadAllPortInternal(cp, liveType, instType);

        // Sort with descending order of load
        List<FlowEntryWithLoad> retFel =
                fel.stream().sorted(Comparators.FLOWENTRY_WITHLOAD_COMPARATOR).
                        limit(topn).collect(Collectors.toList());

        return retFel;
    }

    private long aggregateBytesSet(Set<FlowEntry> setFE) {
        return setFE.stream().mapToLong(FlowEntry::bytes).sum();
    }

    private long aggregateBytesMap(Map<FlowRule, FlowEntry> mapFE) {
        return mapFE.values().stream().mapToLong(FlowEntry::bytes).sum();
    }

    private long getLiveTypePollInterval(FlowEntry.FlowLiveType liveType) {
        // returns the flow live type poll interval value
        PollInterval pollIntervalInstance = PollInterval.getInstance();

        switch (liveType) {
            case LONG:
                return pollIntervalInstance.getLongPollInterval();
            case MID:
                return pollIntervalInstance.getMidPollInterval();
            case SHORT:
            case IMMEDIATE:
            default: // UNKNOWN
                return pollIntervalInstance.getPollInterval();
        }
    }

    private TypedStoredFlowEntry.FlowLiveType toTypedStoredFlowEntryLiveType(FlowEntry.FlowLiveType liveType) {
        if (liveType == null) {
            return null;
        }

        // convert TypedStoredFlowEntry flow live type to FlowEntry one
        switch (liveType) {
            case IMMEDIATE:
                return TypedStoredFlowEntry.FlowLiveType.IMMEDIATE_FLOW;
            case SHORT:
                return TypedStoredFlowEntry.FlowLiveType.SHORT_FLOW;
            case MID:
                return TypedStoredFlowEntry.FlowLiveType.MID_FLOW;
            case LONG:
                return TypedStoredFlowEntry.FlowLiveType.LONG_FLOW;
            default:
                return TypedStoredFlowEntry.FlowLiveType.UNKNOWN_FLOW;
        }
    }

    private Map<ConnectPoint, List<TypedFlowEntryWithLoad>> toFlowEntryWithLoadMap(
            Map<ConnectPoint, List<FlowEntryWithLoad>> loadMap) {
        // convert FlowEntryWithLoad list to TypedFlowEntryWithLoad list
        Map<ConnectPoint, List<TypedFlowEntryWithLoad>> allLoad =
                                        new TreeMap<>(Comparators.CONNECT_POINT_COMPARATOR);

        loadMap.forEach((k, v) -> {
            List<TypedFlowEntryWithLoad> tfelList =
                    toFlowEntryWithLoad(v);
            allLoad.put(k, tfelList);
        });

        return allLoad;
    }

    private List<TypedFlowEntryWithLoad> toFlowEntryWithLoad(List<FlowEntryWithLoad> loadList) {
        // convert FlowEntryWithLoad list to TypedFlowEntryWithLoad list
        List<TypedFlowEntryWithLoad> tfelList = new ArrayList<>();
        loadList.forEach(fel -> {
            StoredFlowEntry sfe = fel.storedFlowEntry();
            TypedStoredFlowEntry.FlowLiveType liveType = toTypedStoredFlowEntryLiveType(sfe.liveType());
            TypedStoredFlowEntry tfe = new DefaultTypedFlowEntry(sfe, liveType);
            TypedFlowEntryWithLoad tfel = new TypedFlowEntryWithLoad(fel.connectPoint(), tfe, fel.load());
            tfelList.add(tfel);
        });

        return tfelList;
    }

    /**
     * Internal data class holding two set of flow entries included flow liveType.
     */
    private static class TypedStatistics {
        private final ImmutableSet<FlowEntry> current;
        private final ImmutableSet<FlowEntry> previous;

        private final Map<FlowRule, FlowEntry> currentAll = new HashMap<>();
        private final Map<FlowRule, FlowEntry> previousAll = new HashMap<>();

        private final Map<FlowRule, FlowEntry> currentImmediate = new HashMap<>();
        private final Map<FlowRule, FlowEntry> previousImmediate = new HashMap<>();

        private final Map<FlowRule, FlowEntry> currentShort = new HashMap<>();
        private final Map<FlowRule, FlowEntry> previousShort = new HashMap<>();

        private final Map<FlowRule, FlowEntry> currentMid = new HashMap<>();
        private final Map<FlowRule, FlowEntry> previousMid = new HashMap<>();

        private final Map<FlowRule, FlowEntry> currentLong = new HashMap<>();
        private final Map<FlowRule, FlowEntry> previousLong = new HashMap<>();

        private final Map<FlowRule, FlowEntry> currentUnknown = new HashMap<>();
        private final Map<FlowRule, FlowEntry> previousUnknown = new HashMap<>();

        public TypedStatistics(Set<FlowEntry> current, Set<FlowEntry> previous) {
            this.current = ImmutableSet.copyOf(checkNotNull(current));
            this.previous = ImmutableSet.copyOf(checkNotNull(previous));

            current.forEach(fe -> {
                switch (fe.liveType()) {
                    case IMMEDIATE:
                        currentImmediate.put(fe, fe);
                        break;
                    case SHORT:
                        currentShort.put(fe, fe);
                        break;
                    case MID:
                        currentMid.put(fe, fe);
                        break;
                    case LONG:
                        currentLong.put(fe, fe);
                        break;
                    default: // unknown
                        currentUnknown.put(fe, fe);
                        break;
                }
                currentAll.put(fe, fe);
            });

            previous.forEach(fe -> {
                switch (fe.liveType()) {
                    case IMMEDIATE:
                        if (currentImmediate.containsKey(fe)) {
                            previousImmediate.put(fe, fe);
                        } else if (currentShort.containsKey(fe)) {
                            previousShort.put(fe, fe);
                        } else if (currentMid.containsKey(fe)) {
                            previousMid.put(fe, fe);
                        } else if (currentLong.containsKey(fe)) {
                            previousLong.put(fe, fe);
                        } else {
                            previousUnknown.put(fe, fe);
                        }
                        break;
                    case SHORT:
                        if (currentShort.containsKey(fe)) {
                            previousShort.put(fe, fe);
                        } else if (currentMid.containsKey(fe)) {
                            previousMid.put(fe, fe);
                        } else if (currentLong.containsKey(fe)) {
                            previousLong.put(fe, fe);
                        } else {
                            previousUnknown.put(fe, fe);
                        }
                        break;
                    case MID:
                        if (currentMid.containsKey(fe)) {
                            previousMid.put(fe, fe);
                        } else if (currentLong.containsKey(fe)) {
                            previousLong.put(fe, fe);
                        } else {
                            previousUnknown.put(fe, fe);
                        }
                        break;
                    case LONG:
                        if (currentLong.containsKey(fe)) {
                            previousLong.put(fe, fe);
                        } else {
                            previousUnknown.put(fe, fe);
                        }
                        break;
                    default: // unknown
                        previousUnknown.put(fe, fe);
                        break;
                }
                previousAll.put(fe, fe);
            });
        }

        /**
         * Returns flow entries as the current value.
         *
         * @return flow entries as the current value
         */
        public ImmutableSet<FlowEntry> current() {
            return current;
        }

        /**
         * Returns flow entries as the previous value.
         *
         * @return flow entries as the previous value
         */
        public ImmutableSet<FlowEntry> previous() {
            return previous;
        }

        public Map<FlowRule, FlowEntry> currentAll() {
            return currentAll;
        }

        public Map<FlowRule, FlowEntry> previousAll() {
            return previousAll;
        }

        public Map<FlowRule, FlowEntry> currentImmediate() {
            return currentImmediate;
        }
        public Map<FlowRule, FlowEntry> previousImmediate() {
            return previousImmediate;
        }
        public Map<FlowRule, FlowEntry> currentShort() {
            return currentShort;
        }
        public Map<FlowRule, FlowEntry> previousShort() {
            return previousShort;
        }
        public Map<FlowRule, FlowEntry> currentMid() {
            return currentMid;
        }
        public Map<FlowRule, FlowEntry> previousMid() {
            return previousMid;
        }
        public Map<FlowRule, FlowEntry> currentLong() {
            return currentLong;
        }
        public Map<FlowRule, FlowEntry> previousLong() {
            return previousLong;
        }
        public Map<FlowRule, FlowEntry> currentUnknown() {
            return currentUnknown;
        }
        public Map<FlowRule, FlowEntry> previousUnknown() {
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
     * Creates a predicate that checks the flow type of a flow entry is the same as
     * the specified live type.
     *
     * @param liveType flow live type to be checked
     * @return predicate
     */
    private static Predicate<FlowEntry> hasLiveType(FlowEntry.FlowLiveType liveType) {
        return flowEntry -> flowEntry.liveType() == liveType;
    }
}
