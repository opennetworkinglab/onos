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

import java.util.Optional;

import org.apache.karaf.shell.commands.Argument;
import org.apache.karaf.shell.commands.Command;
import org.apache.karaf.shell.commands.Option;
import org.onlab.packet.IpAddress;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.core.GroupId;
import org.onosproject.incubator.net.tunnel.DefaultOpticalTunnelEndPoint;
import org.onosproject.incubator.net.tunnel.DefaultTunnelDescription;
import org.onosproject.incubator.net.tunnel.IpTunnelEndPoint;
import org.onosproject.incubator.net.tunnel.OpticalLogicId;
import org.onosproject.incubator.net.tunnel.OpticalTunnelEndPoint;
import org.onosproject.incubator.net.tunnel.Tunnel;
import org.onosproject.incubator.net.tunnel.TunnelDescription;
import org.onosproject.incubator.net.tunnel.TunnelEndPoint;
import org.onosproject.incubator.net.tunnel.TunnelId;
import org.onosproject.incubator.net.tunnel.TunnelName;
import org.onosproject.incubator.net.tunnel.TunnelProvider;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.SparseAnnotations;
import org.onosproject.net.provider.ProviderId;

/**
 * Supports for creating a tunnel by using IP address and optical as tunnel end
 * point.
 */
@Command(scope = "onos", name = "tunnel-create",
description = "Supports for creating a tunnel by using IP address and optical as tunnel end point now.")
public class TunnelCreateCommand extends AbstractShellCommand {

    @Argument(index = 0, name = "src", description = "Source tunnel point."
            + " Only supports for IpTunnelEndPoint and OpticalTunnelEndPoint as end point now."
            + " If creates a ODUK or OCH or VLAN type tunnel, the formatter of this argument is DeviceId-PortNumber."
            + " Otherwise src means IP address.", required = true, multiValued = false)
    String src = null;

    @Argument(index = 1, name = "dst", description = "Destination tunnel point."
            + " Only supports for IpTunnelEndPoint and OpticalTunnelEndPoint as end point now."
            + " If creates a ODUK or OCH or VLAN type tunnel, the formatter of this argument is DeviceId-PortNumber."
            + " Otherwise dst means IP address.", required = true, multiValued = false)
    String dst = null;
    @Argument(index = 2, name = "type", description = "The type of tunnels,"
            + " It includes MPLS, VLAN, VXLAN, GRE, ODUK, OCH", required = true, multiValued = false)
    String type = null;
    @Option(name = "-g", aliases = "--groupId",
            description = "Group flow table id which a tunnel match up", required = false, multiValued = false)
    String groupId = "0";

    @Option(name = "-n", aliases = "--tunnelName",
            description = "The name of tunnels", required = false, multiValued = false)
    String tunnelName = "onos";

    @Option(name = "-b", aliases = "--bandwidth",
            description = "The bandwidth attribute of tunnel", required = false, multiValued = false)
    String bandwidth = "1024";

    private static final String FMT = "The tunnel identity is %s";

    @Override
    protected void execute() {
        TunnelProvider service = get(TunnelProvider.class);
        ProviderId producerName = new ProviderId("default",
                                                 "org.onosproject.provider.tunnel.default");
        TunnelEndPoint srcPoint = null;
        TunnelEndPoint dstPoint = null;
        Tunnel.Type trueType = null;
        if ("MPLS".equals(type)) {
            trueType = Tunnel.Type.MPLS;
            srcPoint = IpTunnelEndPoint.ipTunnelPoint(IpAddress.valueOf(src));
            dstPoint = IpTunnelEndPoint.ipTunnelPoint(IpAddress.valueOf(dst));
        } else if ("VLAN".equals(type)) {
            trueType = Tunnel.Type.VLAN;
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
        } else if ("VXLAN".equals(type)) {
            trueType = Tunnel.Type.VXLAN;
            srcPoint = IpTunnelEndPoint.ipTunnelPoint(IpAddress.valueOf(src));
            dstPoint = IpTunnelEndPoint.ipTunnelPoint(IpAddress.valueOf(dst));
        } else if ("GRE".equals(type)) {
            trueType = Tunnel.Type.GRE;
            srcPoint = IpTunnelEndPoint.ipTunnelPoint(IpAddress.valueOf(src));
            dstPoint = IpTunnelEndPoint.ipTunnelPoint(IpAddress.valueOf(dst));
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

        SparseAnnotations annotations = DefaultAnnotations
                .builder()
                .set("bandwidth", bandwidth == null || "".equals(bandwidth) ? "0" : bandwidth)
                .build();
        TunnelDescription tunnel = new DefaultTunnelDescription(
                                                                null,
                                                                srcPoint,
                                                                dstPoint,
                                                                trueType,
                                                                new GroupId(Integer.parseInt(groupId)),
                                                                producerName,
                                                                TunnelName
                                                                        .tunnelName(tunnelName),
                                                                        null,
                                                                annotations);
        TunnelId tunnelId = service.tunnelAdded(tunnel);
        if (tunnelId == null) {
            error("Create tunnel failed.");
            return;
        }
        print(FMT, tunnelId.id());
    }

}
