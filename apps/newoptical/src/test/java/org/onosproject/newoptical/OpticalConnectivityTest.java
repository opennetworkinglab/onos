/*
 * Copyright 2016-present Open Networking Foundation
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

package org.onosproject.newoptical;

import com.google.common.collect.ImmutableSet;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.graph.Weight;
import org.onlab.util.Bandwidth;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.core.IdGenerator;
import org.onosproject.net.CltSignalType;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DefaultLink;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.OduSignalType;
import org.onosproject.net.Path;
import org.onosproject.net.PortNumber;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.OpticalCircuitIntent;
import org.onosproject.net.intent.OpticalConnectivityIntent;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.newoptical.api.OpticalConnectivityId;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * Test class for OpticalConnectivity.
 */
public class OpticalConnectivityTest {

    private final ApplicationId appId = new DefaultApplicationId(0, "PacketLinkRealizedByOpticalTest");
    private ProviderId providerId = new ProviderId("of", "foo");
    private IdGenerator idGenerator;

    @Before
    public void setUp() {
        idGenerator = new IdGenerator() {
            int counter = 1;

            @Override
            public long getNewId() {
                return counter++;
            }
        };

        Intent.unbindIdGenerator(idGenerator);
        Intent.bindIdGenerator(idGenerator);
    }

    @After
    public void tearDown() {
        Intent.unbindIdGenerator(idGenerator);
    }

    /**
     * Checks the construction of OpticalConnectivity object.
     */
    @Test
    public void testCreate() {
        Bandwidth bandwidth = Bandwidth.bps(100);
        Duration latency = Duration.ofMillis(10);

        // Mock 3-nodes linear topology
        ConnectPoint cp12 = createConnectPoint(1, 2);
        ConnectPoint cp21 = createConnectPoint(2, 1);
        ConnectPoint cp22 = createConnectPoint(2, 2);
        ConnectPoint cp31 = createConnectPoint(3, 1);

        Link link1 = createLink(cp12, cp21);
        Link link2 = createLink(cp22, cp31);
        List<Link> links = Stream.of(link1, link2).collect(Collectors.toList());

        OpticalConnectivityId cid = OpticalConnectivityId.of(1L);
        OpticalConnectivity oc = new OpticalConnectivity(cid, links, bandwidth, latency,
                Collections.emptySet(), Collections.emptySet());

        assertNotNull(oc);
        assertEquals(oc.id(), cid);
        assertEquals(oc.links(), links);
        assertEquals(oc.bandwidth(), bandwidth);
        assertEquals(oc.latency(), latency);
    }

    /**
     * Checks that isAllRealizingLink(Not)Established works for OpticalConnectivityIntent.
     */
    @Test
    public void testLinkEstablishedByConnectivityIntent() {
        // Mock 7-nodes linear topology
        ConnectPoint cp12 = createConnectPoint(1, 2);
        ConnectPoint cp21 = createConnectPoint(2, 1);
        ConnectPoint cp22 = createConnectPoint(2, 2);
        ConnectPoint cp31 = createConnectPoint(3, 1);
        ConnectPoint cp32 = createConnectPoint(3, 2);
        ConnectPoint cp41 = createConnectPoint(4, 1);
        ConnectPoint cp42 = createConnectPoint(4, 2);
        ConnectPoint cp51 = createConnectPoint(5, 1);
        ConnectPoint cp52 = createConnectPoint(5, 2);
        ConnectPoint cp61 = createConnectPoint(6, 1);
        ConnectPoint cp62 = createConnectPoint(6, 2);
        ConnectPoint cp71 = createConnectPoint(7, 1);

        Link link1 = createLink(cp12, cp21);
        Link link2 = createLink(cp22, cp31);
        Link link3 = createLink(cp32, cp41);
        Link link4 = createLink(cp42, cp51);
        Link link5 = createLink(cp52, cp61);
        Link link6 = createLink(cp62, cp71);
        List<Link> links = Stream.of(link1, link2, link3, link4, link5, link6).collect(Collectors.toList());

        // Mocks 2 intents to create OduCtl connectivity
        OpticalConnectivityIntent connIntent1 = createConnectivityIntent(cp21, cp32);
        PacketLinkRealizedByOptical oduLink1 = PacketLinkRealizedByOptical.create(cp12, cp41,
                connIntent1);

        OpticalConnectivityIntent connIntent2 = createConnectivityIntent(cp51, cp62);
        PacketLinkRealizedByOptical oduLink2 = PacketLinkRealizedByOptical.create(cp42, cp71,
                connIntent2);

        Set<PacketLinkRealizedByOptical> plinks = ImmutableSet.of(oduLink1, oduLink2);

        Bandwidth bandwidth = Bandwidth.bps(100);
        Duration latency = Duration.ofMillis(10);

        OpticalConnectivityId cid = OpticalConnectivityId.of(1L);
        OpticalConnectivity oc1 = new OpticalConnectivity(cid, links, bandwidth, latency,
                plinks, Collections.emptySet());

        assertTrue(oc1.isAllRealizingLinkNotEstablished());
        assertFalse(oc1.isAllRealizingLinkEstablished());

        // Sets link realized by connIntent1 to be established
        OpticalConnectivity oc2 = oc1.setLinkEstablished(cp12, cp41, true);

        assertFalse(oc2.isAllRealizingLinkNotEstablished());
        assertFalse(oc2.isAllRealizingLinkEstablished());

        // Sets link realized by connIntent2 to be established
        OpticalConnectivity oc3 = oc2.setLinkEstablished(cp42, cp71, true);

        assertFalse(oc3.isAllRealizingLinkNotEstablished());
        assertTrue(oc3.isAllRealizingLinkEstablished());
    }

