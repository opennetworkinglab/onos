package org.onosproject.segmentrouting;

import com.google.common.collect.Lists;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip4Prefix;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onosproject.segmentrouting.grouphandler.DeviceProperties;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.segmentrouting.config.NetworkConfig.SwitchConfig;
import org.onosproject.segmentrouting.config.NetworkConfigManager;
import org.onosproject.segmentrouting.config.SegmentRouterConfig;
import org.onosproject.segmentrouting.config.SegmentRouterConfig.Subnet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Segment Routing configuration component that reads the
 * segment routing related configuration from Network Configuration Manager
 * component and organizes in more accessible formats.
 *
 * TODO: Merge multiple Segment Routing configuration wrapper classes into one.
 */
public class DeviceConfiguration implements DeviceProperties {

    private static final Logger log = LoggerFactory
            .getLogger(DeviceConfiguration.class);
    private final List<Integer> allSegmentIds = new ArrayList<Integer>();
    private final HashMap<DeviceId, SegmentRouterInfo> deviceConfigMap = new HashMap<>();
    private final NetworkConfigManager configService;

    private class SegmentRouterInfo {
        int nodeSid;
        DeviceId deviceId;
        Ip4Address ip;
        MacAddress mac;
        boolean isEdge;
        HashMap<PortNumber, Ip4Address> gatewayIps;
        HashMap<PortNumber, Ip4Prefix> subnets;
        List<SegmentRouterConfig.AdjacencySid> adjacencySids;
    }

    /**
     * Constructor. Reads all the configuration for all devices of type
     * Segment Router and organizes into various maps for easier access.
     *
     * @param configService handle to network configuration manager
     * component from where the relevant configuration is retrieved.
     */
    public DeviceConfiguration(NetworkConfigManager configService) {
        this.configService = checkNotNull(configService);
        List<SwitchConfig> allSwitchCfg =
                this.configService.getConfiguredAllowedSwitches();
        for (SwitchConfig cfg : allSwitchCfg) {
            if (!(cfg instanceof SegmentRouterConfig)) {
                continue;
            }
            SegmentRouterInfo info = new SegmentRouterInfo();
            info.nodeSid = ((SegmentRouterConfig) cfg).getNodeSid();
            info.deviceId = ((SegmentRouterConfig) cfg).getDpid();
            info.mac = MacAddress.valueOf(((
                    SegmentRouterConfig) cfg).getRouterMac());
            String routerIp = ((SegmentRouterConfig) cfg).getRouterIp();
            Ip4Prefix prefix = checkNotNull(IpPrefix.valueOf(routerIp).getIp4Prefix());
            info.ip = prefix.address();
            info.isEdge = ((SegmentRouterConfig) cfg).isEdgeRouter();
            info.subnets = new HashMap<>();
            info.gatewayIps = new HashMap<PortNumber, Ip4Address>();
            for (Subnet s: ((SegmentRouterConfig) cfg).getSubnets()) {
                info.subnets.put(PortNumber.portNumber(s.getPortNo()),
                                 Ip4Prefix.valueOf(s.getSubnetIp()));
                String gatewayIp = s.getSubnetIp().
                        substring(0, s.getSubnetIp().indexOf('/'));
                info.gatewayIps.put(PortNumber.portNumber(s.getPortNo()),
                                    Ip4Address.valueOf(gatewayIp));
            }
            info.adjacencySids = ((SegmentRouterConfig) cfg).getAdjacencySids();
            this.deviceConfigMap.put(info.deviceId, info);
            this.allSegmentIds.add(info.nodeSid);

        }
    }

    /**
     * Returns the segment id of a segment router.
     *
     * @param deviceId device identifier
     * @return segment id
     */
    @Override
    public int getSegmentId(DeviceId deviceId) {
        if (deviceConfigMap.get(deviceId) != null) {
            log.debug("getSegmentId for device{} is {}",
                    deviceId,
                    deviceConfigMap.get(deviceId).nodeSid);
            return deviceConfigMap.get(deviceId).nodeSid;
        } else {
            log.warn("getSegmentId for device {} "
                    + "throwing IllegalStateException "
                    + "because device does not exist in config", deviceId);
            throw new IllegalStateException();
        }
    }

    /**
     * Returns the segment id of a segment router given its mac address.
     *
     * @param routerMac router mac address
     * @return segment id
     */
    public int getSegmentId(MacAddress routerMac) {
        for (Map.Entry<DeviceId, SegmentRouterInfo> entry:
                    deviceConfigMap.entrySet()) {
            if (entry.getValue().mac.equals(routerMac)) {
                return entry.getValue().nodeSid;
            }
        }

        return -1;
    }

    /**
     * Returns the segment id of a segment router given its router ip address.
     *
     * @param routerAddress router ip address
     * @return segment id
     */
    public int getSegmentId(Ip4Address routerAddress) {
        for (Map.Entry<DeviceId, SegmentRouterInfo> entry:
            deviceConfigMap.entrySet()) {
            if (entry.getValue().ip.equals(routerAddress)) {
                return entry.getValue().nodeSid;
            }
        }

        return -1;
    }

    /**
     * Returns the router mac of a segment router.
     *
     * @param deviceId device identifier
     * @return router mac address
     */
    @Override
    public MacAddress getDeviceMac(DeviceId deviceId) {
        if (deviceConfigMap.get(deviceId) != null) {
            log.debug("getDeviceMac for device{} is {}",
                    deviceId,
                    deviceConfigMap.get(deviceId).mac);
            return deviceConfigMap.get(deviceId).mac;
        } else {
            log.warn("getDeviceMac for device {} "
                    + "throwing IllegalStateException "
                    + "because device does not exist in config", deviceId);
            throw new IllegalStateException();
        }
    }

