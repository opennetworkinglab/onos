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
import java.util.Optional;

import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.onlab.packet.IpAddress;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.incubator.net.tunnel.DefaultOpticalTunnelEndPoint;
import org.onosproject.incubator.net.tunnel.DefaultTunnelDescription;
import org.onosproject.incubator.net.tunnel.IpTunnelEndPoint;
import org.onosproject.incubator.net.tunnel.OpticalLogicId;
import org.onosproject.incubator.net.tunnel.OpticalTunnelEndPoint;
import org.onosproject.incubator.net.tunnel.Tunnel;
import org.onosproject.incubator.net.tunnel.TunnelDescription;
import org.onosproject.incubator.net.tunnel.TunnelEndPoint;
import org.onosproject.incubator.net.tunnel.TunnelId;
import org.onosproject.incubator.net.tunnel.TunnelProvider;
import org.onosproject.incubator.net.tunnel.TunnelService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.provider.ProviderId;

/**
 * Supports for removing tunnels. It's used by producers.
 */
@Command(scope = "onos", name = "tunnel-remove", description = "Supports for removing tunnels. It's used by producers.")
public class TunnelRemoveCommand extends AbstractShellCommand {
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

    @Override
    protected void execute() {
        TunnelDescription tunnel = null;
        TunnelProvider service = get(TunnelProvider.class);
        ProviderId producerName = new ProviderId("default",
                                                 "org.onosproject.provider.tunnel.default");
        if (!isNull(src) && !isNull(dst) && !isNull(type)) {
            TunnelEndPoint srcPoint = null;
            TunnelEndPoint dstPoint = null;
            Tunnel.Type trueType = null;
            if ("MPLS".equals(type)) {
                trueType = Tunnel.Type.MPLS;
                srcPoint = IpTunnelEndPoint.ipTunnelPoint(IpAddress
                        .valueOf(src));
                dstPoint = IpTunnelEndPoint.ipTunnelPoint(IpAddress
                        .valueOf(dst));
            } else if ("VLAN".equals(type)) {
                trueType = Tunnel.Type.VLAN;
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
            } else if ("ODUK".equals(type)) {
                trueType = Tunnel.Type.ODUK;
                String[] srcArray = src.split("/");
                String[] dstArray = dst.split("/");
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
                String[] srcArray = src.split("/");
                String[] dstArray = dst.split("/");
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

            tunnel = new DefaultTunnelDescription(null, srcPoint, dstPoint,
                                                  trueType, null, producerName,
                                                  null, null);
            service.tunnelRemoved(tunnel);
            return;
        }
        if (!isNull(tunnelId)) {
            TunnelId id = TunnelId.valueOf(tunnelId);
            tunnel = new DefaultTunnelDescription(id, null, null, null, null,
                                                  producerName, null, null);
            service.tunnelRemoved(tunnel);
            return;
        }

        if (!isNull(type)) {
            Tunnel.Type trueType = null;
            Collection<Tunnel> tunnelSet = null;
            TunnelService tunnelService = get(TunnelService.class);
            if ("MPLS".equals(type)) {
                trueType = Tunnel.Type.MPLS;
            } else if ("VLAN".equals(type)) {
                trueType = Tunnel.Type.VLAN;
            } else if ("VXLAN".equals(type)) {
                trueType = Tunnel.Type.VXLAN;
            } else if ("GRE".equals(type)) {
                trueType = Tunnel.Type.GRE;
            } else if ("ODUK".equals(type)) {
                trueType = Tunnel.Type.ODUK;
            } else if ("OCH".equals(type)) {
                trueType = Tunnel.Type.OCH;
            } else {
                print("Illegal tunnel type. Please input MPLS, VLAN, VXLAN, GRE, ODUK or OCH.");
                return;
            }
            tunnelSet = tunnelService.queryTunnel(trueType);
            if (tunnelSet != null) {
                for (Tunnel tunnelTemp : tunnelSet) {
                    tunnel = new DefaultTunnelDescription(tunnelTemp.tunnelId(), null, null, null, null,
                                                          producerName, null, null);
                    service.tunnelRemoved(tunnel);
                }
            }
        }
    }

    private boolean isNull(String s) {
        return s == null || "".equals(s);
    }
}
