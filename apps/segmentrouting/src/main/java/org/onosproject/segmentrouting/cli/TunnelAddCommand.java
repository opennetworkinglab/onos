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

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.segmentrouting.DefaultTunnel;
import org.onosproject.segmentrouting.SegmentRoutingService;
import org.onosproject.segmentrouting.Tunnel;
import org.onosproject.segmentrouting.TunnelHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Command to add a new tunnel.
 */
@Command(scope = "onos", name = "sr-tunnel-add",
        description = "Create a new tunnel")
public class TunnelAddCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "tunnel ID",
            description = "tunnel ID",
            required = true, multiValued = false)
    String tunnelId;

    @Argument(index = 1, name = "label path",
            description = "label path",
            required = true, multiValued = false)
    String labels;


    @Override
    protected void execute() {

        SegmentRoutingService srService =
                AbstractShellCommand.get(SegmentRoutingService.class);

        List<Integer> labelIds = new ArrayList<>();
        StringTokenizer strToken = new StringTokenizer(labels, ",");
        while (strToken.hasMoreTokens()) {
            labelIds.add(Integer.valueOf(strToken.nextToken()));
        }
        Tunnel tunnel = new DefaultTunnel(tunnelId, labelIds);

        TunnelHandler.Result result = srService.createTunnel(tunnel);
        switch (result) {
            case ID_EXISTS:
                print("ERROR: the same tunnel ID exists");
                break;
            case TUNNEL_EXISTS:
                print("ERROR: the same tunnel exists");
                break;
            case INTERNAL_ERROR:
                print("ERROR: internal tunnel creation error");
                break;
            case WRONG_PATH:
                print("ERROR: the tunnel path is wrong");
                break;
            default:
                break;
        }
    }
}
