/*
 * Copyright 2015 Open Networking Laboratory
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

package org.onosproject.cordvtn.cli;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onlab.packet.IpAddress;
import org.onlab.packet.TpPort;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.cordvtn.CordVtnService;
import org.onosproject.cordvtn.CordVtnNode;
import org.onosproject.net.DeviceId;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Adds a new node to the service.
 */
@Command(scope = "onos", name = "cordvtn-node-add",
        description = "Adds a new node to CORD VTN service")
public class CordVtnNodeAddCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "hostname", description = "Hostname",
            required = true, multiValued = false)
    private String hostname = null;

    @Argument(index = 1, name = "ovsdb",
            description = "OVSDB server listening address (ip:port)",
            required = true, multiValued = false)
    private String ovsdb = null;

    @Argument(index = 2, name = "bridgeId",
            description = "Device ID of integration bridge",
            required = true, multiValued = false)
    private String bridgeId = null;

    @Override
    protected void execute() {
        checkArgument(ovsdb.contains(":"), "OVSDB address should be ip:port format");
        checkArgument(bridgeId.startsWith("of:"), "bridgeId should be of:dpid format");

        CordVtnService service = AbstractShellCommand.get(CordVtnService.class);
        String[] ipPort = ovsdb.split(":");
        CordVtnNode node = new CordVtnNode(hostname,
                                           IpAddress.valueOf(ipPort[0]),
                                           TpPort.tpPort(Integer.parseInt(ipPort[1])),
                                           DeviceId.deviceId(bridgeId));
        service.addNode(node);
    }
}
