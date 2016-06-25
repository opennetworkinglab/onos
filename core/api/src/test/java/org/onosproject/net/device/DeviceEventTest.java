/*
 * Copyright 2014-present Open Networking Laboratory
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
package org.onosproject.net.device;

import static org.junit.Assert.assertEquals;
import static org.onosproject.net.DeviceId.deviceId;

import org.junit.Test;
import org.onosproject.event.AbstractEventTest;
import org.onosproject.net.DefaultDevice;
import org.onosproject.net.DefaultPort;
import org.onosproject.net.Device;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.provider.ProviderId;
import org.onlab.packet.ChassisId;

/**
 * Tests of the device event.
 */
public class DeviceEventTest extends AbstractEventTest {

    private Device createDevice() {
        return new DefaultDevice(new ProviderId("of", "foo"), deviceId("of:foo"),
                Device.Type.SWITCH, "box", "hw", "sw", "sn", new ChassisId());
    }

    @Override
    @Test
    public void withTime() {
        Device device = createDevice();
        Port port = new DefaultPort(device, PortNumber.portNumber(123), true);
        DeviceEvent event = new DeviceEvent(DeviceEvent.Type.DEVICE_ADDED,
                device, port, 123L);
        validateEvent(event, DeviceEvent.Type.DEVICE_ADDED, device, 123L);
        assertEquals("incorrect port", port, event.port());
    }

    @Override
    @Test
    public void withoutTime() {
        Device device = createDevice();
        Port port = new DefaultPort(device, PortNumber.portNumber(123), true);
        long before = System.currentTimeMillis();
        DeviceEvent event = new DeviceEvent(DeviceEvent.Type.DEVICE_ADDED, device, port);
        long after = System.currentTimeMillis();
        validateEvent(event, DeviceEvent.Type.DEVICE_ADDED, device, before, after);
    }

}
