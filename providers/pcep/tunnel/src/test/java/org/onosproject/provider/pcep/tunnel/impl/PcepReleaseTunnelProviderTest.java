/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.provider.pcep.tunnel.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.onosproject.net.DefaultAnnotations.EMPTY;
import static org.onosproject.pcep.controller.LspType.WITH_SIGNALLING;
import static org.onosproject.pcep.controller.LspType.SR_WITHOUT_SIGNALLING;
import static org.onosproject.pcep.controller.LspType.WITHOUT_SIGNALLING_AND_WITHOUT_SR;
import static org.onosproject.pcep.controller.PcepAnnotationKeys.LSP_SIG_TYPE;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.IpAddress;
import org.onosproject.cfg.ComponentConfigAdapter;
import org.onosproject.core.GroupId;
import org.onosproject.incubator.net.tunnel.DefaultTunnel;
import org.onosproject.incubator.net.tunnel.IpTunnelEndPoint;
import org.onosproject.incubator.net.tunnel.Tunnel;
import org.onosproject.incubator.net.tunnel.TunnelId;
import org.onosproject.incubator.net.tunnel.TunnelName;
import org.onosproject.mastership.MastershipServiceAdapter;
import org.onosproject.net.Annotations;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DefaultLink;
import org.onosproject.net.DefaultPath;
import org.onosproject.net.IpElementId;
import org.onosproject.net.Link;
import org.onosproject.net.Path;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceServiceAdapter;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.pcep.controller.ClientCapability;
import org.onosproject.pcep.controller.PccId;
import org.onosproject.pcepio.types.StatefulIPv4LspIdentifiersTlv;

/**
 * Test for PCEP release tunnel.
 */
public class PcepReleaseTunnelProviderTest {

    private static final String PROVIDER_ID = "org.onosproject.provider.tunnel.pcep";
    private PcepTunnelProvider tunnelProvider = new PcepTunnelProvider();
    private final TunnelProviderRegistryAdapter registry = new TunnelProviderRegistryAdapter();
    private final PcepClientControllerAdapter controller = new PcepClientControllerAdapter();
    private final PcepControllerAdapter ctl = new PcepControllerAdapter();
    private final PcepTunnelApiMapper pcepTunnelAPIMapper = new PcepTunnelApiMapper();
    private final TunnelServiceAdapter  tunnelService = new TunnelServiceAdapter();
    private final DeviceServiceAdapter  deviceService = new DeviceServiceAdapter();
    private final MastershipServiceAdapter  mastershipService = new MastershipServiceAdapter();

    @Before
    public void setUp() throws IOException {
        tunnelProvider.tunnelProviderRegistry = registry;
        tunnelProvider.pcepClientController = controller;
        tunnelProvider.controller = ctl;
        tunnelProvider.deviceService = deviceService;
        tunnelProvider.mastershipService = mastershipService;
        tunnelProvider.tunnelService = tunnelService;
        tunnelProvider.pcepTunnelApiMapper = pcepTunnelAPIMapper;
        tunnelProvider.cfgService = new ComponentConfigAdapter();
        tunnelProvider.activate();
    }

    /**
     * Release tunnel with negotiated capability.
     */
    @Test
    public void testCasePcepReleaseTunnel() {
        Tunnel tunnel;
        Path path;
        List<Link> links = new ArrayList<>();

        ProviderId pid = new ProviderId("pcep", PROVIDER_ID);

        IpAddress srcIp = IpAddress.valueOf(0xB6024E20);
        IpElementId srcElementId = IpElementId.ipElement(srcIp);

        IpAddress dstIp = IpAddress.valueOf(0xB6024E21);
        IpElementId dstElementId = IpElementId.ipElement(dstIp);

        IpTunnelEndPoint ipTunnelEndPointSrc;
        ipTunnelEndPointSrc = IpTunnelEndPoint.ipTunnelPoint(srcIp);

        IpTunnelEndPoint ipTunnelEndPointDst;
        ipTunnelEndPointDst = IpTunnelEndPoint.ipTunnelPoint(dstIp);

        ConnectPoint src = new ConnectPoint(srcElementId, PortNumber.portNumber(10023));

        ConnectPoint dst = new ConnectPoint(dstElementId, PortNumber.portNumber(10023));

        Link link = DefaultLink.builder().providerId(pid).src(src).dst(dst)
                .type(Link.Type.DIRECT).build();
        links.add(link);

        path = new DefaultPath(pid, links, 20, EMPTY);

        Annotations annotations = DefaultAnnotations.builder()
                .set(LSP_SIG_TYPE, WITH_SIGNALLING.name())
                .build();

        tunnel = new DefaultTunnel(pid, ipTunnelEndPointSrc, ipTunnelEndPointDst, Tunnel.Type.MPLS,
                                   new GroupId(0), TunnelId.valueOf("1"), TunnelName.tunnelName("T123"),
                                   path, annotations);

        // for releasing tunnel tunnel should exist in db
        PcepTunnelData pcepTunnelData = new PcepTunnelData(tunnel, path, RequestType.DELETE);
        pcepTunnelData.setPlspId(1);
        StatefulIPv4LspIdentifiersTlv tlv = new StatefulIPv4LspIdentifiersTlv(0, (short) 1, (short) 2, 3, 4);
        pcepTunnelData.setStatefulIpv4IndentifierTlv(tlv);
        tunnelProvider.pcepTunnelApiMapper.addToTunnelIdMap(pcepTunnelData);

        tunnelProvider.pcepTunnelApiMapper.handleCreateTunnelRequestQueue(1, pcepTunnelData);
        controller.getClient(PccId.pccId(IpAddress.valueOf(0xB6024E20))).setCapability(
                new ClientCapability(true, true, true, true, true));

        tunnelProvider.releaseTunnel(tunnel);
        assertThat(tunnelProvider.pcepTunnelApiMapper, not(nullValue()));
    }

