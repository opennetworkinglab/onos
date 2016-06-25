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

import com.google.common.testing.EqualsTester;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;

/**
 * Unit tests for flow id class.
 */
public class FlowIdTest {

    final FlowId flowId1 = FlowId.valueOf(1);
    final FlowId sameAsFlowId1 = FlowId.valueOf(1);
    final FlowId flowId2 = FlowId.valueOf(2);

    /**
     * Checks that the FlowId class is immutable.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(FlowId.class);
    }

    /**
     * Checks the operation of equals(), hashCode() and toString() methods.
     */
    @Test
    public void testEquals() {
        new EqualsTester()
                .addEqualityGroup(flowId1, sameAsFlowId1)
                .addEqualityGroup(flowId2)
                .testEquals();
    }

    /**
     * Checks the construction of a FlowId object.
     */
    @Test
    public void testConstruction() {
        final long flowIdValue = 7777L;
        final FlowId flowId = FlowId.valueOf(flowIdValue);
        assertThat(flowId, is(notNullValue()));
        assertThat(flowId.value(), is(flowIdValue));
    }
}
