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
package org.onosproject.segmentrouting.config;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.SetMultimap;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip4Prefix;
import org.onlab.packet.Ip6Address;
import org.onlab.packet.Ip6Prefix;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.incubator.net.config.basics.ConfigException;
import org.onosproject.incubator.net.config.basics.InterfaceConfig;
import org.onosproject.incubator.net.intf.Interface;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.host.InterfaceIpAddress;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.segmentrouting.SegmentRoutingManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Segment Routing configuration component that reads the
 * segment routing related configuration from Network Configuration Manager
 * component and organizes in more accessible formats.
 */
public class DeviceConfiguration implements DeviceProperties {

    private static final String ERROR_CONFIG = "Configuration error.";
    private static final String NO_SUBNET = "No subnet configured on {}";

    private static final Logger log = LoggerFactory.getLogger(DeviceConfiguration.class);
    private final List<Integer> allSegmentIds = new ArrayList<>();
    private final Map<DeviceId, SegmentRouterInfo> deviceConfigMap = new ConcurrentHashMap<>();
    private SegmentRoutingManager srManager;

    private class SegmentRouterInfo {
        int ipv4NodeSid;
        int ipv6NodeSid;
        DeviceId deviceId;
        Ip4Address ipv4Loopback;
        Ip6Address ipv6Loopback;
        MacAddress mac;
        boolean isEdge;
        SetMultimap<PortNumber, IpAddress> gatewayIps;
        SetMultimap<PortNumber, IpPrefix> subnets;
        Map<Integer, Set<Integer>> adjacencySids;

        public SegmentRouterInfo() {
            gatewayIps = HashMultimap.create();
            subnets = HashMultimap.create();
        }
    }

    /**
     * Constructs device configuration for all Segment Router devices,
     * organizing the data into various maps for easier access.
     *
     * @param srManager Segment Routing Manager
     */
    public DeviceConfiguration(SegmentRoutingManager srManager) {
        this.srManager = srManager;

        // Read config from device subject, excluding gatewayIps and subnets.
        Set<DeviceId> deviceSubjects =
                srManager.cfgService.getSubjects(DeviceId.class, SegmentRoutingDeviceConfig.class);
        deviceSubjects.forEach(subject -> {
            SegmentRoutingDeviceConfig config =
                    srManager.cfgService.getConfig(subject, SegmentRoutingDeviceConfig.class);
            SegmentRouterInfo info = new SegmentRouterInfo();
            info.deviceId = subject;
            info.ipv4NodeSid = config.nodeSidIPv4();
            info.ipv6NodeSid = config.nodeSidIPv6();
            info.ipv4Loopback = config.routerIpv4();
            info.ipv6Loopback = config.routerIpv6();
            info.mac = config.routerMac();
            info.isEdge = config.isEdgeRouter();
            info.adjacencySids = config.adjacencySids();
            deviceConfigMap.put(info.deviceId, info);
            log.info("Read device config for device: {}", info.deviceId);
            /*
             * IPv6 sid is not inserted. this part of the code is not used for now.
             */
            allSegmentIds.add(info.ipv4NodeSid);
        });

        // Read gatewayIps and subnets from port subject. Ignore suppressed ports.
        Set<ConnectPoint> portSubjects = srManager.cfgService
                .getSubjects(ConnectPoint.class, InterfaceConfig.class);
        portSubjects.stream().filter(subject -> !isSuppressedPort(subject)).forEach(subject -> {
            InterfaceConfig config =
                    srManager.cfgService.getConfig(subject, InterfaceConfig.class);
            Set<Interface> networkInterfaces;
            try {
                networkInterfaces = config.getInterfaces();
            } catch (ConfigException e) {
                log.error("Error loading port configuration");
                return;
            }
            networkInterfaces.forEach(networkInterface -> {
                VlanId vlanId = networkInterface.vlan();
                ConnectPoint connectPoint = networkInterface.connectPoint();
                DeviceId dpid = connectPoint.deviceId();
                PortNumber port = connectPoint.port();
                SegmentRouterInfo info = deviceConfigMap.get(dpid);

                // skip if there is no corresponding device for this ConenctPoint
                if (info != null) {
                    // Extract subnet information
                    List<InterfaceIpAddress> interfaceAddresses = networkInterface.ipAddressesList();
                    interfaceAddresses.forEach(interfaceAddress -> {
                        // Do not add /0, /32 and /128 to gateway IP list
                        int prefixLength = interfaceAddress.subnetAddress().prefixLength();
                        IpPrefix ipPrefix = interfaceAddress.subnetAddress();
                        if (ipPrefix.isIp4()) {
                            if (prefixLength != 0 && prefixLength != IpPrefix.MAX_INET_MASK_LENGTH) {
                                info.gatewayIps.put(port, interfaceAddress.ipAddress());
                            }
                            info.subnets.put(port, interfaceAddress.subnetAddress());
                        } else {
                            if (prefixLength != 0 && prefixLength != IpPrefix.MAX_INET6_MASK_LENGTH) {
                                info.gatewayIps.put(port, interfaceAddress.ipAddress());
                            }
                            info.subnets.put(port, interfaceAddress.subnetAddress());
                        }
                    });
                }
            });
            // We register the connect point with the NRS.
            srManager.registerConnectPoint(subject);
        });
    }

