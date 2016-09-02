package org.onosproject.pce.pceservice;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.onlab.graph.GraphPathSearch.ALL_PATHS;
import static org.onosproject.incubator.net.tunnel.Tunnel.State.ESTABLISHED;
import static org.onosproject.incubator.net.tunnel.Tunnel.State.UNSTABLE;
import static org.onosproject.net.MastershipRole.MASTER;
import static org.onosproject.net.resource.Resources.continuous;
import static org.onosproject.pce.pceservice.LspType.SR_WITHOUT_SIGNALLING;
import static org.onosproject.pce.pceservice.LspType.WITHOUT_SIGNALLING_AND_WITHOUT_SR;
import static org.onosproject.pce.pceservice.LspType.WITH_SIGNALLING;
import static org.onosproject.pce.pceservice.PathComputationTest.D1;
import static org.onosproject.pce.pceservice.PathComputationTest.D2;
import static org.onosproject.pce.pceservice.PathComputationTest.D3;
import static org.onosproject.pce.pceservice.PathComputationTest.D4;
import static org.onosproject.pce.pceservice.PathComputationTest.D5;
import static org.onosproject.pce.pceservice.PathComputationTest.DEVICE1;
import static org.onosproject.pce.pceservice.PathComputationTest.DEVICE2;
import static org.onosproject.pce.pceservice.PathComputationTest.DEVICE3;
import static org.onosproject.pce.pceservice.PathComputationTest.DEVICE4;
import static org.onosproject.pce.pceservice.PathComputationTest.DEVICE5;
import static org.onosproject.pce.pceservice.PcepAnnotationKeys.LOCAL_LSP_ID;
import static org.onosproject.pce.pceservice.PcepAnnotationKeys.PLSP_ID;
import static org.onosproject.pce.pceservice.constraint.CostConstraint.Type.COST;
import static org.onosproject.pce.pceservice.constraint.CostConstraint.Type.TE_COST;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import com.google.common.collect.Lists;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.graph.GraphPathSearch;
import org.onlab.junit.TestUtils;
import org.onlab.junit.TestUtils.TestUtilsException;
import org.onlab.util.Bandwidth;
import org.onosproject.common.DefaultTopologyGraph;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreServiceAdapter;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.core.IdGenerator;
import org.onosproject.event.Event;
import org.onosproject.incubator.net.tunnel.DefaultTunnel;
import org.onosproject.incubator.net.tunnel.Tunnel;
import org.onosproject.incubator.net.tunnel.Tunnel.State;
import org.onosproject.incubator.net.tunnel.TunnelEndPoint;
import org.onosproject.incubator.net.tunnel.TunnelEvent;
import org.onosproject.incubator.net.tunnel.TunnelId;
import org.onosproject.incubator.net.tunnel.TunnelListener;
import org.onosproject.mastership.MastershipServiceAdapter;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.Annotations;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DefaultAnnotations.Builder;
import org.onosproject.net.DefaultDevice;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.ElementId;
import org.onosproject.net.Link;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.Path;
import org.onosproject.net.SparseAnnotations;
import org.onosproject.net.intent.Constraint;
import org.onosproject.net.intent.IntentId;
import org.onosproject.net.intent.constraint.BandwidthConstraint;
import org.onosproject.net.link.LinkEvent;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.net.resource.Resource;
import org.onosproject.net.topology.DefaultTopologyEdge;
import org.onosproject.net.topology.DefaultTopologyVertex;
import org.onosproject.net.topology.LinkWeight;
import org.onosproject.net.topology.PathServiceAdapter;
import org.onosproject.net.topology.Topology;
import org.onosproject.net.topology.TopologyEdge;
import org.onosproject.net.topology.TopologyEvent;
import org.onosproject.net.topology.TopologyGraph;
import org.onosproject.net.topology.TopologyListener;
import org.onosproject.net.topology.TopologyServiceAdapter;
import org.onosproject.net.topology.TopologyVertex;
import org.onosproject.pce.pceservice.PathComputationTest.MockNetConfigRegistryAdapter;
import org.onosproject.pce.pceservice.PathComputationTest.MockPathResourceService;
import org.onosproject.pce.pceservice.constraint.CostConstraint;
import org.onosproject.pce.pcestore.api.PceStore;
import org.onosproject.pce.util.MockDeviceService;
import org.onosproject.pce.util.PceStoreAdapter;
import org.onosproject.pce.util.TunnelServiceAdapter;
import org.onosproject.pcep.api.DeviceCapability;
import org.onosproject.store.service.TestStorageService;

import com.google.common.collect.ImmutableSet;

/**
 * Tests the functions of PceManager.
 */
public class PceManagerTest {

    private PathComputationTest pathCompTest = new PathComputationTest();
    private MockPathResourceService resourceService = pathCompTest.new MockPathResourceService();
    private MockTopologyService topologyService = new MockTopologyService();
    private MockMastershipService mastershipService = new MockMastershipService();
    private MockPathService pathService = new MockPathService();
    private PceManager pceManager = new PceManager();
    private MockCoreService coreService = new MockCoreService();
    private MockTunnelServiceAdapter tunnelService = new MockTunnelServiceAdapter();
    private TestStorageService storageService = new TestStorageService();
    private MockDeviceService deviceService = new MockDeviceService();
    private MockNetConfigRegistryAdapter netConfigRegistry = new PathComputationTest.MockNetConfigRegistryAdapter();
    private PceStore pceStore = new PceStoreAdapter();

    public static ProviderId providerId = new ProviderId("pce", "foo");
    private static final String L3 = "L3";
    private static final String LSRID = "lsrId";
    private static final String PCECC_CAPABILITY = "pceccCapability";
    private static final String SR_CAPABILITY = "srCapability";
    private static final String LABEL_STACK_CAPABILITY = "labelStackCapability";

    private TopologyGraph graph = null;
    private Device deviceD1, deviceD2, deviceD3, deviceD4, deviceD5;
    private Device pcepDeviceD1, pcepDeviceD2, pcepDeviceD3, pcepDeviceD4;
    private Link link1, link2, link3, link4, link5, link6;
    protected static int flowsDownloaded;
    private TunnelListener tunnelListener;
    private TopologyListener listener;
    private Topology topology;
    private Set<TopologyEdge> edges;
    private Set<TopologyVertex> vertexes;

    @Before
    public void startUp() throws TestUtilsException {
        listener = TestUtils.getField(pceManager, "topologyListener");
        pceManager.pathService = pathService;
        pceManager.resourceService = resourceService;
        pceManager.topologyService = topologyService;
        pceManager.tunnelService = tunnelService;
        pceManager.coreService = coreService;
        pceManager.storageService = storageService;
        pceManager.deviceService = deviceService;
        pceManager.netCfgService = netConfigRegistry;
        pceManager.pceStore = pceStore;
        pceManager.mastershipService = mastershipService;
        pceManager.activate();
    }

    private class MockMastershipService extends MastershipServiceAdapter {
        @Override
        public MastershipRole getLocalRole(DeviceId deviceId) {
            return MASTER;
        }

        @Override
        public boolean isLocalMaster(DeviceId deviceId) {
            return getLocalRole(deviceId) == MASTER;
        }
    }

