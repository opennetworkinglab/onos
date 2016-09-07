/*
 * Copyright 2015-present Open Networking Laboratory
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

import com.google.common.util.concurrent.ListenableScheduledFuture;
import com.google.common.util.concurrent.ListeningScheduledExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
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
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;

import java.util.Dictionary;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static org.onlab.util.Tools.groupedThreads;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * An app to perform automatic load balancing in response to events.  Load balancing events are triggered by any
 * change in mastership and are limited to a frequency of one every 30 seconds, all load balancing is run on an outside
 * thread executor that must only have one thread due to issues that can occur is multiple balancing events occur in
 * parallel.
 */
@Component(immediate = true)
public class MastershipLoadBalancer {

    private final Logger log = getLogger(getClass());

    private static final int DEFAULT_SCHEDULE_PERIOD = 5;
    @Property(name = "schedulePeriod", intValue = DEFAULT_SCHEDULE_PERIOD,
            label = "Period to schedule balancing the mastership to be shared as evenly as by all online instances.")
    private int schedulePeriod = DEFAULT_SCHEDULE_PERIOD;

    private static final String REBALANCE_MASTERSHIP = "rebalance/mastership";

    private NodeId localId;

    private AtomicBoolean isLeader = new AtomicBoolean(false);

    private AtomicReference<Future> nextTask = new AtomicReference<>();

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MastershipService mastershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MastershipAdminService mastershipAdminService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LeadershipService leadershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ComponentConfigService cfgService;

    private InnerLeadershipListener leadershipListener = new InnerLeadershipListener();

    /* This listener is used to trigger balancing for any mastership event which will include switches changing state
    between active and inactive states as well as the same variety of event occurring with ONOS nodes. Must
    use a listenable executor to ensure events are triggered with no frequency greater than once every 30 seconds.
     */
    private InnerMastershipListener mastershipListener = new InnerMastershipListener();

    //Ensures that all executions do not interfere with one another (single thread)
    private ListeningScheduledExecutorService executorService = MoreExecutors.
            listeningDecorator(newSingleThreadScheduledExecutor(groupedThreads("MastershipLoadBalancer", "%d", log)));

    @Activate
    public void activate(ComponentContext context) {
        cfgService.registerProperties(getClass());
        modified(context);
        mastershipService.addListener(mastershipListener);
        localId = clusterService.getLocalNode().id();
        leadershipService.addListener(leadershipListener);
        leadershipService.runForLeadership(REBALANCE_MASTERSHIP);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        cfgService.unregisterProperties(getClass(), false);
        mastershipService.removeListener(mastershipListener);
        leadershipService.withdraw(REBALANCE_MASTERSHIP);
        leadershipService.removeListener(leadershipListener);
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

    private void scheduleBalance() {
        if (isLeader.get() && nextTask.get() == null) {

            ListenableScheduledFuture task =
                    executorService.schedule(mastershipAdminService::balanceRoles,
                            schedulePeriod, TimeUnit.SECONDS);
            task.addListener(() -> {
                        log.info("Completed balance roles");
                        nextTask.set(null);
                    }, MoreExecutors.directExecutor()
            );
            if (!nextTask.compareAndSet(null, task)) {
                task.cancel(false);
            }
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
                                                             "schedulePeriod");
        if (newSchedulePeriod == null) {
            schedulePeriod = DEFAULT_SCHEDULE_PERIOD;
            log.info("Schedule period is not configured, default value is {}",
                     DEFAULT_SCHEDULE_PERIOD);
        } else {
            schedulePeriod = newSchedulePeriod;
            log.info("Configured. Schedule period is configured to {}", schedulePeriod);
        }
    }

    private class InnerMastershipListener implements MastershipListener {

        @Override
        public void event(MastershipEvent event) {
            //Sets flag at execution to indicate there is currently a scheduled rebalancing, reverts upon completion
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
}
