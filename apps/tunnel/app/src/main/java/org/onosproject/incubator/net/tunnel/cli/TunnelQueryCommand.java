/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.incubator.net.tunnel.cli;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;

import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.action.Option;
import org.onlab.packet.IpAddress;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.incubator.net.tunnel.DefaultOpticalTunnelEndPoint;
import org.onosproject.incubator.net.tunnel.IpTunnelEndPoint;
import org.onosproject.incubator.net.tunnel.OpticalLogicId;
import org.onosproject.incubator.net.tunnel.OpticalTunnelEndPoint;
import org.onosproject.incubator.net.tunnel.Tunnel;
import org.onosproject.incubator.net.tunnel.TunnelEndPoint;
import org.onosproject.incubator.net.tunnel.TunnelId;
import org.onosproject.incubator.net.tunnel.TunnelService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.Path;
import org.onosproject.net.PortNumber;
import org.onosproject.net.provider.ProviderId;

/**
 * Supports for querying tunnels. It's used by consumers.
 */
@Service
@Command(scope = "onos", name = "tunnels", description = "Supports for querying tunnels."
        + " It's used by consumers.")
public class TunnelQueryCommand extends AbstractShellCommand {
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

    private static final String FMT = "tunnelId=%s, src=%s, dst=%s,"
            + "type=%s, state=%s, producerName=%s, tunnelName=%s,"
            + "groupId=%s, path=%s%s";

    @Override
    protected void doExecute() {
        Tunnel.Type trueType = null;
        TunnelService service = get(TunnelService.class);
        ProviderId producerName = new ProviderId("default",
                                                 "org.onosproject.provider.tunnel.default");
        Collection<Tunnel> tunnelSet = null;
        if (isNull(src) && isNull(dst) && isNull(type) && isNull(tunnelId)) {
            tunnelSet = service.queryAllTunnels();
        }

        if (!isNull(src) && !isNull(dst) && !isNull(type)) {
            TunnelEndPoint srcPoint = null;
            TunnelEndPoint dstPoint = null;
            if ("MPLS".equals(type) || "VXLAN".equals(type)
                    || "GRE".equals(type)) {
                srcPoint = IpTunnelEndPoint.ipTunnelPoint(IpAddress
                        .valueOf(src));
                dstPoint = IpTunnelEndPoint.ipTunnelPoint(IpAddress
                        .valueOf(dst));
            } else if ("VLAN".equals(type)) {
                String[] srcArray = src.split("/");
                String[] dstArray = dst.split("/");
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
                String[] srcArray = src.split("/");
                String[] dstArray = dst.split("/");
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
            tunnelSet = service.queryTunnel(srcPoint, dstPoint);
        }
        if (!isNull(type)) {
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
            tunnelSet = service.queryTunnel(trueType);
        }
        if (!isNull(tunnelId)) {
            TunnelId id = TunnelId.valueOf(tunnelId);
            Tunnel tunnel = service.queryTunnel(id);
            tunnelSet = new HashSet<Tunnel>();
            tunnelSet.add(tunnel);
        }
        if (tunnelSet != null) {
            for (Tunnel tunnel : tunnelSet) {
                print(FMT, tunnel.tunnelId().id(), tunnel.src().toString(), tunnel.dst().toString(),
                      tunnel.type(), tunnel.state(), tunnel.providerId(),
                      tunnel.tunnelName(), tunnel.groupId(),
                      showPath(tunnel.path()),
                      annotations(tunnel.annotations()));
            }
        }
    }

    private String showPath(Path path) {
        if (path == null) {
            return "null";
        }
        StringBuilder builder = new StringBuilder("(");
        for (Link link : path.links()) {
            builder.append("(DeviceId:" + link.src().deviceId() + " Port:"
                    + link.src().port().toString());
            builder.append(" DeviceId:" + link.dst().deviceId() + " Port:"
                    + link.dst().port().toString() + ")");
        }
        builder.append(annotations(path.annotations()) + ")");
        return builder.toString();
    }

    private boolean isNull(String s) {
        return s == null || "".equals(s);
    }
}