    private void build4RouterTopo(boolean setCost, boolean setPceccCap, boolean setSrCap,
                                 boolean setLabelStackCap, int bandwidth) {
        link1 = PathComputationTest.addLink(DEVICE1, 10, DEVICE2, 20, setCost, 50);
        link2 = PathComputationTest.addLink(DEVICE2, 30, DEVICE4, 40, setCost, 20);
        link3 = PathComputationTest.addLink(DEVICE1, 80, DEVICE3, 70, setCost, 100);
        link4 = PathComputationTest.addLink(DEVICE3, 60, DEVICE4, 50, setCost, 80);
        link5 = PathComputationTest.addLink(DEVICE2, 60, DEVICE5, 50, setCost, 80);
        link6 = PathComputationTest.addLink(DEVICE4, 60, DEVICE5, 50, setCost, 80);

        Set<TopologyVertex> vertexes = new HashSet<TopologyVertex>();
        vertexes.add(D1);
        vertexes.add(D2);
        vertexes.add(D3);
        vertexes.add(D4);
        vertexes.add(D5);

        this.vertexes = vertexes;

        Set<TopologyEdge> edges = new HashSet<TopologyEdge>();
        TopologyEdge edge1 = new DefaultTopologyEdge(D1, D2, link1);
        edges.add(edge1);

        TopologyEdge edge2 = new DefaultTopologyEdge(D2, D4, link2);
        edges.add(edge2);

        TopologyEdge edge3 = new DefaultTopologyEdge(D1, D3, link3);
        edges.add(edge3);

        TopologyEdge edge4 = new DefaultTopologyEdge(D3, D4, link4);
        edges.add(edge4);

        TopologyEdge edge5 = new DefaultTopologyEdge(D2, D5, link5);
        edges.add(edge5);

        TopologyEdge edge6 = new DefaultTopologyEdge(D4, D5, link6);
        edges.add(edge6);

        this.edges = edges;

        graph = new DefaultTopologyGraph(vertexes, edges);

        DefaultAnnotations.Builder builderDev1 = DefaultAnnotations.builder();
        DefaultAnnotations.Builder builderDev2 = DefaultAnnotations.builder();
        DefaultAnnotations.Builder builderDev3 = DefaultAnnotations.builder();
        DefaultAnnotations.Builder builderDev4 = DefaultAnnotations.builder();
        DefaultAnnotations.Builder builderDev5 = DefaultAnnotations.builder();

        // Making L3 devices
        builderDev1.set(AnnotationKeys.TYPE, L3);
        builderDev1.set(LSRID, "1.1.1.1");

        builderDev2.set(AnnotationKeys.TYPE, L3);
        builderDev2.set(LSRID, "2.2.2.2");

        builderDev3.set(AnnotationKeys.TYPE, L3);
        builderDev3.set(LSRID, "3.3.3.3");

        builderDev4.set(AnnotationKeys.TYPE, L3);
        builderDev4.set(LSRID, "4.4.4.4");

        builderDev5.set(AnnotationKeys.TYPE, L3);
        builderDev5.set(LSRID, "5.5.5.5");

        deviceD1 = new MockDevice(D1.deviceId(), builderDev1.build());
        deviceD2 = new MockDevice(D2.deviceId(), builderDev2.build());
        deviceD3 = new MockDevice(D3.deviceId(), builderDev3.build());
        deviceD4 = new MockDevice(D4.deviceId(), builderDev4.build());
        deviceD5 = new MockDevice(D5.deviceId(), builderDev5.build());

        deviceService.addDevice(deviceD1);
        deviceService.addDevice(deviceD2);
        deviceService.addDevice(deviceD3);
        deviceService.addDevice(deviceD4);
        deviceService.addDevice(deviceD5);

        DeviceCapability device1Cap = netConfigRegistry.addConfig(DeviceId.deviceId("1.1.1.1"), DeviceCapability.class);
        device1Cap.setLabelStackCap(setLabelStackCap)
        .setLocalLabelCap(setPceccCap)
        .setSrCap(setSrCap)
        .apply();

        DeviceCapability device2Cap = netConfigRegistry.addConfig(DeviceId.deviceId("2.2.2.2"), DeviceCapability.class);
        device2Cap.setLabelStackCap(setLabelStackCap)
        .setLocalLabelCap(setPceccCap)
        .setSrCap(setSrCap)
        .apply();

        DeviceCapability device3Cap = netConfigRegistry.addConfig(DeviceId.deviceId("3.3.3.3"), DeviceCapability.class);
        device3Cap.setLabelStackCap(setLabelStackCap)
        .setLocalLabelCap(setPceccCap)
        .setSrCap(setSrCap)
        .apply();

        DeviceCapability device4Cap = netConfigRegistry.addConfig(DeviceId.deviceId("4.4.4.4"), DeviceCapability.class);
        device4Cap.setLabelStackCap(setLabelStackCap)
        .setLocalLabelCap(setPceccCap)
        .setSrCap(setSrCap)
        .apply();

        DeviceCapability device5Cap = netConfigRegistry.addConfig(DeviceId.deviceId("5.5.5.5"), DeviceCapability.class);
        device4Cap.setLabelStackCap(setLabelStackCap)
                .setLocalLabelCap(setPceccCap)
                .setSrCap(setSrCap)
                .apply();

        if (bandwidth != 0) {
            List<Resource> resources = new LinkedList<>();
            resources.add(continuous(link1.src().deviceId(), link1.src().port(), Bandwidth.class).resource(bandwidth));
            resources.add(continuous(link2.src().deviceId(), link2.src().port(), Bandwidth.class).resource(bandwidth));
            resources.add(continuous(link3.src().deviceId(), link3.src().port(), Bandwidth.class).resource(bandwidth));
            resources.add(continuous(link4.src().deviceId(), link4.src().port(), Bandwidth.class).resource(bandwidth));
            resources.add(continuous(link5.src().deviceId(), link5.src().port(), Bandwidth.class).resource(bandwidth));

            resources.add(continuous(link1.dst().deviceId(), link1.dst().port(), Bandwidth.class).resource(bandwidth));
            resources.add(continuous(link2.dst().deviceId(), link2.dst().port(), Bandwidth.class).resource(bandwidth));
            resources.add(continuous(link3.dst().deviceId(), link3.dst().port(), Bandwidth.class).resource(bandwidth));
            resources.add(continuous(link4.dst().deviceId(), link4.dst().port(), Bandwidth.class).resource(bandwidth));
            resources.add(continuous(link5.dst().deviceId(), link5.dst().port(), Bandwidth.class).resource(bandwidth));

            resourceService.allocate(IntentId.valueOf(bandwidth), resources);
        }
    }

    /**
     * Tests path success with (IGP) cost constraint for signalled LSP.
     */
    @Test
    public void setupPathTest1() {
        build4RouterTopo(true, false, false, false, 0); // IGP cost is set here.
        List<Constraint> constraints = new LinkedList<Constraint>();
        CostConstraint costConstraint = new CostConstraint(COST);
        constraints.add(costConstraint);

        boolean result = pceManager.setupPath(D1.deviceId(), D2.deviceId(), "T123", constraints, WITH_SIGNALLING, null);
        assertThat(result, is(true));
    }

    /**
     * Tests path failure with (IGP) cost constraint for signalled LSP.
     */
    @Test
    public void setupPathTest2() {
        build4RouterTopo(false, false, false, false, 0); // TE cost is set here, not IGP.
        List<Constraint> constraints = new LinkedList<Constraint>();
        CostConstraint costConstraint = new CostConstraint(COST);
        constraints.add(costConstraint);

        boolean result = pceManager.setupPath(D1.deviceId(), D2.deviceId(), "T123", constraints, WITH_SIGNALLING, null);
        assertThat(result, is(false));
    }

    /**
     * Tests path success with TE-cost constraint for signalled LSP.
     */
    @Test
    public void setupPathTest3() {
        build4RouterTopo(false, false, false, false, 0); // TE cost is set here.

        List<Constraint> constraints = new LinkedList<Constraint>();
        CostConstraint costConstraint = new CostConstraint(TE_COST);
        constraints.add(costConstraint);

        boolean result = pceManager.setupPath(D1.deviceId(), D2.deviceId(), "T123", constraints, WITH_SIGNALLING, null);
        assertThat(result, is(true));
    }

    /**
     * Tests path failure with TE-cost constraint for signalled LSP.
     */
    @Test
    public void setupPathTest4() {
        build4RouterTopo(true, false, false, false, 0); // IGP cost is set here, not TE.

        List<Constraint> constraints = new LinkedList<Constraint>();
        CostConstraint costConstraint = new CostConstraint(TE_COST);
        constraints.add(costConstraint);

        boolean result = pceManager.setupPath(D1.deviceId(), D2.deviceId(), "T123", constraints, WITH_SIGNALLING, null);
        assertThat(result, is(false));
    }

    /**
     * Tests path success with (IGP) cost constraint for non-SR non-signalled LSP.
     */
    @Test
    public void setupPathTest5() {
        build4RouterTopo(true, true, false, false, 0);

        List<Constraint> constraints = new LinkedList<Constraint>();
        CostConstraint costConstraint = new CostConstraint(COST);
        constraints.add(costConstraint);

        boolean result = pceManager.setupPath(D1.deviceId(), D2.deviceId(), "T123", constraints,
                                              WITHOUT_SIGNALLING_AND_WITHOUT_SR, null);
        assertThat(result, is(true));
    }

