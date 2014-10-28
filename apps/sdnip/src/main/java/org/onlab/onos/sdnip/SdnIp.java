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
package org.onlab.onos.sdnip;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.Collection;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.onos.core.ApplicationId;
import org.onlab.onos.core.CoreService;
import org.onlab.onos.net.host.HostService;
import org.onlab.onos.net.intent.IntentService;
import org.onlab.onos.sdnip.bgp.BgpRouteEntry;
import org.onlab.onos.sdnip.bgp.BgpSessionManager;
import org.onlab.onos.sdnip.config.SdnIpConfigReader;
import org.slf4j.Logger;

/**
 * Component for the SDN-IP peering application.
 */
@Component(immediate = true)
@Service
public class SdnIp implements SdnIpService {

    private static final String SDN_IP_APP = "org.onlab.onos.sdnip";

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected IntentService intentService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;

    private SdnIpConfigReader config;
    private PeerConnectivityManager peerConnectivity;
    private Router router;
    private BgpSessionManager bgpSessionManager;

    @Activate
    protected void activate() {
        log.debug("SDN-IP started");

        config = new SdnIpConfigReader();
        config.init();

        InterfaceService interfaceService = new HostToInterfaceAdaptor(hostService);

        ApplicationId appId = coreService.registerApplication(SDN_IP_APP);

        peerConnectivity = new PeerConnectivityManager(appId, config,
                interfaceService, intentService);
        peerConnectivity.start();

        router = new Router(appId, intentService, hostService, config, interfaceService);
        router.start();

        bgpSessionManager = new BgpSessionManager(router);
        bgpSessionManager.startUp(2000); // TODO

        // TODO need to disable link discovery on external ports

    }

    @Deactivate
    protected void deactivate() {
        log.info("Stopped");
    }

    @Override
    public Collection<BgpRouteEntry> getBgpRoutes() {
        return bgpSessionManager.getBgpRoutes();
    }

    @Override
    public Collection<RouteEntry> getRoutes() {
        return router.getRoutes();
    }

    static String dpidToUri(String dpid) {
        return "of:" + dpid.replace(":", "");
    }
}
