/*
 * Copyright 2015-present Open Networking Foundation
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

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.flow.TrafficTreatment;

import com.google.common.testing.EqualsTester;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;
import static org.onosproject.net.NetTestTools.APP_ID;
import static org.onosproject.net.NetTestTools.connectPoint;
/**
 * Unit tests for the TwoWayP2PIntent class.
 */
public class TwoWayP2PIntentTest extends AbstractIntentTest {

    TrafficSelector selector;
    TrafficTreatment treatment;

    TwoWayP2PIntent intent1;
    TwoWayP2PIntent intent2;

    static final int PRIORITY = 12;

    @Before
    public void twoWatP2PIntentTestSetUp() {
        selector = new IntentTestsMocks.MockSelector();
        treatment = new IntentTestsMocks.MockTreatment();

        intent1 = TwoWayP2PIntent.builder()
                .appId(APP_ID)
                .priority(PRIORITY)
                .selector(selector)
                .treatment(treatment)
                .one(connectPoint("one", 1))
                .two(connectPoint("two", 2))
                .build();

        intent2 = TwoWayP2PIntent.builder()
                .appId(APP_ID)
                .priority(PRIORITY)
                .selector(selector)
                .treatment(treatment)
                .one(connectPoint("two", 2))
                .two(connectPoint("three", 2))
                .build();
    }

    /**
     * Checks that the TwoWayP2PIntent class is immutable.
     */
    @Test
    public void testImmutability() {
        assertThatClassIsImmutable(TwoWayP2PIntent.class);
    }

    /**
     * Checks the operation of equals(), hashCode() and toString() methods.
     */
    @Test
    public void testEquals() {
        new EqualsTester()
                .addEqualityGroup(intent1)
                .addEqualityGroup(intent2)
                .testEquals();
    }

    /**
     * Checks that the optical path ntent objects are created correctly.
     */
    @Test
    public void testContents() {
        assertThat(intent1.appId(), equalTo(APP_ID));
        assertThat(intent1.one(), Matchers.equalTo(connectPoint("one", 1)));
        assertThat(intent1.two(), Matchers.equalTo(connectPoint("two", 2)));
        assertThat(intent1.priority(), is(PRIORITY));
        assertThat(intent1.selector(), is(selector));
        assertThat(intent1.treatment(), is(treatment));
    }
}