    /**
     * Tests path success with TE-cost constraint for non-SR non-sgnalled LSP.
     */
    @Test
    public void setupPathTest6() {
        build4RouterTopo(false, true, false, false, 0);

        List<Constraint> constraints = new LinkedList<Constraint>();
        CostConstraint costConstraint = new CostConstraint(TE_COST);
        constraints.add(costConstraint);

        boolean result = pceManager.setupPath(D1.deviceId(), D2.deviceId(), "T123", constraints,
                                              WITHOUT_SIGNALLING_AND_WITHOUT_SR, null);
        assertThat(result, is(true));
    }

    /**
     * Tests path failure with TE-cost constraint for non-SR non-signalled LSP(CR). Label capability not registered.
     */
    @Test
    public void setupPathTest7() {
        build4RouterTopo(true, false, false, false, 0);

        List<Constraint> constraints = new LinkedList<Constraint>();
        CostConstraint costConstraint = new CostConstraint(TE_COST);
        constraints.add(costConstraint);

        boolean result = pceManager.setupPath(D1.deviceId(), D2.deviceId(), "T123", constraints,
                                              WITHOUT_SIGNALLING_AND_WITHOUT_SR, null);
        assertThat(result, is(false));
    }

    /**
     * Tests path failure as bandwidth is requested but is not registered.
     */
    @Test
    public void setupPathTest8() {
        build4RouterTopo(true, false, false, false, 0);
        List<Constraint> constraints = new LinkedList<Constraint>();
        BandwidthConstraint bwConstraint = new BandwidthConstraint(Bandwidth.bps(10.0));
        CostConstraint costConstraint = new CostConstraint(TE_COST);

        constraints.add(costConstraint);
        constraints.add(bwConstraint);

        boolean result = pceManager.setupPath(D1.deviceId(), D2.deviceId(), "T123", constraints,
                WITH_SIGNALLING, null);
        assertThat(result, is(false));
    }

    /**
     * Tests path failure as bandwidth requested is more than registered.
     */
    @Test
    public void setupPathTest9() {
        build4RouterTopo(false, false, false, false, 5);
        List<Constraint> constraints = new LinkedList<Constraint>();
        BandwidthConstraint bwConstraint = new BandwidthConstraint(Bandwidth.bps(10.0));
        CostConstraint costConstraint = new CostConstraint(TE_COST);

        constraints.add(costConstraint);
        constraints.add(bwConstraint);

        boolean result = pceManager.setupPath(D1.deviceId(), D2.deviceId(), "T123",
                constraints, WITH_SIGNALLING, null);
        assertThat(result, is(false));
    }

    /**
     * Tests path setup failure(without signalling). Label capability is not present.
     */
    @Test
    public void setupPathTest10() {
        build4RouterTopo(false, false, false, false, 0);
        List<Constraint> constraints = new LinkedList<Constraint>();
        CostConstraint costConstraint = new CostConstraint(TE_COST);
        constraints.add(costConstraint);

        boolean result = pceManager.setupPath(D1.deviceId(), D2.deviceId(), "T123", constraints,
             SR_WITHOUT_SIGNALLING, null);
        assertThat(result, is(false));
    }

    /**
     * Tests path setup without signalling and with bandwidth reservation.
     */
    @Test
    public void setupPathTest12() {
        build4RouterTopo(false, true, true, true, 15);
        List<Constraint> constraints = new LinkedList<Constraint>();
        BandwidthConstraint bwConstraint = new BandwidthConstraint(Bandwidth.bps(10.0));
        CostConstraint costConstraint = new CostConstraint(TE_COST);

        constraints.add(costConstraint);
        constraints.add(bwConstraint);

        boolean result = pceManager.setupPath(D1.deviceId(), D2.deviceId(), "T123",
                constraints, SR_WITHOUT_SIGNALLING, null);
        assertThat(result, is(true));
    }

    /**
     * Tests path setup without cost/bandwidth constraints.
     */
    @Test
    public void setupPathTest13() {
        build4RouterTopo(false, false, false, false, 0);

        boolean result = pceManager.setupPath(D1.deviceId(), D2.deviceId(), "T123", null, WITH_SIGNALLING, null);
        assertThat(result, is(true));
    }

    /**
     * Tests path setup with explicit path with loose node D2.
     */
   @Test
    public void setupPathTest14() {
        build4RouterTopo(false, false, false, false, 0);

        List<ExplicitPathInfo> explicitPathInfoList = Lists.newLinkedList();
        ExplicitPathInfo obj = new ExplicitPathInfo(ExplicitPathInfo.Type.LOOSE, D2.deviceId());
        explicitPathInfoList.add(obj);

        boolean result = pceManager.setupPath(D1.deviceId(), D4.deviceId(), "T123", null, WITH_SIGNALLING,
                explicitPathInfoList);

        Tunnel tunnel = pceManager.queryAllPath().iterator().next();
        List<Link> links = new LinkedList<>();
        links.add(link1);
        links.add(link2);

        assertThat(result, is(true));
        assertThat(tunnel.path().links().equals(links), is(true));
    }

    /**
     * Tests path setup with explicit path with loose node D3.
     */
   @Test
   public void setupPathTest15() {
       build4RouterTopo(false, false, false, false, 0);

       List<ExplicitPathInfo> explicitPathInfoList = Lists.newLinkedList();
       ExplicitPathInfo obj = new ExplicitPathInfo(ExplicitPathInfo.Type.LOOSE, D3.deviceId());
       explicitPathInfoList.add(obj);

       boolean result = pceManager.setupPath(D1.deviceId(), D5.deviceId(), "T123", null, WITH_SIGNALLING,
               explicitPathInfoList);

       Tunnel tunnel = pceManager.queryAllPath().iterator().next();
       List<Link> links = new LinkedList<>();
       links.add(link3);
       links.add(link4);
       links.add(link6);

       assertThat(result, is(true));
       assertThat(tunnel.path().links().equals(links), is(true));
   }

    /**
     * Tests path setup with explicit path with loose node D4 , D3 - path fails.
     */
   @Test
   public void setupPathTest16() {
       build4RouterTopo(false, false, false, false, 0);

       List<ExplicitPathInfo> explicitPathInfoList = Lists.newLinkedList();
       ExplicitPathInfo obj = new ExplicitPathInfo(ExplicitPathInfo.Type.LOOSE, D4.deviceId());
       explicitPathInfoList.add(obj);
       obj = new ExplicitPathInfo(ExplicitPathInfo.Type.LOOSE, D3.deviceId());
       explicitPathInfoList.add(obj);

       boolean result = pceManager.setupPath(D1.deviceId(), D5.deviceId(), "T123", null, WITH_SIGNALLING,
               explicitPathInfoList);

       assertThat(result, is(false));
   }

    /**
     * Tests path setup with explicit path with strict node D2 - without reacble to src - path fails.
     */
   @Test
   public void setupPathTest17() {
       build4RouterTopo(false, false, false, false, 0);

       List<ExplicitPathInfo> explicitPathInfoList = Lists.newLinkedList();
       ExplicitPathInfo obj = new ExplicitPathInfo(ExplicitPathInfo.Type.STRICT, D2.deviceId());
       explicitPathInfoList.add(obj);

       boolean result = pceManager.setupPath(D1.deviceId(), D5.deviceId(), "T123", null, WITH_SIGNALLING,
               explicitPathInfoList);

       assertThat(result, is(false));
   }

    /**
     * Tests path setup with explicit path with loose node D2, strict D2.
     */
   @Test
   public void setupPathTest18() {
       build4RouterTopo(false, false, false, false, 0);

       List<ExplicitPathInfo> explicitPathInfoList = Lists.newLinkedList();
       ExplicitPathInfo obj = new ExplicitPathInfo(ExplicitPathInfo.Type.LOOSE, D2.deviceId());
       explicitPathInfoList.add(obj);
       obj = new ExplicitPathInfo(ExplicitPathInfo.Type.STRICT, D2.deviceId());
       explicitPathInfoList.add(obj);

       boolean result = pceManager.setupPath(D1.deviceId(), D5.deviceId(), "T123", null, WITH_SIGNALLING,
               explicitPathInfoList);

       Tunnel tunnel = pceManager.queryAllPath().iterator().next();
       List<Link> links = new LinkedList<>();
       links.add(link1);
       links.add(link5);

       assertThat(result, is(true));
       assertThat(tunnel.path().links().equals(links), is(true));
   }

