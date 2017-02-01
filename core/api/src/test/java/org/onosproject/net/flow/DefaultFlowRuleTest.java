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
import org.onosproject.core.GroupId;
import org.onosproject.net.intent.IntentTestsMocks;

import com.google.common.testing.EqualsTester;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutableBaseClass;
import static org.onosproject.net.NetTestTools.APP_ID;
import static org.onosproject.net.NetTestTools.did;

/**
 * Unit tests for the default flow rule class.
 */
public class DefaultFlowRuleTest {
    private static final IntentTestsMocks.MockSelector SELECTOR =
            new IntentTestsMocks.MockSelector();
    private static final IntentTestsMocks.MockTreatment TREATMENT =
            new IntentTestsMocks.MockTreatment();

    private static byte[] b = new byte[3];
    private static FlowRuleExtPayLoad payLoad = FlowRuleExtPayLoad.flowRuleExtPayLoad(b);
    final FlowRule flowRule1 = new IntentTestsMocks.MockFlowRule(1, payLoad);
    final FlowRule sameAsFlowRule1 = new IntentTestsMocks.MockFlowRule(1, payLoad);
    final DefaultFlowRule defaultFlowRule1 = new DefaultFlowRule(flowRule1);
    final DefaultFlowRule sameAsDefaultFlowRule1 = new DefaultFlowRule(sameAsFlowRule1);

    /**
     * Checks that the DefaultFlowRule class is immutable but can be inherited
     * from.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutableBaseClass(DefaultFlowRule.class);
    }

    /**
     * Tests the equals, hashCode and toString methods using Guava EqualsTester.
     */
    @Test
    public void testEquals() {
        new EqualsTester()
                .addEqualityGroup(defaultFlowRule1, sameAsDefaultFlowRule1)
                .testEquals();
    }

    /**
     * Tests creation of a DefaultFlowRule using a FlowRule constructor.
     */
    @Test
    public void testCreationFromFlowRule() {
        assertThat(defaultFlowRule1.deviceId(), is(flowRule1.deviceId()));
        assertThat(defaultFlowRule1.appId(), is(flowRule1.appId()));
        assertThat(defaultFlowRule1.id(), is(flowRule1.id()));
        assertThat(defaultFlowRule1.isPermanent(), is(flowRule1.isPermanent()));
        assertThat(defaultFlowRule1.priority(), is(flowRule1.priority()));
        assertThat(defaultFlowRule1.selector(), is(flowRule1.selector()));
        assertThat(defaultFlowRule1.treatment(), is(flowRule1.treatment()));
        assertThat(defaultFlowRule1.timeout(), is(flowRule1.timeout()));
        assertThat(defaultFlowRule1.payLoad(), is(flowRule1.payLoad()));
    }

    /**
     * Tests creation of a DefaultFlowRule using a FlowId constructor.
     */

    @Test
    public void testCreationWithFlowId() {
        final FlowRule rule =
                DefaultFlowRule.builder()
                .forDevice(did("1"))
                .withSelector(SELECTOR)
                .withTreatment(TREATMENT)
                .withPriority(22)
                .makeTemporary(44)
                .fromApp(APP_ID)
                .build();

        assertThat(rule.deviceId(), is(did("1")));
        assertThat(rule.isPermanent(), is(false));
        assertThat(rule.priority(), is(22));
        assertThat(rule.selector(), is(SELECTOR));
        assertThat(rule.treatment(), is(TREATMENT));
        assertThat(rule.timeout(), is(44));
    }


    /**
     * Tests creation of a DefaultFlowRule using a PayLoad constructor.
     */
    @Test
    public void testCreationWithPayLoadByFlowTable() {
        final DefaultFlowRule rule =
                new DefaultFlowRule(did("1"), null,
                        null, 22, APP_ID,
                44, false, payLoad);
        assertThat(rule.deviceId(), is(did("1")));
        assertThat(rule.isPermanent(), is(false));
        assertThat(rule.priority(), is(22));
        assertThat(rule.timeout(), is(44));
        assertThat(defaultFlowRule1.payLoad(), is(payLoad));
    }

    /**
     * Tests creation of a DefaultFlowRule using a PayLoad constructor.
     */
    @Test
    public void testCreationWithPayLoadByGroupTable() {
        final DefaultFlowRule rule =
                new DefaultFlowRule(did("1"), null,
                        null, 22, APP_ID, new GroupId(0),
                44, false, payLoad);
        assertThat(rule.deviceId(), is(did("1")));
        assertThat(rule.isPermanent(), is(false));
        assertThat(rule.priority(), is(22));
        assertThat(rule.timeout(), is(44));
        assertThat(rule.groupId(), is(new GroupId(0)));
        assertThat(defaultFlowRule1.payLoad(), is(payLoad));
    }
    /**
     * Tests the creation of a DefaultFlowRule using an AppId constructor.
     */
    @Test
    public void testCreationWithAppId() {
        final FlowRule rule =
                DefaultFlowRule.builder()
                        .forDevice(did("1"))
                        .withSelector(SELECTOR)
                        .withTreatment(TREATMENT)
                        .withPriority(22)
                        .fromApp(APP_ID)
                        .makeTemporary(44)
                        .build();

        assertThat(rule.deviceId(), is(did("1")));
        assertThat(rule.isPermanent(), is(false));
        assertThat(rule.priority(), is(22));
        assertThat(rule.selector(), is(SELECTOR));
        assertThat(rule.treatment(), is(TREATMENT));
        assertThat(rule.timeout(), is(44));
    }

    /**
     * Tests flow ID is consistent.
     */
    @Test
    public void testCreationWithConsistentFlowId() {
        final FlowRule rule1 =
                DefaultFlowRule.builder()
                        .forDevice(did("1"))
                        .withSelector(SELECTOR)
                        .withTreatment(TREATMENT)
                        .withPriority(22)
                        .forTable(1)
                        .fromApp(APP_ID)
                        .makeTemporary(44)
                        .build();

        final FlowRule rule2 =
                DefaultFlowRule.builder()
                        .forDevice(did("1"))
                        .withSelector(SELECTOR)
                        .withTreatment(TREATMENT)
                        .withPriority(22)
                        .forTable(1)
                        .fromApp(APP_ID)
                        .makeTemporary(44)
                        .build();

        new EqualsTester().addEqualityGroup(rule1.id(), rule2.id()).testEquals();
    }
}
