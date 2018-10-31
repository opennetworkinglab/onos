/*
 * Copyright 2015-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onosproject.mlb;

import org.onlab.util.Tools;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.LeadershipEvent;
import org.onosproject.cluster.LeadershipEventListener;
import org.onosproject.cluster.LeadershipService;
import org.onosproject.cluster.NodeId;
import org.onosproject.mastership.MastershipAdminService;
import org.onosproject.mastership.MastershipEvent;
import org.onosproject.mastership.MastershipListener;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.region.RegionEvent;
import org.onosproject.net.region.RegionListener;
import org.onosproject.net.region.RegionService;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;

import java.util.Dictionary;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.mlb.OsgiPropertyConstants.SCHEDULE_PERIOD;
import static org.onosproject.mlb.OsgiPropertyConstants.SCHEDULE_PERIOD_DEFAULT;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * An app to perform automatic load balancing in response to events.  Load balancing events are triggered by any
 * change in mastership and are limited to a frequency of one every 30 seconds, all load balancing is run on an outside
 * thread executor that must only have one thread due to issues that can occur is multiple balancing events occur in
 * parallel.
 */
@Component(
    immediate = true,
    property = {
        SCHEDULE_PERIOD + ":Integer=" + SCHEDULE_PERIOD_DEFAULT
    }
)
public class MastershipLoadBalancer {

    private final Logger log = getLogger(getClass());

    /** Period to schedule balancing the mastership to be shared as evenly as by all online instances. */
    private int schedulePeriod = SCHEDULE_PERIOD_DEFAULT;

    private static final String REBALANCE_MASTERSHIP = "rebalance/mastership";

    private NodeId localId;

    private AtomicBoolean isLeader = new AtomicBoolean(false);

    private AtomicReference<Future> nextTask = new AtomicReference<>();

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected MastershipAdminService mastershipAdminService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected LeadershipService leadershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected RegionService regionService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ComponentConfigService cfgService;

    private InnerLeadershipListener leadershipListener = new InnerLeadershipListener();

    /* This listener is used to trigger balancing for any mastership event
     * which will include switches changing state between active and inactive
     * states as well as the same variety of event occurring with ONOS nodes.
     */
    private InnerMastershipListener mastershipListener = new InnerMastershipListener();

    /* Used to trigger balancing on region events where there was either a
     * change on the master sets of a given region or a change on the devices
     * that belong to a region.
     */
    private InnerRegionListener regionEventListener = new InnerRegionListener();

    /* Ensures that all executions do not interfere with one another (single
     * thread) and that they are apart from each other by at least what is
     * defined as the schedulePeriod.
     */
    private ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor(
            groupedThreads("MastershipLoadBalancer", "%d", log));

    @Activate
    public void activate(ComponentContext context) {
        cfgService.registerProperties(getClass());
        modified(context);
        mastershipService.addListener(mastershipListener);
        localId = clusterService.getLocalNode().id();
        leadershipService.addListener(leadershipListener);
        leadershipService.runForLeadership(REBALANCE_MASTERSHIP);
        regionService.addListener(regionEventListener);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        cfgService.unregisterProperties(getClass(), false);
        mastershipService.removeListener(mastershipListener);
        leadershipService.withdraw(REBALANCE_MASTERSHIP);
        leadershipService.removeListener(leadershipListener);
        regionService.removeListener(regionEventListener);
        cancelBalance();
        executorService.shutdown();
        log.info("Stopped");
    }

    @Modified
    public void modified(ComponentContext context) {
        readComponentConfiguration(context);
        cancelBalance();
        scheduleBalance();
        log.info("modified");
    }

    private synchronized void processLeaderChange(NodeId newLeader) {
        boolean currLeader = newLeader.equals(localId);
        if (isLeader.getAndSet(currLeader) != currLeader) {
            if (currLeader) {
                scheduleBalance();
            } else {
                cancelBalance();
            }
        }
    }

    // Sets flag at execution to indicate there is currently a scheduled
    // rebalancing. As soon as it starts running, the flag is set back to
    // null and another rebalancing can be queued.
    private void scheduleBalance() {
        if (isLeader.get() && nextTask.get() == null) {

            Future task = executorService.schedule(new BalanceTask(),
                    schedulePeriod, TimeUnit.SECONDS);

            if (!nextTask.compareAndSet(null, task)) {
                task.cancel(false);
            }
        }
    }

    private class BalanceTask implements Runnable {

        @Override
        public void run() {
            // nextTask is now running, free the spot so that it is possible
            // to queue up another upcoming task.
            nextTask.set(null);

            mastershipAdminService.balanceRoles();
            log.info("Completed balance roles");
        }
    }

    private void cancelBalance() {
        Future task = nextTask.getAndSet(null);
        if (task != null) {
            task.cancel(false);
        }
    }

    /**
     * Extracts properties from the component configuration context.
     *
     * @param context the component context
     */
    private void readComponentConfiguration(ComponentContext context) {
        Dictionary<?, ?> properties = context.getProperties();

        Integer newSchedulePeriod = Tools.getIntegerProperty(properties,
                                                             SCHEDULE_PERIOD);
        if (newSchedulePeriod == null) {
            schedulePeriod = SCHEDULE_PERIOD_DEFAULT;
            log.info("Schedule period is not configured, default value is {}",
                     SCHEDULE_PERIOD_DEFAULT);
        } else {
            schedulePeriod = newSchedulePeriod;
            log.info("Configured. Schedule period is configured to {}", schedulePeriod);
        }
    }

    private class InnerMastershipListener implements MastershipListener {

        @Override
        public void event(MastershipEvent event) {
            scheduleBalance();
        }
    }

    private class InnerLeadershipListener implements LeadershipEventListener {
        @Override
        public boolean isRelevant(LeadershipEvent event) {
            return REBALANCE_MASTERSHIP.equals(event.subject().topic());
        }

        @Override
        public void event(LeadershipEvent event) {
            processLeaderChange(event.subject().leaderNodeId());
        }
    }

    private class InnerRegionListener implements RegionListener {
        @Override
        public void event(RegionEvent event) {
            switch (event.type()) {
                case REGION_MEMBERSHIP_CHANGED:
                case REGION_UPDATED:
                    scheduleBalance();
                    break;
                default:
                    break;
            }
        }
    }
}
