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

package org.onosproject.cli.net.vnet;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.incubator.net.virtual.VirtualNetworkAdminService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;

/**
 * Creates a new virtual link.
 */
@Command(scope = "onos", name = "vnet-create-link",
        description = "Creates a new virtual link in a network.")
public class VirtualLinkCreateCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "networkId", description = "Network ID",
            required = true, multiValued = false)
    Long networkId = null;

    @Argument(index = 1, name = "srcDeviceId", description = "Source device ID",
            required = true, multiValued = false)
    String srcDeviceId = null;

    @Argument(index = 2, name = "srcPortNum", description = "Source port number",
            required = true, multiValued = false)
    Integer srcPortNum = null;

    @Argument(index = 3, name = "dstDeviceId", description = "Destination device ID",
            required = true, multiValued = false)
    String dstDeviceId = null;

    @Argument(index = 4, name = "dstPortNum", description = "Destination port number",
            required = true, multiValued = false)
    Integer dstPortNum = null;

    @Option(name = "-b", aliases = "--bidirectional",
            description = "If this argument is passed in then the virtual link created will be bidirectional, " +
                    "otherwise the link will be unidirectional.",
            required = false, multiValued = false)
    boolean bidirectional = false;

    @Override
    protected void execute() {
        VirtualNetworkAdminService service = get(VirtualNetworkAdminService.class);
        ConnectPoint src = new ConnectPoint(DeviceId.deviceId(srcDeviceId), PortNumber.portNumber(srcPortNum));
        ConnectPoint dst = new ConnectPoint(DeviceId.deviceId(dstDeviceId), PortNumber.portNumber(dstPortNum));

        service.createVirtualLink(NetworkId.networkId(networkId), src, dst);
        if (bidirectional) {
            service.createVirtualLink(NetworkId.networkId(networkId), dst, src);
        }
        print("Virtual link successfully created.");
    }
}
