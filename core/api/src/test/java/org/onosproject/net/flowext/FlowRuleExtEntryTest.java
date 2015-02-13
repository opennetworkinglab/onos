package org.onosproject.net.flowext;

import static org.junit.Assert.*;

import java.nio.ByteBuffer;

import org.junit.Test;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.net.DeviceId;

/**
 * junit for FlowRuleExtEntry constructor.
 */
public class FlowRuleExtEntryTest {

    @Test
    public void testConstructor() {
        String deviceId = "of:123456";
        ByteBuffer buffer1 = ByteBuffer.wrap(deviceId.getBytes());
        FlowRuleExt entry = new DefaultFlowRuleExt(new DefaultApplicationId((short) 0, "test"),
                   DeviceId.deviceId(deviceId), new DownStreamFlowEntry(buffer1));
        assertArrayEquals(deviceId.getBytes(), entry.getFlowEntryExt().getPayload().array());
        assertEquals(deviceId, entry.deviceId().toString());
        assertEquals(deviceId.getBytes().length, entry.getFlowEntryExt().getPayload().array().length);
    }

    @Test
    public void testEquals() {
        String deviceId = "of:123456";
        ByteBuffer buffer1 = ByteBuffer.wrap(deviceId.getBytes());
        FlowRuleExt entry1 = new DefaultFlowRuleExt(new DefaultApplicationId((short) 0, "test"),
                   DeviceId.deviceId(deviceId), new DownStreamFlowEntry(buffer1));
        FlowRuleExt entry2 = new DefaultFlowRuleExt(new DefaultApplicationId((short) 0, "test"),
                   DeviceId.deviceId(deviceId), new DownStreamFlowEntry(buffer1));
        assertEquals(entry1, entry2);
    }

}
