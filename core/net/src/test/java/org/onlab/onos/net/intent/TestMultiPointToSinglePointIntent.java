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

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.onlab.onos.core.ApplicationId;
import org.onlab.onos.TestApplicationId;
import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.flow.TrafficSelector;
import org.onlab.onos.net.flow.TrafficTreatment;

import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;
import static org.onlab.onos.net.NetTestTools.connectPoint;

/**
 * Unit tests for the MultiPointToSinglePointIntent class.
 */
public class TestMultiPointToSinglePointIntent {

    private static final ApplicationId APPID = new TestApplicationId("foo");

    private ConnectPoint point1 = connectPoint("dev1", 1);
    private ConnectPoint point2 = connectPoint("dev2", 1);
    private ConnectPoint point3 = connectPoint("dev3", 1);

    private TrafficSelector selector = new IntentTestsMocks.MockSelector();
    private TrafficTreatment treatment = new IntentTestsMocks.MockTreatment();

    Set<ConnectPoint> ingress1;
    Set<ConnectPoint> ingress2;

    /**
     * Creates a MultiPointToSinglePointIntent object.
     *
     * @param ingress set of ingress points
     * @param egress  egress point
     * @return MultiPointToSinglePoint intent
     */
    private MultiPointToSinglePointIntent makeIntent(Set<ConnectPoint> ingress,
                                                     ConnectPoint egress) {
        return new MultiPointToSinglePointIntent(APPID, selector, treatment,
                                                 ingress, egress);
    }

    /**
     * Initializes the ingress sets.
     */
    @Before
    public void setup() {
        ingress1 = new HashSet<>();
        ingress2 = new HashSet<>();
    }

    /**
     * Tests the equals() method where two MultiPointToSinglePoint have references
     * to the same Links in different orders. These should compare equal.
     */
    @Test @Ignore("Needs to be merged with other API test")
    public void testSameEquals() {

        Set<ConnectPoint> ingress1 = new HashSet<>();
        ingress1.add(point2);
        ingress1.add(point3);

        Set<ConnectPoint> ingress2 = new HashSet<>();
        ingress2.add(point3);
        ingress2.add(point2);

        Intent i1 = makeIntent(ingress1, point1);
        Intent i2 = makeIntent(ingress2, point1);

        assertThat(i1, is(equalTo(i2)));
    }

    /**
     * Tests the equals() method where two MultiPointToSinglePoint have references
     * to different Links. These should compare not equal.
     */
    @Test @Ignore("Needs to be merged with other API test")
    public void testLinksDifferentEquals() {
        ingress1.add(point3);

        ingress2.add(point3);
        ingress2.add(point2);

        Intent i1 = makeIntent(ingress1, point1);
        Intent i2 = makeIntent(ingress2, point1);

        assertThat(i1, is(not(equalTo(i2))));
    }

    /**
     * Tests that the hashCode() values for two equivalent MultiPointToSinglePoint
     * objects are the same.
     */
    @Test @Ignore("Needs to be merged with other API test")
    public void testHashCodeEquals() {
        ingress1.add(point2);
        ingress1.add(point3);

        ingress2.add(point3);
        ingress2.add(point2);

        Intent i1 = makeIntent(ingress1, point1);
        Intent i2 = makeIntent(ingress2, point1);

        assertThat(i1.hashCode(), is(equalTo(i2.hashCode())));
    }

    /**
     * Tests that the hashCode() values for two distinct MultiPointToSinglePoint
     * objects are different.
     */
    @Test @Ignore("Needs to be merged with other API test")
    public void testHashCodeDifferent() {
        ingress1.add(point2);

        ingress2.add(point3);
        ingress2.add(point2);

        Intent i1 = makeIntent(ingress1, point1);
        Intent i2 = makeIntent(ingress2, point1);


        assertThat(i1.hashCode(), is(not(equalTo(i2.hashCode()))));
    }

    /**
     * Checks that the MultiPointToSinglePointIntent class is immutable.
     */
    @Test
    public void checkImmutability() {
        assertThatClassIsImmutable(MultiPointToSinglePointIntent.class);
    }
}
