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

import static org.onosproject.net.DefaultAnnotations.EMPTY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.Matchers.nullValue;
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

/**
 * Test for PCEP setup tunnel.
 */
public class PcepSetupTunnelProviderTest {

    private static final String PROVIDER_ID = "org.onosproject.provider.tunnel.pcep";
    private PcepTunnelProvider tunnelProvider = new PcepTunnelProvider();
    private final TunnelProviderRegistryAdapter registry = new TunnelProviderRegistryAdapter();
    private final PcepClientControllerAdapter controller = new PcepClientControllerAdapter();
    private final PcepControllerAdapter ctl = new PcepControllerAdapter();
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
        tunnelProvider.cfgService = new ComponentConfigAdapter();
        tunnelProvider.tunnelService = tunnelService;
        tunnelProvider.activate();
    }

    /**
     * Send PcInitiate message to PCC.
     */
    @Test
    public void testCasePcepSetupTunnel() {
        Tunnel tunnel;
        Path path;
        ProviderId pid = new ProviderId("pcep", PROVIDER_ID);
        List<Link> links = new ArrayList<>();
        IpAddress srcIp = IpAddress.valueOf(0xC010101);
        IpElementId srcElementId = IpElementId.ipElement(srcIp);

        IpAddress dstIp = IpAddress.valueOf(0xC010102);
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

        path = new DefaultPath(pid, links, 10, EMPTY);

        Annotations annotations = DefaultAnnotations.builder()
                .set(LSP_SIG_TYPE, WITH_SIGNALLING.name())
                .build();

        tunnel = new DefaultTunnel(pid, ipTunnelEndPointSrc, ipTunnelEndPointDst, Tunnel.Type.MPLS,
                                   new GroupId(0), TunnelId.valueOf("1"), TunnelName.tunnelName("T123"),
                                   path, annotations);
        controller.getClient(PccId.pccId(IpAddress.valueOf(0xC010101))).setCapability(
                new ClientCapability(true, true, true, true, true));

        tunnelProvider.setupTunnel(tunnel, path);
        assertThat(tunnelProvider.pcepTunnelApiMapper, not(nullValue()));
    }

    /**
     * Doesn't send PCInitiate message because PCC doesn't supports PCInitiate and stateful capability.
     */
    @Test
    public void testCasePcepSetupTunnel2() {
        Tunnel tunnel;
        Path path;
        ProviderId pid = new ProviderId("pcep", PROVIDER_ID);
        List<Link> links = new ArrayList<>();
        IpAddress srcIp = IpAddress.valueOf(0xC010103);
        IpElementId srcElementId = IpElementId.ipElement(srcIp);

        IpAddress dstIp = IpAddress.valueOf(0xC010102);
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

        path = new DefaultPath(pid, links, 10, EMPTY);

        Annotations annotations = DefaultAnnotations.builder()
                .set(LSP_SIG_TYPE, WITH_SIGNALLING.name())
                .build();

        tunnel = new DefaultTunnel(pid, ipTunnelEndPointSrc, ipTunnelEndPointDst, Tunnel.Type.MPLS,
                                   new GroupId(0), TunnelId.valueOf("1"), TunnelName.tunnelName("T123"),
                                   path, annotations);
        controller.getClient(PccId.pccId(IpAddress.valueOf(0xC010103))).setCapability(
                new ClientCapability(true, true, true, true, true));

        tunnelProvider.setupTunnel(tunnel, path);
        assertThat(tunnelProvider.pcepTunnelApiMapper.checkFromTunnelRequestQueue(1), is(false));
    }

    /**
     * Sends PCInitiate msg to setup a SR based tunnel.
     */
    @Test
    public void testCasePcepSetupSrTunnel() {
        Tunnel tunnel;
        Path path;
        ProviderId pid = new ProviderId("pcep", PROVIDER_ID);
        List<Link> links = new ArrayList<>();
        IpAddress srcIp = IpAddress.valueOf(0xC010101);
        IpElementId srcElementId = IpElementId.ipElement(srcIp);

        IpAddress dstIp = IpAddress.valueOf(0xC010102);
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

        path = new DefaultPath(pid, links, 10, EMPTY);

        Annotations annotations = DefaultAnnotations.builder()
                .set(LSP_SIG_TYPE, SR_WITHOUT_SIGNALLING.name())
                .build();

        tunnel = new DefaultTunnel(pid, ipTunnelEndPointSrc, ipTunnelEndPointDst, Tunnel.Type.MPLS,
                                   new GroupId(0), TunnelId.valueOf("1"), TunnelName.tunnelName("T123"),
                                   path, annotations);
        controller.getClient(PccId.pccId(IpAddress.valueOf(0xC010101))).setCapability(
                new ClientCapability(true, true, true, true, true));

        tunnelProvider.setupTunnel(tunnel, path);
        assertThat(tunnelProvider.pcepTunnelApiMapper, not(nullValue()));
    }

    /**
     * Sends PCInitiate msg to setup a tunnel without signalling and without SR.
     */
    @Test
    public void testCasePcepSetupTunnelWithoutSigSr() {
        Tunnel tunnel;
        Path path;
        ProviderId pid = new ProviderId("pcep", PROVIDER_ID);
        List<Link> links = new ArrayList<>();
        IpAddress srcIp = IpAddress.valueOf(0xC010101);
        IpElementId srcElementId = IpElementId.ipElement(srcIp);

        IpAddress dstIp = IpAddress.valueOf(0xC010102);
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

        path = new DefaultPath(pid, links, 10, EMPTY);

        Annotations annotations = DefaultAnnotations.builder()
                .set(LSP_SIG_TYPE, WITHOUT_SIGNALLING_AND_WITHOUT_SR.name())
                .build();

        tunnel = new DefaultTunnel(pid, ipTunnelEndPointSrc, ipTunnelEndPointDst, Tunnel.Type.MPLS,
                                   new GroupId(0), TunnelId.valueOf("1"), TunnelName.tunnelName("T123"),
                                   path, annotations);
        controller.getClient(PccId.pccId(IpAddress.valueOf(0xC010101))).setCapability(
                new ClientCapability(true, true, true, true, true));

        tunnelProvider.setupTunnel(tunnel, path);
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
