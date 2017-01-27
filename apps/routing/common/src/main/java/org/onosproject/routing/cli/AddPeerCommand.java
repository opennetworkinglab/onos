/*
 * Copyright 2017-present Open Networking Laboratory
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

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onlab.packet.IpAddress;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.incubator.net.intf.InterfaceService;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.routing.RoutingService;
import org.onosproject.routing.config.BgpConfig;

/**
 * Command to add new BGP peer to existing internal speaker.
 */
@Command(scope = "onos", name = "bgp-peer-add",
        description = "Adds an external BGP router as peer to an existing BGP speaker")
public class AddPeerCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "name",
            description = "Name of the internal BGP speaker",
            required = true, multiValued = false)
    String name = null;

    @Argument(index = 1, name = "ip",
            description = "IP address of the BGP peer",
            required = true, multiValued = false)
    String ip = null;

    private static final String PEER_ADD_SUCCESS = "Peer Successfully Added.";
    private static final String NO_CONFIGURATION = "No speakers configured";
    private static final String SPEAKER_NOT_FOUND =
            "Speaker with name \'%s\' not found";
    private static final String NO_INTERFACE =
            "No matching interface found for IP \'%s\'";

    private IpAddress peerAddress = null;

    @Override
    protected void execute() {
        peerAddress = IpAddress.valueOf(ip);

        NetworkConfigService configService = get(NetworkConfigService.class);
        CoreService coreService = get(CoreService.class);
        ApplicationId appId = coreService.getAppId(RoutingService.ROUTER_APP_ID);

        BgpConfig config = configService.getConfig(appId, BgpConfig.class);
        if (config == null || config.bgpSpeakers().isEmpty()) {
            print(NO_CONFIGURATION);
            return;
        }

        BgpConfig.BgpSpeakerConfig speaker = config.getSpeakerWithName(name);
        if (speaker == null) {
            print(SPEAKER_NOT_FOUND, name);
            return;
        } else {
            if (speaker.isConnectedToPeer(peerAddress)) {
                return; // Peering already exists.
            }
        }

        InterfaceService interfaceService = get(InterfaceService.class);
        if (interfaceService.getMatchingInterface(peerAddress) == null) {
            print(NO_INTERFACE, ip);
            return;
        }

        addPeerToSpeakerConf(config);
        configService.applyConfig(appId, BgpConfig.class, config.node());

        print(PEER_ADD_SUCCESS);
    }

    private void addPeerToSpeakerConf(BgpConfig config) {
        log.debug("Creating BGP configuration for new peer: {}", ip);
        config.addPeerToSpeaker(name, peerAddress);
    }
}
