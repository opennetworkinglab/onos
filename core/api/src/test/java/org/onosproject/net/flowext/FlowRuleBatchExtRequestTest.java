package org.onosproject.net.flowext;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import java.nio.ByteBuffer;
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
        String deviceId2 = "of:234567";
        ByteBuffer buffer1 = ByteBuffer.wrap(deviceId1.getBytes());
        ByteBuffer buffer2 = ByteBuffer.wrap(deviceId2.getBytes());
        FlowRuleExt entry1 = new DefaultFlowRuleExt(DeviceId
                     .deviceId(deviceId1), new DownStreamFlowEntry(buffer1), null);
        FlowRuleExt entry2 = new DefaultFlowRuleExt(DeviceId
                     .deviceId(deviceId2), new DownStreamFlowEntry(buffer2), null);
        Collection<FlowRuleExt> toAdd = new ArrayList<FlowRuleExt>();
        toAdd.add(entry1);
        toAdd.add(entry2);
        FlowRuleBatchExtRequest request = new FlowRuleBatchExtRequest(1, toAdd);
        assertThat(request.getBatch(), hasSize(2));
        assertThat(request.batchId(), is(1));
        assertThat(request.getBatch().toArray()[0], equalTo(entry1));
        assertThat(request.getBatch().toArray()[1], equalTo(entry2));
    }
}
