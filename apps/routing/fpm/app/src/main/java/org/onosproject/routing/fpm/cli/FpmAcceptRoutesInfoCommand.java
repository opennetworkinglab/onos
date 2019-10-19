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
import org.onosproject.routing.fpm.FpmPeerInfo;

import java.util.Comparator;
import java.util.Map;

/**
 * Displays the acceptRoute flag value for given peer.
 */
@Service
@Command(scope = "onos", name = "fpm-get-accept-route",
        description = "Displays the acceptRoute flag value for given peer")
public class FpmAcceptRoutesInfoCommand extends AbstractShellCommand {

    private static final String FORMAT = "peer %s  port %s acceptRoutes %s";

    @Argument(index = 0, name = "peerAddress", description = "Peer Ip address",
            required = false, multiValued = false)
    String peerAddress = null;



    @Override
    protected void doExecute() {
        FpmInfoService fpmInfo = get(FpmInfoService.class);
        if (peerAddress != null) {
            IpAddress address = IpAddress.valueOf(peerAddress);
            fpmInfo.peers().entrySet().stream()
                    .filter(peer -> peer.getKey().address().equals(address))
                    .map(Map.Entry::getValue)
                    .forEach(this::print);
        } else {
            fpmInfo.peers().entrySet().stream()
                    .sorted(Comparator.<Map.Entry<FpmPeer, FpmPeerInfo>, IpAddress>comparing(e -> e.getKey().address())
                            .thenComparing(e -> e.getKey().port()))
                    .map(Map.Entry::getValue)
                    .forEach(this::print);
        }
    }

    private void print(FpmPeerInfo info) {
        info.connections().forEach(cinfo ->
            print(FORMAT, cinfo.peer().address(),
                    cinfo.peer().port(),
                    cinfo.isAcceptRoutes())
        );
    }
}