    /**
     * Doesn't send initiate message because PCC doesn't supports PCInitiate and stateful capability.
     */
    @Test
    public void testCasePcepReleaseTunnel2() {
        Tunnel tunnel;
        Path path;
        List<Link> links = new ArrayList<>();

        ProviderId pid = new ProviderId("pcep", PROVIDER_ID);

        IpAddress srcIp = IpAddress.valueOf(0xB6024E22);
        IpElementId srcElementId = IpElementId.ipElement(srcIp);

        IpAddress dstIp = IpAddress.valueOf(0xB6024E21);
        IpElementId dstElementId = IpElementId.ipElement(dstIp);

        IpTunnelEndPoint ipTunnelEndPointSrc;
        ipTunnelEndPointSrc = IpTunnelEndPoint.ipTunnelPoint(srcIp);

        IpTunnelEndPoint ipTunnelEndPointDst;
        ipTunnelEndPointDst = IpTunnelEndPoint.ipTunnelPoint(dstIp);

        ConnectPoint src = new ConnectPoint(srcElementId, PortNumber.portNumber(10023));

        ConnectPoint dst = new ConnectPoint(dstElementId, PortNumber.portNumber(10023));

        Link link = DefaultLink.builder().providerId(pid).src(src).dst(dst)
                .type(Link.Type.DIRECT).build();
        links.add(link);

        path = new DefaultPath(pid, links, 20, EMPTY);

        Annotations annotations = DefaultAnnotations.builder()
                .set(LSP_SIG_TYPE, WITH_SIGNALLING.name())
                .build();

        tunnel = new DefaultTunnel(pid, ipTunnelEndPointSrc, ipTunnelEndPointDst, Tunnel.Type.MPLS,
                                   new GroupId(0), TunnelId.valueOf("1"), TunnelName.tunnelName("T123"),
                                   path, annotations);

        // for releasing tunnel tunnel should exist in db
        PcepTunnelData pcepTunnelData = new PcepTunnelData(tunnel, path, RequestType.DELETE);
        pcepTunnelData.setPlspId(1);
        StatefulIPv4LspIdentifiersTlv tlv = new StatefulIPv4LspIdentifiersTlv(0, (short) 1, (short) 2, 3, 4);
        pcepTunnelData.setStatefulIpv4IndentifierTlv(tlv);
        tunnelProvider.pcepTunnelApiMapper.addToTunnelIdMap(pcepTunnelData);

        tunnelProvider.pcepTunnelApiMapper.handleCreateTunnelRequestQueue(1, pcepTunnelData);
        controller.getClient(PccId.pccId(IpAddress.valueOf(0xB6024E22))).setCapability(
                new ClientCapability(true, false, false, true, true));

        tunnelProvider.releaseTunnel(tunnel);
        assertThat(tunnelProvider.pcepTunnelApiMapper.checkFromTunnelRequestQueue(1), is(false));
    }

