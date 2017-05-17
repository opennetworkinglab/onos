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

package org.onosproject.provider.of.flow.impl;

import com.google.common.collect.Iterables;
import org.onlab.osgi.DefaultServiceDirectory;
import org.onosproject.net.DeviceId;
import org.onosproject.net.driver.DriverService;
import org.onosproject.net.flow.FlowEntry;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.flow.StoredFlowEntry;
import org.onosproject.net.flow.instructions.Instruction;
import org.onosproject.net.flow.instructions.Instructions;
import org.onosproject.net.statistic.DefaultLoad;
import org.onosproject.net.statistic.PollInterval;
import org.onosproject.openflow.controller.Dpid;
import org.onosproject.openflow.controller.OpenFlowSwitch;
import org.onosproject.openflow.controller.RoleState;
import org.projectfloodlight.openflow.protocol.OFFlowStatsRequest;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.TableId;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.Thread.sleep;
import static org.onlab.util.Tools.groupedThreads;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Efficiently and adaptively collects flow statistics for the specified switch.
 */
public class NewAdaptiveFlowStatsCollector implements SwitchDataCollector {
    private final Logger log = getLogger(getClass());

    private static final String CHECK_AND_MOVE_LOG =
            "checkAndMoveLiveFlowInternal: flowId={}, state={}, afterLiveType={}"
                    + ", liveTime={}, life={}, bytes={}, packets={}, fromLastSeen={}"
                    + ", priority={}, selector={}, treatment={} dpid={}";

    private static final String CHECK_AND_MOVE_COUNT_LOG =
            "checkAndMoveLiveFlowAll: Total Flow_Count={}, "
                    + ", IMMEDIATE_FLOW_Count={}, SHORT_FLOW_Count={}"
                    + ", MID_FLOW_Count={}, LONG_FLOW_Count={}, UNKNOWN_FLOW_Count={}";

    private static final int SLEEP_LOOP_COUNT = 10;
    private static final int SLEEP_MS = 100;

    private final DriverService driverService;
    private final OpenFlowSwitch sw;
    private final DeviceId did;

    private ScheduledExecutorService adaptiveFlowStatsScheduler =
            Executors.newScheduledThreadPool(4, groupedThreads("onos/flow", "device-stats-collector-%d", log));
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

    private boolean isFirstTimeStart = true;

    public static final long NO_FLOW_MISSING_XID = (-1);
    private long flowMissingXid = NO_FLOW_MISSING_XID;

    private FlowRuleService flowRuleService;

    /**
     * Creates a new adaptive collector for the given switch and default cal_and_poll frequency.
     *
     * @param driverService driver service reference
     * @param sw            switch to pull
     * @param pollInterval  cal and immediate poll frequency in seconds
     */
    NewAdaptiveFlowStatsCollector(DriverService driverService, OpenFlowSwitch sw, int pollInterval) {
        this.driverService = driverService;
        this.sw = sw;
        this.did = DeviceId.deviceId(Dpid.uri(sw.getId()));

        flowRuleService = get(FlowRuleService.class);

        initMemberVars(pollInterval);
    }

