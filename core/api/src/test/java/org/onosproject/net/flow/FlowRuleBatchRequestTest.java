/*
 * Copyright 2014 Open Networking Laboratory
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
import java.util.List;

import org.junit.Test;
import org.onosproject.net.intent.IntentTestsMocks;

import static org.onosproject.net.flow.FlowRuleBatchEntry.FlowRuleOperation.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

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
        final List<FlowRuleBatchEntry> toAdd = new LinkedList<>();
        toAdd.add(new FlowRuleBatchEntry(ADD, rule1));
        final List<FlowRuleBatchEntry> toRemove = new LinkedList<>();
        toRemove.add(new FlowRuleBatchEntry(REMOVE, rule2));


        final FlowRuleBatchRequest request =
                new FlowRuleBatchRequest(1, toAdd, toRemove);

        assertThat(request.toAdd(), hasSize(1));
        assertThat(request.toAdd().get(0), is(rule1));
        assertThat(request.toRemove(), hasSize(1));
        assertThat(request.toRemove().get(0), is(rule2));
        assertThat(request.batchId(), is(1));

        final FlowRuleBatchOperation op = request.asBatchOperation();
        assertThat(op.size(), is(2));

        final List<FlowRuleBatchEntry> ops = op.getOperations();
        assertThat(ops, hasSize(2));
    }

}