    /**
     * Tests path setup with explicit path with loose D1-D2, strict D2.
     */
   @Test
   public void setupPathTest19() {
       build4RouterTopo(false, false, false, false, 0);

       List<ExplicitPathInfo> explicitPathInfoList = Lists.newLinkedList();
       ExplicitPathInfo obj = new ExplicitPathInfo(ExplicitPathInfo.Type.LOOSE, link1);
       explicitPathInfoList.add(obj);
       obj = new ExplicitPathInfo(ExplicitPathInfo.Type.STRICT, D2.deviceId());
       explicitPathInfoList.add(obj);

       boolean result = pceManager.setupPath(D1.deviceId(), D5.deviceId(), "T123", null, WITH_SIGNALLING,
               explicitPathInfoList);

       Tunnel tunnel = pceManager.queryAllPath().iterator().next();
       List<Link> links = new LinkedList<>();
       links.add(link1);
       links.add(link5);

       assertThat(result, is(true));
       assertThat(tunnel.path().links().equals(links), is(true));
   }

    /**
     * Tests path update with increase in bandwidth.
     */
    @Test
    public void updatePathTest1() {
        build4RouterTopo(false, true, true, true, 100);

        // Setup tunnel.
        List<Constraint> constraints = new LinkedList<>();
        BandwidthConstraint bwConstraint = new BandwidthConstraint(Bandwidth.bps(60.0));
        constraints.add(bwConstraint);
        CostConstraint costConstraint = new CostConstraint(TE_COST);
        constraints.add(costConstraint);

        boolean result = pceManager.setupPath(D1.deviceId(), D4.deviceId(), "T123",
                constraints, WITH_SIGNALLING, null);
        assertThat(result, is(true));

        // Change constraint and update it.
        constraints = new LinkedList<>();
        bwConstraint = new BandwidthConstraint(Bandwidth.bps(50.0));
        constraints.add(bwConstraint);
        constraints.add(costConstraint);

        Collection<Tunnel> tunnels = (Collection<Tunnel>) pceManager.queryAllPath();
        assertThat(tunnels.size(), is(1));

        Tunnel tunnel = tunnels.iterator().next();

        // Stimulate the effect of LSP ids from protocol msg.
        tunnelService.updateTunnelWithLspIds(tunnel, "123", "1", State.ACTIVE);

        result = pceManager.updatePath(tunnel.tunnelId(), constraints);
        assertThat(result, is(true));

        tunnels = (Collection<Tunnel>) pceManager.queryAllPath();
        assertThat(tunnels.size(), is(2));
    }

    /**
     * Tests path update with decrease in bandwidth.
     */
    @Test
    public void updatePathTest2() {
        build4RouterTopo(false, true, true, true, 100);

        // Setup tunnel.
        List<Constraint> constraints = new LinkedList<Constraint>();
        BandwidthConstraint bwConstraint = new BandwidthConstraint(Bandwidth.bps(60.0));
        constraints.add(bwConstraint);
        CostConstraint costConstraint = new CostConstraint(TE_COST);
        constraints.add(costConstraint);

        boolean result = pceManager.setupPath(D1.deviceId(), D2.deviceId(), "T123",
                constraints, SR_WITHOUT_SIGNALLING, null);
        assertThat(result, is(true));

        // Change constraint and update it.
        constraints.remove(bwConstraint);
        bwConstraint = new BandwidthConstraint(Bandwidth.bps(70.0));
        constraints.add(bwConstraint);

        Collection<Tunnel> tunnels = (Collection<Tunnel>) pceManager.queryAllPath();
        assertThat(tunnels.size(), is(1));

        for (Tunnel tunnel : tunnels) {
            result = pceManager.updatePath(tunnel.tunnelId(), constraints);
            assertThat(result, is(true));
        }

        tunnels = (Collection<Tunnel>) pceManager.queryAllPath();
        assertThat(tunnels.size(), is(2));
    }

    /**
     * Tests path update without cost/bandwidth constraints.
     */
    @Test
    public void updatePathTest3() {
        build4RouterTopo(false, true, true, true, 100);

        // Setup tunnel.
        List<Constraint> constraints = new LinkedList<Constraint>();
        CostConstraint costConstraint = new CostConstraint(TE_COST);
        constraints.add(costConstraint);
        boolean result = pceManager.setupPath(D1.deviceId(), D2.deviceId(), "T123",
                constraints, WITH_SIGNALLING, null);
        assertThat(result, is(true));

        Collection<Tunnel> tunnels = (Collection<Tunnel>) pceManager.queryAllPath();
        assertThat(tunnels.size(), is(1));

        for (Tunnel tunnel : tunnels) {
            result = pceManager.updatePath(tunnel.tunnelId(), null);
            assertThat(result, is(true));
        }

        Iterable<Tunnel> queryTunnelResult = pceManager.queryAllPath();
        assertThat((int) queryTunnelResult.spliterator().getExactSizeIfKnown(), is(2));
    }

    /**
     * Tests path update without cost/bandwidth constraints and with explicit path object.
     */
    @Test
    public void updatePathTest4() {
        build4RouterTopo(false, true, true, true, 100);

        // Setup tunnel.
        List<Constraint> constraints = new LinkedList<>();
        BandwidthConstraint bwConstraint = new BandwidthConstraint(Bandwidth.bps(60.0));
        constraints.add(bwConstraint);
        CostConstraint costConstraint = new CostConstraint(TE_COST);
        constraints.add(costConstraint);

        List<ExplicitPathInfo> explicitPathInfoList = Lists.newLinkedList();
        ExplicitPathInfo obj = new ExplicitPathInfo(ExplicitPathInfo.Type.LOOSE, link1);
        explicitPathInfoList.add(obj);
        obj = new ExplicitPathInfo(ExplicitPathInfo.Type.STRICT, D2.deviceId());
        explicitPathInfoList.add(obj);

        boolean result = pceManager.setupPath(D1.deviceId(), D4.deviceId(), "T123",
                constraints, WITH_SIGNALLING, explicitPathInfoList);
        assertThat(result, is(true));

        // Change constraint and update it.
        constraints = new LinkedList<>();
        bwConstraint = new BandwidthConstraint(Bandwidth.bps(50.0));
        constraints.add(bwConstraint);
        constraints.add(costConstraint);

        Collection<Tunnel> tunnels = (Collection<Tunnel>) pceManager.queryAllPath();
        assertThat(tunnels.size(), is(1));

        Tunnel tunnel = tunnels.iterator().next();

        // Stimulate the effect of LSP ids from protocol msg.
        tunnelService.updateTunnelWithLspIds(tunnel, "123", "1", State.ACTIVE);

        result = pceManager.updatePath(tunnel.tunnelId(), constraints);
        assertThat(result, is(true));

        tunnels = (Collection<Tunnel>) pceManager.queryAllPath();
        assertThat(tunnels.size(), is(2));
    }

    /**
     * Tests path release.
     */
    @Test
    public void releasePathTest1() {
        build4RouterTopo(false, false, false, false, 5);
        List<Constraint> constraints = new LinkedList<Constraint>();
        CostConstraint costConstraint = new CostConstraint(TE_COST);
        BandwidthConstraint bwConst = new BandwidthConstraint(Bandwidth.bps(3));
        constraints.add(bwConst);
        constraints.add(costConstraint);

        List<ExplicitPathInfo> explicitPathInfoList = Lists.newLinkedList();
        ExplicitPathInfo obj = new ExplicitPathInfo(ExplicitPathInfo.Type.LOOSE, link1);
        explicitPathInfoList.add(obj);
        obj = new ExplicitPathInfo(ExplicitPathInfo.Type.STRICT, D2.deviceId());
        explicitPathInfoList.add(obj);

        pceManager.setupPath(D1.deviceId(), D2.deviceId(), "T123", constraints, WITH_SIGNALLING, explicitPathInfoList);

        Collection<Tunnel> tunnels = (Collection<Tunnel>) pceManager.queryAllPath();
        assertThat(tunnels.size(), is(1));
        boolean result;
        for (Tunnel tunnel : tunnels) {
            result = pceManager.releasePath(tunnel.tunnelId());
            assertThat(result, is(true));
        }
        tunnels = (Collection<Tunnel>) pceManager.queryAllPath();
        assertThat(tunnels.size(), is(0));
    }

