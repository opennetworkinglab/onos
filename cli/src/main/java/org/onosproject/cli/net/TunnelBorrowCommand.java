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
package org.onosproject.cli.net;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.onlab.packet.IpAddress;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.incubator.net.tunnel.DefaultOpticalTunnelEndPoint;
import org.onosproject.incubator.net.tunnel.IpTunnelEndPoint;
import org.onosproject.incubator.net.tunnel.OpticalLogicId;
import org.onosproject.incubator.net.tunnel.OpticalTunnelEndPoint;
import org.onosproject.incubator.net.tunnel.Tunnel;
import org.onosproject.incubator.net.tunnel.TunnelEndPoint;
import org.onosproject.incubator.net.tunnel.TunnelId;
import org.onosproject.incubator.net.tunnel.TunnelName;
import org.onosproject.incubator.net.tunnel.TunnelService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.provider.ProviderId;

/**
 * Borrows tunnels. It's used by consumers.
 */
@Command(scope = "onos", name = "tunnel-borrow", description = "Borrows tunnels. It's used by consumers.")
public class TunnelBorrowCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "consumerId",
            description = "consumer id means application id.", required = true, multiValued = false)
    String consumerId = null;

    @Option(name = "-s", aliases = "--src", description = "Source tunnel point."
            + " Only supports for IpTunnelEndPoint and OpticalTunnelEndPoint as end point now."
            + " If deletess a ODUK or OCH type tunnel, the formatter of this argument is DeviceId-PortNumber."
            + " Otherwise src means IP address.", required = false, multiValued = false)
    String src = null;

    @Option(name = "-d", aliases = "--dst", description = "Destination tunnel point."
            + " Only supports for IpTunnelEndPoint and OpticalTunnelEndPoint as end point now."
            + " If deletess a ODUK or OCH type tunnel, the formatter of this argument is DeviceId-PortNumber."
            + " Otherwise dst means IP address.", required = false, multiValued = false)
    String dst = null;

    @Option(name = "-t", aliases = "--type", description = "The type of tunnels,"
            + " It includes MPLS, VLAN, VXLAN, GRE, ODUK, OCH", required = false, multiValued = false)
    String type = null;

    @Option(name = "-i", aliases = "--tunnelId",
            description = "the tunnel identity.", required = false, multiValued = false)
    String tunnelId = null;

    @Option(name = "-n", aliases = "--tunnelName",
            description = "The name of tunnels", required = false, multiValued = false)
    String tunnelName = null;
    private static final String FMT = "src=%s, dst=%s,"
            + "type=%s, state=%s, producerName=%s, tunnelName=%s,"
            + "groupId=%s";

    @Override
    protected void execute() {
        Collection<Tunnel> tunnelSet = null;
        Tunnel.Type trueType = null;
        TunnelService service = get(TunnelService.class);
        ApplicationId appId = new DefaultApplicationId(1, consumerId);
        ProviderId producerName = new ProviderId("default",
                                                 "org.onosproject.provider.tunnel.default");
        if (!isNull(src) && !isNull(dst) && !isNull(type)) {
            TunnelEndPoint srcPoint = null;
            TunnelEndPoint dstPoint = null;
            if ("MPLS".equals(type)) {
                trueType = Tunnel.Type.MPLS;
                srcPoint = IpTunnelEndPoint.ipTunnelPoint(IpAddress
                        .valueOf(src));
                dstPoint = IpTunnelEndPoint.ipTunnelPoint(IpAddress
                        .valueOf(dst));
            } else if ("VXLAN".equals(type)) {
                trueType = Tunnel.Type.VXLAN;
                srcPoint = IpTunnelEndPoint.ipTunnelPoint(IpAddress
                        .valueOf(src));
                dstPoint = IpTunnelEndPoint.ipTunnelPoint(IpAddress
                        .valueOf(dst));
            } else if ("GRE".equals(type)) {
                trueType = Tunnel.Type.GRE;
                srcPoint = IpTunnelEndPoint.ipTunnelPoint(IpAddress
                        .valueOf(src));
                dstPoint = IpTunnelEndPoint.ipTunnelPoint(IpAddress
                        .valueOf(dst));
            } else if ("VLAN".equals(type)) {
                trueType = Tunnel.Type.VLAN;
                String[] srcArray = src.split("-");
                String[] dstArray = dst.split("-");
                srcPoint = new DefaultOpticalTunnelEndPoint(
                                                            producerName,
                                                            Optional.of(DeviceId
                                                                    .deviceId(srcArray[0])),
                                                            Optional.of(PortNumber
                                                                    .portNumber(srcArray[1])),
                                                            null,
                                                            null,
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
                                                            null,
                                                            OpticalLogicId
                                                                    .logicId(0),
                                                            true);
            } else if ("ODUK".equals(type)) {
                trueType = Tunnel.Type.ODUK;
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
            } else if ("OCH".equals(type)) {
                trueType = Tunnel.Type.OCH;
                String[] srcArray = src.split("-");
                String[] dstArray = dst.split("-");
                srcPoint = new DefaultOpticalTunnelEndPoint(
                                                            producerName,
                                                            Optional.of(DeviceId
                                                                    .deviceId(srcArray[0])),
                                                            Optional.of(PortNumber
                                                                    .portNumber(srcArray[1])),
                                                            null,
                                                            OpticalTunnelEndPoint.Type.TIMESLOT,
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
                                                            OpticalTunnelEndPoint.Type.TIMESLOT,
                                                            OpticalLogicId
                                                                    .logicId(0),
                                                            true);
            } else {
                print("Illegal tunnel type. Please input MPLS, VLAN, VXLAN, GRE, ODUK or OCH.");
                return;
            }
            tunnelSet = service.borrowTunnel(appId, srcPoint, dstPoint, trueType);
        }
        if (!isNull(tunnelId)) {
            TunnelId id = TunnelId.valueOf(tunnelId);
            Tunnel tunnel = service.borrowTunnel(appId, id);
            tunnelSet = new HashSet<Tunnel>();
            tunnelSet.add(tunnel);
        }
        if (!isNull(tunnelName)) {
            TunnelName name = TunnelName.tunnelName(tunnelName);
            tunnelSet = service.borrowTunnel(appId, name);
        }
        for (Tunnel tunnel : tunnelSet) {
            print(FMT, tunnel.src(), tunnel.dst(), tunnel.type(),
                  tunnel.state(), tunnel.providerId(), tunnel.tunnelName(),
                  tunnel.groupId());
        }
    }

    private boolean isNull(String s) {
        return s == null || "".equals(s);
    }
}
