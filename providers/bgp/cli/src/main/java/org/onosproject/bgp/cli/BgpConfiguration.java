/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.bgp.cli;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.bgp.controller.BgpCfg;
import org.onosproject.bgp.controller.BgpConnectPeer;
import org.onosproject.bgp.controller.BgpController;
import org.onosproject.bgp.controller.BgpPeerCfg;
import org.onosproject.cli.AbstractShellCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.TreeMap;


@Command(scope = "onos", name = "bgp", description = "lists configuration")
public class BgpConfiguration extends AbstractShellCommand {
    private static final Logger log = LoggerFactory.getLogger(BgpConfiguration.class);
    private static final String CONFIGURATION = "configuration";
    private static final String PEER = "peer";
    protected BgpController bgpController;
    protected BgpConnectPeer bgpConnectPeer;
    protected BgpPeerCfg bgpPeerCfg;
    protected BgpCfg bgpCfg;
    @Argument(index = 0, name = "name",
            description = "configuration" + "\n" + "peer",
            required = true, multiValued = false)
    String name = null;
    @Argument(index = 1, name = "peer",
            description = "peerIp",
            required = false, multiValued = false)
    String peer = null;

    @Override
    protected void execute() {
        switch (name) {
            case CONFIGURATION:
                displayBgpConfiguration();
                break;
            case PEER:
                displayBgpPeerConfiguration();
                break;
            default:
                System.out.print("Unknown command...!!");
                break;
        }
    }

    private void displayBgpConfiguration() {
        try {

            this.bgpController = get(BgpController.class);
            bgpCfg = bgpController.getConfig();
            print("RouterID = %s, ASNumber = %s, MaxSession = %s, HoldingTime = %s, LsCapabality = %s," +
                            " LargeAsCapabality = %s, FlowSpecCapabality = %s", bgpCfg.getRouterId(),
                    bgpCfg.getAsNumber(), bgpCfg.getMaxSession(), bgpCfg.getHoldTime(),
                    bgpCfg.getLsCapability(), bgpCfg.getLargeASCapability(), bgpCfg.flowSpecCapability());

        } catch (Exception e) {
            log.debug("Error occurred while displaying BGP configuration: {}", e.getMessage());
        }
    }

    private void displayBgpPeerConfiguration() {
        try {
            this.bgpController = get(BgpController.class);
            BgpCfg bgpCfg = bgpController.getConfig();
            if (bgpCfg == null) {
                return;
            }
            TreeMap<String, BgpPeerCfg> displayPeerTree = bgpCfg.getPeerTree();
            Set<String> peerKey = displayPeerTree.keySet();
            if (peer != null) {
                if (!peerKey.isEmpty()) {
                    for (String peerIdKey : peerKey) {
                        bgpPeerCfg = displayPeerTree.get(peerIdKey);
                        bgpConnectPeer = bgpPeerCfg.connectPeer();
                        if (peerIdKey.equals(peer)) {
                            print("PeerRouterID = %s, PeerHoldingTime = %s, ASNumber = %s, PeerState = %s," +
                                            " PeerPort = %s, ConnectRetryCounter = %s",
                                    peer, bgpPeerCfg.getHoldtime(), bgpPeerCfg.getAsNumber(),
                                    bgpPeerCfg.getState(), bgpConnectPeer.getPeerPort(),
                                    bgpConnectPeer.getConnectRetryCounter());
                        }
                    }
                }
            } else {
                if (!peerKey.isEmpty()) {
                    for (String peerIdKey : peerKey) {
                        bgpPeerCfg = displayPeerTree.get(peerIdKey);
                        bgpConnectPeer = bgpPeerCfg.connectPeer();
                        print("PeerRouterID = %s, PeerHoldingTime = %s, ASNumber = %s, PeerState = %s, PeerPort = %s," +
                                        " ConnectRetryCounter = %s",
                                bgpPeerCfg.getPeerRouterId(), bgpPeerCfg.getHoldtime(), bgpPeerCfg.getAsNumber(),
                                bgpPeerCfg.getState(), bgpConnectPeer.getPeerPort(),
                                bgpConnectPeer.getConnectRetryCounter());
                    }
                }

            }
        } catch (Exception e) {
            log.debug("Error occurred while displaying BGP peer configuration: {}", e.getMessage());
        }
    }


}