    @Override
    public boolean isConfigured(DeviceId deviceId) {
        return deviceConfigMap.get(deviceId) != null;
    }

    @Override
    public int getIPv4SegmentId(DeviceId deviceId) throws DeviceConfigNotFoundException {
        SegmentRouterInfo srinfo = deviceConfigMap.get(deviceId);
        if (srinfo != null) {
            log.trace("getIPv4SegmentId for device{} is {}", deviceId, srinfo.ipv4NodeSid);
            return srinfo.ipv4NodeSid;
        } else {
            String message = "getIPv4SegmentId fails for device: " + deviceId + ".";
            throw new DeviceConfigNotFoundException(message);
        }
    }

    @Override
    public int getIPv6SegmentId(DeviceId deviceId) throws DeviceConfigNotFoundException {
        SegmentRouterInfo srinfo = deviceConfigMap.get(deviceId);
        if (srinfo != null) {
            log.trace("getIPv6SegmentId for device{} is {}", deviceId, srinfo.ipv6NodeSid);
            return srinfo.ipv6NodeSid;
        } else {
            String message = "getIPv6SegmentId fails for device: " + deviceId + ".";
            throw new DeviceConfigNotFoundException(message);
        }
    }

    /**
     * Returns the IPv4 Node segment id of a segment router given its Router mac address.
     *
     * @param routerMac router mac address
     * @return node segment id, or -1 if not found in config
     */
    public int getIPv4SegmentId(MacAddress routerMac) {
        for (Map.Entry<DeviceId, SegmentRouterInfo> entry:
                    deviceConfigMap.entrySet()) {
            if (entry.getValue().mac.equals(routerMac)) {
                return entry.getValue().ipv4NodeSid;
            }
        }

        return -1;
    }

    /**
     * Returns the IPv6 Node segment id of a segment router given its Router mac address.
     *
     * @param routerMac router mac address
     * @return node segment id, or -1 if not found in config
     */
    public int getIPv6SegmentId(MacAddress routerMac) {
        for (Map.Entry<DeviceId, SegmentRouterInfo> entry:
                deviceConfigMap.entrySet()) {
            if (entry.getValue().mac.equals(routerMac)) {
                return entry.getValue().ipv6NodeSid;
            }
        }

        return -1;
    }

