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
import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.flow.TrafficSelector;
import org.onlab.onos.net.flow.TrafficTreatment;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.onlab.onos.net.NetTestTools.connectPoint;

/**
 * Unit tests for the HostToHostIntent class.
 */
public class TestPointToPointIntent {

    private static final ApplicationId APPID = new TestApplicationId("foo");

    private TrafficSelector selector = new IntentTestsMocks.MockSelector();
    private TrafficTreatment treatment = new IntentTestsMocks.MockTreatment();

    private ConnectPoint point1 = connectPoint("dev1", 1);
    private ConnectPoint point2 = connectPoint("dev2", 1);

    private PointToPointIntent makePointToPoint(ConnectPoint ingress,
                                                ConnectPoint egress) {
        return new PointToPointIntent(APPID, selector, treatment, ingress, egress);
    }

    /**
     * Tests the equals() method where two PointToPointIntents have references
     * to the same ingress and egress points. These should compare equal.
     */
    @Test @Ignore("Needs to be merged with other API test")
    public void testSameEquals() {
        PointToPointIntent i1 = makePointToPoint(point1, point2);
        PointToPointIntent i2 = makePointToPoint(point1, point2);

        assertThat(i1, is(equalTo(i2)));
    }

    /**
     * Tests the equals() method where two HostToHostIntents have references
     * to different Hosts. These should compare not equal.
     */
    @Test @Ignore("Needs to be merged with other API test")
    public void testLinksDifferentEquals() {
        PointToPointIntent i1 = makePointToPoint(point1, point2);
        PointToPointIntent i2 = makePointToPoint(point2, point1);

        assertThat(i1, is(not(equalTo(i2))));
    }

    /**
     * Tests that the hashCode() values for two equivalent HostToHostIntent
     * objects are the same.
     */
    @Test @Ignore("Needs to be merged with other API test")
    public void testHashCodeEquals() {
        PointToPointIntent i1 = makePointToPoint(point1, point2);
        PointToPointIntent i2 = makePointToPoint(point1, point2);

        assertThat(i1.hashCode(), is(equalTo(i2.hashCode())));
    }

    /**
     * Tests that the hashCode() values for two distinct LinkCollectionIntent
     * objects are different.
     */
    @Test @Ignore("Needs to be merged with other API test")
    public void testHashCodeDifferent() {
        PointToPointIntent i1 = makePointToPoint(point1, point2);
        PointToPointIntent i2 = makePointToPoint(point2, point1);

        assertThat(i1.hashCode(), is(not(equalTo(i2.hashCode()))));
    }
}
