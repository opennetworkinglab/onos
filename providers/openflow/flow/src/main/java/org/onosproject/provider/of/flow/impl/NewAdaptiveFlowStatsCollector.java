/*
 * Copyright 2015 Open Networking Laboratory
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

package org.onosproject.provider.of.flow.impl;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import org.onosproject.net.flow.DefaultTypedFlowEntry;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowId;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flow.StoredFlowEntry;
import org.onosproject.net.flow.TypedStoredFlowEntry;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.openflow.controller.OpenFlowSwitch;
import org.onosproject.openflow.controller.RoleState;
import org.projectfloodlight.openflow.protocol.OFFlowStatsRequest;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.TableId;
import org.slf4j.Logger;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.net.flow.TypedStoredFlowEntry.*;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Efficiently and adaptively collects flow statistics for the specified switch.
 */
public class NewAdaptiveFlowStatsCollector {

    private final Logger log = getLogger(getClass());

    private final OpenFlowSwitch sw;

    private ScheduledExecutorService adaptiveFlowStatsScheduler =
            Executors.newScheduledThreadPool(4, groupedThreads("onos/flow", "device-stats-collector-%d"));
    private ScheduledFuture<?> calAndShortFlowsThread;
    private ScheduledFuture<?> midFlowsThread;
    private ScheduledFuture<?> longFlowsThread;

    // Task that calculates all flowEntries' FlowLiveType and collects stats IMMEDIATE flows every calAndPollInterval
    private CalAndShortFlowsTask calAndShortFlowsTask;
    // Task that collects stats MID flows every 2*calAndPollInterval
    private MidFlowsTask midFlowsTask;
    // Task that collects stats LONG flows every 3*calAndPollInterval
    private LongFlowsTask longFlowsTask;

    private static final int CAL_AND_POLL_TIMES = 1; // must be always 0
    private static final int MID_POLL_TIMES = 2;     // variable greater or equal than 1
    private static final int LONG_POLL_TIMES = 3;    // variable greater or equal than MID_POLL_TIMES
    //TODO: make ENTIRE_POLL_TIMES configurable with enable or disable
    // must be variable greater or equal than common multiple of MID_POLL_TIMES and LONG_POLL_TIMES
    private static final int ENTIRE_POLL_TIMES = 6;

    private static final int DEFAULT_CAL_AND_POLL_FREQUENCY = 5;
    private static final int MIN_CAL_AND_POLL_FREQUENCY = 2;
    private static final int MAX_CAL_AND_POLL_FREQUENCY = 60;

    private int calAndPollInterval; // CAL_AND_POLL_TIMES * DEFAULT_CAL_AND_POLL_FREQUENCY;
    private int midPollInterval; // MID_POLL_TIMES * DEFAULT_CAL_AND_POLL_FREQUENCY;
    private int longPollInterval; // LONG_POLL_TIMES * DEFAULT_CAL_AND_POLL_FREQUENCY;
    // only used for checking condition at each task if it collects entire flows from a given switch or not
    private int entirePollInterval; // ENTIRE_POLL_TIMES * DEFAULT_CAL_AND_POLL_FREQUENCY;

    // Number of call count of each Task,
    // for undoing collection except only entire flows collecting task in CalAndShortFlowsTask
    private int callCountCalAndShortFlowsTask = 0; // increased CAL_AND_POLL_TIMES whenever Task is called
    private int callCountMidFlowsTask = 0;   // increased MID_POLL_TIMES whenever Task is called
    private int callCountLongFlowsTask = 0;  // increased LONG_POLL_TIMES whenever Task is called

    private InternalDeviceFlowTable deviceFlowTable = new InternalDeviceFlowTable();

    private boolean isFirstTimeStart = true;

    public static final long NO_FLOW_MISSING_XID = (-1);
    private long flowMissingXid = NO_FLOW_MISSING_XID;