    /**
     * Returns the IPv4 Node segment id of a segment router given its Router ip address.
     *
     * @param routerAddress router ip address
     * @return node segment id, or -1 if not found in config
     */
    public int getIPv4SegmentId(Ip4Address routerAddress) {
        for (Map.Entry<DeviceId, SegmentRouterInfo> entry:
            deviceConfigMap.entrySet()) {
            if (entry.getValue().ipv4Loopback.equals(routerAddress)) {
                return entry.getValue().ipv4NodeSid;
            }
        }

        return -1;
    }

    /**
     * Returns the IPv6 Node segment id of a segment router given its Router ip address.
     *
     * @param routerAddress router ip address
     * @return node segment id, or -1 if not found in config
     */
    public int getIPv6SegmentId(Ip6Address routerAddress) {
        for (Map.Entry<DeviceId, SegmentRouterInfo> entry:
                deviceConfigMap.entrySet()) {
            if (entry.getValue().ipv6Loopback.equals(routerAddress)) {
                return entry.getValue().ipv6NodeSid;
            }
        }

        return -1;
    }

    @Override
    public MacAddress getDeviceMac(DeviceId deviceId) throws DeviceConfigNotFoundException {
        SegmentRouterInfo srinfo = deviceConfigMap.get(deviceId);
        if (srinfo != null) {
            log.trace("getDeviceMac for device{} is {}", deviceId, srinfo.mac);
            return srinfo.mac;
        } else {
            String message = "getDeviceMac fails for device: " + deviceId + ".";
            throw new DeviceConfigNotFoundException(message);
        }
    }

    @Override
    public Ip4Address getRouterIpv4(DeviceId deviceId) throws DeviceConfigNotFoundException {
        SegmentRouterInfo srinfo = deviceConfigMap.get(deviceId);
        if (srinfo != null) {
            log.trace("getRouterIpv4 for device{} is {}", deviceId, srinfo.ipv4Loopback);
            return srinfo.ipv4Loopback;
        } else {
            String message = "getRouterIpv4 fails for device: " + deviceId + ".";
            throw new DeviceConfigNotFoundException(message);
        }
    }

    @Override
    public Ip6Address getRouterIpv6(DeviceId deviceId) throws DeviceConfigNotFoundException {
        SegmentRouterInfo srinfo = deviceConfigMap.get(deviceId);
        if (srinfo != null) {
            log.trace("getRouterIpv6 for device{} is {}", deviceId, srinfo.ipv6Loopback);
            return srinfo.ipv6Loopback;
        } else {
            String message = "getRouterIpv6 fails for device: " + deviceId + ".";
            throw new DeviceConfigNotFoundException(message);
        }
    }

    @Override
    public boolean isEdgeDevice(DeviceId deviceId) throws DeviceConfigNotFoundException {
        SegmentRouterInfo srinfo = deviceConfigMap.get(deviceId);
        if (srinfo != null) {
            log.trace("isEdgeDevice for device{} is {}", deviceId, srinfo.isEdge);
            return srinfo.isEdge;
        } else {
            String message = "isEdgeDevice fails for device: " + deviceId + ".";
            throw new DeviceConfigNotFoundException(message);
        }
    }

    @Override
    public List<Integer> getAllDeviceSegmentIds() {
        return allSegmentIds;
    }

    @Override
    public Map<IpPrefix, List<PortNumber>> getSubnetPortsMap(DeviceId deviceId)
            throws DeviceConfigNotFoundException {
        SegmentRouterInfo srinfo = deviceConfigMap.get(deviceId);
        if (srinfo == null) {
            String message = "getSubnetPortsMap fails for device: " + deviceId + ".";
            throw new DeviceConfigNotFoundException(message);
        }
        // Construct subnet-port mapping from port-subnet mapping
        SetMultimap<PortNumber, IpPrefix> portSubnetMap = srinfo.subnets;
        Map<IpPrefix, List<PortNumber>> subnetPortMap = new HashMap<>();

        portSubnetMap.entries().forEach(entry -> {
            PortNumber port = entry.getKey();
            IpPrefix subnet = entry.getValue();

            if (subnet.prefixLength() == IpPrefix.MAX_INET_MASK_LENGTH ||
                    subnet.prefixLength() == IpPrefix.MAX_INET6_MASK_LENGTH) {
                return;
            }

            if (subnetPortMap.containsKey(subnet)) {
                subnetPortMap.get(subnet).add(port);
            } else {
                ArrayList<PortNumber> ports = new ArrayList<>();
                ports.add(port);
                subnetPortMap.put(subnet, ports);
            }
        });
        return subnetPortMap;
    }

