package org.onosproject.segmentrouting;

import org.onlab.packet.Ip4Address;
import org.onlab.packet.MacAddress;
import org.onosproject.grouphandler.DeviceProperties;
import org.onosproject.net.DeviceId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DeviceConfiguration implements DeviceProperties {

    private static final Logger log = LoggerFactory
            .getLogger(DeviceConfiguration.class);
    private final List<Integer> allSegmentIds =
            Arrays.asList(101, 102, 103, 104, 105, 106);
    private HashMap<DeviceId, Integer> deviceSegmentIdMap =
            new HashMap<DeviceId, Integer>() {
                {
                    put(DeviceId.deviceId("of:0000000000000001"), 101);
                    put(DeviceId.deviceId("of:0000000000000002"), 102);
                    put(DeviceId.deviceId("of:0000000000000003"), 103);
                    put(DeviceId.deviceId("of:0000000000000004"), 104);
                    put(DeviceId.deviceId("of:0000000000000005"), 105);
                    put(DeviceId.deviceId("of:0000000000000006"), 106);
                }
            };
    private final HashMap<DeviceId, MacAddress> deviceMacMap =
            new HashMap<DeviceId, MacAddress>() {
                {
                    put(DeviceId.deviceId("of:0000000000000001"),
                            MacAddress.valueOf("00:00:00:00:00:01"));
                    put(DeviceId.deviceId("of:0000000000000002"),
                            MacAddress.valueOf("00:00:00:00:00:02"));
                    put(DeviceId.deviceId("of:0000000000000003"),
                            MacAddress.valueOf("00:00:00:00:00:03"));
                    put(DeviceId.deviceId("of:0000000000000004"),
                            MacAddress.valueOf("00:00:00:00:00:04"));
                    put(DeviceId.deviceId("of:0000000000000005"),
                            MacAddress.valueOf("00:00:00:00:00:05"));
                    put(DeviceId.deviceId("of:0000000000000006"),
                            MacAddress.valueOf("00:00:00:00:00:06"));
                }
            };

    private final HashMap<DeviceId, Ip4Address> deviceIpMap =
            new HashMap<DeviceId, Ip4Address>() {
                {
                    put(DeviceId.deviceId("of:0000000000000001"),
                            Ip4Address.valueOf("192.168.0.1"));
                    put(DeviceId.deviceId("of:0000000000000002"),
                            Ip4Address.valueOf("192.168.0.2"));
                    put(DeviceId.deviceId("of:0000000000000003"),
                            Ip4Address.valueOf("192.168.0.3"));
                    put(DeviceId.deviceId("of:0000000000000004"),
                            Ip4Address.valueOf("192.168.0.4"));
                    put(DeviceId.deviceId("of:0000000000000005"),
                            Ip4Address.valueOf("192.168.0.5"));
                    put(DeviceId.deviceId("of:0000000000000006"),
                            Ip4Address.valueOf("192.168.0.6"));
                }
            };

    @Override
    public int getSegmentId(DeviceId deviceId) {
        if (deviceSegmentIdMap.get(deviceId) != null) {
            log.debug("getSegmentId for device{} is {}",
                    deviceId,
                    deviceSegmentIdMap.get(deviceId));
            return deviceSegmentIdMap.get(deviceId);
        } else {
            throw new IllegalStateException();
        }
    }


    @Override
    public MacAddress getDeviceMac(DeviceId deviceId) {
        if (deviceMacMap.get(deviceId) != null) {
            log.debug("getDeviceMac for device{} is {}",
                    deviceId,
                    deviceMacMap.get(deviceId));
            return deviceMacMap.get(deviceId);
        } else {
            throw new IllegalStateException();
        }
    }


    @Override
    public boolean isEdgeDevice(DeviceId deviceId) {
        if (deviceId.equals(DeviceId.deviceId("of:0000000000000001"))
                || deviceId.equals(DeviceId.deviceId("of:0000000000000006"))) {
            return true;
        }

        return false;
    }

    @Override
    public List<Integer> getAllDeviceSegmentIds() {
        return allSegmentIds;
    }


    /**
     * Returns Segment ID for the router with the MAC address given.
     *
     * @param targetMac Mac address for the router
     * @return Segment ID for the router with the MAC address
     */
    public int getSegmentId(MacAddress targetMac) {
        for (Map.Entry<DeviceId, MacAddress> entry: deviceMacMap.entrySet()) {
            if (entry.getValue().equals(targetMac)) {
                return deviceSegmentIdMap.get(entry.getKey());
            }
        }

        return -1;
    }

    /**
     * Returns Segment ID for the router withe IP address given.
     *
     * @param targetAddress IP address of the router
     * @return Segment ID for the router with the IP address
     */
    public int getSegmentId(Ip4Address targetAddress) {
        for (Map.Entry<DeviceId, Ip4Address> entry: deviceIpMap.entrySet()) {
            if (entry.getValue().equals(targetAddress)) {
                return deviceSegmentIdMap.get(entry.getKey());
            }
        }

        return -1;
    }

    /**
     * Returns Router IP address for the router with the device ID given.
     *
     * @param deviceId device ID of the router
     * @return IP address of the router
     */
    public Ip4Address getRouterIp(DeviceId deviceId) {
        if (deviceIpMap.get(deviceId) != null) {
            log.debug("getDeviceIp for device{} is {}",
                    deviceId,
                    deviceIpMap.get(deviceId));
            return deviceIpMap.get(deviceId);
        } else {
            throw new IllegalStateException();
        }
    }

    /**
     * Returns the Device ID of the router with the Segment ID given.
     *
     * @param sid Segment ID of the router
     * @return Device ID of the router
     */
    public DeviceId getDeviceId(int sid) {
        for (Map.Entry<DeviceId, Integer> entry: deviceSegmentIdMap.entrySet()) {
            if (entry.getValue() == sid) {
                return entry.getKey();
            }
        }

        return null;
    }

    /**
     * Returns the Device ID of the router with the IP address given.
     *
     * @param ipAddress IP address of the router
     * @return Device ID of the router
     */
    public DeviceId getDeviceId(Ip4Address ipAddress) {
        for (Map.Entry<DeviceId, Ip4Address> entry: deviceIpMap.entrySet()) {
            if (entry.getValue().equals(ipAddress)) {
                return entry.getKey();
            }
        }

        return null;
    }
}