    /**
     * Tests path release failure.
     */
    @Test
    public void releasePathTest2() {
        build4RouterTopo(false, false, false, false, 5);
        List<Constraint> constraints = new LinkedList<Constraint>();
        CostConstraint costConstraint = new CostConstraint(TE_COST);
        constraints.add(costConstraint);

        pceManager.setupPath(D1.deviceId(), D2.deviceId(), "T123", constraints, WITH_SIGNALLING, null);

        Collection<Tunnel> tunnels = (Collection<Tunnel>) pceManager.queryAllPath();
        assertThat(tunnels.size(), is(1));

        // Random tunnel id.
        boolean result = pceManager.releasePath(TunnelId.valueOf("111"));
        assertThat(result, is(false));

        tunnels = (Collection<Tunnel>) pceManager.queryAllPath();
        assertThat(tunnels.size(), is(1));
    }

    /**
     * Tests path release failure.
     */
    @Test
    public void releasePathTest3() {
        build4RouterTopo(false, false, false, false, 5);
        List<Constraint> constraints = new LinkedList<Constraint>();
        CostConstraint costConstraint = new CostConstraint(TE_COST);
        BandwidthConstraint bwConst = new BandwidthConstraint(Bandwidth.bps(3));
        constraints.add(bwConst);
        constraints.add(costConstraint);

        pceManager.setupPath(D1.deviceId(), D2.deviceId(), "T123", constraints, WITH_SIGNALLING, null);

        Collection<Tunnel> tunnels = (Collection<Tunnel>) pceManager.queryAllPath();
        assertThat(tunnels.size(), is(1));
        boolean result;
        for (Tunnel tunnel : tunnels) {
            result = pceManager.releasePath(tunnel.tunnelId());
            assertThat(result, is(true));
        }
        tunnels = (Collection<Tunnel>) pceManager.queryAllPath();
        assertThat(tunnels.size(), is(0));
    }

    /**
     * Tests tunnel events added and removed.
     */
    @Test
    public void tunnelEventTest1() {
        build4RouterTopo(false, true, true, true, 15);
        List<Constraint> constraints = new LinkedList<Constraint>();
        BandwidthConstraint bwConstraint = new BandwidthConstraint(Bandwidth.bps(10.0));
        CostConstraint costConstraint = new CostConstraint(TE_COST);

        constraints.add(costConstraint);
        constraints.add(bwConstraint);

        pceManager.setupPath(D1.deviceId(), D2.deviceId(), "T1", constraints, SR_WITHOUT_SIGNALLING, null);
        assertThat(pceStore.getTunnelInfoCount(), is(1));

        Collection<Tunnel> tunnels = (Collection<Tunnel>) pceManager.queryAllPath();

        for (Tunnel tunnel : tunnels) {
            TunnelEvent event = new TunnelEvent(TunnelEvent.Type.TUNNEL_ADDED, tunnel);
            tunnelListener.event(event);

            pceManager.releasePath(tunnel.tunnelId());

            event = new TunnelEvent(TunnelEvent.Type.TUNNEL_REMOVED, tunnel);
            tunnelListener.event(event);
        }

        assertThat(pceStore.getTunnelInfoCount(), is(0));
    }

    /**
     * Tests label allocation/removal in CR case based on tunnel event.
     */
    @Test
    public void tunnelEventTest2() {
        build4RouterTopo(false, true, true, true, 15);
        List<Constraint> constraints = new LinkedList<Constraint>();
        BandwidthConstraint bwConstraint = new BandwidthConstraint(Bandwidth.bps(10.0));
        CostConstraint costConstraint = new CostConstraint(TE_COST);

        constraints.add(costConstraint);
        constraints.add(bwConstraint);

        pceManager.setupPath(D1.deviceId(), D2.deviceId(), "T2", constraints,
                WITHOUT_SIGNALLING_AND_WITHOUT_SR, null);
        assertThat(pceStore.getTunnelInfoCount(), is(1));

        TunnelEvent event;
        Collection<Tunnel> tunnels = (Collection<Tunnel>) pceManager.queryAllPath();
        for (Tunnel tunnel : tunnels) {
            event = new TunnelEvent(TunnelEvent.Type.TUNNEL_ADDED, tunnel);
            tunnelListener.event(event);

            // Stimulate the effect of LSP ids from protocol msg.
            tunnelService.updateTunnelWithLspIds(tunnel, "123", "1", ESTABLISHED);
        }

        tunnels = (Collection<Tunnel>) pceManager.queryAllPath();
        for (Tunnel tunnel : tunnels) {
            event = new TunnelEvent(TunnelEvent.Type.TUNNEL_UPDATED, tunnel);
            tunnelListener.event(event);

            pceManager.releasePath(tunnel.tunnelId());

            event = new TunnelEvent(TunnelEvent.Type.TUNNEL_REMOVED, tunnel);
            tunnelListener.event(event);
        }

        assertThat(pceStore.getTunnelInfoCount(), is(0));
    }

    /**
     * Tests handling UNSTABLE state based on tunnel event.
     */
    @Test
    public void tunnelEventTest3() {
        build4RouterTopo(false, true, true, true, 15);
        List<Constraint> constraints = new LinkedList<Constraint>();
        BandwidthConstraint bwConstraint = new BandwidthConstraint(Bandwidth.bps(10.0));
        CostConstraint costConstraint = new CostConstraint(TE_COST);

        constraints.add(costConstraint);
        constraints.add(bwConstraint);

        pceManager.setupPath(D1.deviceId(), D2.deviceId(), "T2", constraints,
                WITHOUT_SIGNALLING_AND_WITHOUT_SR, null);
        assertThat(pceStore.getTunnelInfoCount(), is(1));
        assertThat(pceStore.getFailedPathInfoCount(), is(0));

        TunnelEvent event;
        Collection<Tunnel> tunnels = (Collection<Tunnel>) pceManager.queryAllPath();
        for (Tunnel tunnel : tunnels) {
            event = new TunnelEvent(TunnelEvent.Type.TUNNEL_ADDED, tunnel);
            tunnelListener.event(event);

            // Stimulate the effect of LSP ids from protocol msg.
            tunnelService.updateTunnelWithLspIds(tunnel, "123", "1", UNSTABLE);
        }

        tunnels = (Collection<Tunnel>) pceManager.queryAllPath();
        for (Tunnel tunnel : tunnels) {
            event = new TunnelEvent(TunnelEvent.Type.TUNNEL_UPDATED, tunnel);
            tunnelListener.event(event);
        }
        assertThat(pceStore.getTunnelInfoCount(), is(1));
        assertThat(pceStore.getFailedPathInfoCount(), is(1));
    }

    /**
     * Tests resilency when L2 link is down.
     */
    @Test
    public void resilencyTest1() {
        build4RouterTopo(true, false, false, false, 10);


        List<Constraint> constraints = new LinkedList<Constraint>();
        CostConstraint costConstraint = new CostConstraint(COST);
        constraints.add(costConstraint);
        BandwidthConstraint localBwConst = new BandwidthConstraint(Bandwidth.bps(10));
        constraints.add(localBwConst);

        //Setup the path , tunnel created
        boolean result = pceManager.setupPath(D1.deviceId(), D4.deviceId(), "T123",
                constraints, WITH_SIGNALLING, null);
        assertThat(result, is(true));
        assertThat(pceStore.getTunnelInfoCount(), is(1));
        assertThat(pceStore.getFailedPathInfoCount(), is(0));

        List<Event> reasons = new LinkedList<>();
        final LinkEvent linkEvent = new LinkEvent(LinkEvent.Type.LINK_REMOVED, link2);
        reasons.add(linkEvent);
        final TopologyEvent event = new TopologyEvent(
                TopologyEvent.Type.TOPOLOGY_CHANGED,
                topology,
                reasons);

        //Change Topology : remove link2
        Set<TopologyEdge> tempEdges = new HashSet<>();
        tempEdges.add(new DefaultTopologyEdge(D2, D4, link2));
        topologyService.changeInTopology(getGraph(null,  tempEdges));
        listener.event(event);

        List<Link> links = new LinkedList<>();
        links.add(link3);
        links.add(link4);

        //Path is D1-D3-D4
        assertThat(pathService.paths().iterator().next().links(), is(links));
        assertThat(pathService.paths().iterator().next().cost(), is((double) 180));
    }