    /**
     * Returns the device identifier or data plane identifier (dpid)
     * of a segment router given its segment id.
     *
     * @param sid segment id
     * @return deviceId device identifier
     */
    public DeviceId getDeviceId(int sid) {
        for (Map.Entry<DeviceId, SegmentRouterInfo> entry:
            deviceConfigMap.entrySet()) {
            if (entry.getValue().ipv4NodeSid == sid ||
                    entry.getValue().ipv6NodeSid == sid) {
                return entry.getValue().deviceId;
            }
        }

        return null;
    }

    /**
     * Returns the device identifier or data plane identifier (dpid)
     * of a segment router given its router ip address.
     *
     * @param ipAddress router ip address
     * @return deviceId device identifier
     */
    public DeviceId getDeviceId(Ip4Address ipAddress) {
        for (Map.Entry<DeviceId, SegmentRouterInfo> entry:
            deviceConfigMap.entrySet()) {
            if (entry.getValue().ipv4Loopback.equals(ipAddress)) {
                return entry.getValue().deviceId;
            }
        }

        return null;
    }

    /**
     * Returns the device identifier or data plane identifier (dpid)
     * of a segment router given its router ipv6 address.
     *
     * @param ipAddress router ipv6 address
     * @return deviceId device identifier
     */
    public DeviceId getDeviceId(Ip6Address ipAddress) {
        for (Map.Entry<DeviceId, SegmentRouterInfo> entry:
                deviceConfigMap.entrySet()) {
            if (entry.getValue().ipv6Loopback.equals(ipAddress)) {
                return entry.getValue().deviceId;
            }
        }

        return null;
    }

    /**
     * Returns the configured port ip addresses for a segment router.
     * These addresses serve as gateway IP addresses for the subnets configured
     * on those ports.
     *
     * @param deviceId device identifier
     * @return immutable set of ip addresses configured on the ports or null if not found
     */
    public Set<IpAddress> getPortIPs(DeviceId deviceId) {
        SegmentRouterInfo srinfo = deviceConfigMap.get(deviceId);
        if (srinfo != null) {
            log.trace("getSubnetGatewayIps for device{} is {}", deviceId,
                      srinfo.gatewayIps.values());
            return ImmutableSet.copyOf(srinfo.gatewayIps.values());
        }
        return null;
    }

    /**
     * Returns the configured subnet prefixes for a segment router.
     *
     * @param deviceId device identifier
     * @return list of ip prefixes or null if not found
     */
    public Set<IpPrefix> getSubnets(DeviceId deviceId) {
        SegmentRouterInfo srinfo = deviceConfigMap.get(deviceId);
        if (srinfo != null) {
            ImmutableSet.Builder<IpPrefix> builder = ImmutableSet.builder();
            return builder.addAll(srinfo.subnets.values()).build();
        }
        return null;
    }


    /**
     * Returns the subnet configuration of given device and port.
     *
     * @param deviceId Device ID
     * @param port Port number
     * @return The subnets configured on given port or empty set if
     *         the port is unconfigured or suppressed.
     */
    public Set<IpPrefix> getPortSubnets(DeviceId deviceId, PortNumber port) {
        ConnectPoint connectPoint = new ConnectPoint(deviceId, port);

        if (isSuppressedPort(connectPoint)) {
            return Collections.emptySet();
        }

        Set<IpPrefix> subnets =
                srManager.interfaceService.getInterfacesByPort(connectPoint).stream()
                        .flatMap(intf -> intf.ipAddressesList().stream())
                        .map(InterfaceIpAddress::subnetAddress)
                        .collect(Collectors.toSet());

        if (subnets.isEmpty()) {
            log.debug(NO_SUBNET, connectPoint);
            return Collections.emptySet();
        }

        return subnets;
    }

