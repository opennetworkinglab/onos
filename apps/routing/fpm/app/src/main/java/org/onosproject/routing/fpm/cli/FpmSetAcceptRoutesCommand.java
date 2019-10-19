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

package org.onosproject.routing.fpm.cli;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.onlab.packet.IpAddress;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.routing.fpm.FpmInfoService;
import org.onosproject.routing.fpm.FpmPeer;
import org.onosproject.routing.fpm.FpmPeerAcceptRoutes;

import java.util.Collections;

/**
 * Sets acceptRoute flag for given peer.
 */
@Service
@Command(scope = "onos", name = "fpm-set-accept-routes",
        description = "Adds a flag to Fpm peer to accept or discard routes")
public class FpmSetAcceptRoutesCommand extends AbstractShellCommand {
    @Argument(index = 0, name = "peerAddress", description = "IpAddress of peer",
            required = true)
    String peerAddressString = null;

    @Argument(index = 1, name = "peerPort", description = "Port of peer",
            required = true)
    String peerPort = null;

    @Argument(index = 2, name = "acceptRoutes", description = "Flag to accept or discard routes",
            required = true)
    String acceptRoutesString = null;

    @Override
    protected void doExecute() {
        FpmInfoService service = AbstractShellCommand.get(FpmInfoService.class);

        IpAddress peerAddress = IpAddress.valueOf(peerAddressString);
        boolean isAcceptRoutes =  Boolean.parseBoolean(acceptRoutesString);
        int port = Integer.parseInt(peerPort);
        FpmPeer peer = new FpmPeer(peerAddress, port);
        service.updateAcceptRouteFlag(Collections.singleton(new FpmPeerAcceptRoutes(peer, isAcceptRoutes)));

    }
}