    /**
     * Tests resilency when L2 and L4 link is down.
     */
    @Test
    public void resilencyTest2() {
        build4RouterTopo(true, false, false, false, 10);

        List<Constraint> constraints = new LinkedList<Constraint>();
        CostConstraint costConstraint = new CostConstraint(COST);
        constraints.add(costConstraint);
        BandwidthConstraint localBwConst = new BandwidthConstraint(Bandwidth.bps(10));
        constraints.add(localBwConst);

        //Setup the path , tunnel created
        boolean result = pceManager.setupPath(D1.deviceId(), D4.deviceId(), "T123",
                constraints, WITH_SIGNALLING, null);
        assertThat(result, is(true));

        List<Event> reasons = new LinkedList<>();
        LinkEvent linkEvent = new LinkEvent(LinkEvent.Type.LINK_REMOVED, link2);
        reasons.add(linkEvent);
        linkEvent = new LinkEvent(LinkEvent.Type.LINK_REMOVED, link4);
        reasons.add(linkEvent);
        final TopologyEvent event = new TopologyEvent(
                TopologyEvent.Type.TOPOLOGY_CHANGED,
                topology,
                reasons);

        //Change Topology : remove link2 and link4
        Set<TopologyEdge> tempEdges = new HashSet<>();
        tempEdges.add(new DefaultTopologyEdge(D2, D4, link2));
        tempEdges.add(new DefaultTopologyEdge(D3, D4, link4));
        topologyService.changeInTopology(getGraph(null,  tempEdges));
        listener.event(event);

        //No Path
        assertThat(pathService.paths().size(), is(0));
    }

    /**
     * Tests resilency when D2 device is down.
     */
    @Test
    public void resilencyTest3() {
        build4RouterTopo(true, false, false, false, 10);

        List<Constraint> constraints = new LinkedList<Constraint>();
        CostConstraint costConstraint = new CostConstraint(COST);
        constraints.add(costConstraint);
        BandwidthConstraint localBwConst = new BandwidthConstraint(Bandwidth.bps(10));
        constraints.add(localBwConst);

        //Setup the path , tunnel created
        boolean result = pceManager.setupPath(D1.deviceId(), D4.deviceId(), "T123",
                constraints, WITH_SIGNALLING, null);
        assertThat(result, is(true));

        List<Event> reasons = new LinkedList<>();
        LinkEvent linkEvent = new LinkEvent(LinkEvent.Type.LINK_REMOVED, link2);
        reasons.add(linkEvent);
        linkEvent = new LinkEvent(LinkEvent.Type.LINK_REMOVED, link1);
        reasons.add(linkEvent);
        final TopologyEvent event = new TopologyEvent(
                TopologyEvent.Type.TOPOLOGY_CHANGED,
                topology,
                reasons);

        //Change Topology : remove link2 and link1
        Set<TopologyEdge> tempEdges = new HashSet<>();
        tempEdges.add(new DefaultTopologyEdge(D2, D4, link2));
        tempEdges.add(new DefaultTopologyEdge(D1, D2, link1));
        topologyService.changeInTopology(getGraph(null,  tempEdges));
        listener.event(event);

        List<Link> links = new LinkedList<>();
        links.add(link3);
        links.add(link4);

        //Path is D1-D3-D4
        assertThat(pathService.paths().iterator().next().links(), is(links));
        assertThat(pathService.paths().iterator().next().cost(), is((double) 180));
    }

    /**
     * Tests resilency when ingress device is down.
     */
    @Test
    public void resilencyTest4() {
        build4RouterTopo(true, false, false, false, 10);

        List<Constraint> constraints = new LinkedList<Constraint>();
        CostConstraint costConstraint = new CostConstraint(COST);
        constraints.add(costConstraint);
        BandwidthConstraint localBwConst = new BandwidthConstraint(Bandwidth.bps(10));
        constraints.add(localBwConst);

        //Setup the path , tunnel created
        boolean result = pceManager.setupPath(D1.deviceId(), D4.deviceId(), "T123",
                constraints, WITH_SIGNALLING, null);
        assertThat(result, is(true));

        List<Event> reasons = new LinkedList<>();
        LinkEvent linkEvent = new LinkEvent(LinkEvent.Type.LINK_REMOVED, link3);
        reasons.add(linkEvent);
        linkEvent = new LinkEvent(LinkEvent.Type.LINK_REMOVED, link1);
        reasons.add(linkEvent);
        final TopologyEvent event = new TopologyEvent(
                TopologyEvent.Type.TOPOLOGY_CHANGED,
                topology,
                reasons);

        //Change Topology : remove link2 and link1
        Set<TopologyEdge> tempEdges = new HashSet<>();
        tempEdges.add(new DefaultTopologyEdge(D1, D3, link3));
        tempEdges.add(new DefaultTopologyEdge(D1, D2, link1));
        topologyService.changeInTopology(getGraph(null,  tempEdges));
        listener.event(event);

        //No path
        assertThat(pathService.paths().size(), is(0));
    }

    /**
     * Tests resilency when D2 and D3 devices are down.
     */
    @Test
    public void resilencyTest5() {
        build4RouterTopo(true, false, false, false, 10);

        List<Constraint> constraints = new LinkedList<Constraint>();
        CostConstraint costConstraint = new CostConstraint(COST);
        constraints.add(costConstraint);
        BandwidthConstraint localBwConst = new BandwidthConstraint(Bandwidth.bps(10));
        constraints.add(localBwConst);

        //Setup the path , tunnel created
        boolean result = pceManager.setupPath(D1.deviceId(), D4.deviceId(), "T123",
                constraints, WITH_SIGNALLING, null);
        assertThat(result, is(true));

        List<Event> reasons = new LinkedList<>();
        LinkEvent linkEvent = new LinkEvent(LinkEvent.Type.LINK_REMOVED, link2);
        reasons.add(linkEvent);
        linkEvent = new LinkEvent(LinkEvent.Type.LINK_REMOVED, link1);
        reasons.add(linkEvent);
        linkEvent = new LinkEvent(LinkEvent.Type.LINK_REMOVED, link3);
        reasons.add(linkEvent);
        linkEvent = new LinkEvent(LinkEvent.Type.LINK_REMOVED, link4);
        reasons.add(linkEvent);

        final TopologyEvent event = new TopologyEvent(
                TopologyEvent.Type.TOPOLOGY_CHANGED,
                topology,
                reasons);

        //Change Topology : remove device2, device3 and all links
        Set<TopologyEdge> tempEdges = new HashSet<>();
        tempEdges.add(new DefaultTopologyEdge(D1, D2, link1));
        tempEdges.add(new DefaultTopologyEdge(D2, D4, link2));
        tempEdges.add(new DefaultTopologyEdge(D1, D3, link3));
        tempEdges.add(new DefaultTopologyEdge(D3, D4, link4));
        Set<TopologyVertex> tempVertexes = new HashSet<>();
        tempVertexes.add(D2);
        tempVertexes.add(D3);
        topologyService.changeInTopology(getGraph(tempVertexes, tempEdges));
        listener.event(event);

        //No path
        assertThat(pathService.paths().size(), is(0));
    }

    /**
     * Tests resilency when egress device is down.
     */
    @Test
    public void resilencyTest6() {
        build4RouterTopo(true, false, false, false, 10);

        List<Constraint> constraints = new LinkedList<Constraint>();
        CostConstraint costConstraint = new CostConstraint(COST);
        constraints.add(costConstraint);
        BandwidthConstraint localBwConst = new BandwidthConstraint(Bandwidth.bps(10));
        constraints.add(localBwConst);

        //Setup the path , tunnel created
        boolean result = pceManager.setupPath(D1.deviceId(), D4.deviceId(), "T123",
                constraints, WITH_SIGNALLING, null);
        assertThat(result, is(true));

        List<Event> reasons = new LinkedList<>();
        LinkEvent linkEvent = new LinkEvent(LinkEvent.Type.LINK_REMOVED, link2);
        reasons.add(linkEvent);
        linkEvent = new LinkEvent(LinkEvent.Type.LINK_REMOVED, link4);
        reasons.add(linkEvent);

        final TopologyEvent event = new TopologyEvent(
                TopologyEvent.Type.TOPOLOGY_CHANGED,
                topology,
                reasons);

        //Change Topology : remove device4 , link2 and link4
        Set<TopologyEdge> tempEdges = new HashSet<>();
        tempEdges.add(new DefaultTopologyEdge(D2, D4, link2));
        tempEdges.add(new DefaultTopologyEdge(D3, D4, link4));
        Set<TopologyVertex> tempVertexes = new HashSet<>();
        tempVertexes.add(D4);
        topologyService.changeInTopology(getGraph(tempVertexes, tempEdges));
        listener.event(event);

        //No path
        assertThat(pathService.paths().size(), is(0));
    }

