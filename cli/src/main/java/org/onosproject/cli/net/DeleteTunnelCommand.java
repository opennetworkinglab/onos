/*
 * Copyright 2014-2015 Open Networking Laboratory
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
package org.onosproject.cli.net;

import java.util.Optional;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.onlab.packet.IpAddress;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.net.tunnel.DefaultOpticalTunnelEndPoint;
import org.onosproject.net.tunnel.IpTunnelEndPoint;
import org.onosproject.net.tunnel.OpticalLogicId;
import org.onosproject.net.tunnel.OpticalTunnelEndPoint;
import org.onosproject.net.tunnel.TunnelAdminService;
import org.onosproject.net.tunnel.TunnelEndPoint;

/**
 * Supports for deleting all tunnels by using IP address and optical as tunnel
 * end point now. It's used by consumers.
 */
@Command(scope = "onos", name = "delete-tunnels", description = "Supports for deleting all tunnels by using IP address"
        + " and optical as tunnel end point now. It's used by consumers.")
public class DeleteTunnelCommand extends AbstractShellCommand {
    static String applicationId = "DEFAULT_APP_ID";
    @Argument(index = 0, name = "src", description = "Source tunnel point."
            + " Only supports for IpTunnelEndPoint and OpticalTunnelEndPoint as end point now."
            + " If deletess a ODUK or OCH type tunnel, the formatter of this argument is DeviceId-PortNumber."
            + " Otherwise src means IP address.", required = true, multiValued = false)
    String src = null;
    @Argument(index = 1, name = "dst", description = "Destination tunnel point."
            + " Only supports for IpTunnelEndPoint and OpticalTunnelEndPoint as end point now."
            + " If deletess a ODUK or OCH type tunnel, the formatter of this argument is DeviceId-PortNumber."
            + " Otherwise dst means IP address.", required = true, multiValued = false)
    String dst = null;

    @Argument(index = 2, name = "type", description = "The type of tunnels,"
            + " It includes MPLS, VLAN, VXLAN, GRE, ODUK, OCH", required = true, multiValued = false)
    String type = null;

    @Override
    protected void execute() {
        TunnelAdminService adminService = get(TunnelAdminService.class);
        ProviderId producerName = new ProviderId("default",
                                                 "org.onosproject.provider.tunnel.default");
        TunnelEndPoint srcPoint = null;
        TunnelEndPoint dstPoint = null;
        if ("MPLS".equals(type) || "VLAN".equals(type) || "VXLAN".equals(type) || "GRE".equals(type)) {
            srcPoint = IpTunnelEndPoint.ipTunnelPoint(IpAddress.valueOf(src));
            dstPoint = IpTunnelEndPoint.ipTunnelPoint(IpAddress.valueOf(dst));
        } else if ("ODUK".equals(type) || "OCH".equals(type)) {
            String[] srcArray = src.split("-");
            String[] dstArray = dst.split("-");
            srcPoint = new DefaultOpticalTunnelEndPoint(
                                                        producerName,
                                                        Optional.of(DeviceId
                                                                .deviceId(srcArray[0])),
                                                        Optional.of(PortNumber
                                                                .portNumber(srcArray[1])),
                                                        null,
                                                        OpticalTunnelEndPoint.Type.LAMBDA,
                                                        OpticalLogicId
                                                                .logicId(0),
                                                        true);
            dstPoint = new DefaultOpticalTunnelEndPoint(
                                                        producerName,
                                                        Optional.of(DeviceId
                                                                .deviceId(dstArray[0])),
                                                        Optional.of(PortNumber
                                                                .portNumber(dstArray[1])),
                                                        null,
                                                        OpticalTunnelEndPoint.Type.LAMBDA,
                                                        OpticalLogicId
                                                                .logicId(0),
                                                        true);
        } else {
            print("Illegal tunnel type. Please input MPLS, VLAN, VXLAN, GRE, ODUK or OCH.");
            return;
        }

        adminService.removeTunnels(srcPoint, dstPoint, producerName);
    }

}
