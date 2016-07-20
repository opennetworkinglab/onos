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

import java.util.LinkedList;

import org.junit.Test;
import org.onosproject.net.intent.IntentTestsMocks;

import com.google.common.testing.EqualsTester;

/**
 * Unit tests for flow rule batch classes.
 */
public class FlowRuleBatchOperationTest {

    /**
     * Tests the equals(), hashCode() and toString() methods.
     */
    @Test
    public void testEquals() {
        final FlowRule rule = new IntentTestsMocks.MockFlowRule(1);
        final FlowRuleBatchEntry entry1 = new FlowRuleBatchEntry(
                FlowRuleBatchEntry.FlowRuleOperation.ADD, rule);
        final FlowRuleBatchEntry entry2 = new FlowRuleBatchEntry(
                FlowRuleBatchEntry.FlowRuleOperation.MODIFY, rule);
        final FlowRuleBatchEntry entry3 = new FlowRuleBatchEntry(
                FlowRuleBatchEntry.FlowRuleOperation.REMOVE, rule);
        final LinkedList<FlowRuleBatchEntry> ops1 = new LinkedList<>();
        ops1.add(entry1);
        final LinkedList<FlowRuleBatchEntry> ops2 = new LinkedList<>();
        ops1.add(entry2);
        final LinkedList<FlowRuleBatchEntry> ops3 = new LinkedList<>();
        ops3.add(entry3);

        final FlowRuleBatchOperation operation1 = new FlowRuleBatchOperation(ops1, null, 0);
        final FlowRuleBatchOperation sameAsOperation1 = new FlowRuleBatchOperation(ops1, null, 0);
        final FlowRuleBatchOperation operation2 = new FlowRuleBatchOperation(ops2, null, 0);
        final FlowRuleBatchOperation operation3 = new FlowRuleBatchOperation(ops3, null, 0);

        new EqualsTester()
                .addEqualityGroup(operation1, sameAsOperation1)
                .addEqualityGroup(operation2)
                .addEqualityGroup(operation3)
                .testEquals();
    }
}
