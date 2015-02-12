package org.onosproject.net.flowext;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.event.AbstractEventTest;
import org.onosproject.net.DeviceId;

import com.google.common.testing.EqualsTester;

/**
 * Unit Tests for the FlowRuleExtEventTest class.
 */
public class FlowRuleExtEventTest extends AbstractEventTest {

    @Test
    public void testEquals() {
        String deviceId1 = "of:123456";
        String deviceId2 = "of:234567";
        ByteBuffer buffer1 = ByteBuffer.wrap(deviceId1.getBytes());
        ByteBuffer buffer2 = ByteBuffer.wrap(deviceId2.getBytes());
        FlowRuleExt entry1 = new DefaultFlowRuleExt(new DefaultApplicationId((short) 0, "test"),
                     DeviceId.deviceId(deviceId1), new DownStreamFlowEntry(buffer1));
        FlowRuleExt entry2 = new DefaultFlowRuleExt(new DefaultApplicationId((short) 0, "test"),
                     DeviceId.deviceId(deviceId2), new DownStreamFlowEntry(buffer2));
        final long time = 123L;
        final FlowRuleExtEvent event1 =
                new FlowRuleExtEvent(FlowRuleExtEvent.Type.RULE_ADDED, entry1, time);
        final FlowRuleExtEvent sameAsEvent1 =
                new FlowRuleExtEvent(FlowRuleExtEvent.Type.RULE_ADDED, entry1, time);
        final FlowRuleExtEvent event2 =
                new FlowRuleExtEvent(FlowRuleExtEvent.Type.RULE_ADD_REQUESTED,
                                     entry2, time);

        // Equality for events is based on Object, these should all compare
        // as different.
        new EqualsTester()
                .addEqualityGroup(event1)
                .addEqualityGroup(sameAsEvent1)
                .addEqualityGroup(event2)
                .testEquals();
    }

    /**
     * Tests the constructor where a time is passed in.
     */
    @Test
    public void testTimeConstructor() {
        final long time = 123L;
        String deviceId1 = "of:123456";
        ByteBuffer buffer1 = ByteBuffer.wrap(deviceId1.getBytes());
        FlowRuleExt entry1 = new DefaultFlowRuleExt(new DefaultApplicationId((short) 0, "test"),
                      DeviceId.deviceId(deviceId1), new DownStreamFlowEntry(buffer1));
        final FlowRuleExtEvent event =
                new FlowRuleExtEvent(FlowRuleExtEvent.Type.RULE_REMOVE_REQUESTED, entry1, time);
        validateEvent(event, FlowRuleExtEvent.Type.RULE_REMOVE_REQUESTED, entry1, time);
    }

    /**
     * Tests the constructor with the default time value.
     */
    @Test
    public void testConstructor() {
        final long time = System.currentTimeMillis();
        String deviceId1 = "of:123456";
        ByteBuffer buffer1 = ByteBuffer.wrap(deviceId1.getBytes());
        FlowRuleExt entry1 = new DefaultFlowRuleExt(new DefaultApplicationId((short) 0, "test"),
                      DeviceId.deviceId(deviceId1), new DownStreamFlowEntry(buffer1));
        final FlowRuleExtEvent event =
                new FlowRuleExtEvent(FlowRuleExtEvent.Type.RULE_UPDATED, entry1);
        validateEvent(event, FlowRuleExtEvent.Type.RULE_UPDATED, entry1, time,
                time + TimeUnit.SECONDS.toMillis(30));
    }
}