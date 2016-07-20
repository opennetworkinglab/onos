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

package org.onosproject.provider.snmp.device.impl;

import com.btisystems.pronx.ems.core.snmp.ISnmpSession;
import org.onosproject.incubator.net.faultmanagement.alarm.DefaultAlarm;
import org.onosproject.net.DeviceId;
import org.onosproject.snmp.SnmpController;
import org.onosproject.snmp.SnmpDevice;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Test Adapter for SnmpController API.
 */
public class SnmpControllerAdapter implements SnmpController {

    protected Map<DeviceId, SnmpDevice> devices = new ConcurrentHashMap<>();
    @Override
    public Collection<SnmpDevice> getDevices() {
        return devices.values();
    }

    @Override
    public SnmpDevice getDevice(DeviceId deviceId) {
        return devices.get(deviceId);
    }

    @Override
    public void removeDevice(DeviceId deviceId) {
        devices.remove(deviceId);
    }

    @Override
    public void addDevice(DeviceId deviceId, SnmpDevice snmpDevice) {
        devices.put(deviceId, snmpDevice);
    }

    @Override
    public ISnmpSession getSession(DeviceId deviceId) throws IOException {
        return null;
    }

    @Override
    public DefaultAlarm buildWalkFailedAlarm(DeviceId deviceId) {
        return null;
    }
}