    /**
     * Returns the IPv4 subnet configured of given device and port.
     *
     * @param deviceId Device ID
     * @param port Port number
     * @return The IPv4 subnet configured on given port or null if
     *         the port is unconfigured, misconfigured or suppressed.
     */
    public Ip4Prefix getPortIPv4Subnet(DeviceId deviceId, PortNumber port) {
        return getPortSubnets(deviceId, port).stream()
                .filter(IpPrefix::isIp4)
                .map(IpPrefix::getIp4Prefix)
                .findFirst().orElse(null);
    }

    /**
     * Returns the IPv6 subnet configured of given device and port.
     *
     * @param deviceId Device ID
     * @param port Port number
     * @return The IPV6 subnet configured on given port or null if
     *         the port is unconfigured, misconfigured or suppressed.
     */
    public Ip6Prefix getPortIPv6Subnet(DeviceId deviceId, PortNumber port) {
        return getPortSubnets(deviceId, port).stream()
                .filter(IpPrefix::isIp6)
                .map(IpPrefix::getIp6Prefix)
                .findFirst().orElse(null);
    }

    /**
     * Returns the router ip address of segment router that has the
     * specified ip address in its subnets.
     *
     * @param destIpAddress target ip address
     * @return router ip address
     */
    public Ip4Address getRouterIpAddressForASubnetHost(Ip4Address destIpAddress) {
        Interface matchIntf = srManager.interfaceService.getMatchingInterface(destIpAddress);

        if (matchIntf == null) {
            log.debug("No router was found for {}", destIpAddress);
            return null;
        }

        DeviceId routerDeviceId = matchIntf.connectPoint().deviceId();
        SegmentRouterInfo srInfo = deviceConfigMap.get(routerDeviceId);
        if (srInfo == null) {
            log.debug("No device config was found for {}", routerDeviceId);
            return null;
        }

        return srInfo.ipv4Loopback;
    }

    /**
     * Returns the router ipv6 address of segment router that has the
     * specified ip address in its subnets.
     *
     * @param destIpAddress target ip address
     * @return router ip address
     */
    public Ip6Address getRouterIpAddressForASubnetHost(Ip6Address destIpAddress) {
        Interface matchIntf = srManager.interfaceService.getMatchingInterface(destIpAddress);

        if (matchIntf == null) {
            log.debug("No router was found for {}", destIpAddress);
            return null;
        }

        DeviceId routerDeviceId = matchIntf.connectPoint().deviceId();
        SegmentRouterInfo srInfo = deviceConfigMap.get(routerDeviceId);
        if (srInfo == null) {
            log.debug("No device config was found for {}", routerDeviceId);
            return null;
        }

        return srInfo.ipv6Loopback;
    }

    /**
     * Returns the router mac address of segment router that has the
     * specified ip address as one of its subnet gateway ip address.
     *
     * @param gatewayIpAddress router gateway ip address
     * @return router mac address or null if not found
     */
    public MacAddress getRouterMacForAGatewayIp(IpAddress gatewayIpAddress) {
        for (Map.Entry<DeviceId, SegmentRouterInfo> entry:
                deviceConfigMap.entrySet()) {
            if (entry.getValue().gatewayIps.
                    values().contains(gatewayIpAddress)) {
                return entry.getValue().mac;
            }
        }

        log.debug("Cannot find a router for {}", gatewayIpAddress);
        return null;
    }