    /**
     * Creates a new adaptive collector for the given switch and default cal_and_poll frequency.
     *
     * @param sw           switch to pull
     * @param pollInterval cal and immediate poll frequency in seconds
     */
    NewAdaptiveFlowStatsCollector(OpenFlowSwitch sw, int pollInterval) {
        this.sw = sw;

        initMemberVars(pollInterval);
    }

    // check calAndPollInterval validity and set all pollInterval values and finally initialize each task call count
    private void initMemberVars(int pollInterval) {
        if (pollInterval < MIN_CAL_AND_POLL_FREQUENCY) {
            this.calAndPollInterval = MIN_CAL_AND_POLL_FREQUENCY;
        } else if (pollInterval >= MAX_CAL_AND_POLL_FREQUENCY) {
            this.calAndPollInterval = MAX_CAL_AND_POLL_FREQUENCY;
        } else {
            this.calAndPollInterval = pollInterval;
        }

        calAndPollInterval = CAL_AND_POLL_TIMES * calAndPollInterval;
        midPollInterval = MID_POLL_TIMES * calAndPollInterval;
        longPollInterval = LONG_POLL_TIMES * calAndPollInterval;
        entirePollInterval = ENTIRE_POLL_TIMES * calAndPollInterval;

        callCountCalAndShortFlowsTask = 0;
        callCountMidFlowsTask = 0;
        callCountLongFlowsTask = 0;

        flowMissingXid = NO_FLOW_MISSING_XID;
    }

    /**
     * Adjusts adaptive poll frequency.
     *
     * @param pollInterval poll frequency in seconds
     */
    synchronized void adjustCalAndPollInterval(int pollInterval) {
        initMemberVars(pollInterval);

        if (calAndShortFlowsThread != null) {
            calAndShortFlowsThread.cancel(false);
        }
        if (midFlowsThread != null) {
            midFlowsThread.cancel(false);
        }
        if (longFlowsThread != null) {
            longFlowsThread.cancel(false);
        }

        calAndShortFlowsTask = new CalAndShortFlowsTask();
        calAndShortFlowsThread = adaptiveFlowStatsScheduler.scheduleWithFixedDelay(
                calAndShortFlowsTask,
                0,
                calAndPollInterval,
                TimeUnit.SECONDS);

        midFlowsTask = new MidFlowsTask();
        midFlowsThread = adaptiveFlowStatsScheduler.scheduleWithFixedDelay(
                midFlowsTask,
                0,
                midPollInterval,
                TimeUnit.SECONDS);

        longFlowsTask = new LongFlowsTask();
        longFlowsThread = adaptiveFlowStatsScheduler.scheduleWithFixedDelay(
                longFlowsTask,
                0,
                longPollInterval,
                TimeUnit.SECONDS);

        log.debug("calAndPollInterval=" + calAndPollInterval + "is adjusted");
    }

    private class CalAndShortFlowsTask implements Runnable {
        @Override
        public void run() {
            if (sw.getRole() == RoleState.MASTER) {
                log.trace("CalAndShortFlowsTask Collecting AdaptiveStats for {}", sw.getStringId());

                if (isFirstTimeStart) {
                    // isFirstTimeStart, get entire flow stats from a given switch sw
                    log.trace("CalAndShortFlowsTask Collecting Entire AdaptiveStats at first time start for {}",
                            sw.getStringId());
                    ofFlowStatsRequestAllSend();

                    callCountCalAndShortFlowsTask += CAL_AND_POLL_TIMES;
                    isFirstTimeStart = false;
                } else  if (callCountCalAndShortFlowsTask == ENTIRE_POLL_TIMES) {
                    // entire_poll_times, get entire flow stats from a given switch sw
                    log.trace("CalAndShortFlowsTask Collecting Entire AdaptiveStats for {}", sw.getStringId());
                    ofFlowStatsRequestAllSend();

                    callCountCalAndShortFlowsTask = CAL_AND_POLL_TIMES;
                    //TODO: check flows deleted in switch, but exist in controller flow table, then remove them
                    //
                } else {
                    calAndShortFlowsTaskInternal();
                    callCountCalAndShortFlowsTask += CAL_AND_POLL_TIMES;
                }
            }
        }
    }