    /**
     * Tests resilency when egress device is down.
     */
    @Test
    public void resilencyTest7() {
        build4RouterTopo(true, false, false, false, 10);

        List<Constraint> constraints = new LinkedList<Constraint>();
        CostConstraint costConstraint = new CostConstraint(COST);
        constraints.add(costConstraint);
        BandwidthConstraint localBwConst = new BandwidthConstraint(Bandwidth.bps(10));
        constraints.add(localBwConst);

        //Setup the path , tunnel created
        boolean result = pceManager.setupPath(D1.deviceId(), D4.deviceId(), "T123",
                constraints, WITH_SIGNALLING, null);
        assertThat(result, is(true));

        List<Event> reasons = new LinkedList<>();
        LinkEvent linkEvent = new LinkEvent(LinkEvent.Type.LINK_REMOVED, link2);
        reasons.add(linkEvent);
        linkEvent = new LinkEvent(LinkEvent.Type.LINK_REMOVED, link4);
        reasons.add(linkEvent);

        final TopologyEvent event = new TopologyEvent(
                TopologyEvent.Type.TOPOLOGY_CHANGED,
                topology,
                reasons);

        //Change Topology : remove device4 , link2 and link4
        Set<TopologyEdge> tempEdges = new HashSet<>();
        tempEdges.add(new DefaultTopologyEdge(D2, D4, link2));
        tempEdges.add(new DefaultTopologyEdge(D3, D4, link4));
        Set<TopologyVertex> tempVertexes = new HashSet<>();
        tempVertexes.add(D4);
        topologyService.changeInTopology(getGraph(tempVertexes, tempEdges));
        listener.event(event);

        //No path
        assertThat(pathService.paths().size(), is(0));
    }

    /**
     * Tests resilency when D2 device is suspended.
     */
    @Test
    public void resilencyTest8() {
        build4RouterTopo(true, false, false, false, 10);

        List<Constraint> constraints = new LinkedList<Constraint>();
        CostConstraint costConstraint = new CostConstraint(COST);
        constraints.add(costConstraint);
        BandwidthConstraint localBwConst = new BandwidthConstraint(Bandwidth.bps(10));
        constraints.add(localBwConst);

        //Setup the path , tunnel created
        boolean result = pceManager.setupPath(D1.deviceId(), D4.deviceId(), "T123",
                constraints, WITH_SIGNALLING, null);
        assertThat(result, is(true));

        List<Event> reasons = new LinkedList<>();
        LinkEvent linkEvent = new LinkEvent(LinkEvent.Type.LINK_REMOVED, link1);
        reasons.add(linkEvent);
        linkEvent = new LinkEvent(LinkEvent.Type.LINK_REMOVED, link2);
        reasons.add(linkEvent);

        final TopologyEvent event = new TopologyEvent(
                TopologyEvent.Type.TOPOLOGY_CHANGED,
                topology,
                reasons);

        //Change Topology : remove device2 , link1 and link2
        Set<TopologyEdge> tempEdges = new HashSet<>();
        tempEdges.add(new DefaultTopologyEdge(D1, D2, link1));
        tempEdges.add(new DefaultTopologyEdge(D2, D4, link2));
        Set<TopologyVertex> tempVertexes = new HashSet<>();
        tempVertexes.add(D2);
        topologyService.changeInTopology(getGraph(tempVertexes, tempEdges));
        listener.event(event);

        List<Link> links = new LinkedList<>();
        links.add(link3);
        links.add(link4);

        //Path is D1-D3-D4
        assertThat(pathService.paths().iterator().next().links(), is(links));
        assertThat(pathService.paths().iterator().next().cost(), is((double) 180));
    }

    /**
     * Tests resilency when D2 device availability is changed.
     */
    @Test
    public void resilencyTest11() {
        build4RouterTopo(true, false, false, false, 10);

        List<Constraint> constraints = new LinkedList<Constraint>();
        CostConstraint costConstraint = new CostConstraint(COST);
        constraints.add(costConstraint);
        BandwidthConstraint localBwConst = new BandwidthConstraint(Bandwidth.bps(10));
        constraints.add(localBwConst);

        //Setup the path , tunnel created
        boolean result = pceManager.setupPath(D1.deviceId(), D4.deviceId(), "T123",
                constraints, WITH_SIGNALLING, null);
        assertThat(result, is(true));

        List<Event> reasons = new LinkedList<>();
        LinkEvent linkEvent = new LinkEvent(LinkEvent.Type.LINK_REMOVED, link1);
        reasons.add(linkEvent);
        linkEvent = new LinkEvent(LinkEvent.Type.LINK_REMOVED, link2);
        reasons.add(linkEvent);

        final TopologyEvent event = new TopologyEvent(
                TopologyEvent.Type.TOPOLOGY_CHANGED,
                topology,
                reasons);

        //Change Topology : remove device2 , link1 and link2
        Set<TopologyEdge> tempEdges = new HashSet<>();
        tempEdges.add(new DefaultTopologyEdge(D1, D2, link1));
        tempEdges.add(new DefaultTopologyEdge(D2, D4, link2));
        Set<TopologyVertex> tempVertexes = new HashSet<>();
        tempVertexes.add(D2);
        topologyService.changeInTopology(getGraph(tempVertexes, tempEdges));
        listener.event(event);

        List<Link> links = new LinkedList<>();
        links.add(link3);
        links.add(link4);

        //Path is D1-D3-D4
        assertThat(pathService.paths().iterator().next().links(), is(links));
        assertThat(pathService.paths().iterator().next().cost(), is((double) 180));
    }

    /**
     * Tests resilency when link2 availability is changed.
     */
    @Test
    public void resilencyTest12() {
        build4RouterTopo(true, false, false, false, 10);

        List<Constraint> constraints = new LinkedList<Constraint>();
        CostConstraint costConstraint = new CostConstraint(COST);
        constraints.add(costConstraint);
        BandwidthConstraint localBwConst = new BandwidthConstraint(Bandwidth.bps(10));
        constraints.add(localBwConst);

        List<ExplicitPathInfo> explicitPathInfoList = Lists.newLinkedList();
        ExplicitPathInfo obj = new ExplicitPathInfo(ExplicitPathInfo.Type.LOOSE, link1);
        explicitPathInfoList.add(obj);
        obj = new ExplicitPathInfo(ExplicitPathInfo.Type.STRICT, D2.deviceId());
        explicitPathInfoList.add(obj);

        //Setup the path , tunnel created
        boolean result = pceManager.setupPath(D1.deviceId(), D4.deviceId(), "T123",
                constraints, WITH_SIGNALLING, explicitPathInfoList);
        assertThat(result, is(true));
        assertThat(pceStore.getTunnelInfoCount(), is(1));
        assertThat(pceStore.getFailedPathInfoCount(), is(0));

        List<Event> reasons = new LinkedList<>();
        final LinkEvent linkEvent = new LinkEvent(LinkEvent.Type.LINK_REMOVED, link2);
        reasons.add(linkEvent);
        final TopologyEvent event = new TopologyEvent(
                TopologyEvent.Type.TOPOLOGY_CHANGED,
                topology,
                reasons);

        //Change Topology : remove link2
        Set<TopologyEdge> tempEdges = new HashSet<>();
        tempEdges.add(new DefaultTopologyEdge(D2, D4, link2));
        topologyService.changeInTopology(getGraph(null,  tempEdges));
        listener.event(event);

        List<Link> links = new LinkedList<>();
        links.add(link3);
        links.add(link4);

        //Path fails - no alternate path
        assertThat(pathService.paths().iterator().hasNext(), is(false));
    }

