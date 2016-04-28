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


import com.google.common.collect.Lists;
import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.segmentrouting.DefaultTunnel;
import org.onosproject.segmentrouting.SegmentRoutingService;
import org.onosproject.segmentrouting.Tunnel;
import org.onosproject.segmentrouting.TunnelHandler;

/**
 * Command to remove a tunnel.
 */
@Command(scope = "onos", name = "sr-tunnel-remove",
        description = "Remove a tunnel")
public class TunnelRemoveCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "tunnel ID",
            description = "tunnel ID",
            required = true, multiValued = false)
    String tunnelId;

    @Override
    protected void execute() {
        SegmentRoutingService srService =
                AbstractShellCommand.get(SegmentRoutingService.class);

        Tunnel tunnel = new DefaultTunnel(tunnelId, Lists.newArrayList());
        TunnelHandler.Result result = srService.removeTunnel(tunnel);
        switch (result) {
            case TUNNEL_IN_USE:
                print("ERROR: the tunnel is still in use");
                break;
            case TUNNEL_NOT_FOUND:
                print("ERROR: the tunnel is not found");
                break;
            default:
                break;
        }
    }
}