    /**
     * Returns the reference to the implementation of the specified service.
     *
     * @param serviceClass service class
     * @param <T>          type of service
     * @return service implementation
     * @throws org.onlab.osgi.ServiceNotFoundException if service is unavailable
     */
    private static <T> T get(Class<T> serviceClass) {
        return DefaultServiceDirectory.getService(serviceClass);
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

        // Set the PollInterval values for statistic manager and others usage
        DefaultLoad.setPollInterval(calAndPollInterval);

        PollInterval pollInterval1Instance = PollInterval.getInstance();

        pollInterval1Instance.setPollInterval(calAndPollInterval);
        pollInterval1Instance.setMidPollInterval(midPollInterval);
        pollInterval1Instance.setLongPollInterval(longPollInterval);
        pollInterval1Instance.setEntirePollInterval(entirePollInterval);

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

        log.debug("calAndPollInterval={} is adjusted", calAndPollInterval);
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
                } else  if (callCountCalAndShortFlowsTask >= ENTIRE_POLL_TIMES) {
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
    private synchronized void ofFlowStatsRequestAllSend() {
        OFFlowStatsRequest request = sw.factory().buildFlowStatsRequest()
                .setMatch(sw.factory().matchWildcardAll())
                .setTableId(TableId.ALL)
                .setOutPort(OFPort.NO_MASK)
                .build();

        // set the request xid to check the reply in OpenFlowRuleProvider
        // After processing the reply of this request message,
        // this must be set to NO_FLOW_MISSING_XID(-1) by provider
        setFlowMissingXid(request.getXid());
        log.debug("ofFlowStatsRequestAllSend: request={}, dpid={}",
                    request.toString(), sw.getStringId());

        sw.sendMsg(request);
    }

    // send openflow flow stats request message with getting the specific flow entry(fe) to a given switch sw
    private void ofFlowStatsRequestFlowSend(FlowEntry fe) {
        // set find match
        Match match = FlowModBuilder.builder(fe, sw.factory(), Optional.empty(),
                Optional.of(driverService)).buildMatch();
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

        // Wait for 1 second until the FlowRuleProvider finishes to process FlowStatReply message
        int loop = 0;
        boolean interrupted = false;
        while (!interrupted && getFlowMissingXid() != NO_FLOW_MISSING_XID) {
            if (loop++ < SLEEP_LOOP_COUNT) {
                log.debug("ofFlowStatsRequestFlowSend: previous FlowStatsRequestAll (xid={})" +
                                  " does not be processed yet, do sleep for {} ms, for {}",
                          getFlowMissingXid(),
                          SLEEP_MS,
                          sw.getStringId());
                try {
                    sleep(SLEEP_MS);
                } catch (InterruptedException ie) {
                    log.debug("ofFlowStatsRequestFlowSend: Interrupted Exception = {}, for {}",
                              ie.toString(),
                              sw.getStringId());
                    // for exiting while loop gracefully
                    interrupted = true;
                }
            } else {
                log.debug("ofFlowStatsRequestFlowSend: previous FlowStatsRequestAll (xid={})" +
                                  " does not be processed yet, for {} ms," +
                                  " just set xid with NO_FLOW_MISSING_XID, for {}",
                          getFlowMissingXid(),
                          loop * SLEEP_MS,
                          sw.getStringId());

                setFlowMissingXid(NO_FLOW_MISSING_XID);
                break;
            }
        }

        sw.sendMsg(request);

    }

    private void calAndShortFlowsTaskInternal() {
        checkAndMoveLiveFlowAll();

        ofFlowStatsRequestInternal(FlowEntry.FlowLiveType.SHORT);
    }

    private void ofFlowStatsRequestInternal(FlowEntry.FlowLiveType liveType) {

        Iterable<FlowEntry> flowEntries =
                flowRuleService.getFlowEntriesByLiveType(did, liveType);

        flowEntries.forEach(fe -> {
            ofFlowStatsRequestFlowSend(fe);
        });
    }

    private class MidFlowsTask implements Runnable {
        @Override
        public void run() {
            if (sw.getRole() == RoleState.MASTER) {
                log.trace("MidFlowsTask Collecting AdaptiveStats for {}", sw.getStringId());

                // skip collecting because CalAndShortFlowsTask collects entire flow stats from a given switch sw
                if (callCountMidFlowsTask >= ENTIRE_POLL_TIMES) {
                    callCountMidFlowsTask = MID_POLL_TIMES;
                } else {
                    midFlowsTaskInternal();
                    callCountMidFlowsTask += MID_POLL_TIMES;
                }
            }
        }
    }

    private void midFlowsTaskInternal() {
        ofFlowStatsRequestInternal(FlowEntry.FlowLiveType.MID);
    }

    private class LongFlowsTask implements Runnable {
        @Override
        public void run() {
            if (sw.getRole() == RoleState.MASTER) {
                log.trace("LongFlowsTask Collecting AdaptiveStats for {}", sw.getStringId());

                // skip collecting because CalAndShortFlowsTask collects entire flow stats from a given switch sw
                if (callCountLongFlowsTask >= ENTIRE_POLL_TIMES) {
                    callCountLongFlowsTask = LONG_POLL_TIMES;
                } else {
                    longFlowsTaskInternal();
                    callCountLongFlowsTask += LONG_POLL_TIMES;
                }
            }
        }
    }

    private void longFlowsTaskInternal() {
        ofFlowStatsRequestInternal(FlowEntry.FlowLiveType.LONG);
    }

    /**
     * Starts adaptive flow statistic collection.
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
     * Stops adaptive flow statistic collection.
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
     * Returns flowMissingXid that indicates the execution of flowMissing process or not(NO_FLOW_MISSING_XID(-1)).
     *
     * @return xid of missing flow
     */
    public long getFlowMissingXid() {
        return flowMissingXid;
    }

    /**
     * Sets flowMissingXid, namely OFFlowStatsRequest match any ALL message Id.
     *
     * @param flowMissingXid the OFFlowStatsRequest message Id
     */
    public void setFlowMissingXid(long flowMissingXid) {
        this.flowMissingXid = flowMissingXid;
    }

    /**
     * Calculates the flow live type.
     *
     * @param life the flow life time in seconds
     * @return computed flow live type
     */
    public FlowEntry.FlowLiveType calFlowLiveType(long life) {
        if (life < 0) {
            return FlowEntry.FlowLiveType.UNKNOWN;
        } else if (life < calAndPollInterval) {
            return FlowEntry.FlowLiveType.IMMEDIATE;
        } else if (life < midPollInterval) {
            return FlowEntry.FlowLiveType.SHORT;
        } else if (life < longPollInterval) {
            return FlowEntry.FlowLiveType.MID;
        } else { // >= longPollInterval
            return FlowEntry.FlowLiveType.LONG;
        }
    }

    /**
     * Calculates and set the flow live type.
     * It maybe called pushFlowMetrics of FlowRuleService for the ReplyFlowStat message
     * at the first time and every entire polling time.
     *
     * @param fe the flow entry rule
     * @return computed flow live type
     */
    public FlowEntry.FlowLiveType calAndSetFlowLiveType(StoredFlowEntry fe) {
        checkNotNull(fe);

        long life = fe.life();

        if (life < 0) {
            fe.setLiveType(FlowEntry.FlowLiveType.UNKNOWN);
        } else if (life < calAndPollInterval) {
            fe.setLiveType(FlowEntry.FlowLiveType.IMMEDIATE);
        } else if (life < midPollInterval) {
            fe.setLiveType(FlowEntry.FlowLiveType.SHORT);
        } else if (life < longPollInterval) {
            fe.setLiveType(FlowEntry.FlowLiveType.MID);
        } else { // >= longPollInterval
            fe.setLiveType(FlowEntry.FlowLiveType.LONG);
        }

        return fe.liveType();
    }

    /**
     * Check and move live type for all type flow entries in table at every calAndPollInterval time.
     *
     */
    private void checkAndMoveLiveFlowAll() {

        Iterable<FlowEntry> flowEntries = flowRuleService.getFlowEntries(did);

        flowEntries.forEach(fe -> {
            checkAndMoveLiveFlowInternal((StoredFlowEntry) fe);
        });

        // print table counts for debug
        if (log.isDebugEnabled()) {
            Iterable<FlowEntry> fes;
            synchronized (this) {
                long totalFlowCount = flowRuleService.getFlowRuleCount();
                fes = flowRuleService.getFlowEntriesByLiveType(
                        did, FlowEntry.FlowLiveType.IMMEDIATE);
                long immediateFlowCount = Iterables.size(fes);
                fes = flowRuleService.getFlowEntriesByLiveType(
                        did, FlowEntry.FlowLiveType.SHORT);
                long shortFlowCount = Iterables.size(fes);
                fes = flowRuleService.getFlowEntriesByLiveType(
                        did, FlowEntry.FlowLiveType.MID);
                long midFlowCount = Iterables.size(fes);
                fes = flowRuleService.getFlowEntriesByLiveType(
                        did, FlowEntry.FlowLiveType.LONG);
                long longFlowCount = Iterables.size(fes);
                fes = flowRuleService.getFlowEntriesByLiveType(
                        did, FlowEntry.FlowLiveType.UNKNOWN);
                long unknownFlowCount = Iterables.size(fes);

                log.trace(CHECK_AND_MOVE_COUNT_LOG, totalFlowCount,
                            immediateFlowCount, shortFlowCount, midFlowCount, longFlowCount, unknownFlowCount);

                if (immediateFlowCount < 0) {
                    log.error("Immediate flow count is negative");
                }
            }
        }
        log.trace("checkAndMoveLiveFlowAll, AdaptiveStats for {}", sw.getStringId());
    }

    // check and set the flow live type based on current time
    private boolean checkAndMoveLiveFlowInternal(StoredFlowEntry fe) {
        long fromLastSeen = ((System.currentTimeMillis() - fe.lastSeen()) / 1000);
        // fe.life() unit is SECOND!
        long liveTime = fe.life() + fromLastSeen;

        FlowEntry.FlowLiveType oldLiveType = fe.liveType();

        switch (fe.liveType()) {
            case IMMEDIATE:
                if (liveTime >= calAndPollInterval) {
                    fe.setLiveType(FlowEntry.FlowLiveType.SHORT);
                }
                break;
            case SHORT:
                if (liveTime >= midPollInterval) {
                    fe.setLiveType(FlowEntry.FlowLiveType.MID);
                }
                break;
            case MID:
                if (liveTime >= longPollInterval) {
                    fe.setLiveType(FlowEntry.FlowLiveType.LONG);
                }
                break;
            case LONG:
                if (fromLastSeen > entirePollInterval) {
                    log.trace("checkAndMoveLiveFlowInternal, flow may be already removed at switch.");
                    return false;
                }
                break;
            case UNKNOWN: // Unknown live type is calculated and set with correct flow live type here.
                calAndSetFlowLiveType(fe);
                break;
            default:
                // Error Live Type
                log.error("checkAndMoveLiveFlowInternal, Unknown Live Type error!"
                            + " AdaptiveStats collection thread for {}",
                            sw.getStringId());
                return false;
        }

        if (log.isTraceEnabled()) {
            log.trace(CHECK_AND_MOVE_LOG, fe.id(), fe.state(), fe.liveType(),
                        liveTime, fe.life(), fe.bytes(), fe.packets(), fromLastSeen,
                        fe.priority(), fe.selector().criteria(), fe.treatment(),
                        sw.getStringId());
        }

        return true;
    }
}
