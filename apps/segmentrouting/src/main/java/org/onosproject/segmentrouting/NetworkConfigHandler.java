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
package org.onosproject.segmentrouting;

import com.google.common.collect.Lists;

import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip4Prefix;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.PortNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Set;

/**
 * This class is temporary class and used only for test.
 * It will be replaced with "real" Network Config Manager.
 *
 * TODO: Knock off this wrapper and directly use DeviceConfiguration class
 */

public class NetworkConfigHandler {

    private static Logger log = LoggerFactory.getLogger(NetworkConfigHandler.class);
    private SegmentRoutingManager srManager;
    private DeviceConfiguration deviceConfig;

    public NetworkConfigHandler(SegmentRoutingManager srManager,
                                DeviceConfiguration deviceConfig) {
        this.srManager = srManager;
        this.deviceConfig = deviceConfig;
    }

    public List<Ip4Address> getGatewayIpAddress(DeviceId deviceId) {
        return this.deviceConfig.getSubnetGatewayIps(deviceId);
    }

    public IpPrefix getRouterIpAddress(DeviceId deviceId) {
        return IpPrefix.valueOf(deviceConfig.getRouterIp(deviceId), 32);
    }

    public MacAddress getRouterMacAddress(DeviceId deviceId) {
        return deviceConfig.getDeviceMac(deviceId);
    }

    public boolean inSameSubnet(DeviceId deviceId, Ip4Address destIp) {

        List<Ip4Prefix> subnets = getSubnetInfo(deviceId);
        if (subnets == null) {
            return false;
        }

        return subnets.stream()
               .anyMatch((subnet) -> subnet.contains(destIp));
    }

    public boolean inSameSubnet(Ip4Address address, int sid) {
        DeviceId deviceId = deviceConfig.getDeviceId(sid);
        if (deviceId == null) {
            log.warn("Cannot find a device for SID {}", sid);
            return false;
        }

        return inSameSubnet(deviceId, address);
    }

    public List<Ip4Prefix> getSubnetInfo(DeviceId deviceId) {
        return deviceConfig.getSubnets(deviceId);
    }

    public int getMplsId(DeviceId deviceId) {
        return deviceConfig.getSegmentId(deviceId);
    }

    public int getMplsId(MacAddress routerMac) {
        return deviceConfig.getSegmentId(routerMac);
    }

    public int getMplsId(Ip4Address routerIpAddress) {
        return deviceConfig.getSegmentId(routerIpAddress);
    }

    public boolean isEcmpNotSupportedInTransit(DeviceId deviceId) {
        //TODO: temporarily changing to true to test with Dell
        return true;
    }

    public boolean isTransitRouter(DeviceId deviceId) {
        return !(deviceConfig.isEdgeDevice(deviceId));
    }


    public boolean isEdgeRouter(DeviceId deviceId) {
        return deviceConfig.isEdgeDevice(deviceId);
    }

    private List<PortNumber> getPortsToNeighbors(DeviceId deviceId, List<DeviceId> fwdSws) {

        List<PortNumber> portNumbers = Lists.newArrayList();

        Set<Link> links = srManager.linkService.getDeviceEgressLinks(deviceId);
        for (Link link: links) {
            for (DeviceId swId: fwdSws) {
                if (link.dst().deviceId().equals(swId)) {
                    portNumbers.add(link.src().port());
                    break;
                }
            }
        }

        return portNumbers;
    }

    public List<PortNumber> getPortsToDevice(DeviceId deviceId) {
        List<PortNumber> portNumbers = Lists.newArrayList();

        Set<Link> links = srManager.linkService.getDeviceEgressLinks(deviceId);
        for (Link link: links) {
            if (link.dst().deviceId().equals(deviceId)) {
                portNumbers.add(link.src().port());
            }
        }

        return portNumbers;
    }


    public Ip4Address getDestinationRouterAddress(Ip4Address destIpAddress) {
        return deviceConfig.getRouterIpAddressForASubnetHost(destIpAddress);
    }

    public DeviceId getDeviceId(Ip4Address ip4Address) {
        return deviceConfig.getDeviceId(ip4Address);
    }

    public MacAddress getRouterMac(Ip4Address targetAddress) {
        return deviceConfig.getRouterMacForAGatewayIp(targetAddress);
    }
}
