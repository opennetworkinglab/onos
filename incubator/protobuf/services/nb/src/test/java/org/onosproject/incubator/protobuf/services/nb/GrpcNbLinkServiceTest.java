/*
* Copyright 2017-present Open Networking Foundation
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

package org.onosproject.incubator.protobuf.services.nb;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.grpc.BindableService;
import io.grpc.ManagedChannel;
import io.grpc.inprocess.InProcessChannelBuilder;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.onosproject.grpc.nb.net.link.LinkServiceGrpc;
import org.onosproject.grpc.nb.net.link.LinkServiceGrpc.LinkServiceBlockingStub;
import org.onosproject.grpc.nb.net.link.LinkServiceNb;
import org.onosproject.grpc.net.models.LinkProtoOuterClass;
import org.onosproject.incubator.protobuf.models.net.ConnectPointProtoTranslator;
import org.onosproject.incubator.protobuf.models.net.LinkProtoTranslator;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultLink;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.PortNumber;
import org.onosproject.net.link.LinkService;
import org.onosproject.net.provider.ProviderId;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertTrue;
import static org.onosproject.net.DeviceId.deviceId;
import static org.onosproject.net.PortNumber.portNumber;

public class GrpcNbLinkServiceTest {
    private static InProcessServer<BindableService> inprocessServer;
    private static ManagedChannel channel;
    private static LinkServiceBlockingStub blockingStub;

    private static final String D1 = "d1";
    private static final String D2 = "d2";
    private static final String D3 = "d3";
    private static final String D4 = "d4";
    private static final String D5 = "d5";
    private static final String D6 = "d6";
    private static final String D7 = "d7";
    private static final String D8 = "d8";
    private static final String D9 = "d9";

    private static final String[][] LINK_CONNECT_DATA = {
            {D1, "12", D2, "21"},
            {D2, "23", D3, "32"},
            {D4, "41", D1, "14"},
            {D5, "51", D1, "15"},
            {D6, "61", D1, "16"},
            {D7, "73", D3, "37"},
            {D8, "83", D3, "38"},
            {D9, "93", D3, "39"},
    };

    private static final DeviceId DEVID_1 = deviceId(D1);
    private static final PortNumber PORT_14 = portNumber("14");

    private static final DeviceId DEVID_4 = deviceId(D4);
    private static final PortNumber PORT_41 = portNumber("41");

    private static final DeviceId DEVID_3 = deviceId(D3);
    private static final PortNumber PORT_32 = portNumber("32");

    private static final ConnectPoint DEVID_1_14 = new ConnectPoint(DEVID_1, PORT_14);
    private static final ConnectPoint DEVID_4_41 = new ConnectPoint(DEVID_4, PORT_41);

    private static final ProviderId PID = new ProviderId("Test", "Test");

    private static List<Link> allLinks = new ArrayList<Link>();

    private static final LinkService MOCK_LINK = new MockLinkService();

    /**
     * Creates a list of links.
     *
     */
    private static void populateLinks() {
        for (String[] linkPair : LINK_CONNECT_DATA) {
            allLinks.addAll(makeLinkPair(linkPair));
        }
    }

    /**
     * Synthesizes a pair of unidirectional links between two devices. The
     * string array should be of the form:
     * <pre>
     *     { "device-A-id", "device-A-port", "device-B-id", "device-B-port" }
     * </pre>
     *
     * @param linkPairData device ids and ports
     * @return pair of synthesized links
     */
    private static List<Link> makeLinkPair(String[] linkPairData) {
        DeviceId devA = deviceId(linkPairData[0]);
        PortNumber portA = portNumber(linkPairData[1]);

        DeviceId devB = deviceId(linkPairData[2]);
        PortNumber portB = portNumber(linkPairData[3]);

        Link linkA = DefaultLink.builder()
                .providerId(PID)
                .type(Link.Type.DIRECT)
                .src(new ConnectPoint(devA, portA))
                .dst(new ConnectPoint(devB, portB))
                .build();

        Link linkB = DefaultLink.builder()
                .providerId(PID)
                .type(Link.Type.DIRECT)
                .src(new ConnectPoint(devB, portB))
                .dst(new ConnectPoint(devA, portA))
                .build();

        return ImmutableList.of(linkA, linkB);
    }

    public GrpcNbLinkServiceTest() {
    }

    @Test
    public void testGetLinkCount() throws InterruptedException {
        LinkServiceNb.getLinkCountRequest request = LinkServiceNb.getLinkCountRequest.getDefaultInstance();
        LinkServiceNb.getLinkCountReply response;

        try {
            response = blockingStub.getLinkCount(request);
            int linkCount = response.getLinkCount();
            assertTrue(allLinks.size() == linkCount);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testGetLink() throws InterruptedException {
        LinkServiceNb.getLinkRequest request = LinkServiceNb.getLinkRequest.newBuilder()
                .setSrc(ConnectPointProtoTranslator.translate(DEVID_1_14))
                .setDst(ConnectPointProtoTranslator.translate(DEVID_4_41))
                .build();
        LinkServiceNb.getLinkReply response;

        try {
            response = blockingStub.getLink(request);
            LinkProtoOuterClass.LinkProto link = response.getLink();
            assertTrue(LinkProtoTranslator.translate(allLinks.get(5)).equals(link));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testGetLinks() throws InterruptedException {
        LinkServiceNb.getLinksRequest request = LinkServiceNb.getLinksRequest.newBuilder()
                .setConnectPoint(ConnectPointProtoTranslator.translate(DEVID_1_14))
                .build();
        LinkServiceNb.getLinksReply response;

        try {
            response = blockingStub.getLinks(request);

            Set<Link> actualLinks = new HashSet<Link>();
            for (LinkProtoOuterClass.LinkProto link : response.getLinkList()) {
                actualLinks.add(LinkProtoTranslator.translate(link));
            }
            Set<Link> expectedLinks = new HashSet<Link>();
            expectedLinks.add(allLinks.get(4));
            expectedLinks.add(allLinks.get(5));

            assertTrue(expectedLinks.equals(actualLinks));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testGetActiveLinks() throws InterruptedException {
        LinkServiceNb.getActiveLinksRequest request = LinkServiceNb.getActiveLinksRequest.getDefaultInstance();
        LinkServiceNb.getActiveLinksReply response;

        try {
            response = blockingStub.getActiveLinks(request);

            Set<Link> actualLinks = new HashSet<Link>();
            for (LinkProtoOuterClass.LinkProto link : response.getLinkList()) {
                actualLinks.add(LinkProtoTranslator.translate(link));
            }

            Set<Link> expectedLinks = new HashSet<Link>(allLinks);
            assertTrue((expectedLinks).equals(actualLinks));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testGetDeviceLinks() throws InterruptedException {
        LinkServiceNb.getDeviceLinksRequest request = LinkServiceNb.getDeviceLinksRequest.newBuilder()
                .setDeviceId(D1)
                .build();
        LinkServiceNb.getDeviceLinksReply response;

        try {
            response = blockingStub.getDeviceLinks(request);

            Set<Link> actualLinks = new HashSet<Link>();
            for (LinkProtoOuterClass.LinkProto link : response.getLinkList()) {
                actualLinks.add(LinkProtoTranslator.translate(link));
            }

            Set<Link> expectedLinks = new HashSet<Link>();
            expectedLinks.add(allLinks.get(4));
            expectedLinks.add(allLinks.get(5));
            expectedLinks.add(allLinks.get(6));
            expectedLinks.add(allLinks.get(7));
            expectedLinks.add(allLinks.get(8));
            expectedLinks.add(allLinks.get(9));
            expectedLinks.add(allLinks.get(0));
            expectedLinks.add(allLinks.get(1));

            assertTrue((expectedLinks).equals(actualLinks));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testGetDeviceEgressLinks() throws InterruptedException {
        LinkServiceNb.getDeviceEgressLinksRequest request = LinkServiceNb.getDeviceEgressLinksRequest.newBuilder()
                .setDeviceId(D1)
                .build();
        LinkServiceNb.getDeviceEgressLinksReply response;

        try {
            response = blockingStub.getDeviceEgressLinks(request);

            Set<Link> actualLinks = new HashSet<Link>();
            for (LinkProtoOuterClass.LinkProto link : response.getLinkList()) {
                actualLinks.add(LinkProtoTranslator.translate(link));
            }
            Set<Link> expectedLinks = new HashSet<Link>();
            expectedLinks.add(allLinks.get(0));
            expectedLinks.add(allLinks.get(5));
            expectedLinks.add(allLinks.get(7));
            expectedLinks.add(allLinks.get(9));

            assertTrue((expectedLinks).equals(actualLinks));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testGetDeviceIngressLinks() throws InterruptedException {
        LinkServiceNb.getDeviceIngressLinksRequest request = LinkServiceNb.getDeviceIngressLinksRequest.newBuilder()
                .setDeviceId(D1)
                .build();
        LinkServiceNb.getDeviceIngressLinksReply response;

        try {
            response = blockingStub.getDeviceIngressLinks(request);

            Set<Link> actualLinks = new HashSet<Link>();
            for (LinkProtoOuterClass.LinkProto link : response.getLinkList()) {
                actualLinks.add(LinkProtoTranslator.translate(link));
            }

            Set<Link> expectedLinks = new HashSet<Link>();
            expectedLinks.add(allLinks.get(1));
            expectedLinks.add(allLinks.get(4));
            expectedLinks.add(allLinks.get(6));
            expectedLinks.add(allLinks.get(8));

            assertTrue((expectedLinks).equals(actualLinks));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testGetEgressLinks() throws InterruptedException {
        LinkServiceNb.getEgressLinksRequest request = LinkServiceNb.getEgressLinksRequest.newBuilder()
                .setConnectPoint(ConnectPointProtoTranslator.translate(DEVID_1_14))
                .build();
        LinkServiceNb.getEgressLinksReply response;

        try {
            response = blockingStub.getEgressLinks(request);

            Set<Link> actualLinks = new HashSet<Link>();
            for (LinkProtoOuterClass.LinkProto link : response.getLinkList()) {
                actualLinks.add(LinkProtoTranslator.translate(link));
            }
            Set<Link> expectedLinks = new HashSet<Link>();
            expectedLinks.add(allLinks.get(5));

            assertTrue((expectedLinks).equals(actualLinks));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testGetIngressLinks() throws InterruptedException {
        LinkServiceNb.getIngressLinksRequest request = LinkServiceNb.getIngressLinksRequest.newBuilder()
                .setConnectPoint(ConnectPointProtoTranslator.translate(DEVID_1_14))
                .build();
        LinkServiceNb.getIngressLinksReply response;

        try {
            response = blockingStub.getIngressLinks(request);

            Set<Link> actualLinks = new HashSet<Link>();
            for (LinkProtoOuterClass.LinkProto link : response.getLinkList()) {
                actualLinks.add(LinkProtoTranslator.translate(link));
            }
            Set<Link> expectedLinks = new HashSet<Link>();
            expectedLinks.add(allLinks.get(4));

            assertTrue((expectedLinks).equals(actualLinks));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @BeforeClass
    public static void beforeClass() throws InstantiationException, IllegalAccessException, IOException {
        GrpcNbLinkService linkService = new GrpcNbLinkService();
        linkService.linkService = MOCK_LINK;
        inprocessServer = linkService.registerInProcessServer();
        inprocessServer.start();

        channel = InProcessChannelBuilder.forName("test").directExecutor()
                // Channels are secure by default (via SSL/TLS). For the example we disable TLS to avoid
                // needing certificates.
                .usePlaintext(true).build();
        blockingStub = LinkServiceGrpc.newBlockingStub(channel);

        populateLinks();
    }

    @AfterClass
    public static void afterClass() {
        channel.shutdownNow();

        inprocessServer.stop();
    }

    private static class MockLinkService implements LinkService {
        MockLinkService() {
        }

        @Override
        public int getLinkCount() {
            return allLinks.size();
        }

        @Override
        public Iterable<Link> getLinks() {
            return ImmutableSet.copyOf(allLinks);
        }

        @Override
        public Iterable<Link> getActiveLinks() {
            return FluentIterable.from(getLinks())
                    .filter(input -> input.state() == Link.State.ACTIVE);
        }

        @Override
        public Set<Link> getDeviceLinks(DeviceId deviceId) {
            return FluentIterable.from(getLinks())
                    .filter(input -> (input.src().deviceId().equals(deviceId)) ||
                            (input.dst().deviceId().equals(deviceId))).toSet();
        }

        @Override
        public Set<Link> getDeviceEgressLinks(DeviceId deviceId) {
            return FluentIterable.from(getLinks())
                    .filter(input -> (input.src().deviceId().equals(deviceId))).toSet();
        }

        @Override
        public Set<Link> getDeviceIngressLinks(DeviceId deviceId) {
            return FluentIterable.from(getLinks())
                    .filter(input -> (input.dst().deviceId().equals(deviceId))).toSet();
        }

        @Override
        public Set<Link> getLinks(ConnectPoint connectPoint) {
            return FluentIterable.from(getLinks())
                    .filter(input -> (input.src().equals(connectPoint)) || (input.dst().equals(connectPoint))).toSet();
        }

        @Override
        public Set<Link> getEgressLinks(ConnectPoint connectPoint) {
            return FluentIterable.from(getLinks())
                    .filter(input -> (input.src().equals(connectPoint))).toSet();
        }

        @Override
        public Set<Link> getIngressLinks(ConnectPoint connectPoint) {
            return FluentIterable.from(getLinks())
                    .filter(input -> (input.dst().equals(connectPoint))).toSet();
        }

        @Override
        public Link getLink(ConnectPoint src, ConnectPoint dst) {
            return FluentIterable.from(getLinks())
                    .filter(input -> (input.src().equals(src)) && (input.dst().equals(dst)))
                    .first().get();
        }

        @Override
        public void addListener(org.onosproject.net.link.LinkListener listener) {
        }

        @Override
        public void removeListener(org.onosproject.net.link.LinkListener listener) {
        }
    }
}