    // send openflow flow stats request message with getting all flow entries to a given switch sw
    private void ofFlowStatsRequestAllSend() {
        OFFlowStatsRequest request = sw.factory().buildFlowStatsRequest()
                .setMatch(sw.factory().matchWildcardAll())
                .setTableId(TableId.ALL)
                .setOutPort(OFPort.NO_MASK)
                .build();

        synchronized (this) {
            // set the request xid to check the reply in OpenFlowRuleProvider
            // After processing the reply of this request message,
            // this must be set to NO_FLOW_MISSING_XID(-1) by provider
            setFlowMissingXid(request.getXid());
            log.debug("ofFlowStatsRequestAllSend,Request={},for {}", request.toString(), sw.getStringId());

            sw.sendMsg(request);
        }
    }

    // send openflow flow stats request message with getting the specific flow entry(fe) to a given switch sw
    private void ofFlowStatsRequestFlowSend(FlowEntry fe) {
        // set find match
        Match match = FlowModBuilder.builder(fe, sw.factory(), Optional.empty()).buildMatch();
        // set find tableId
        TableId tableId = TableId.of(fe.tableId());
        // set output port
        Instruction ins = fe.treatment().allInstructions().stream()
                .filter(i -> (i.type() == Instruction.Type.OUTPUT))
                .findFirst()
                .orElse(null);
        OFPort ofPort = OFPort.NO_MASK;
        if (ins != null) {
            Instructions.OutputInstruction out = (Instructions.OutputInstruction) ins;
            ofPort = OFPort.of((int) ((out.port().toLong())));
        }

        OFFlowStatsRequest request = sw.factory().buildFlowStatsRequest()
                .setMatch(match)
                .setTableId(tableId)
                .setOutPort(ofPort)
                .build();

        synchronized (this) {
            if (getFlowMissingXid() != NO_FLOW_MISSING_XID) {
                log.debug("ofFlowStatsRequestFlowSend: previous FlowStatsRequestAll does not be processed yet,"
                                + " set no flow missing xid anyway, for {}",
                        sw.getStringId());
                setFlowMissingXid(NO_FLOW_MISSING_XID);
            }

            sw.sendMsg(request);
        }
    }

    private void calAndShortFlowsTaskInternal() {
        deviceFlowTable.checkAndMoveLiveFlowAll();

        deviceFlowTable.getShortFlows().forEach(fe -> {
            ofFlowStatsRequestFlowSend(fe);
        });
    }

    private class MidFlowsTask implements Runnable {
        @Override
        public void run() {
            if (sw.getRole() == RoleState.MASTER) {
                log.trace("MidFlowsTask Collecting AdaptiveStats for {}", sw.getStringId());

                // skip collecting because CalAndShortFlowsTask collects entire flow stats from a given switch sw
                if (callCountMidFlowsTask == ENTIRE_POLL_TIMES) {
                    callCountMidFlowsTask = MID_POLL_TIMES;
                } else {
                    midFlowsTaskInternal();
                    callCountMidFlowsTask += MID_POLL_TIMES;
                }
            }
        }
    }

    private void midFlowsTaskInternal() {
        deviceFlowTable.getMidFlows().forEach(fe -> {
            ofFlowStatsRequestFlowSend(fe);
        });
    }

    private class LongFlowsTask implements Runnable {
        @Override
        public void run() {
            if (sw.getRole() == RoleState.MASTER) {
                log.trace("LongFlowsTask Collecting AdaptiveStats for {}", sw.getStringId());

                // skip collecting because CalAndShortFlowsTask collects entire flow stats from a given switch sw
                if (callCountLongFlowsTask == ENTIRE_POLL_TIMES) {
                    callCountLongFlowsTask = LONG_POLL_TIMES;
                } else {
                    longFlowsTaskInternal();
                    callCountLongFlowsTask += LONG_POLL_TIMES;
                }
            }
        }
    }

