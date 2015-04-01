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

import java.net.URI;
import java.util.List;
import java.util.Set;

/**
 * This class is temporary class and used only for test.
 * It will be replaced with "real" Network Config Manager.
 */

public class NetworkConfigHandler {

    private static Logger log = LoggerFactory.getLogger(NetworkConfigHandler.class);
    private SegmentRoutingManager srManager;
    private DeviceConfiguration deviceConfig = new DeviceConfiguration();

    public NetworkConfigHandler(SegmentRoutingManager srManager) {
        this.srManager = srManager;
    }

    public Ip4Address getGatewayIpAddress(DeviceId deviceId) {

        if (deviceId.uri().equals(URI.create("of:0000000000000001"))) {
            return Ip4Address.valueOf("10.0.1.128");
        } else if (deviceId.uri().equals(URI.create("of:0000000000000006"))) {
            return Ip4Address.valueOf("7.7.7.128");
        }

        log.warn("No gateway Ip address was found for {}", deviceId);
        return Ip4Address.valueOf("0.0.0.0");
    }

    public IpPrefix getRouterIpAddress(DeviceId deviceId) {

        return IpPrefix.valueOf(deviceConfig.getRouterIp(deviceId), 32);
    }

    public MacAddress getRouterMacAddress(DeviceId deviceId) {
        return deviceConfig.getDeviceMac(deviceId);
    }

    public boolean inSameSubnet(DeviceId deviceId, Ip4Address destIp) {

        String subnetInfo = getSubnetInfo(deviceId);
        if (subnetInfo == null) {
            return false;
        }

        IpPrefix prefix = IpPrefix.valueOf(subnetInfo);
        if (prefix.contains(destIp)) {
            return true;
        }

        return false;
    }

    public boolean inSameSubnet(Ip4Address address, int sid) {
        DeviceId deviceId = deviceConfig.getDeviceId(sid);
        if (deviceId == null) {
            log.warn("Cannot find a device for SID {}", sid);
            return false;
        }

        String subnetInfo = getSubnetInfo(deviceId);
        if (subnetInfo == null) {
            log.warn("Cannot find the subnet info for {}", deviceId);
            return false;
        }

        Ip4Prefix subnet = Ip4Prefix.valueOf(subnetInfo);
        if (subnet.contains(address)) {
            return true;
        }

        return false;

    }

    public String getSubnetInfo(DeviceId deviceId) {
        // TODO : supports multiple subnet
        if (deviceId.uri().equals(URI.create("of:0000000000000001"))) {
            return "10.0.1.1/24";
        } else if (deviceId.uri().equals(URI.create("of:0000000000000006"))) {
            return "7.7.7.7/24";
        } else {
            log.error("Switch {} is not an edge router", deviceId);
            return null;
        }
    }

    public int getMplsId(DeviceId deviceId) {
        return deviceConfig.getSegmentId(deviceId);
    }

    public int getMplsId(MacAddress mac) {
        return deviceConfig.getSegmentId(mac);
    }

    public int getMplsId(Ip4Address address) {
        return deviceConfig.getSegmentId(address);
    }

    public boolean isEcmpNotSupportedInTransit(DeviceId deviceId) {
        return false;
    }

    public boolean isTransitRouter(DeviceId deviceId) {
        return true;
    }


    public boolean isEdgeRouter(DeviceId deviceId) {
        if (deviceId.uri().equals(URI.create("of:0000000000000001"))
                || deviceId.uri().equals(URI.create("of:0000000000000006"))) {
            return true;
        }

        return false;
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
        // TODO: need to check the subnet info
        if (destIpAddress.toString().equals("10.0.1.1")) {
            return Ip4Address.valueOf("192.168.0.1");
        } else if (destIpAddress.toString().equals("7.7.7.7")) {
            return Ip4Address.valueOf("192.168.0.6");
        } else {
            log.warn("No router was found for {}", destIpAddress);
            return null;
        }

    }

    public DeviceId getDeviceId(Ip4Address ip4Address) {
        return deviceConfig.getDeviceId(ip4Address);
    }

    public MacAddress getRouterMac(Ip4Address targetAddress) {
        if (targetAddress.toString().equals("10.0.1.128")) {
            return MacAddress.valueOf("00:00:00:00:00:01");
        } else if (targetAddress.toString().equals("7.7.7.128")) {
            return MacAddress.valueOf("00:00:00:00:00:06");
        } else {
            log.warn("Cannot find a router for {}", targetAddress);
            return null;
        }
    }
}
