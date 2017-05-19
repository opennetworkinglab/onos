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
package org.onosproject.net.intent.constraint;

/**
 * Test for constraint of intermediate nodes not passed.
 */
import com.google.common.collect.ImmutableSet;
import com.google.common.testing.EqualsTester;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DefaultHost;
import org.onosproject.net.DefaultLink;
import org.onosproject.net.DefaultPath;
import org.onosproject.net.DeviceId;
import org.onosproject.net.HostId;
import org.onosproject.net.HostLocation;
import org.onosproject.net.Path;
import org.onosproject.net.PortNumber;
import org.onosproject.net.intent.ResourceContext;
import org.onosproject.net.provider.ProviderId;

import java.util.Arrays;

import static org.easymock.EasyMock.createMock;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.onosproject.net.DefaultEdgeLink.createEdgeLink;
import static org.onosproject.net.DefaultLinkTest.cp;
import static org.onosproject.net.DeviceId.deviceId;
import static org.onosproject.net.Link.Type.DIRECT;

public class ObstacleConstraintTest {

    private static final DeviceId DID1 = deviceId("of:1");
    private static final DeviceId DID2 = deviceId("of:2");
    private static final DeviceId DID3 = deviceId("of:3");
    private static final DeviceId DID4 = deviceId("of:4");
    private static final DeviceId DID5 = deviceId("of:5");
    private static final DeviceId DID6 = deviceId("of:6");
    private static final PortNumber PN1 = PortNumber.portNumber(1);
    private static final PortNumber PN2 = PortNumber.portNumber(2);
    private static final PortNumber PN3 = PortNumber.portNumber(3);
    private static final PortNumber PN4 = PortNumber.portNumber(4);
    private static final ProviderId PROVIDER_ID = new ProviderId("of", "foo");

    private ResourceContext resourceContext;

    private Path path;
    private Path pathWithEdgeLink;
    private DefaultLink link1;
    private DefaultLink link2;
    private DefaultLink edgelink1;
    private DefaultLink edgelink2;
    private DefaultHost host1;
    private DefaultHost host2;

    private ObstacleConstraint sut;

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
        host1 = new DefaultHost(PROVIDER_ID, HostId.hostId("00:00:00:00:00:01/None"),
                MacAddress.valueOf(0), VlanId.vlanId(),
                new HostLocation(DID5, PN1, 1),
                ImmutableSet.of(), DefaultAnnotations.EMPTY);
        host2 = new DefaultHost(PROVIDER_ID, HostId.hostId("00:00:00:00:00:02/None"),
                MacAddress.valueOf(0), VlanId.vlanId(),
                new HostLocation(DID6, PN1, 1),
                ImmutableSet.of(), DefaultAnnotations.EMPTY);
        edgelink1 = createEdgeLink(host1, true);
        edgelink2 = createEdgeLink(host2, false);

        path = new DefaultPath(PROVIDER_ID, Arrays.asList(link1, link2), 10);
        pathWithEdgeLink = new DefaultPath(PROVIDER_ID,
                Arrays.asList(edgelink1, link1, link2, edgelink2), 10);
    }

    @Test
    public void testEquality() {
        ObstacleConstraint o1 = new ObstacleConstraint(DID1, DID2, DID3);
        ObstacleConstraint o2 = new ObstacleConstraint(DID3, DID2, DID1);
        ObstacleConstraint o3 = new ObstacleConstraint(DID1, DID2);
        ObstacleConstraint o4 = new ObstacleConstraint(DID2, DID1);

        new EqualsTester()
                .addEqualityGroup(o1, o2)
                .addEqualityGroup(o3, o4)
                .testEquals();
    }

    /**
     * Tests the specified path avoids the specified obstacle.
     */
    @Test
    public void testPathNotThroughObstacles() {
        sut = new ObstacleConstraint(DID4);

        assertThat(sut.validate(path, resourceContext), is(true));
    }

    /**
     * Test the specified path does not avoid the specified obstacle.
     */
    @Test
    public void testPathThroughObstacle() {
        sut = new ObstacleConstraint(DID1);

        assertThat(sut.validate(path, resourceContext), is(false));
    }

    /**
     * Test the specified path does not avoid the specified obstacle including edge links.
     */
    @Test
    public void testPathThroughObstacleWithEdgeLink() {
        sut = new ObstacleConstraint(DID1);

        assertThat(sut.validate(pathWithEdgeLink, resourceContext), is(false));
    }
}
