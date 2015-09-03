/*
 * Copyright 2014-2015 Open Networking Laboratory
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
package org.onosproject.sdnip;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.LeadershipEvent;
import org.onosproject.cluster.LeadershipEventListener;
import org.onosproject.cluster.LeadershipService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.incubator.net.intf.InterfaceService;
import org.onosproject.net.host.HostService;
import org.onosproject.net.intent.IntentService;
import org.onosproject.routing.IntentSynchronizationService;
import org.onosproject.routing.RoutingService;
import org.onosproject.routing.SdnIpService;
import org.onosproject.routing.config.RoutingConfigurationService;
import org.slf4j.Logger;

import java.util.Objects;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Component for the SDN-IP peering application.
 */
@Component(immediate = true)
@Service
public class SdnIp implements SdnIpService {

    private static final String SDN_IP_APP = "org.onosproject.sdnip";
    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected IntentService intentService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ClusterService clusterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LeadershipService leadershipService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected RoutingService routingService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected RoutingConfigurationService config;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected NetworkConfigService networkConfigService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected InterfaceService interfaceService;

    private IntentSynchronizer intentSynchronizer;
    private PeerConnectivityManager peerConnectivity;
    private SdnIpFib fib;

    private LeadershipEventListener leadershipEventListener =
        new InnerLeadershipEventListener();
    private ApplicationId appId;
    private ControllerNode localControllerNode;

    @Activate
    protected void activate() {
        log.info("SDN-IP started");

        appId = coreService.registerApplication(SDN_IP_APP);

        localControllerNode = clusterService.getLocalNode();

        intentSynchronizer = new IntentSynchronizer(appId, intentService);
        intentSynchronizer.start();

        peerConnectivity = new PeerConnectivityManager(appId,
                                                       intentSynchronizer,
                                                       networkConfigService,
                coreService.getAppId(RoutingService.ROUTER_APP_ID),
                                                       interfaceService);
        peerConnectivity.start();

        fib = new SdnIpFib(appId, interfaceService, intentSynchronizer);

        routingService.addFibListener(fib);
        routingService.start();

        leadershipService.addListener(leadershipEventListener);
        leadershipService.runForLeadership(appId.name());
    }

    @Deactivate
    protected void deactivate() {
        routingService.stop();
        peerConnectivity.stop();
        intentSynchronizer.stop();

        leadershipService.withdraw(appId.name());
        leadershipService.removeListener(leadershipEventListener);

        log.info("SDN-IP Stopped");
    }

    @Override
    public void modifyPrimary(boolean isPrimary) {
        intentSynchronizer.leaderChanged(isPrimary);
    }

    @Override
    public IntentSynchronizationService getIntentSynchronizationService() {
        return intentSynchronizer;
    }

    /**
     * Converts DPIDs of the form xx:xx:xx:xx:xx:xx:xx to OpenFlow provider
     * device URIs.
     *
     * @param dpid the DPID string to convert
     * @return the URI string for this device
     */
    static String dpidToUri(String dpid) {
        return "of:" + dpid.replace(":", "");
    }

    /**
     * A listener for Leadership Events.
     */
    private class InnerLeadershipEventListener
        implements LeadershipEventListener {

        @Override
        public void event(LeadershipEvent event) {
            log.debug("Leadership Event: time = {} type = {} event = {}",
                      event.time(), event.type(), event);

            if (!event.subject().topic().equals(appId.name())) {
                return;         // Not our topic: ignore
            }
            if (!Objects.equals(event.subject().leader(), localControllerNode.id())) {
                return;         // The event is not about this instance: ignore
            }

            switch (event.type()) {
            case LEADER_ELECTED:
                log.info("SDN-IP Leader Elected");
                intentSynchronizer.leaderChanged(true);
                break;
            case LEADER_BOOTED:
                log.info("SDN-IP Leader Lost Election");
                intentSynchronizer.leaderChanged(false);
                break;
            case LEADER_REELECTED:
                break;
            default:
                break;
            }
        }
    }
}