    /**
     * Checks if the host is in the subnet defined in the router with the
     * device ID given.
     *
     * @param deviceId device identification of the router
     * @param hostIp   host IP address to check
     * @return true if the host is within the subnet of the router,
     * false if no subnet is defined under the router or if the host is not
     * within the subnet defined in the router
     */
    public boolean inSameSubnet(DeviceId deviceId, IpAddress hostIp) {

        Set<IpPrefix> subnets = getSubnets(deviceId);
        if (subnets == null) {
            return false;
        }

        for (IpPrefix subnet: subnets) {
            // Exclude /0 since it is a special case used for default route
            if (subnet.prefixLength() != 0 && subnet.contains(hostIp)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if the IP is in the subnet defined on given connect point.
     *
     * @param connectPoint Connect point
     * @param ip The IP address to check
     * @return True if the IP belongs to the subnet.
     *         False if the IP does not belong to the subnet, or
     *         there is no subnet configuration on given connect point.
     */
    public boolean inSameSubnet(ConnectPoint connectPoint, IpAddress ip) {
        Ip4Prefix ipv4Subnet = getPortIPv4Subnet(connectPoint.deviceId(), connectPoint.port());
        Ip6Prefix ipv6Subnet = getPortIPv6Subnet(connectPoint.deviceId(), connectPoint.port());
        return (ipv4Subnet != null && ipv4Subnet.contains(ip)) ||
                (ipv6Subnet != null && ipv6Subnet.contains(ip));
    }

    /**
     * Returns the ports corresponding to the adjacency Sid given.
     *
     * @param deviceId device identification of the router
     * @param sid adjacency Sid
     * @return set of port numbers
     */
    public Set<Integer> getPortsForAdjacencySid(DeviceId deviceId, int sid) {
        SegmentRouterInfo srinfo = deviceConfigMap.get(deviceId);
        return srinfo != null ?
                ImmutableSet.copyOf(srinfo.adjacencySids.get(sid)) :
                ImmutableSet.copyOf(new HashSet<>());
    }

    /**
     * Check if the Sid given is whether adjacency Sid of the router device or not.
     *
     * @param deviceId device identification of the router
     * @param sid Sid to check
     * @return true if the Sid given is the adjacency Sid of the device,
     * otherwise false
     */
    public boolean isAdjacencySid(DeviceId deviceId, int sid) {
        SegmentRouterInfo srinfo = deviceConfigMap.get(deviceId);
        return srinfo != null && srinfo.adjacencySids.containsKey(sid);
    }

    /**
     * Add subnet to specific connect point.
     *
     * @param cp connect point
     * @param ipPrefix subnet being added to the device
     */
    public void addSubnet(ConnectPoint cp, IpPrefix ipPrefix) {
        checkNotNull(cp);
        checkNotNull(ipPrefix);
        SegmentRouterInfo srinfo = deviceConfigMap.get(cp.deviceId());
        if (srinfo == null) {
            log.warn("Device {} is not configured. Abort.", cp.deviceId());
            return;
        }
        srinfo.subnets.put(cp.port(), ipPrefix);
    }

    /**
     * Remove subnet from specific connect point.
     *
     * @param cp connect point
     * @param ipPrefix subnet being removed to the device
     */
    public void removeSubnet(ConnectPoint cp, IpPrefix ipPrefix) {
        checkNotNull(cp);
        checkNotNull(ipPrefix);
        SegmentRouterInfo srinfo = deviceConfigMap.get(cp.deviceId());
        if (srinfo == null) {
            log.warn("Device {} is not configured. Abort.", cp.deviceId());
            return;
        }
        srinfo.subnets.remove(cp.port(), ipPrefix);
    }

    private boolean isSuppressedPort(ConnectPoint connectPoint) {
        SegmentRoutingAppConfig appConfig = srManager.cfgService
                .getConfig(srManager.appId(), SegmentRoutingAppConfig.class);
        if (appConfig != null && appConfig.suppressSubnet().contains(connectPoint)) {
            log.info("Interface configuration on port {} is ignored", connectPoint);
            return true;
        }
        return false;
    }
}
