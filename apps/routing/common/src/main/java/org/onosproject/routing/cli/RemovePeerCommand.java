/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.routing.cli;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onlab.packet.IpAddress;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.routing.RoutingService;
import org.onosproject.routing.config.BgpConfig;

/**
 * Command to remove existing BGP peer.
 */
@Service
@Command(scope = "onos", name = "bgp-peer-remove",
        description = "Removes a BGP peer")
public class RemovePeerCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "ip",
            description = "IP address of the BGP peer",
            required = true, multiValued = false)
    String ip = null;

    private static final String PEER_REMOVE_SUCCESS = "Peer Successfully Removed.";
    private static final String NO_CONFIGURATION = "No speakers configured";
    private static final String PEER_NOT_FOUND =
            "Peer with IP \'%s\' not found";

    private IpAddress peerAddress = null;

    @Override
    protected void doExecute() {
        peerAddress = IpAddress.valueOf(ip);

        NetworkConfigService configService = get(NetworkConfigService.class);
        CoreService coreService = get(CoreService.class);
        ApplicationId appId = coreService.getAppId(RoutingService.ROUTER_APP_ID);

        BgpConfig config = configService.getConfig(appId, BgpConfig.class);
        if (config == null || config.bgpSpeakers().isEmpty()) {
            print(NO_CONFIGURATION);
            return;
        }

        peerAddress = IpAddress.valueOf(ip);

        BgpConfig.BgpSpeakerConfig speaker = config.getSpeakerFromPeer(peerAddress);
        if (speaker == null) {
            print(PEER_NOT_FOUND, ip);
            return;
        }

        removePeerFromSpeakerConf(speaker, config);
        configService.applyConfig(appId, BgpConfig.class, config.node());

        print(PEER_REMOVE_SUCCESS);
    }

    private void removePeerFromSpeakerConf(BgpConfig.BgpSpeakerConfig speaker,
                                           BgpConfig config) {
        log.debug("Removing BGP configuration for peer: {}", ip);
        config.removePeerFromSpeaker(speaker, peerAddress);
    }
}
