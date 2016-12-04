/*
 * Copyright 2016-present Open Networking Laboratory
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

package org.onosproject.tenbi.tunnel;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.incubator.net.tunnel.TunnelId;
import org.onosproject.tetopology.management.api.TeTopologyKey;
import org.onosproject.tetopology.management.api.node.TeNodeKey;
import org.onosproject.tetopology.management.api.node.TtpKey;
import org.onosproject.tetunnel.api.TeTunnelAdminService;
import org.onosproject.tetunnel.api.TeTunnelService;
import org.onosproject.tetunnel.api.lsp.TeLsp;
import org.onosproject.tetunnel.api.lsp.TeLspKey;
import org.onosproject.tetunnel.api.tunnel.DefaultTeTunnel;
import org.onosproject.tetunnel.api.tunnel.TeTunnel;
import org.onosproject.tetunnel.api.tunnel.TeTunnelKey;
import org.onosproject.tetunnel.api.tunnel.path.DefaultTePath;
import org.onosproject.tetunnel.api.tunnel.path.DefaultTeRouteUnnumberedLink;
import org.onosproject.tetunnel.api.tunnel.path.TePath;
import org.onosproject.tetunnel.api.tunnel.path.TeRouteSubobject;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev20130715.ietfinettypes.IpAddress;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.rev20160705.IetfTe;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.rev20160705.IetfTeOpParam;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.rev20160705.ietfte.DefaultTe;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.rev20160705.ietfte.Te;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.rev20160705.ietfte.pathparamsconfig.Type;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.rev20160705.ietfte.pathparamsconfig.type.DefaultExplicit;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.rev20160705.ietfte.pathparamsconfig.type.explicit.DefaultExplicitRouteObjects;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.rev20160705.ietfte.pathparamsconfig.type.explicit.ExplicitRouteObjects;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.rev20160705.ietfte.tunnelproperties.Config;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.rev20160705.ietfte.tunnelproperties.DefaultConfig;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.rev20160705.ietfte.tunnelproperties.DefaultPrimaryPaths;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.rev20160705.ietfte.tunnelproperties.PrimaryPaths;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.rev20160705.ietfte.tunnelproperties.State;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.rev20160705.ietfte.tunnelsgrouping.DefaultTunnels;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.rev20160705.ietfte.tunnelsgrouping.Tunnels;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.rev20160705.ietfte.tunnelsgrouping.tunnels.DefaultTunnel;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.rev20160705.ietfte.tunnelsgrouping.tunnels.Tunnel;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.types.rev20160705.ietftetypes.LspProtUnprotected;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.types.rev20160705.ietftetypes.RouteIncludeEro;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.types.rev20160705.ietftetypes.StateUp;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.types.rev20160705.ietftetypes.TunnelP2p;
import org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.types.rev20160705.ietftetypes.explicitroutesubobject.type.DefaultUnnumberedLink;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.onosproject.tetunnel.api.tunnel.TeTunnel.LspProtectionType.LSP_PROT_REROUTE;
import static org.onosproject.tetunnel.api.tunnel.TeTunnel.Type.P2P;
import static org.onosproject.tetunnel.api.tunnel.path.TeRouteSubobject.Type.UNNUMBERED_LINK;

/**
 * Unit tests for TeTunnelNbiManager.
 */
public class TeTunnelNbiManagerTest {
    private static final String TE_REQ_FAILED = "IETF TE reqeust failed: ";
    private static final String NAME = "testTunnel";
    private IpAddress srcIp = IpAddress.fromString("1.1.1.1");
    private IpAddress dstIp = IpAddress.fromString("2.2.2.2");
    private byte[] bytes1 = new byte[]{1, 1, 1, 1, 0, 0, 0, 0};
    private byte[] bytes2 = new byte[]{2, 2, 2, 2, 0, 0, 0, 0};
    private long id1 = 16843009;
    private long id2 = 33686018;


    private TeTunnelNbiManager manager;
    private TeTunnel testTeTunnel;


    @Before
    public void setUp() throws Exception {
        manager = new TeTunnelNbiManager();
    }

