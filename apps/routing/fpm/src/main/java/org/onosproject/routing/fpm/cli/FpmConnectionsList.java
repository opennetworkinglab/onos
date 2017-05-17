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

package org.onosproject.routing.fpm.cli;

import org.apache.karaf.shell.commands.Command;
import org.onlab.packet.IpAddress;
import org.onlab.util.Tools;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.cluster.ClusterService;
import org.onosproject.routing.fpm.FpmPeerInfo;
import org.onosproject.routing.fpm.FpmInfoService;
import org.onosproject.routing.fpm.FpmPeer;

import java.util.Comparator;
import java.util.Map;

/**
 * Displays the current FPM connections.
 */
@Command(scope = "onos", name = "fpm-connections",
        description = "Displays the current FPM connections")
public class FpmConnectionsList extends AbstractShellCommand {

    private static final String FORMAT = "peer %s:%s connected to %s since %s %s (%d routes locally)";

    @Override
    protected void execute() {
        FpmInfoService fpmInfo = get(FpmInfoService.class);

        fpmInfo.peers().entrySet().stream()
                .sorted(Comparator.<Map.Entry<FpmPeer, FpmPeerInfo>, IpAddress>comparing(e -> e.getKey().address())
                        .thenComparing(e -> e.getKey().port()))
                .map(Map.Entry::getValue)
                .forEach(this::print);
    }

    private void print(FpmPeerInfo info) {
        ClusterService clusterService = get(ClusterService.class);

        info.connections().forEach(cinfo ->
            print(FORMAT, cinfo.peer().address(), cinfo.peer().port(),
                    cinfo.connectedTo(), Tools.timeAgo(cinfo.connectTime()),
                    cinfo.connectedTo().equals(clusterService.getLocalNode().id()) ? "*" : "",
                    info.routes())
        );
    }
}
