/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.segmentrouting.cli;

import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.segmentrouting.SegmentRoutingService;
import org.onosproject.segmentrouting.Tunnel;

/**
 * Command to show the list of tunnels.
 */
@Command(scope = "onos", name = "sr-tunnel-list",
        description = "Lists all tunnels")
public class TunnelListCommand extends AbstractShellCommand {

    private static final String FORMAT_MAPPING =
            "  id=%s, path=%s";

    @Override
    protected void execute() {

        SegmentRoutingService srService =
                AbstractShellCommand.get(SegmentRoutingService.class);

        srService.getTunnels().forEach(tunnel -> printTunnel(tunnel));
    }

    private void printTunnel(Tunnel tunnel) {
        print(FORMAT_MAPPING, tunnel.id(), tunnel.labelIds());
    }
}
