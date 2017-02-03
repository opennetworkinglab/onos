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
package org.onosproject.net.intent;

import org.junit.Test;
import org.onlab.util.DataRateUnit;
import org.onosproject.TestApplicationId;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.HostId;
import org.onosproject.net.ResourceGroup;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.intent.constraint.BandwidthConstraint;
import com.google.common.collect.ImmutableList;
import com.google.common.testing.EqualsTester;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;
import static org.onosproject.net.NetTestTools.hid;

/**
 * Unit tests for the HostToHostIntent class.
 */
public class HostToHostIntentTest extends IntentTest {
    private final TrafficSelector selector = new IntentTestsMocks.MockSelector();
    private final IntentTestsMocks.MockTreatment treatment = new IntentTestsMocks.MockTreatment();
    private final HostId id1 = hid("12:34:56:78:91:ab/1");
    private final HostId id2 = hid("12:34:56:78:92:ab/1");
    private final HostId id3 = hid("12:34:56:78:93:ab/1");
    private final ResourceGroup resourceGrouop = ResourceGroup.of(0L);

    private static final ApplicationId APPID = new TestApplicationId("foo");

    private HostToHostIntent makeHostToHost(HostId one, HostId two) {
        return HostToHostIntent.builder()
                .appId(APPID)
                .one(one)
                .two(two)
                .selector(selector)
                .treatment(treatment)
                .build();
    }

    /**
     * Tests the equals() method where two HostToHostIntents have references
     * to the same hosts. These should compare equal.
     */
    @Test
    public void testSameEquals() {

        HostId one = hid("00:00:00:00:00:01/-1");
        HostId two = hid("00:00:00:00:00:02/-1");
        HostToHostIntent i1 = makeHostToHost(one, two);
        HostToHostIntent i2 = makeHostToHost(one, two);

        assertThat(i1.one(), is(equalTo(i2.one())));
        assertThat(i1.two(), is(equalTo(i2.two())));
    }

    /**
     * Checks that the HostToHostIntent class is immutable.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(HostToHostIntent.class);
    }

    /**
     * Tests equals(), hashCode() and toString() methods.
     */
    @Test
    public void testEquals() {
        final HostToHostIntent intent1 = HostToHostIntent.builder()
                .appId(APPID)
                .one(id1)
                .two(id2)
                .selector(selector)
                .treatment(treatment)
                .build();

        final HostToHostIntent intent2 = HostToHostIntent.builder()
                .appId(APPID)
                .one(id2)
                .two(id3)
                .selector(selector)
                .treatment(treatment)
                .build();

        new EqualsTester()
                .addEqualityGroup(intent1)
                .addEqualityGroup(intent2)
                .testEquals();
    }

    @Test
    public void testImplicitConstraintsAreAdded() {
        final Constraint other = BandwidthConstraint.of(1, DataRateUnit.GBPS);
        final HostToHostIntent intent = HostToHostIntent.builder()
                .appId(APPID)
                .one(id1)
                .two(id2)
                .selector(selector)
                .treatment(treatment)
                .constraints(ImmutableList.of(other))
                .build();

        assertThat(intent.constraints(), hasItem(HostToHostIntent.NOT_OPTICAL));
    }

    @Test
    public void testImplicitConstraints() {
        final HostToHostIntent implicit = HostToHostIntent.builder()
                .appId(APPID)
                .one(id1)
                .two(id2)
                .selector(selector)
                .treatment(treatment)
                .build();
        final HostToHostIntent empty = HostToHostIntent.builder()
                .appId(APPID)
                .one(id1)
                .two(id2)
                .selector(selector)
                .treatment(treatment)
                .constraints(ImmutableList.of())
                .build();
        final HostToHostIntent exact = HostToHostIntent.builder()
                .appId(APPID)
                .one(id1)
                .two(id2)
                .selector(selector)
                .treatment(treatment)
                .constraints(ImmutableList.of(HostToHostIntent.NOT_OPTICAL))
                .build();

        new EqualsTester()
            .addEqualityGroup(implicit.constraints(),
                              empty.constraints(),
                              exact.constraints())
            .testEquals();

    }

    @Test
    public void testResourceGroup() {
        final HostToHostIntent intent = (HostToHostIntent) createWithResourceGroup();
        assertThat("incorrect app id", intent.appId(), is(APPID));
        assertThat("incorrect host one", intent.one(), is(id1));
        assertThat("incorrect host two", intent.two(), is(id3));
        assertThat("incorrect selector", intent.selector(), is(selector));
        assertThat("incorrect treatment", intent.treatment(), is(treatment));
        assertThat("incorrect resource group", intent.resourceGroup(), is(resourceGrouop));

    }

    @Override
    protected Intent createOne() {
        return HostToHostIntent.builder()
                .appId(APPID)
                .one(id1)
                .two(id2)
                .selector(selector)
                .treatment(treatment)
                .build();
    }

    @Override
    protected Intent createAnother() {
        return HostToHostIntent.builder()
                .appId(APPID)
                .one(id1)
                .two(id3)
                .selector(selector)
                .treatment(treatment)
                .build();
    }

    protected Intent createWithResourceGroup() {
        return HostToHostIntent.builder()
                .appId(APPID)
                .one(id1)
                .two(id3)
                .selector(selector)
                .treatment(treatment)
                .resourceGroup(resourceGrouop)
                .build();
    }
}
