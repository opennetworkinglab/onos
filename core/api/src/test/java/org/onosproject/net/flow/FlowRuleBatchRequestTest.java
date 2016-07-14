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

import org.junit.Test;
import org.onosproject.net.intent.IntentTestsMocks;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.onosproject.net.flow.FlowRuleBatchEntry.FlowRuleOperation.ADD;
import static org.onosproject.net.flow.FlowRuleBatchEntry.FlowRuleOperation.REMOVE;

/**
 * Unit tests for the FlowRuleBatchRequest class.
 */
public class FlowRuleBatchRequestTest {

    /**
     * Tests that construction of FlowRuleBatchRequest objects returns the
     * correct objects.
     */
    @Test
    public void testConstruction() {
        final FlowRule rule1 = new IntentTestsMocks.MockFlowRule(1);
        final FlowRule rule2 = new IntentTestsMocks.MockFlowRule(2);
        final Set<FlowRuleBatchEntry> batch = new HashSet<>();
        batch.add(new FlowRuleBatchEntry(ADD, rule1));

        batch.add(new FlowRuleBatchEntry(REMOVE, rule2));


        final FlowRuleBatchRequest request =
                new FlowRuleBatchRequest(1, batch);

        assertThat(request.ops(), hasSize(2));
        assertThat(request.batchId(), is(1L));

        final FlowRuleBatchOperation op = request.asBatchOperation(rule1.deviceId());
        assertThat(op.size(), is(2));

        final List<FlowRuleBatchEntry> ops = op.getOperations();
        assertThat(ops, hasSize(2));
    }

}