    /**
     * Tests releasing SR based tunnel.
     */
    @Test
    public void testCasePcepReleaseSrTunnel() {
        Tunnel tunnel;
        Path path;
        List<Link> links = new ArrayList<>();

        ProviderId pid = new ProviderId("pcep", PROVIDER_ID);

        IpAddress srcIp = IpAddress.valueOf(0xB6024E20);
        IpElementId srcElementId = IpElementId.ipElement(srcIp);

        IpAddress dstIp = IpAddress.valueOf(0xB6024E21);
        IpElementId dstElementId = IpElementId.ipElement(dstIp);

        IpTunnelEndPoint ipTunnelEndPointSrc;
        ipTunnelEndPointSrc = IpTunnelEndPoint.ipTunnelPoint(srcIp);

        IpTunnelEndPoint ipTunnelEndPointDst;
        ipTunnelEndPointDst = IpTunnelEndPoint.ipTunnelPoint(dstIp);

        ConnectPoint src = new ConnectPoint(srcElementId, PortNumber.portNumber(10023));

        ConnectPoint dst = new ConnectPoint(dstElementId, PortNumber.portNumber(10023));

        Link link = DefaultLink.builder().providerId(pid).src(src).dst(dst)
                .type(Link.Type.DIRECT).build();
        links.add(link);

        path = new DefaultPath(pid, links, 20, EMPTY);

        Annotations annotations = DefaultAnnotations.builder()
                .set(LSP_SIG_TYPE, SR_WITHOUT_SIGNALLING.name())
                .build();

        tunnel = new DefaultTunnel(pid, ipTunnelEndPointSrc, ipTunnelEndPointDst, Tunnel.Type.MPLS,
                                   new GroupId(0), TunnelId.valueOf("1"), TunnelName.tunnelName("T123"),
                                   path, annotations);

        // for releasing tunnel tunnel should exist in db
        PcepTunnelData pcepTunnelData = new PcepTunnelData(tunnel, path, RequestType.DELETE);
        pcepTunnelData.setPlspId(1);
        StatefulIPv4LspIdentifiersTlv tlv = new StatefulIPv4LspIdentifiersTlv(0, (short) 1, (short) 2, 3, 4);
        pcepTunnelData.setStatefulIpv4IndentifierTlv(tlv);
        tunnelProvider.pcepTunnelApiMapper.addToTunnelIdMap(pcepTunnelData);

        tunnelProvider.pcepTunnelApiMapper.handleCreateTunnelRequestQueue(1, pcepTunnelData);
        controller.getClient(PccId.pccId(IpAddress.valueOf(0xB6024E20))).setCapability(
                new ClientCapability(true, true, true, true, true));

        tunnelProvider.releaseTunnel(tunnel);
        assertThat(tunnelProvider.pcepTunnelApiMapper, not(nullValue()));
    }

    /**
     * Tests releasing tunnel without SR and without signalling.
     */
    @Test
    public void testCasePcepReleaseTunnelWithoutSigSr() {
        Tunnel tunnel;
        Path path;
        List<Link> links = new ArrayList<>();

        ProviderId pid = new ProviderId("pcep", PROVIDER_ID);

        IpAddress srcIp = IpAddress.valueOf(0xB6024E20);
        IpElementId srcElementId = IpElementId.ipElement(srcIp);

        IpAddress dstIp = IpAddress.valueOf(0xB6024E21);
        IpElementId dstElementId = IpElementId.ipElement(dstIp);

        IpTunnelEndPoint ipTunnelEndPointSrc;
        ipTunnelEndPointSrc = IpTunnelEndPoint.ipTunnelPoint(srcIp);

        IpTunnelEndPoint ipTunnelEndPointDst;
        ipTunnelEndPointDst = IpTunnelEndPoint.ipTunnelPoint(dstIp);

        ConnectPoint src = new ConnectPoint(srcElementId, PortNumber.portNumber(10023));

        ConnectPoint dst = new ConnectPoint(dstElementId, PortNumber.portNumber(10023));

        Link link = DefaultLink.builder().providerId(pid).src(src).dst(dst)
                .type(Link.Type.DIRECT).build();
        links.add(link);

        path = new DefaultPath(pid, links, 20, EMPTY);

        Annotations annotations = DefaultAnnotations.builder()
                .set(LSP_SIG_TYPE, WITHOUT_SIGNALLING_AND_WITHOUT_SR.name())
                .build();

        tunnel = new DefaultTunnel(pid, ipTunnelEndPointSrc, ipTunnelEndPointDst, Tunnel.Type.MPLS,
                                   new GroupId(0), TunnelId.valueOf("1"), TunnelName.tunnelName("T123"),
                                   path, annotations);

        // for releasing tunnel tunnel should exist in db
        PcepTunnelData pcepTunnelData = new PcepTunnelData(tunnel, path, RequestType.DELETE);
        pcepTunnelData.setPlspId(1);
        StatefulIPv4LspIdentifiersTlv tlv = new StatefulIPv4LspIdentifiersTlv(0, (short) 1, (short) 2, 3, 4);
        pcepTunnelData.setStatefulIpv4IndentifierTlv(tlv);
        tunnelProvider.pcepTunnelApiMapper.addToTunnelIdMap(pcepTunnelData);

        tunnelProvider.pcepTunnelApiMapper.handleCreateTunnelRequestQueue(1, pcepTunnelData);
        controller.getClient(PccId.pccId(IpAddress.valueOf(0xB6024E20))).setCapability(
                new ClientCapability(true, true, true, true, true));

        tunnelProvider.releaseTunnel(tunnel);
        assertThat(tunnelProvider.pcepTunnelApiMapper, not(nullValue()));
    }

    @After
    public void tearDown() throws IOException {
        tunnelProvider.deactivate();
        tunnelProvider.controller = null;
        tunnelProvider.pcepClientController = null;
        tunnelProvider.tunnelProviderRegistry = null;
        tunnelProvider.deviceService = null;
        tunnelProvider.mastershipService = null;
    }
}