    @Test
    public void getIetfTe() throws Exception {
        TeTunnelService tunnelService = createMock(TeTunnelService.class);
        expect(tunnelService.getTeTunnels())
                .andReturn(ImmutableList.of(buildTunnel()))
                .once();
        replay(tunnelService);

        manager.tunnelService = tunnelService;

        IetfTe ietfTe = manager.getIetfTe((IetfTeOpParam) buildGetIetfTeParams());

        assertNotNull(TE_REQ_FAILED + "te null", ietfTe.te());
        assertNotNull(TE_REQ_FAILED + "tunnel null", ietfTe.te().tunnels());

        List<Tunnel> tunnelList = ietfTe.te().tunnels().tunnel();
        assertEquals(TE_REQ_FAILED + "wrong tunnel size", 1, tunnelList.size());

        Tunnel tunnel = tunnelList.get(0);
        List<PrimaryPaths> pathsList = tunnel.primaryPaths();
        assertNotNull(TE_REQ_FAILED + "path null", pathsList);
        assertEquals(TE_REQ_FAILED + "wrong path size", 1, pathsList.size());

        Type type = pathsList.get(0).state().type();
        assertTrue(TE_REQ_FAILED + "wrong path type",
                   type instanceof DefaultExplicit);
        DefaultExplicit explicitPath = (DefaultExplicit) type;
        List<ExplicitRouteObjects> routeObjectses =
                explicitPath.explicitRouteObjects();
        assertEquals(TE_REQ_FAILED + "wrong route size", 2, routeObjectses.size());

        ExplicitRouteObjects routeObjects = routeObjectses.get(1);
        assertTrue(TE_REQ_FAILED + "wrong route object type",
                   routeObjects.type() instanceof DefaultUnnumberedLink);

        DefaultUnnumberedLink link = (DefaultUnnumberedLink) routeObjects.type();
        assertEquals(TE_REQ_FAILED + "wrong route id",
                     IpAddress.fromString("0.0.0.2"), link.routerId());
        assertEquals(TE_REQ_FAILED + "wrong interface id", 2, link.interfaceId());

        State state = tunnel.state();
        assertEquals(TE_REQ_FAILED + "wrong state",
                     StateUp.class, state.adminStatus());
        assertEquals(TE_REQ_FAILED + "wrong source",
                     IpAddress.fromString("0.0.0.1"), state.source());
    }

    @Test
    public void setIetfTe() throws Exception {
        manager.tunnelAdminService = new TestTunnelAdmin();
        manager.setIetfTe((IetfTeOpParam) buildPostIetfTeParams());
        assertEquals(NAME, testTeTunnel.name());
        List<TePath> tePaths = testTeTunnel.primaryPaths();
        assertEquals(1, tePaths.size());
        TePath tePath = tePaths.get(0);
        List<TeRouteSubobject> teRouteSubobjects = tePath.explicitRoute();
        assertEquals(2, teRouteSubobjects.size());
        TeRouteSubobject routeSubobject = teRouteSubobjects.get(1);
        assertEquals(UNNUMBERED_LINK, routeSubobject.type());
        DefaultTeRouteUnnumberedLink link =
                (DefaultTeRouteUnnumberedLink) routeSubobject;
        assertEquals(id2, link.node().teNodeId());
        assertEquals(id2, link.ttp().ttpId());

    }

    private IetfTe buildGetIetfTeParams() {
        Te te = new DefaultTe
                .TeBuilder()
                .yangTeOpType(IetfTe.OnosYangOpType.NONE)
                .build();
        return new IetfTeOpParam
                .IetfTeBuilder()
                .te(te)
                .yangIetfTeOpType(IetfTe.OnosYangOpType.NONE)
                .build();
    }

    private IetfTe buildPostIetfTeParams() {
        Tunnel tunnel = buildYangTunnel();
        Tunnels teTunnels = new DefaultTunnels
                .TunnelsBuilder()
                .tunnel(Lists.newArrayList(tunnel))
                .build();
        Te te = new DefaultTe
                .TeBuilder()
                .tunnels(teTunnels)
                .yangTeOpType(IetfTe.OnosYangOpType.NONE)
                .build();
        return new IetfTeOpParam
                .IetfTeBuilder()
                .te(te)
                .yangIetfTeOpType(IetfTe.OnosYangOpType.NONE)
                .build();
    }

    private Tunnel buildYangTunnel() {
        TeTunnel teTunnel = buildTunnel();
        checkNotNull(teTunnel);
        Config config = new DefaultConfig.ConfigBuilder()
                .name(NAME)
                .adminStatus(StateUp.class)
                .source(srcIp)
                .destination(dstIp)
                .srcTpId(bytes1)
                .dstTpId(bytes2)
                .type(TunnelP2p.class)
                .lspProtectionType(LspProtUnprotected.class)
                .build();

        org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.types.
                rev20160705.ietftetypes.explicitroutesubobject.type.
                UnnumberedLink yangLink1 = DefaultUnnumberedLink.builder()
                .routerId(srcIp)
                .interfaceId(id1)
                .build();

        org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.types.
                rev20160705.ietftetypes.explicitroutesubobject.type.
                UnnumberedLink yangLink2 = DefaultUnnumberedLink.builder()
                .routerId(dstIp)
                .interfaceId(id2)
                .build();

        ExplicitRouteObjects routeObject1 = DefaultExplicitRouteObjects.builder()
                .type(yangLink1)
                .explicitRouteUsage(RouteIncludeEro.class)
                .build();

        ExplicitRouteObjects routeObject2 = DefaultExplicitRouteObjects.builder()
                .type(yangLink2)
                .explicitRouteUsage(RouteIncludeEro.class)
                .build();


        org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.
                rev20160705.ietfte.pathparamsconfig.type.Explicit explicit
                = DefaultExplicit.builder()
                .explicitRouteObjects(ImmutableList.of(routeObject1, routeObject2))
                .build();
        org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.te.
                rev20160705.ietfte.p2pprimarypathparams.Config pathConfig
                = org.onosproject.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.
                te.rev20160705.ietfte.p2pprimarypathparams.DefaultConfig.builder()
                .pathNamedConstraint("onlyPath")
                .lockdown(true)
                .noCspf(true)
                .type(explicit)
                .build();

        PrimaryPaths primaryPaths = DefaultPrimaryPaths.builder()
                .config(pathConfig).build();

        return DefaultTunnel.builder()
                .name(config.name())
                .type(config.type())
                .config(config)
                .primaryPaths(Lists.newArrayList(primaryPaths))
                .build();
    }