    private void longFlowsTaskInternal() {
        deviceFlowTable.getLongFlows().forEach(fe -> {
            ofFlowStatsRequestFlowSend(fe);
        });
    }

    /**
     * start adaptive flow statistic collection.
     *
     */
    public synchronized void start() {
        log.debug("Starting AdaptiveStats collection thread for {}", sw.getStringId());
        callCountCalAndShortFlowsTask = 0;
        callCountMidFlowsTask = 0;
        callCountLongFlowsTask = 0;

        isFirstTimeStart = true;

        // Initially start polling quickly. Then drop down to configured value
        calAndShortFlowsTask = new CalAndShortFlowsTask();
        calAndShortFlowsThread = adaptiveFlowStatsScheduler.scheduleWithFixedDelay(
                calAndShortFlowsTask,
                1,
                calAndPollInterval,
                TimeUnit.SECONDS);

        midFlowsTask = new MidFlowsTask();
        midFlowsThread = adaptiveFlowStatsScheduler.scheduleWithFixedDelay(
                midFlowsTask,
                1,
                midPollInterval,
                TimeUnit.SECONDS);

        longFlowsTask = new LongFlowsTask();
        longFlowsThread = adaptiveFlowStatsScheduler.scheduleWithFixedDelay(
                longFlowsTask,
                1,
                longPollInterval,
                TimeUnit.SECONDS);

        log.info("Started");
    }

    /**
     * stop adaptive flow statistic collection.
     *
     */
    public synchronized void stop() {
        log.debug("Stopping AdaptiveStats collection thread for {}", sw.getStringId());
        if (calAndShortFlowsThread != null) {
            calAndShortFlowsThread.cancel(true);
        }
        if (midFlowsThread != null) {
            midFlowsThread.cancel(true);
        }
        if (longFlowsThread != null) {
            longFlowsThread.cancel(true);
        }

        adaptiveFlowStatsScheduler.shutdownNow();

        isFirstTimeStart = false;

        log.info("Stopped");
    }

    /**
     * add typed flow entry from flow rule into the internal flow table.
     *
     * @param flowRules the flow rules
     *
     */
    public synchronized void addWithFlowRule(FlowRule... flowRules) {
        for (FlowRule fr : flowRules) {
            // First remove old entry unconditionally, if exist
            deviceFlowTable.remove(fr);

            // add new flow entry, we suppose IMMEDIATE_FLOW
            TypedStoredFlowEntry newFlowEntry = new DefaultTypedFlowEntry(fr,
                    FlowLiveType.IMMEDIATE_FLOW);
            deviceFlowTable.addWithCalAndSetFlowLiveType(newFlowEntry);
        }
    }

