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

package org.onosproject.snmp.ctl;

import com.btisystems.pronx.ems.core.snmp.DefaultSnmpConfigurationFactory;
import com.btisystems.pronx.ems.core.snmp.ISnmpConfiguration;
import com.btisystems.pronx.ems.core.snmp.ISnmpSession;
import com.btisystems.pronx.ems.core.snmp.ISnmpSessionFactory;
import com.btisystems.pronx.ems.core.snmp.SnmpSessionFactory;
import com.btisystems.pronx.ems.core.snmp.V2cSnmpConfiguration;
import com.google.common.base.Preconditions;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.incubator.net.faultmanagement.alarm.Alarm;
import org.onosproject.incubator.net.faultmanagement.alarm.DefaultAlarm;
import org.onosproject.net.DeviceId;
import org.onosproject.snmp.SnmpController;
import org.onosproject.snmp.SnmpDevice;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default implementation of the SNMP sub-controller.
 */
@Component(immediate = true)
@Service
public class DefaultSnmpController implements SnmpController {

    private final Logger log = LoggerFactory
            .getLogger(getClass());

    protected ISnmpSessionFactory sessionFactory;

    protected final Map<DeviceId, ISnmpSession> sessionMap = new HashMap<>();
    protected final Map<DeviceId, SnmpDevice> snmpDeviceMap = new ConcurrentHashMap<>();

    @Activate
    public void activate(ComponentContext context) {
        sessionFactory = new SnmpSessionFactory(
                new DefaultSnmpConfigurationFactory(new V2cSnmpConfiguration()));
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        sessionMap.clear();
        snmpDeviceMap.clear();
        log.info("Stopped");
    }

    @Override
    public ISnmpSession getSession(DeviceId deviceId) throws IOException {
        if (!sessionMap.containsKey(deviceId)) {
            SnmpDevice device = snmpDeviceMap.get(deviceId);
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

            ISnmpConfiguration config = new V2cSnmpConfiguration();
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
    public void removeDevice(DeviceId did) {
        snmpDeviceMap.remove(did);
    }

    @Override
    public void addDevice(DeviceId did, SnmpDevice device) {
        snmpDeviceMap.put(did, device);
    }

    @Override
    public DefaultAlarm buildWalkFailedAlarm(DeviceId deviceId) {
        return new DefaultAlarm.Builder(
                deviceId, "SNMP alarm retrieval failed",
                Alarm.SeverityLevel.CRITICAL,
                System.currentTimeMillis()).build();
    }
}