    /**
     * Returns the router ip address of a segment router.
     *
     * @param deviceId device identifier
     * @return router ip address
     */
    public Ip4Address getRouterIp(DeviceId deviceId) {
        if (deviceConfigMap.get(deviceId) != null) {
            log.debug("getDeviceIp for device{} is {}",
                    deviceId,
                    deviceConfigMap.get(deviceId).ip);
            return deviceConfigMap.get(deviceId).ip;
        } else {
            log.warn("getRouterIp for device {} "
                    + "throwing IllegalStateException "
                    + "because device does not exist in config", deviceId);
            throw new IllegalStateException();
        }
    }

    /**
     * Indicates if the segment router is a edge router or
     * a transit/back bone router.
     *
     * @param deviceId device identifier
     * @return boolean
     */
    @Override
    public boolean isEdgeDevice(DeviceId deviceId) {
        if (deviceConfigMap.get(deviceId) != null) {
            log.debug("isEdgeDevice for device{} is {}",
                    deviceId,
                    deviceConfigMap.get(deviceId).isEdge);
            return deviceConfigMap.get(deviceId).isEdge;
        } else {
            log.warn("isEdgeDevice for device {} "
                    + "throwing IllegalStateException "
                    + "because device does not exist in config", deviceId);
            throw new IllegalStateException();
        }
    }

    /**
     * Returns the segment ids of all configured segment routers.
     *
     * @return list of segment ids
     */
    @Override
    public List<Integer> getAllDeviceSegmentIds() {
        return allSegmentIds;
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
            if (entry.getValue().nodeSid == sid) {
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
            if (entry.getValue().ip.equals(ipAddress)) {
                return entry.getValue().deviceId;
            }
        }

        return null;
    }

    /**
     * Returns the configured subnet gateway ip addresses for a segment router.
     *
     * @param deviceId device identifier
     * @return list of ip addresses
     */
    public List<Ip4Address> getSubnetGatewayIps(DeviceId deviceId) {
        if (deviceConfigMap.get(deviceId) != null) {
            log.debug("getSubnetGatewayIps for device{} is {}",
                    deviceId,
                    deviceConfigMap.get(deviceId).gatewayIps.values());
            return new ArrayList<Ip4Address>(deviceConfigMap.
                    get(deviceId).gatewayIps.values());
        } else {
            return null;
        }
    }

    /**
     * Returns the configured subnet prefixes for a segment router.
     *
     * @param deviceId device identifier
     * @return list of ip prefixes
     */
    public List<Ip4Prefix> getSubnets(DeviceId deviceId) {
        if (deviceConfigMap.get(deviceId) != null) {
            log.debug("getSubnets for device{} is {}",
                    deviceId,
                    deviceConfigMap.get(deviceId).subnets.values());
            return new ArrayList<Ip4Prefix>(deviceConfigMap.
                    get(deviceId).subnets.values());
        } else {
            return null;
        }
    }

    /**
     * Returns the router ip address of segment router that has the
     * specified ip address in its subnets.
     *
     * @param destIpAddress target ip address
     * @return router ip address
     */
    public Ip4Address getRouterIpAddressForASubnetHost(Ip4Address destIpAddress) {
        for (Map.Entry<DeviceId, SegmentRouterInfo> entry:
                    deviceConfigMap.entrySet()) {
            for (Ip4Prefix prefix:entry.getValue().subnets.values()) {
                if (prefix.contains(destIpAddress)) {
                    return entry.getValue().ip;
                }
            }
        }

        log.debug("No router was found for {}", destIpAddress);
        return null;
    }

    /**
     * Returns the router mac address of segment router that has the
     * specified ip address as one of its subnet gateway ip address.
     *
     * @param gatewayIpAddress router gateway ip address
     * @return router mac address
     */
    public MacAddress getRouterMacForAGatewayIp(Ip4Address gatewayIpAddress) {
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
    public boolean inSameSubnet(DeviceId deviceId, Ip4Address hostIp) {

        List<Ip4Prefix> subnets = getSubnets(deviceId);
        if (subnets == null) {
            return false;
        }

        for (Ip4Prefix subnet: subnets) {
            if (subnet.contains(hostIp)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns the ports corresponding to the adjacency Sid given.
     *
     * @param deviceId device identification of the router
     * @param sid adjacency Sid
     * @return list of port numbers
     */
    public List<Integer> getPortsForAdjacencySid(DeviceId deviceId, int sid) {
        if (deviceConfigMap.get(deviceId) != null) {
            for (SegmentRouterConfig.AdjacencySid asid : deviceConfigMap.get(deviceId).adjacencySids) {
                if (asid.getAdjSid() == sid) {
                    return asid.getPorts();
                }
            }
        }

        return Lists.newArrayList();
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
        if (deviceConfigMap.get(deviceId) != null) {
            if (deviceConfigMap.get(deviceId).adjacencySids.isEmpty()) {
                return false;
            } else {
                for (SegmentRouterConfig.AdjacencySid asid:
                        deviceConfigMap.get(deviceId).adjacencySids) {
                    if (asid.getAdjSid() == sid) {
                        return true;
                    }
                }
                return false;
            }
        }

        return false;
    }
}
