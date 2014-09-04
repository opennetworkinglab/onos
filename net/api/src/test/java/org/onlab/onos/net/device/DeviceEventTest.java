package org.onlab.onos.net.device;

import org.junit.Test;
import org.onlab.onos.event.AbstractEventTest;
import org.onlab.onos.net.DefaultDevice;
import org.onlab.onos.net.Device;
import org.onlab.onos.net.provider.ProviderId;

import static org.onlab.onos.net.DeviceId.deviceId;

/**
 * Tests of the device event.
 */
public class DeviceEventTest extends AbstractEventTest {

    private Device createDevice() {
        return new DefaultDevice(new ProviderId("foo"), deviceId("of:foo"),
                                 Device.Type.SWITCH, "box", "hw", "sw", "sn");
    }

    @Test
    public void withTime() {
        Device device = createDevice();
        DeviceEvent event = new DeviceEvent(DeviceEvent.Type.DEVICE_ADDED,
                                            device, 123L);
        validateEvent(event, DeviceEvent.Type.DEVICE_ADDED, device, 123L);
    }

    @Test
    public void withoutTime() {
        Device device = createDevice();
        long before = System.currentTimeMillis();
        DeviceEvent event = new DeviceEvent(DeviceEvent.Type.DEVICE_ADDED,
                                            device);
        long after = System.currentTimeMillis();
        validateEvent(event, DeviceEvent.Type.DEVICE_ADDED, device, before, after);
    }

}
