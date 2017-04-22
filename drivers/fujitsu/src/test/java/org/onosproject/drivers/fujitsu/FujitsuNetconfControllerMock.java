/*
 * Copyright 2016-present Open Networking Laboratory
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

package org.onosproject.drivers.fujitsu;

import com.google.common.collect.ImmutableMap;
import org.onlab.packet.IpAddress;
import org.onosproject.net.DeviceId;
import org.onosproject.net.driver.DefaultDriver;
import org.onosproject.net.driver.DefaultDriverData;
import org.onosproject.netconf.NetconfDevice;
import org.onosproject.netconf.NetconfDeviceInfo;
import org.onosproject.netconf.NetconfException;
import org.onosproject.netconf.ctl.NetconfControllerImpl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


/**
 * Mock NetconfControllerImpl.
 */
class FujitsuNetconfControllerMock extends NetconfControllerImpl {

    private static final String VOLT_DRIVER_NAME = "fujitsu-volt-netconf";
    private static final String VOLT_DEVICE_USERNAME = "abc";
    private static final String VOLT_DEVICE_PASSWORD = "123";
    private static final String VOLT_DEVICE_IP = "10.10.10.11";
    private static final int VOLT_DEVICE_PORT = 830;

    private Map<DeviceId, NetconfDevice> netconfDeviceMap = new ConcurrentHashMap<>();

    @Override
    public NetconfDevice getNetconfDevice(DeviceId deviceInfo) {
        return netconfDeviceMap.get(deviceInfo);
    }

    @Override
    public NetconfDevice getNetconfDevice(IpAddress ip, int port) {
        for (DeviceId info : netconfDeviceMap.keySet()) {
            if (info.uri().getSchemeSpecificPart().equals(ip.toString() + ":" + port)) {
                return netconfDeviceMap.get(info);
            }
        }
        return null;
    }

    @Override
    public NetconfDevice connectDevice(DeviceId deviceId) throws NetconfException {
        if (netconfDeviceMap.containsKey(deviceId)) {
            log.debug("Device {} is already present", deviceId);
            return netconfDeviceMap.get(deviceId);
        } else {
            log.debug("Creating NETCONF device {}", deviceId);
            String ip;
            int port;
            String[] info = deviceId.toString().split(":");
            if (info.length == 3) {
                ip = info[1];
                port = Integer.parseInt(info[2]);
            } else {
                ip = Arrays.asList(info).stream().filter(el -> !el.equals(info[0])
                && !el.equals(info[info.length - 1]))
                        .reduce((t, u) -> t + ":" + u)
                        .get();
                log.debug("ip v6 {}", ip);
                port = Integer.parseInt(info[info.length - 1]);
            }
            try {
                NetconfDeviceInfo deviceInfo = new NetconfDeviceInfo(
                                               VOLT_DEVICE_USERNAME,
                                               VOLT_DEVICE_PASSWORD,
                                               IpAddress.valueOf(ip),
                                               port);
                NetconfDevice netconfDevice = new FujitsuNetconfDeviceMock(deviceInfo);
                netconfDeviceMap.put(deviceInfo.getDeviceId(), netconfDevice);
                return netconfDevice;
            } catch (NullPointerException e) {
                throw new NetconfException("Cannot connect a device " + deviceId, e);
            }
        }
    }

    @Override
    public void disconnectDevice(DeviceId deviceId, boolean remove) {
        if (!netconfDeviceMap.containsKey(deviceId)) {
            log.warn("Device {} is not present", deviceId);
        } else {
            netconfDeviceMap.remove(deviceId);
        }
    }

    @Override
    public Map<DeviceId, NetconfDevice> getDevicesMap() {
        return netconfDeviceMap;
    }

    @Override
    public Set<DeviceId> getNetconfDevices() {
        return netconfDeviceMap.keySet();
    }

    @Override
    public void removeDevice(DeviceId deviceId) {
        if (!netconfDeviceMap.containsKey(deviceId)) {
            log.warn("Device {} is not present", deviceId);
        } else {
            netconfDeviceMap.remove(deviceId);
        }
    }

    /**
     * Sets up initial test environment.
     *
     * @param listener listener to be added
     * @return driver handler
     * @throws NetconfException when there is a problem
     */
    public FujitsuDriverHandlerAdapter setUp(FujitsuNetconfSessionListenerTest listener) throws NetconfException {
        try {
            NetconfDeviceInfo deviceInfo = new NetconfDeviceInfo(
                    VOLT_DEVICE_USERNAME, VOLT_DEVICE_PASSWORD,
                    IpAddress.valueOf(VOLT_DEVICE_IP), VOLT_DEVICE_PORT);

            NetconfDevice netconfDevice = connectDevice(deviceInfo.getDeviceId());

            FujitsuNetconfSessionMock session = (FujitsuNetconfSessionMock) netconfDevice.getSession();
            session.setListener(listener);

            DeviceId deviceId = deviceInfo.getDeviceId();
            DefaultDriver driver = new DefaultDriver(
                    VOLT_DRIVER_NAME, new ArrayList<>(),
                    "Fujitsu", "1.0", "1.0",
                    ImmutableMap.of(), ImmutableMap.of());

            DefaultDriverData driverData = new DefaultDriverData(driver, deviceId);

            FujitsuDriverHandlerAdapter driverHandler;
            driverHandler = new FujitsuDriverHandlerAdapter(driverData);
            driverHandler.setUp(this);

            return driverHandler;
        } catch (NetconfException e) {
            throw new NetconfException("Cannot create a device ", e);
        }
    }

}