    /**
     * Checks that isAllRealizingLink(Not)Established works for OpticalCircuitIntent.
     */
    @Test
    public void testLinkEstablishedByCircuitIntent() {
        // Mock 7-nodes linear topology
        ConnectPoint cp12 = createConnectPoint(1, 2);
        ConnectPoint cp21 = createConnectPoint(2, 1);
        ConnectPoint cp22 = createConnectPoint(2, 2);
        ConnectPoint cp31 = createConnectPoint(3, 1);
        ConnectPoint cp32 = createConnectPoint(3, 2);
        ConnectPoint cp41 = createConnectPoint(4, 1);
        ConnectPoint cp42 = createConnectPoint(4, 2);
        ConnectPoint cp51 = createConnectPoint(5, 1);
        ConnectPoint cp52 = createConnectPoint(5, 2);
        ConnectPoint cp61 = createConnectPoint(6, 1);
        ConnectPoint cp62 = createConnectPoint(6, 2);
        ConnectPoint cp71 = createConnectPoint(7, 1);

        Link link1 = createLink(cp12, cp21);
        Link link2 = createLink(cp22, cp31);
        Link link3 = createLink(cp32, cp41);
        Link link4 = createLink(cp42, cp51);
        Link link5 = createLink(cp52, cp61);
        Link link6 = createLink(cp62, cp71);
        List<Link> links = Stream.of(link1, link2, link3, link4, link5, link6).collect(Collectors.toList());

        // Mocks 2 intents to create Och connectivity
        OpticalCircuitIntent circuitIntent1 = createCircuitIntent(cp21, cp32);
        PacketLinkRealizedByOptical ochLink1 = PacketLinkRealizedByOptical.create(cp12, cp41,
                circuitIntent1);

        OpticalCircuitIntent circuitIntent2 = createCircuitIntent(cp51, cp62);
        PacketLinkRealizedByOptical ochLink2 = PacketLinkRealizedByOptical.create(cp42, cp71,
                circuitIntent2);

        Set<PacketLinkRealizedByOptical> plinks = ImmutableSet.of(ochLink1, ochLink2);

        Bandwidth bandwidth = Bandwidth.bps(100);
        Duration latency = Duration.ofMillis(10);

        OpticalConnectivityId cid = OpticalConnectivityId.of(1L);
        OpticalConnectivity oc1 = new OpticalConnectivity(cid, links, bandwidth, latency,
                plinks, Collections.emptySet());

        assertTrue(oc1.isAllRealizingLinkNotEstablished());
        assertFalse(oc1.isAllRealizingLinkEstablished());

        // Sets link realized by circuitIntent1 to be established
        OpticalConnectivity oc2 = oc1.setLinkEstablished(cp12, cp41, true);

        assertFalse(oc2.isAllRealizingLinkNotEstablished());
        assertFalse(oc2.isAllRealizingLinkEstablished());

        // Sets link realized by circuitIntent2 to be established
        OpticalConnectivity oc3 = oc2.setLinkEstablished(cp42, cp71, true);

        assertFalse(oc3.isAllRealizingLinkNotEstablished());
        assertTrue(oc3.isAllRealizingLinkEstablished());
    }

    private ConnectPoint createConnectPoint(long devIdNum, long portIdNum) {
        return new ConnectPoint(
                DeviceId.deviceId(String.format("of:%016d", devIdNum)),
                PortNumber.portNumber(portIdNum));
    }

    private Link createLink(ConnectPoint src, ConnectPoint dst) {
        return DefaultLink.builder()
                .providerId(providerId)
                .src(src)
                .dst(dst)
                .type(Link.Type.DIRECT)
                .annotations(DefaultAnnotations.EMPTY)
                .build();
    }

    private OpticalCircuitIntent createCircuitIntent(ConnectPoint src, ConnectPoint dst) {
        OpticalCircuitIntent intent = OpticalCircuitIntent.builder()
                .appId(appId)
                .bidirectional(true)
                .src(src)
                .dst(dst)
                .signalType(CltSignalType.CLT_100GBE)
                .build();

        return intent;
    }

    private OpticalConnectivityIntent createConnectivityIntent(ConnectPoint src, ConnectPoint dst) {
        OpticalConnectivityIntent intent = OpticalConnectivityIntent.builder()
                .appId(appId)
                .bidirectional(true)
                .src(src)
                .dst(dst)
                .signalType(OduSignalType.ODU4)
                .build();

        return intent;
    }

    private class MockPath extends DefaultLink implements Path {
        List<Link> links;

        protected MockPath(ConnectPoint src, ConnectPoint dst, List<Link> links) {
            super(providerId, src, dst, Type.INDIRECT, State.ACTIVE);
            this.links = links;
        }

        @Override
        public List<Link> links() {
            return links;
        }

        @Override
        public double cost() {
            return 0;
        }

        @Override
        public Weight weight() {
            return null;
        }
    }
}