    private TeTunnel buildTunnel() {
        TeTopologyKey topologyKey = new TeTopologyKey(1, 2, 3);
        TeTunnelKey teTunnelKey = new TeTunnelKey(topologyKey, 1);

        TeNodeKey srcNodeKey = new TeNodeKey(topologyKey, 1);
        TeNodeKey dstNodeKey = new TeNodeKey(topologyKey, 2);

        TtpKey srcTtpKey = new TtpKey(srcNodeKey, 1);
        TtpKey dstTtpKey = new TtpKey(srcNodeKey, 2);

        TeLspKey lspKey = new TeLspKey(teTunnelKey, 1);

        DefaultTeRouteUnnumberedLink unnumberedLink1 =
                new DefaultTeRouteUnnumberedLink(srcNodeKey, srcTtpKey);
        DefaultTeRouteUnnumberedLink unnumberedLink2 =
                new DefaultTeRouteUnnumberedLink(dstNodeKey, dstTtpKey);
        List<TeRouteSubobject> explicitRouteList = new ArrayList<>();

        explicitRouteList.add(unnumberedLink1);
        explicitRouteList.add(unnumberedLink2);
        TePath tePath = new DefaultTePath(TePath.Type.EXPLICIT,
                                          Lists.newArrayList(lspKey),
                                          explicitRouteList,
                                          Lists.newArrayList());

        return DefaultTeTunnel.builder()
                .teTunnelKey(teTunnelKey)
                .name(NAME)
                .type(P2P)
                .adminState(TeTunnel.State.UP)
                .srcNode(srcNodeKey)
                .dstNode(dstNodeKey)
                .srcTp(srcTtpKey)
                .dstTp(dstTtpKey)
                .lspProtectionType(LSP_PROT_REROUTE)
                .primaryPaths(Lists.newArrayList(tePath))
                .build();
    }

    private class TestTunnelAdmin implements TeTunnelAdminService {

        @Override
        public TunnelId createTeTunnel(TeTunnel teTunnel) {
            TunnelId tunnelId = TunnelId.valueOf(teTunnel.teTunnelKey().toString());
            testTeTunnel = teTunnel;
            return tunnelId;
        }

        @Override
        public void setTunnelId(TeTunnelKey teTunnelKey, TunnelId tunnelId) {

        }

        @Override
        public void updateTeTunnel(TeTunnel teTunnel) {

        }

        @Override
        public void updateTunnelState(TeTunnelKey key, org.onosproject.incubator.net.tunnel.Tunnel.State state) {

        }

        @Override
        public void removeTeTunnel(TeTunnelKey teTunnelKey) {

        }

        @Override
        public void removeTeTunnels() {

        }

        @Override
        public void setSegmentTunnel(TeTunnelKey e2eTunnelKey, List<TeTunnelKey> segmentTunnels) {

        }

        @Override
        public TeTunnel getTeTunnel(TeTunnelKey teTunnelKey) {
            return null;
        }

        @Override
        public TeTunnel getTeTunnel(TunnelId tunnelId) {
            return null;
        }

        @Override
        public TunnelId getTunnelId(TeTunnelKey teTunnelKey) {
            return null;
        }

        @Override
        public Collection<TeTunnel> getTeTunnels() {
            return null;
        }

        @Override
        public Collection<TeTunnel> getTeTunnels(TeTunnel.Type type) {
            return null;
        }

        @Override
        public Collection<TeTunnel> getTeTunnels(TeTopologyKey teTopologyKey) {
            return null;
        }

        @Override
        public TeLsp getTeLsp(TeLspKey key) {
            return null;
        }

        @Override
        public Collection<TeLsp> getTeLsps() {
            return null;
        }
    }

}