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
package org.onlab.onos.net.intent;

import org.junit.Ignore;
import org.junit.Test;
import org.onlab.onos.core.ApplicationId;
import org.onlab.onos.TestApplicationId;
import org.onlab.onos.net.HostId;
import org.onlab.onos.net.flow.TrafficSelector;
import org.onlab.onos.net.flow.TrafficTreatment;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;
import static org.onlab.onos.net.NetTestTools.hid;

/**
 * Unit tests for the HostToHostIntent class.
 */
public class TestHostToHostIntent {

    private static final ApplicationId APPID = new TestApplicationId("foo");

    private TrafficSelector selector = new IntentTestsMocks.MockSelector();
    private TrafficTreatment treatment = new IntentTestsMocks.MockTreatment();

    private HostToHostIntent makeHostToHost(HostId one, HostId two) {
        return new HostToHostIntent(APPID, one, two, selector, treatment);
    }

    /**
     * Tests the equals() method where two HostToHostIntents have references
     * to the same hosts. These should compare equal.
     */
    @Test @Ignore("Needs to be merged with other API test")
    public void testSameEquals() {

        HostId one = hid("00:00:00:00:00:01/-1");
        HostId two = hid("00:00:00:00:00:02/-1");
        HostToHostIntent i1 = makeHostToHost(one, two);
        HostToHostIntent i2 = makeHostToHost(one, two);

        assertThat(i1, is(equalTo(i2)));
    }

    /**
     * Tests the equals() method where two HostToHostIntents have references
     * to different Hosts. These should compare not equal.
     */
    @Test @Ignore("Needs to be merged with other API test")
    public void testSameEquals2() {
        HostId one = hid("00:00:00:00:00:01/-1");
        HostId two = hid("00:00:00:00:00:02/-1");
        HostToHostIntent i1 = makeHostToHost(one, two);
        HostToHostIntent i2 = makeHostToHost(two, one);

        assertThat(i1, is(equalTo(i2)));
    }

    /**
     * Tests that the hashCode() values for two equivalent HostToHostIntent
     * objects are the same.
     */
    @Test @Ignore("Needs to be merged with other API test")
    public void testHashCodeEquals() {
        HostId one = hid("00:00:00:00:00:01/-1");
        HostId two = hid("00:00:00:00:00:02/-1");
        HostToHostIntent i1 = makeHostToHost(one, two);
        HostToHostIntent i2 = makeHostToHost(one, two);

        assertThat(i1.hashCode(), is(equalTo(i2.hashCode())));
    }

    /**
     * Tests that the hashCode() values for two distinct LinkCollectionIntent
     * objects are different.
     */
    @Test @Ignore("Needs to be merged with other API test")
    public void testHashCodeEquals2() {
        HostId one = hid("00:00:00:00:00:01/-1");
        HostId two = hid("00:00:00:00:00:02/-1");
        HostToHostIntent i1 = makeHostToHost(one, two);
        HostToHostIntent i2 = makeHostToHost(two, one);

        assertThat(i1.hashCode(), is(equalTo(i2.hashCode())));
    }

    /**
     * Tests that the hashCode() values for two distinct LinkCollectionIntent
     * objects are different.
     */
    @Test @Ignore("Needs to be merged with other API test")
    public void testHashCodeDifferent() {
        HostId one = hid("00:00:00:00:00:01/-1");
        HostId two = hid("00:00:00:00:00:02/-1");
        HostId three = hid("00:00:00:00:00:32/-1");
        HostToHostIntent i1 = makeHostToHost(one, two);
        HostToHostIntent i2 = makeHostToHost(one, three);

        assertThat(i1.hashCode(), is(not(equalTo(i2.hashCode()))));
    }

    /**
     * Checks that the HostToHostIntent class is immutable.
     */
    @Test
    public void checkImmutability() {
        assertThatClassIsImmutable(HostToHostIntent.class);
    }
}
