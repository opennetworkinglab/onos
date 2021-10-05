/*
 * Copyright 2016-present Open Networking Foundation
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

package org.onosproject.snmp.ctl;

import com.btisystems.pronx.ems.core.snmp.DefaultSnmpConfigurationFactory;
import com.btisystems.pronx.ems.core.snmp.ISnmpConfiguration;
import com.btisystems.pronx.ems.core.snmp.ISnmpSession;
import com.btisystems.pronx.ems.core.snmp.ISnmpSessionFactory;
import com.btisystems.pronx.ems.core.snmp.SnmpSessionFactory;
import com.btisystems.pronx.ems.core.snmp.V2cSnmpConfiguration;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import org.onosproject.alarm.Alarm;
import org.onosproject.alarm.AlarmId;
import org.onosproject.alarm.DefaultAlarm;
import org.onosproject.net.DeviceId;
import org.onosproject.snmp.SnmpController;
import org.onosproject.snmp.SnmpDevice;
import org.onosproject.snmp.SnmpException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snmp4j.mp.SnmpConstants;

import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default implementation of the SNMP sub-controller.
 */
@Component(immediate = true, service = SnmpController.class)
public class DefaultSnmpController implements SnmpController {

    private final Logger log = LoggerFactory
            .getLogger(getClass());

    protected Map<Integer, ISnmpSessionFactory> factoryMap;

    protected final Map<DeviceId, ISnmpSession> sessionMap = new HashMap<>();
    protected final Map<DeviceId, SnmpDevice> snmpDeviceMap = new ConcurrentHashMap<>();

    @Activate
    public void activate(ComponentContext context) {
        factoryMap = Maps.newHashMap();
        factoryMap.put(SnmpConstants.version2c, new SnmpSessionFactory(
                new DefaultSnmpConfigurationFactory(new V2cSnmpConfiguration())));
        factoryMap.put(SnmpConstants.version3, new SnmpSessionFactory(
                new DefaultSnmpConfigurationFactory(new V3SnmpConfiguration())));
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        sessionMap.clear();
        snmpDeviceMap.clear();
        log.info("Stopped");
    }

    @Override
    @Deprecated
    public ISnmpSession getSession(DeviceId deviceId) throws IOException {
        if (!sessionMap.containsKey(deviceId)) {
            SnmpDevice device = snmpDeviceMap.get(deviceId);
            ISnmpSessionFactory sessionFactory = factoryMap.get(device.getVersion());
            if (Objects.isNull(sessionFactory)) {
                log.error("Invalid session factory", deviceId);
                throw new SnmpException("Invalid session factory");
            }
            String ipAddress = null;
            int port = -1;
            if (device != null) {
                ipAddress = device.getSnmpHost();
                port = device.getSnmpPort();
            } else {
                String[] deviceComponents = deviceId.toString().split(":");
                if (deviceComponents.length > 1) {
                    ipAddress = deviceComponents[1];
                    port = Integer.parseInt(deviceComponents[2]);

                } else {
                    log.error("Cannot obtain correct information from device id", deviceId);
                }
            }
            Preconditions.checkNotNull(ipAddress, "ip address is empty, cannot start session");
            Preconditions.checkArgument(port != -1, "port is incorrect, cannot start session");
            ISnmpConfiguration config;
            if (device.getVersion() == SnmpConstants.version2c) {
                config = new V2cSnmpConfiguration();
                config.setCommunity(device.getCommunity());
            } else if (device.getVersion() == SnmpConstants.version3) {
                DefaultSnmpv3Device v3Device = (DefaultSnmpv3Device) device;
                config = V3SnmpConfiguration.builder()
                        .setAddress(ipAddress)
                        .setSecurityName(v3Device.getSecurityName())
                        .setSecurityLevel(v3Device.getSecurityLevel())
                        .setAuthenticationProtocol(v3Device.getAuthProtocol())
                        .setAuthenticationPassword(v3Device.getAuthPassword())
                        .setPrivacyProtocol(v3Device.getPrivProtocol())
                        .setPrivacyPassword(v3Device.getPrivPassword())
                        .setContextName(v3Device.getContextName())
                        .build();
            } else {
                throw new SnmpException(
                        String.format("Invalid snmp version %d", device.getVersion()));
            }
            config.setPort(port);
            sessionMap.put(deviceId, sessionFactory.createSession(config, ipAddress));
        }
        return sessionMap.get(deviceId);
    }

    @Override
    public Collection<SnmpDevice> getDevices() {
        return snmpDeviceMap.values();
    }

    @Override
    public SnmpDevice getDevice(DeviceId did) {
        return snmpDeviceMap.get(did);
    }

    @Override
    public SnmpDevice getDevice(URI uri) {
            //this assumes that only one device is associated with one deviceId
            return snmpDeviceMap.entrySet()
                    .stream().filter(p -> p.getValue()
                    .getSnmpHost().equals(uri.toString()))
                    .map(Map.Entry::getValue).findFirst().orElse(null);
    }

    @Override
    public void removeDevice(DeviceId did) {
        snmpDeviceMap.remove(did);
    }

    @Override
    public void addDevice(SnmpDevice device) {
        log.info("Adding device {}", device.deviceId());
        snmpDeviceMap.put(device.deviceId(), device);
    }

    @Override
    public DefaultAlarm buildWalkFailedAlarm(DeviceId deviceId) {
        long timeRaised = System.currentTimeMillis();
        return new DefaultAlarm.Builder(
                AlarmId.alarmId(deviceId, Long.toString(timeRaised)),
                deviceId, "SNMP alarm retrieval failed",
                Alarm.SeverityLevel.CRITICAL, timeRaised).build();
    }
}
