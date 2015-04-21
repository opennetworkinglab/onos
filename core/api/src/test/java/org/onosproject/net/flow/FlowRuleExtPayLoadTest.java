package org.onosproject.net.flow;

import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;

import org.junit.Test;

import com.google.common.testing.EqualsTester;
/**
 * Test for FlowRuleExtPayLoad.
 */
public class FlowRuleExtPayLoadTest {
    final byte[] b = new byte[3];
    final byte[] b1 = new byte[5];
    final FlowRuleExtPayLoad payLoad1 = FlowRuleExtPayLoad.flowRuleExtPayLoad(b);
    final FlowRuleExtPayLoad sameAsPayLoad1 = FlowRuleExtPayLoad.flowRuleExtPayLoad(b);
    final FlowRuleExtPayLoad payLoad2 = FlowRuleExtPayLoad.flowRuleExtPayLoad(b1);

    /**
     * Checks that the FlowRuleExtPayLoad class is immutable.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(FlowRuleExtPayLoad.class);
    }

    /**
     * Checks the operation of equals(), hashCode() and toString() methods.
     */
    @Test
    public void testEquals() {
        new EqualsTester()
                .addEqualityGroup(payLoad1, sameAsPayLoad1)
                .addEqualityGroup(payLoad2)
                .testEquals();
    }
}