    @After
    public void tearDown() {
        pceManager.deactivate();
        pceManager.pathService = null;
        pceManager.resourceService = null;
        pceManager.tunnelService = null;
        pceManager.coreService = null;
        pceManager.storageService = null;
        pceManager.deviceService = null;
        pceManager.netCfgService = null;
        pceManager.pceStore = null;
        pceManager.topologyService = null;
        pceManager.mastershipService = null;
        flowsDownloaded = 0;
    }

    private class MockTopologyService extends TopologyServiceAdapter {
        private void changeInTopology(TopologyGraph graphModified) {
            graph = graphModified;
        }

        @Override
        public Set<Path> getPaths(Topology topology, DeviceId src, DeviceId dst, LinkWeight weight) {
            DefaultTopologyVertex srcV = new DefaultTopologyVertex(src);
            DefaultTopologyVertex dstV = new DefaultTopologyVertex(dst);
            Set<TopologyVertex> vertices = graph.getVertexes();
            if (!vertices.contains(srcV) || !vertices.contains(dstV)) {
                // src or dst not part of the current graph
                return ImmutableSet.of();
            }

            GraphPathSearch.Result<TopologyVertex, TopologyEdge> result = PathComputationTest.graphSearch()
                    .search(graph, srcV, dstV, weight, ALL_PATHS);
            ImmutableSet.Builder<Path> builder = ImmutableSet.builder();
            for (org.onlab.graph.Path<TopologyVertex, TopologyEdge> path : result.paths()) {
                builder.add(PathComputationTest.networkPath(path));
            }
            return builder.build();
        }
    }

    private TopologyGraph getGraph(Set<TopologyVertex> removedVertex, Set<TopologyEdge> removedEdges) {
        if (removedVertex != null) {
            vertexes.remove(removedVertex);
            removedVertex.forEach(v ->
            {
                vertexes.remove(v);
            });
        }

        if (removedEdges != null) {
            removedEdges.forEach(e ->
            {
                edges.remove(e);
            });
        }

        return new DefaultTopologyGraph(vertexes, edges);
    }

    private class MockPathService extends PathServiceAdapter {
        Set<Path> computedPaths;
        @Override
        public Set<Path> getPaths(ElementId src, ElementId dst, LinkWeight weight) {
            // If either edge is null, bail with no paths.
            if (src == null || dst == null) {
                return ImmutableSet.of();
            }

            // Otherwise get all paths between the source and destination edge
            // devices.
            computedPaths = topologyService.getPaths(null, (DeviceId) src, (DeviceId) dst, weight);
            return computedPaths;
        }

        private Set<Path> paths() {
            return computedPaths;
        }
    }

    private class MockTunnelServiceAdapter extends TunnelServiceAdapter {
        private HashMap<TunnelId, Tunnel> tunnelIdAsKeyStore = new HashMap<TunnelId, Tunnel>();
        private int tunnelIdCounter = 0;

        @Override
        public TunnelId setupTunnel(ApplicationId producerId, ElementId srcElementId, Tunnel tunnel, Path path) {
            TunnelId tunnelId = TunnelId.valueOf(String.valueOf(++tunnelIdCounter));
            Tunnel tunnelToInsert = new DefaultTunnel(tunnel.providerId(), tunnel.src(), tunnel.dst(), tunnel.type(),
                                                      tunnel.state(), tunnel.groupId(), tunnelId, tunnel.tunnelName(),
                                                      path, tunnel.annotations());
            tunnelIdAsKeyStore.put(tunnelId, tunnelToInsert);
            return tunnelId;
        }

        @Override
        public void addListener(TunnelListener listener) {
            tunnelListener = listener;
        }

        /**
         * Stimulates the effect of receiving PLSP id and LSP id from protocol PCRpt msg.
         */
        public TunnelId updateTunnelWithLspIds(Tunnel tunnel, String pLspId, String localLspId, State state) {
            TunnelId tunnelId = tunnel.tunnelId();
            Builder annotationBuilder = DefaultAnnotations.builder();
            annotationBuilder.putAll(tunnel.annotations());

            // PCRpt in response to PCInitate msg will carry PLSP id allocated by PCC.
            if (tunnel.annotations().value(PLSP_ID) == null) {
                annotationBuilder.set(PLSP_ID, pLspId);
            }

            // Signalled LSPs will carry local LSP id allocated by signalling protocol(PCC).
            if (tunnel.annotations().value(LOCAL_LSP_ID) == null) {
                annotationBuilder.set(LOCAL_LSP_ID, localLspId);
            }
            SparseAnnotations annotations = annotationBuilder.build();
            tunnelIdAsKeyStore.remove(tunnelId, tunnel);

            Tunnel tunnelToInsert = new DefaultTunnel(tunnel.providerId(), tunnel.src(), tunnel.dst(), tunnel.type(),
                                                      state, tunnel.groupId(), tunnelId, tunnel.tunnelName(),
                                                      tunnel.path(), annotations);

            tunnelIdAsKeyStore.put(tunnelId, tunnelToInsert);

            return tunnelId;
        }

        @Override
        public boolean downTunnel(ApplicationId producerId, TunnelId tunnelId) {
            for (TunnelId tunnelIdKey : tunnelIdAsKeyStore.keySet()) {
                if (tunnelIdKey.equals(tunnelId)) {
                    tunnelIdAsKeyStore.remove(tunnelId);
                    return true;
                }
            }
            return false;
        }

        @Override
        public Tunnel queryTunnel(TunnelId tunnelId) {
            for (TunnelId tunnelIdKey : tunnelIdAsKeyStore.keySet()) {
                if (tunnelIdKey.equals(tunnelId)) {
                    return tunnelIdAsKeyStore.get(tunnelId);
                }
            }
            return null;
        }

        @Override
        public Collection<Tunnel> queryTunnel(TunnelEndPoint src, TunnelEndPoint dst) {
            Collection<Tunnel> result = new HashSet<Tunnel>();
            Tunnel tunnel = null;
            for (TunnelId tunnelId : tunnelIdAsKeyStore.keySet()) {
                tunnel = tunnelIdAsKeyStore.get(tunnelId);

                if ((null != tunnel) && (src.equals(tunnel.src())) && (dst.equals(tunnel.dst()))) {
                    result.add(tunnel);
                }
            }

            return result.size() == 0 ? Collections.emptySet() : ImmutableSet.copyOf(result);
        }

        @Override
        public Collection<Tunnel> queryTunnel(Tunnel.Type type) {
            Collection<Tunnel> result = new HashSet<Tunnel>();

            for (TunnelId tunnelId : tunnelIdAsKeyStore.keySet()) {
                result.add(tunnelIdAsKeyStore.get(tunnelId));
            }

            return result.size() == 0 ? Collections.emptySet() : ImmutableSet.copyOf(result);
        }

        @Override
        public Collection<Tunnel> queryAllTunnels() {
            Collection<Tunnel> result = new HashSet<Tunnel>();

            for (TunnelId tunnelId : tunnelIdAsKeyStore.keySet()) {
                result.add(tunnelIdAsKeyStore.get(tunnelId));
            }

            return result.size() == 0 ? Collections.emptySet() : ImmutableSet.copyOf(result);
        }

        @Override
        public Iterable<Tunnel> getTunnels(DeviceId deviceId) {
            List<Tunnel> tunnelList = new LinkedList<>();

            for (Tunnel t : tunnelIdAsKeyStore.values()) {
                for (Link l : t.path().links()) {
                    if (l.src().deviceId().equals(deviceId) || l.dst().deviceId().equals(deviceId)) {
                        tunnelList.add(t);
                        break;
                    }
                }
            }
            return tunnelList;
        }
    }

    public static class MockCoreService extends CoreServiceAdapter {

        @Override
        public ApplicationId registerApplication(String name) {
            return new DefaultApplicationId(1, name);
        }

        @Override
        public IdGenerator getIdGenerator(String topic) {
            return new IdGenerator() {
                private AtomicLong counter = new AtomicLong(0);

                @Override
                public long getNewId() {
                    return counter.getAndIncrement();
                }
            };
        }
    }

    private class MockDevice extends DefaultDevice {
        MockDevice(DeviceId id, Annotations annotations) {
            super(null, id, null, null, null, null, null, null, annotations);
        }
    }
}
