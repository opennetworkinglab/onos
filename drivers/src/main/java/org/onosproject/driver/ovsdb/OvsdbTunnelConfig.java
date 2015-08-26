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
package org.onosproject.driver.ovsdb;

import java.util.Collection;

import java.util.Set;
import java.util.stream.Collectors;

import org.onlab.packet.IpAddress;
import org.onosproject.net.DeviceId;
import org.onosproject.net.behaviour.DefaultTunnelDescription;
import org.onosproject.net.behaviour.IpTunnelEndPoint;
import org.onosproject.net.behaviour.TunnelConfig;
import org.onosproject.net.behaviour.TunnelDescription;
import org.onosproject.net.behaviour.TunnelName;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.driver.DriverHandler;
import org.onosproject.ovsdb.controller.OvsdbClientService;
import org.onosproject.ovsdb.controller.OvsdbController;
import org.onosproject.ovsdb.controller.OvsdbNodeId;
import org.onosproject.ovsdb.controller.OvsdbTunnel;

/**
 * OVSDB-based implementation of tunnel config behaviour.
 */
public class OvsdbTunnelConfig extends AbstractHandlerBehaviour
        implements TunnelConfig {

    private static final String DEFAULT_ADDRESS = "0.0.0.0";

    @Override
    public void createTunnel(TunnelDescription tunnel) {
        DriverHandler handler = handler();
        OvsdbClientService ovsdbNode = getOvsdbNode(handler);
        IpTunnelEndPoint ipSrc = IpTunnelEndPoint.ipTunnelPoint(IpAddress
                .valueOf(DEFAULT_ADDRESS));
        IpTunnelEndPoint ipDst = IpTunnelEndPoint.ipTunnelPoint(IpAddress
                .valueOf(DEFAULT_ADDRESS));
        if (tunnel.src() instanceof IpTunnelEndPoint) {
            ipSrc = (IpTunnelEndPoint) tunnel.src();
        }
        if (tunnel.dst() instanceof IpTunnelEndPoint) {
            ipDst = (IpTunnelEndPoint) tunnel.dst();
        }
        //Even if source point ip or destination point ip equals 0:0:0:0, it is still work-in-progress.
        ovsdbNode.createTunnel(ipSrc.ip(), ipDst.ip());
    }

    @Override
    public void removeTunnel(TunnelDescription tunnel) {
        DriverHandler handler = handler();
        OvsdbClientService ovsdbNode = getOvsdbNode(handler);
        IpTunnelEndPoint ipSrc = IpTunnelEndPoint.ipTunnelPoint(IpAddress
                .valueOf(DEFAULT_ADDRESS));
        IpTunnelEndPoint ipDst = IpTunnelEndPoint.ipTunnelPoint(IpAddress
                .valueOf(DEFAULT_ADDRESS));
        if (tunnel.src() instanceof IpTunnelEndPoint) {
            ipSrc = (IpTunnelEndPoint) tunnel.src();
        }
        if (tunnel.dst() instanceof IpTunnelEndPoint) {
            ipDst = (IpTunnelEndPoint) tunnel.dst();
        }
      //Even if source point ip or destination point ip equals 0:0:0:0, it is still work-in-progress.
        ovsdbNode.dropTunnel(ipSrc.ip(), ipDst.ip());
    }

    @Override
    public void updateTunnel(TunnelDescription tunnel) {
        // TODO Auto-generated method stub

    }

    @Override
    public Collection<TunnelDescription> getTunnels() {
        DriverHandler handler = handler();
        OvsdbClientService ovsdbNode = getOvsdbNode(handler);
        Set<OvsdbTunnel> tunnels = ovsdbNode.getTunnels();

        return tunnels.stream()
                .map(x ->
                        new DefaultTunnelDescription(
                                IpTunnelEndPoint.ipTunnelPoint(x.localIp()),
                                IpTunnelEndPoint.ipTunnelPoint(x.remoteIp()),
                                TunnelDescription.Type.VXLAN,
                                TunnelName.tunnelName(x.tunnelName().toString())
                        )
                )
                .collect(Collectors.toSet());
    }

    // OvsdbNodeId(IP:port) is used in the adaptor while DeviceId(ovsdb:IP:port)
    // is used in the core. So DeviceId need be changed to OvsdbNodeId.
    private OvsdbNodeId changeDeviceIdToNodeId(DeviceId deviceId) {
        int lastColon = deviceId.toString().lastIndexOf(":");
        int fistColon = deviceId.toString().indexOf(":");
        String ip = deviceId.toString().substring(fistColon + 1, lastColon);
        String port = deviceId.toString().substring(lastColon + 1);
        IpAddress ipAddress = IpAddress.valueOf(ip);
        long portL = Long.parseLong(port);
        return new OvsdbNodeId(ipAddress, portL);
    }

    private OvsdbClientService getOvsdbNode(DriverHandler handler) {
        OvsdbController ovsController = handler.get(OvsdbController.class);
        DeviceId deviceId = handler.data().deviceId();
        OvsdbNodeId nodeId = changeDeviceIdToNodeId(deviceId);
        return ovsController.getOvsdbClient(nodeId);
    }
}