    /**
     * add or update typed flow entry from flow entry into the internal flow table.
     *
     * @param flowEntries the flow entries
     *
     */
    public synchronized void addOrUpdateFlows(FlowEntry... flowEntries) {
       for (FlowEntry fe : flowEntries) {
           // check if this new rule is an update to an existing entry
           TypedStoredFlowEntry stored = deviceFlowTable.getFlowEntry(fe);

           if (stored != null) {
               // duplicated flow entry is collected!, just skip
               if (fe.bytes() == stored.bytes() && fe.packets() == stored.packets()
                       && fe.life() == stored.life()) {
                   log.debug("addOrUpdateFlows:, FlowId=" + Long.toHexString(fe.id().value())
                                   + ",is DUPLICATED stats collection, just skip."
                                   + " AdaptiveStats collection thread for {}",
                           sw.getStringId());

                   stored.setLastSeen();
                   continue;
               } else if (fe.life() < stored.life()) {
                   // Invalid updates the stats values, i.e., bytes, packets, durations ...
                   log.debug("addOrUpdateFlows():" +
                               " Invalid Flow Update! The new life is SMALLER than the previous one, jus skip." +
                               " new flowId=" + Long.toHexString(fe.id().value()) +
                               ", old flowId=" + Long.toHexString(stored.id().value()) +
                               ", new bytes=" + fe.bytes() + ", old bytes=" + stored.bytes() +
                               ", new life=" + fe.life() + ", old life=" + stored.life() +
                               ", new lastSeen=" + fe.lastSeen() + ", old lastSeen=" + stored.lastSeen());
                   // go next
                   stored.setLastSeen();
                   continue;
               }

               // update now
               stored.setLife(fe.life());
               stored.setPackets(fe.packets());
               stored.setBytes(fe.bytes());
               stored.setLastSeen();
               if (stored.state() == FlowEntry.FlowEntryState.PENDING_ADD) {
                   // flow is really RULE_ADDED
                   stored.setState(FlowEntry.FlowEntryState.ADDED);
               }
               // flow is RULE_UPDATED, skip adding and just updating flow live table
               //deviceFlowTable.calAndSetFlowLiveType(stored);
               continue;
           }

           // add new flow entry, we suppose IMMEDIATE_FLOW
           TypedStoredFlowEntry newFlowEntry = new DefaultTypedFlowEntry(fe,
                    FlowLiveType.IMMEDIATE_FLOW);
           deviceFlowTable.addWithCalAndSetFlowLiveType(newFlowEntry);
        }
    }

    /**
     * remove typed flow entry from the internal flow table.
     *
     * @param flowRules the flow entries
     *
     */
    public synchronized void removeFlows(FlowRule...  flowRules) {
        for (FlowRule rule : flowRules) {
            deviceFlowTable.remove(rule);
        }
    }

    // same as removeFlows() function
    /**
     * remove typed flow entry from the internal flow table.
     *
     * @param flowRules the flow entries
     *
     */
    public void flowRemoved(FlowRule... flowRules) {
        removeFlows(flowRules);
    }

    // same as addOrUpdateFlows() function
    /**
     * add or update typed flow entry from flow entry into the internal flow table.
     *
     * @param flowEntries the flow entry list
     *
     */
    public void pushFlowMetrics(List<FlowEntry> flowEntries) {
        flowEntries.forEach(fe -> {
            addOrUpdateFlows(fe);
        });
    }

    /**
     * returns flowMissingXid that indicates the execution of flowMissing process or not(NO_FLOW_MISSING_XID(-1)).
     *
     * @return xid of missing flow
     */
    public long getFlowMissingXid() {
        return flowMissingXid;
    }

    /**
     * set flowMissingXid, namely OFFlowStatsRequest match any ALL message Id.
     *
     * @param flowMissingXid the OFFlowStatsRequest message Id
     *
     */
    public void setFlowMissingXid(long flowMissingXid) {
        this.flowMissingXid = flowMissingXid;
    }

    private class InternalDeviceFlowTable {

        private final Map<FlowId, Set<TypedStoredFlowEntry>>
                flowEntries = Maps.newConcurrentMap();

        private final Set<StoredFlowEntry> shortFlows = new HashSet<>();
        private final Set<StoredFlowEntry> midFlows = new HashSet<>();
        private final Set<StoredFlowEntry> longFlows = new HashSet<>();

        // Assumed latency adjustment(default=500 millisecond) between FlowStatsRequest and Reply
        private final long latencyFlowStatsRequestAndReplyMillis = 500;


        // Statistics for table operation
        private long addCount = 0, addWithSetFlowLiveTypeCount = 0;
        private long removeCount = 0;

        /**
         * Resets all count values with zero.
         *
         */
        public void resetAllCount() {
            addCount = 0;
            addWithSetFlowLiveTypeCount = 0;
            removeCount = 0;
        }

        // get set of flow entries for the given flowId
        private Set<TypedStoredFlowEntry> getFlowEntriesInternal(FlowId flowId) {
            return flowEntries.computeIfAbsent(flowId, id -> Sets.newCopyOnWriteArraySet());
        }

