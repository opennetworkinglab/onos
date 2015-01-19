package org.onosproject.net.flowext;

import static org.junit.Assert.*;

import org.junit.Test;
import org.onosproject.net.DeviceId;

/**
 * junit for FlowRuleExtEntry constructor.
 */
public class FlowRuleExtEntryTest {

    @Test
    public void testConstructor() {
        String deviceId = "of:123456";
        FlowRuleExtEntry entry = new FlowRuleExtEntry(DeviceId.deviceId(deviceId), deviceId.getBytes());
        assertArrayEquals(deviceId.getBytes(), entry.getFlowEntryExt());
        assertEquals(deviceId, entry.getDeviceId().toString());
        assertEquals(deviceId.getBytes().length, entry.getLength());
    }

    @Test
    public void testEquals() {
        String deviceId = "of:123456";
        FlowRuleExtEntry entry1 = new FlowRuleExtEntry(DeviceId.deviceId(deviceId), deviceId.getBytes());
        FlowRuleExtEntry entry2 = new FlowRuleExtEntry(DeviceId.deviceId(deviceId), deviceId.getBytes());
        assertEquals(entry1, entry2);
    }

}
