package org.onosproject.net.flowext;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.Test;
import org.onosproject.net.DeviceId;

/**
 * junit for FlowRuleBatchExtRequest constructor.
 */
public class FlowRuleBatchExtRequestTest {

    @Test
    public void testConstructor() {
        String deviceId1 = "of:123456";
        FlowRuleExtEntry entry1 = new FlowRuleExtEntry(DeviceId.deviceId(deviceId1), null, deviceId1.getBytes());
        String deviceId2 = "of:234567";
        FlowRuleExtEntry entry2 = new FlowRuleExtEntry(DeviceId.deviceId(deviceId2), null, deviceId2.getBytes());
        Collection<FlowRuleExtEntry> toAdd = new ArrayList<FlowRuleExtEntry>();
        toAdd.add(entry1);
        toAdd.add(entry2);
        FlowRuleBatchExtRequest request = new FlowRuleBatchExtRequest(1, toAdd);
        assertThat(request.getBatch(), hasSize(2));
        assertThat(request.batchId(), is(1));
        assertThat(request.getBatch().toArray()[0], equalTo(entry1));
        assertThat(request.getBatch().toArray()[1], equalTo(entry2));
    }
}
