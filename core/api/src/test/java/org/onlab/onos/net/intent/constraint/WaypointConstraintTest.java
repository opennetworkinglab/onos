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
package org.onlab.onos.net.intent.constraint;

import org.junit.Before;
import org.junit.Test;
import org.onlab.onos.net.DefaultLink;
import org.onlab.onos.net.DefaultPath;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.Path;
import org.onlab.onos.net.PortNumber;
import org.onlab.onos.net.provider.ProviderId;
import org.onlab.onos.net.resource.LinkResourceService;

import java.util.Arrays;

import static org.easymock.EasyMock.createMock;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.onlab.onos.net.DefaultLinkTest.cp;
import static org.onlab.onos.net.DeviceId.deviceId;
import static org.onlab.onos.net.Link.Type.DIRECT;

/**
 * Test for constraint of intermediate elements.
 */
public class WaypointConstraintTest {

    public static final DeviceId DID1 = deviceId("of:1");
    public static final DeviceId DID2 = deviceId("of:2");
    public static final DeviceId DID3 = deviceId("of:3");
    public static final DeviceId DID4 = deviceId("of:4");
    public static final PortNumber PN1 = PortNumber.portNumber(1);
    public static final PortNumber PN2 = PortNumber.portNumber(2);
    public static final PortNumber PN3 = PortNumber.portNumber(3);
    public static final PortNumber PN4 = PortNumber.portNumber(4);
    public static final ProviderId PROVIDER_ID = new ProviderId("of", "foo");

    private WaypointConstraint sut;
    private LinkResourceService linkResourceService;

    private Path path;
    private DefaultLink link2;
    private DefaultLink link1;

    @Before
    public void setUp() {
        linkResourceService = createMock(LinkResourceService.class);

        link1 = new DefaultLink(PROVIDER_ID, cp(DID1, PN1), cp(DID2, PN2), DIRECT);
        link2 = new DefaultLink(PROVIDER_ID, cp(DID2, PN3), cp(DID3, PN4), DIRECT);
        path = new DefaultPath(PROVIDER_ID, Arrays.asList(link1, link2), 10);
    }

    /**
     * Tests that all of the specified waypoints are included in the specified path in order.
     */
    @Test
    public void testSatisfyWaypoints() {
        sut = new WaypointConstraint(DID1, DID2, DID3);

        assertThat(sut.validate(path, linkResourceService), is(true));
    }

    /**
     * Tests that the specified path does not includes the specified waypoint.
     */
    @Test
    public void testNotSatisfyWaypoint() {
        sut = new WaypointConstraint(DID4);

        assertThat(sut.validate(path, linkResourceService), is(false));
    }
}