        // get flow entry for the given flow rule
        private TypedStoredFlowEntry getFlowEntryInternal(FlowRule rule) {
            Set<TypedStoredFlowEntry> flowEntries = getFlowEntriesInternal(rule.id());
            return flowEntries.stream()
                    .filter(entry -> Objects.equal(entry, rule))
                    .findAny()
                    .orElse(null);
        }

        // get the flow entries for all flows in flow table
        private Set<TypedStoredFlowEntry> getFlowEntriesInternal() {
            Set<TypedStoredFlowEntry> result = Sets.newHashSet();

            flowEntries.values().forEach(result::addAll);
            return result;
        }

        /**
         * Gets the number of flow entry in flow table.
         *
         * @return the number of flow entry.
         *
         */
        public long getFlowCount() {
            return flowEntries.values().stream().mapToLong(Set::size).sum();
        }

        /**
         * Gets the number of flow entry in flow table.
         *
         * @param rule the flow rule
         * @return the typed flow entry.
         *
         */
        public TypedStoredFlowEntry getFlowEntry(FlowRule rule) {
            checkNotNull(rule);

            return getFlowEntryInternal(rule);
        }

        /**
         * Gets the all typed flow entries in flow table.
         *
         * @return the set of typed flow entry.
         *
         */
        public Set<TypedStoredFlowEntry> getFlowEntries() {
            return getFlowEntriesInternal();
        }

        /**
         * Gets the short typed flow entries in flow table.
         *
         * @return the set of typed flow entry.
         *
         */
        public Set<StoredFlowEntry> getShortFlows() {
            return ImmutableSet.copyOf(shortFlows); //Sets.newHashSet(shortFlows);
        }

        /**
         * Gets the mid typed flow entries in flow table.
         *
         * @return the set of typed flow entry.
         *
         */
        public Set<StoredFlowEntry> getMidFlows() {
            return ImmutableSet.copyOf(midFlows); //Sets.newHashSet(midFlows);
        }

        /**
         * Gets the long typed flow entries in flow table.
         *
         * @return the set of typed flow entry.
         *
         */
        public Set<StoredFlowEntry> getLongFlows() {
            return ImmutableSet.copyOf(longFlows); //Sets.newHashSet(longFlows);
        }

        /**
         * Add typed flow entry into table only.
         *
         * @param rule the flow rule
         *
         */
        public synchronized void add(TypedStoredFlowEntry rule) {
            checkNotNull(rule);

            //rule have to be new DefaultTypedFlowEntry
            boolean result = getFlowEntriesInternal(rule.id()).add(rule);

            if (result) {
                addCount++;
            }
        }

        /**
         * Calculates and set the flow live type at the first time,
         * and then add it into a corresponding typed flow table.
         *
         * @param rule the flow rule
         *
         */
        public void calAndSetFlowLiveType(TypedStoredFlowEntry rule) {
            checkNotNull(rule);

            calAndSetFlowLiveTypeInternal(rule);
        }

        /**
         * Add the typed flow entry into table, and calculates and set the flow live type,
         * and then add it into a corresponding typed flow table.
         *
         * @param rule the flow rule
         *
         */
       public synchronized void addWithCalAndSetFlowLiveType(TypedStoredFlowEntry rule) {
            checkNotNull(rule);

            //rule have to be new DefaultTypedFlowEntry
            boolean result = getFlowEntriesInternal(rule.id()).add(rule);
            if (result) {
                calAndSetFlowLiveTypeInternal(rule);
                addWithSetFlowLiveTypeCount++;
            } else {
                log.debug("addWithCalAndSetFlowLiveType, FlowId=" + Long.toHexString(rule.id().value())
                                + " ADD Failed, cause it may already exists in table !!!,"
                                + " AdaptiveStats collection thread for {}",
                        sw.getStringId());
            }
        }

