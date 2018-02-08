/*
 * Copyright 2014-present Open Networking Foundation
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
package org.onosproject.net.intent.constraint;

import com.google.common.testing.EqualsTester;
import org.junit.Before;
import org.junit.Test;
import org.onlab.graph.ScalarWeight;
import org.onosproject.net.DefaultLink;
import org.onosproject.net.DefaultPath;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Path;
import org.onosproject.net.PortNumber;
import org.onosproject.net.intent.Constraint;
import org.onosproject.net.intent.ResourceContext;
import org.onosproject.net.provider.ProviderId;

import java.util.Arrays;

import static org.easymock.EasyMock.createMock;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.onosproject.net.DefaultLinkTest.cp;
import static org.onosproject.net.DeviceId.deviceId;
import static org.onosproject.net.Link.Type.DIRECT;

/**
 * Test for constraint of intermediate elements.
 */
public class WaypointConstraintTest {

    private static final DeviceId DID1 = deviceId("of:1");
    private static final DeviceId DID2 = deviceId("of:2");
    private static final DeviceId DID3 = deviceId("of:3");
    private static final DeviceId DID4 = deviceId("of:4");
    private static final PortNumber PN1 = PortNumber.portNumber(1);
    private static final PortNumber PN2 = PortNumber.portNumber(2);
    private static final PortNumber PN3 = PortNumber.portNumber(3);
    private static final PortNumber PN4 = PortNumber.portNumber(4);
    private static final ProviderId PROVIDER_ID = new ProviderId("of", "foo");

    private WaypointConstraint sut;
    private ResourceContext resourceContext;

    private Path path;
    private DefaultLink link2;
    private DefaultLink link1;

    @Before
    public void setUp() {
        resourceContext = createMock(ResourceContext.class);

        link1 = DefaultLink.builder()
                .providerId(PROVIDER_ID)
                .src(cp(DID1, PN1))
                .dst(cp(DID2, PN2))
                .type(DIRECT)
                .build();
        link2 = DefaultLink.builder()
                .providerId(PROVIDER_ID)
                .src(cp(DID2, PN3))
                .dst(cp(DID3, PN4))
                .type(DIRECT)
                .build();

        path = new DefaultPath(PROVIDER_ID, Arrays.asList(link1, link2), ScalarWeight.toWeight(10));
    }

    /**
     * Tests that all of the specified waypoints are included in the specified path in order.
     */
    @Test
    public void testSatisfyWaypoints() {
        sut = new WaypointConstraint(DID1, DID2, DID3);

        assertThat(sut.validate(path, resourceContext), is(true));
    }

    /**
     * Tests that the specified path does not includes the specified waypoint.
     */
    @Test
    public void testNotSatisfyWaypoint() {
        sut = new WaypointConstraint(DID4);

        assertThat(sut.validate(path, resourceContext), is(false));
    }

    @Test
    public void testEquality() {
        Constraint c1 = new WaypointConstraint(DID1, DID2);
        Constraint c2 = new WaypointConstraint(DID1, DID2);

        Constraint c3 = new WaypointConstraint(DID2);
        Constraint c4 = new WaypointConstraint(DID3);

        new EqualsTester()
                .addEqualityGroup(c1, c2)
                .addEqualityGroup(c3)
                .addEqualityGroup(c4)
                .testEquals();
    }
}
