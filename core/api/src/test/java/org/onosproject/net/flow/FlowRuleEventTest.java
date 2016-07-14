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
package org.onosproject.net.flow;

import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.onosproject.event.AbstractEventTest;
import org.onosproject.net.intent.IntentTestsMocks;

import com.google.common.testing.EqualsTester;

/**
 * Unit Tests for the FlowRuleEvent class.
 */
public class FlowRuleEventTest extends AbstractEventTest {

    @Test
    public void testEquals() {
        final FlowRule flowRule1 = new IntentTestsMocks.MockFlowRule(1);
        final FlowRule flowRule2 = new IntentTestsMocks.MockFlowRule(2);
        final long time = 123L;
        final FlowRuleEvent event1 =
                new FlowRuleEvent(FlowRuleEvent.Type.RULE_ADDED, flowRule1, time);
        final FlowRuleEvent sameAsEvent1 =
                new FlowRuleEvent(FlowRuleEvent.Type.RULE_ADDED, flowRule1, time);
        final FlowRuleEvent event2 =
                new FlowRuleEvent(FlowRuleEvent.Type.RULE_ADD_REQUESTED,
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
        final FlowRule flowRule = new IntentTestsMocks.MockFlowRule(1);
        final FlowRuleEvent event =
                new FlowRuleEvent(FlowRuleEvent.Type.RULE_REMOVE_REQUESTED, flowRule, time);
        validateEvent(event, FlowRuleEvent.Type.RULE_REMOVE_REQUESTED, flowRule, time);
    }

    /**
     * Tests the constructor with the default time value.
     */
    @Test
    public void testConstructor() {
        final long time = System.currentTimeMillis();
        final FlowRule flowRule = new IntentTestsMocks.MockFlowRule(1);
        final FlowRuleEvent event =
                new FlowRuleEvent(FlowRuleEvent.Type.RULE_UPDATED, flowRule);
        validateEvent(event, FlowRuleEvent.Type.RULE_UPDATED, flowRule, time,
                time + TimeUnit.SECONDS.toMillis(30));
    }
}
