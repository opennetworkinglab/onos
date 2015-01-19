package org.onosproject.net.flowext;

import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.onosproject.event.AbstractEventTest;
import org.onosproject.net.DeviceId;

import com.google.common.testing.EqualsTester;

/**
 * Unit Tests for the FlowRuleExtEventTest class.
 */
public class FlowRuleExtEventTest extends AbstractEventTest {

    @Test
    public void testEquals() {
        final FlowRuleExtEntry flowRule1 = new FlowRuleExtEntry(DeviceId.deviceId("of:123456"), "of:123456".getBytes());
        final FlowRuleExtEntry flowRule2 = new FlowRuleExtEntry(DeviceId.deviceId("of:234567"), "of:234567".getBytes());
        final long time = 123L;
        final FlowRuleExtEvent event1 =
                new FlowRuleExtEvent(FlowRuleExtEvent.Type.RULE_ADDED, flowRule1, time);
        final FlowRuleExtEvent sameAsEvent1 =
                new FlowRuleExtEvent(FlowRuleExtEvent.Type.RULE_ADDED, flowRule1, time);
        final FlowRuleExtEvent event2 =
                new FlowRuleExtEvent(FlowRuleExtEvent.Type.RULE_ADD_REQUESTED,
                                  flowRule2, time);

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
        final FlowRuleExtEntry flowRule = new FlowRuleExtEntry(DeviceId.deviceId("of:123456"), "of:123456".getBytes());
        final FlowRuleExtEvent event =
                new FlowRuleExtEvent(FlowRuleExtEvent.Type.RULE_REMOVE_REQUESTED, flowRule, time);
        validateEvent(event, FlowRuleExtEvent.Type.RULE_REMOVE_REQUESTED, flowRule, time);
        
    }

    /**
     * Tests the constructor with the default time value.
     */
    @Test
    public void testConstructor() {
        final long time = System.currentTimeMillis();
        final FlowRuleExtEntry flowRule = new FlowRuleExtEntry(DeviceId.deviceId("of:123456"), "of:123456".getBytes());
        final FlowRuleExtEvent event =
                new FlowRuleExtEvent(FlowRuleExtEvent.Type.RULE_UPDATED, flowRule);
        validateEvent(event, FlowRuleExtEvent.Type.RULE_UPDATED, flowRule, time,
                time + TimeUnit.SECONDS.toMillis(30));
    }

}
