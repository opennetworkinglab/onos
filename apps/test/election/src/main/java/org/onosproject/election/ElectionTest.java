/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onosproject.election;

import static org.slf4j.LoggerFactory.getLogger;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.cluster.ClusterService;
import org.onosproject.core.CoreService;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.LeadershipEvent;
import org.onosproject.cluster.LeadershipEventListener;
import org.onosproject.cluster.LeadershipService;
import org.onosproject.core.ApplicationId;

import org.slf4j.Logger;


/**
 * Simple application to test leadership election.
 */
@Component(immediate = true)
public class ElectionTest {

    private final Logger log = getLogger(getClass());

    private static final String ELECTION_APP = "org.onosproject.election";
    private ApplicationId appId;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LeadershipService leadershipService;

    private LeadershipEventListener leadershipEventListener =
            new InnerLeadershipEventListener();

    private ControllerNode localControllerNode;


    @Activate
    protected void activate() {
        log.info("Election-test app started");

        appId = coreService.registerApplication(ELECTION_APP);

        localControllerNode = clusterService.getLocalNode();

        leadershipService.addListener(leadershipEventListener);
        leadershipService.runForLeadership(appId.name());
    }

    @Deactivate
    protected void deactivate() {

        leadershipService.withdraw(appId.name());
        leadershipService.removeListener(leadershipEventListener);

        log.info("Election-test app Stopped");
    }

    /**
     * A listener for Leadership Events.
     */
    private class InnerLeadershipEventListener
            implements LeadershipEventListener {

        @Override
        public void event(LeadershipEvent event) {


            if (!event.subject().topic().equals(appId.name())) {
                return;         // Not our topic: ignore
            }

            //only log what pertains to us
            log.debug("Leadership Event: time = {} type = {} event = {}",
                    event.time(), event.type(), event);

            if (!event.subject().leader().equals(
                    localControllerNode.id())) {
                return;         // The event is not about this instance: ignore
            }

            switch (event.type()) {
                case LEADER_ELECTED:
                    log.info("Election-test app leader elected");
                    break;
                case LEADER_BOOTED:
                    log.info("Election-test app lost election");
                    break;
                case LEADER_REELECTED:
                    log.debug("Election-test app was re-elected");
                    break;
                default:
                    break;
            }
        }
    }

}
