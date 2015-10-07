/*
 * Copyright 2015 Open Networking Laboratory
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
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.LeadershipEvent;
import org.onosproject.cluster.LeadershipEventListener;
import org.onosproject.cluster.LeadershipService;
import org.onosproject.cluster.NodeId;
import org.onosproject.mastership.MastershipAdminService;
import org.onosproject.mastership.MastershipEvent;
import org.onosproject.mastership.MastershipListener;
import org.onosproject.mastership.MastershipService;
import org.slf4j.Logger;

import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

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

    private InnerLeadershipListener leadershipListener = new InnerLeadershipListener();

    /* This listener is used to trigger balancing for any mastership event which will include switches changing state
    between active and inactive states as well as the same variety of event occurring with ONOS nodes. Must
    use a listenable executor to ensure events are triggered with no frequency greater than once every 30 seconds.
     */
    private InnerMastershipListener mastershipListener = new InnerMastershipListener();

    //Ensures that all executions do not interfere with one another (single thread)
    private ListeningScheduledExecutorService executorService = MoreExecutors.
            listeningDecorator(Executors.newSingleThreadScheduledExecutor());

    @Activate
    public void activate() {
        mastershipService.addListener(mastershipListener);
        localId = clusterService.getLocalNode().id();
        leadershipService.addListener(leadershipListener);
        leadershipService.runForLeadership(REBALANCE_MASTERSHIP);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        mastershipService.removeListener(mastershipListener);
        leadershipService.withdraw(REBALANCE_MASTERSHIP);
        leadershipService.removeListener(leadershipListener);
        cancelBalance();
        executorService.shutdown();
        log.info("Stopped");
    }

    private synchronized void processLeadershipChange(NodeId newLeader) {
        if (newLeader == null) {
            return;
        }
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

            ListenableScheduledFuture task = executorService.schedule(mastershipAdminService::balanceRoles, 30,
                    TimeUnit.SECONDS);
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
            processLeadershipChange(event.subject().leader());
        }
    }
}