        // In real, calculates and set the flow live type at the first time,
        // and then add it into a corresponding typed flow table
        private void calAndSetFlowLiveTypeInternal(TypedStoredFlowEntry rule) {
            long life = rule.life();
            FlowLiveType prevFlowLiveType = rule.flowLiveType();

            if (life >= longPollInterval) {
                rule.setFlowLiveType(FlowLiveType.LONG_FLOW);
                longFlows.add(rule);
            } else if (life >= midPollInterval) {
                rule.setFlowLiveType(FlowLiveType.MID_FLOW);
                midFlows.add(rule);
            } else if (life >= calAndPollInterval) {
                rule.setFlowLiveType(FlowLiveType.SHORT_FLOW);
                shortFlows.add(rule);
            } else if (life >= 0) {
                rule.setFlowLiveType(FlowLiveType.IMMEDIATE_FLOW);
            } else { // life < 0
                rule.setFlowLiveType(FlowLiveType.UNKNOWN_FLOW);
            }

            if (rule.flowLiveType() != prevFlowLiveType) {
                switch (prevFlowLiveType) {
                    // delete it from previous flow table
                    case SHORT_FLOW:
                        shortFlows.remove(rule);
                        break;
                    case MID_FLOW:
                        midFlows.remove(rule);
                        break;
                    case LONG_FLOW:
                        longFlows.remove(rule);
                        break;
                    default:
                        break;
                }
            }
        }


        // check the flow live type based on current time, then set and add it into corresponding table
        private boolean checkAndMoveLiveFlowInternal(TypedStoredFlowEntry fe, long cTime) {
            long curTime = (cTime > 0 ? cTime : System.currentTimeMillis());
            // For latency adjustment(default=500 millisecond) between FlowStatsRequest and Reply
            long fromLastSeen = ((curTime - fe.lastSeen() + latencyFlowStatsRequestAndReplyMillis) / 1000);
            // fe.life() unit is SECOND!
            long liveTime = fe.life() + fromLastSeen;


            switch (fe.flowLiveType()) {
                case IMMEDIATE_FLOW:
                    if (liveTime >= longPollInterval) {
                        fe.setFlowLiveType(FlowLiveType.LONG_FLOW);
                         longFlows.add(fe);
                    } else if (liveTime >= midPollInterval) {
                        fe.setFlowLiveType(FlowLiveType.MID_FLOW);
                        midFlows.add(fe);
                    } else if (liveTime >= calAndPollInterval) {
                        fe.setFlowLiveType(FlowLiveType.SHORT_FLOW);
                        shortFlows.add(fe);
                    }
                    break;
                case SHORT_FLOW:
                    if (liveTime >= longPollInterval) {
                        fe.setFlowLiveType(FlowLiveType.LONG_FLOW);
                        shortFlows.remove(fe);
                        longFlows.add(fe);
                    } else if (liveTime >= midPollInterval) {
                        fe.setFlowLiveType(FlowLiveType.MID_FLOW);
                        shortFlows.remove(fe);
                        midFlows.add(fe);
                    }
                    break;
                case MID_FLOW:
                    if (liveTime >= longPollInterval) {
                        fe.setFlowLiveType(FlowLiveType.LONG_FLOW);
                        midFlows.remove(fe);
                        longFlows.add(fe);
                    }
                    break;
                case LONG_FLOW:
                    if (fromLastSeen > entirePollInterval) {
                        log.trace("checkAndMoveLiveFlowInternal, flow is already removed at switch.");
                        return false;
                    }
                    break;
                case UNKNOWN_FLOW: // Unknown flow is an internal error flow type, just fall through
                default :
                    // Error Unknown Live Type
                    log.error("checkAndMoveLiveFlowInternal, Unknown Live Type error!"
                            + "AdaptiveStats collection thread for {}",
                            sw.getStringId());
                    return false;
            }

            log.debug("checkAndMoveLiveFlowInternal, FlowId=" + Long.toHexString(fe.id().value())
                            + ", state=" + fe.state()
                            + ", After liveType=" + fe.flowLiveType()
                            + ", liveTime=" + liveTime
                            + ", life=" + fe.life()
                            + ", bytes=" + fe.bytes()
                            + ", packets=" + fe.packets()
                            + ", fromLastSeen=" + fromLastSeen
                            + ", priority=" + fe.priority()
                            + ", selector=" + fe.selector().criteria()
                            + ", treatment=" + fe.treatment()
                            + " AdaptiveStats collection thread for {}",
                    sw.getStringId());

            return true;
        }

