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

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.onlab.junit.ImmutableClassChecker.assertThatClassIsImmutable;
import static org.onlab.onos.net.NetTestTools.link;

import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.onlab.onos.core.ApplicationId;
import org.onlab.onos.TestApplicationId;
import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.Link;
import org.onlab.onos.net.PortNumber;
import org.onlab.onos.net.flow.TrafficSelector;
import org.onlab.onos.net.flow.TrafficTreatment;

/**
 * Unit tests for the LinkCollectionIntent class.
 */
public class TestLinkCollectionIntent {

    private static final ApplicationId APPID = new TestApplicationId("foo");

    private Link link1 = link("dev1", 1, "dev2", 2);
    private Link link2 = link("dev1", 1, "dev3", 2);
    private Link link3 = link("dev2", 1, "dev3", 2);

    private Set<Link> links1;
    private Set<Link> links2;

    private ConnectPoint egress1 = new ConnectPoint(DeviceId.deviceId("dev1"),
            PortNumber.portNumber(3));
    private ConnectPoint egress2 = new ConnectPoint(DeviceId.deviceId("dev2"),
            PortNumber.portNumber(3));

    private TrafficSelector selector = new IntentTestsMocks.MockSelector();
    private TrafficTreatment treatment = new IntentTestsMocks.MockTreatment();

    private LinkCollectionIntent makeLinkCollection(Set<Link> links,
            ConnectPoint egress) {
        return new LinkCollectionIntent(APPID, selector, treatment, links, egress);
    }

    @Before
    public void setup() {
        links1 = new HashSet<>();
        links2 = new HashSet<>();
    }

    /**
     * Tests the equals() method where two LinkCollectionIntents have references
     * to the same Links in different orders. These should compare equal.
     */
    @Test @Ignore("Needs to be merged with other API test")
    public void testSameEquals() {
        links1.add(link1);
        links1.add(link2);
        links1.add(link3);

        links2.add(link3);
        links2.add(link2);
        links2.add(link1);

        LinkCollectionIntent i1 = makeLinkCollection(links1, egress1);
        LinkCollectionIntent i2 = makeLinkCollection(links2, egress1);

        assertThat(i1, is(equalTo(i2)));
    }

    /**
     * Tests the equals() method where two LinkCollectionIntents have references
     * to different Links. These should compare not equal.
     */
    @Test @Ignore("Needs to be merged with other API test")
    public void testLinksDifferentEquals() {
        links1.add(link1);
        links1.add(link2);

        links2.add(link3);
        links2.add(link1);

        LinkCollectionIntent i1 = makeLinkCollection(links1, egress1);
        LinkCollectionIntent i2 = makeLinkCollection(links2, egress1);

        assertThat(i1, is(not(equalTo(i2))));
    }

    /**
     * Tests the equals() method where two LinkCollectionIntents have references
     * to the same Links but different egress points. These should compare not equal.
     */
    @Test @Ignore("Needs to be merged with other API test")
    public void testEgressDifferentEquals() {
        links1.add(link1);
        links1.add(link2);
        links1.add(link3);

        links2.add(link3);
        links2.add(link2);
        links2.add(link1);

        LinkCollectionIntent i1 = makeLinkCollection(links1, egress1);
        LinkCollectionIntent i2 = makeLinkCollection(links2, egress2);

        assertThat(i1, is(not(equalTo(i2))));
    }

    /**
     * Tests that the hashCode() values for two equivalent LinkCollectionIntent
     * objects are the same.
     */
    @Test @Ignore("Needs to be merged with other API test")
    public void testHashCodeEquals() {
        links1.add(link1);
        links1.add(link2);
        links1.add(link3);

        links2.add(link3);
        links2.add(link2);
        links2.add(link1);

        LinkCollectionIntent i1 = makeLinkCollection(links1, egress1);
        LinkCollectionIntent i2 = makeLinkCollection(links2, egress1);

        assertThat(i1.hashCode(), is(equalTo(i2.hashCode())));
    }

    /**
     * Tests that the hashCode() values for two distinct LinkCollectionIntent
     * objects are different.
     */
    @Test @Ignore("Needs to be merged with other API test")
    public void testHashCodeDifferent() {
        links1.add(link1);
        links1.add(link2);

        links2.add(link1);
        links2.add(link3);

        LinkCollectionIntent i1 = makeLinkCollection(links1, egress1);
        LinkCollectionIntent i2 = makeLinkCollection(links2, egress2);

        assertThat(i1.hashCode(), is(not(equalTo(i2.hashCode()))));
    }

    /**
     * Checks that the HostToHostIntent class is immutable.
     */
    @Test
    public void checkImmutability() {
        assertThatClassIsImmutable(LinkCollectionIntent.class);
    }
}