        /**
         * Check and move live type for all type flow entries in table at every calAndPollInterval time.
         *
         */
        public void checkAndMoveLiveFlowAll() {
            Set<TypedStoredFlowEntry> typedFlowEntries = getFlowEntriesInternal();

            long calCurTime = System.currentTimeMillis();
            typedFlowEntries.forEach(fe -> {
                if (!checkAndMoveLiveFlowInternal(fe, calCurTime)) {
                    remove(fe);
                }
            });

            // print table counts for debug
            if (log.isDebugEnabled()) {
                synchronized (this) {
                    long totalFlowCount = getFlowCount();
                    long shortFlowCount = shortFlows.size();
                    long midFlowCount = midFlows.size();
                    long longFlowCount = longFlows.size();
                    long immediateFlowCount = totalFlowCount - shortFlowCount - midFlowCount - longFlowCount;
                    long calTotalCount = addCount + addWithSetFlowLiveTypeCount - removeCount;

                    log.debug("--------------------------------------------------------------------------- for {}",
                            sw.getStringId());
                    log.debug("checkAndMoveLiveFlowAll, Total Flow_Count=" + totalFlowCount
                            + ", add - remove_Count=" + calTotalCount
                            + ", IMMEDIATE_FLOW_Count=" + immediateFlowCount
                            + ", SHORT_FLOW_Count=" + shortFlowCount
                            + ", MID_FLOW_Count=" + midFlowCount
                            + ", LONG_FLOW_Count=" + longFlowCount
                            + ", add_Count=" + addCount
                            + ", addWithSetFlowLiveType_Count=" + addWithSetFlowLiveTypeCount
                            + ", remove_Count=" + removeCount
                            + " AdaptiveStats collection thread for {}", sw.getStringId());
                    log.debug("--------------------------------------------------------------------------- for {}",
                            sw.getStringId());
                    if (totalFlowCount != calTotalCount) {
                        log.error("checkAndMoveLiveFlowAll, Real total flow count and "
                                + "calculated total flow count do NOT match, something is wrong internally "
                                + "or check counter value bound is over!");
                    }
                    if (immediateFlowCount < 0) {
                        log.error("checkAndMoveLiveFlowAll, IMMEDIATE_FLOW count is negative, "
                                + "something is wrong internally "
                                + "or check counter value bound is over!");
                    }
                }
            }
            log.trace("checkAndMoveLiveFlowAll, AdaptiveStats for {}", sw.getStringId());
        }

        /**
         * Remove the typed flow entry from table.
         *
         * @param rule the flow rule
         *
         */
        public synchronized void remove(FlowRule rule) {
            checkNotNull(rule);

            TypedStoredFlowEntry removeStore = getFlowEntryInternal(rule);
            if (removeStore != null) {
                removeLiveFlowsInternal((TypedStoredFlowEntry) removeStore);
                boolean result = getFlowEntriesInternal(rule.id()).remove(removeStore);

                if (result) {
                    removeCount++;
                }
            }
       }

        // Remove the typed flow entry from corresponding table
        private void removeLiveFlowsInternal(TypedStoredFlowEntry fe) {
            switch (fe.flowLiveType()) {
                case IMMEDIATE_FLOW:
                    // do nothing
                    break;
                case SHORT_FLOW:
                    shortFlows.remove(fe);
                    break;
                case MID_FLOW:
                    midFlows.remove(fe);
                    break;
                case LONG_FLOW:
                    longFlows.remove(fe);
                    break;
                default: // error in Flow Live Type
                    log.error("removeLiveFlowsInternal, Unknown Live Type error!");
                    break;
            }
        }
    }
}
