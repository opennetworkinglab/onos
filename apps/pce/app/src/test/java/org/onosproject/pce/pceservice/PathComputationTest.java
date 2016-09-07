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
package org.onosproject.pce.pceservice;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.graph.AbstractGraphPathSearch;
import org.onlab.graph.AdjacencyListsGraph;
import org.onlab.graph.DijkstraGraphSearch;
import org.onlab.graph.Graph;
import org.onlab.graph.GraphPathSearch;
import org.onlab.packet.ChassisId;
import org.onlab.util.Bandwidth;
import org.onlab.util.Tools;
import org.onosproject.net.AnnotationKeys;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DefaultDevice;
import org.onosproject.net.DefaultLink;
import org.onosproject.net.DefaultPath;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.net.Path;
import org.onosproject.net.PortNumber;
import org.onosproject.net.intent.Constraint;
import org.onosproject.net.intent.IntentId;
import org.onosproject.net.Device.Type;
import org.onosproject.net.config.Config;
import org.onosproject.net.config.ConfigApplyDelegate;
import org.onosproject.net.config.ConfigFactory;
import org.onosproject.net.config.NetworkConfigRegistryAdapter;
import org.onosproject.net.intent.constraint.BandwidthConstraint;
import org.onosproject.net.device.DeviceServiceAdapter;
import org.onosproject.net.resource.ContinuousResource;
import org.onosproject.net.resource.ContinuousResourceId;
import org.onosproject.net.resource.DiscreteResource;
import org.onosproject.net.resource.DiscreteResourceId;
import org.onosproject.net.resource.Resource;
import org.onosproject.net.resource.ResourceAllocation;
import org.onosproject.net.resource.ResourceConsumer;
import org.onosproject.net.resource.ResourceId;
import org.onosproject.net.resource.Resources;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.net.topology.DefaultTopologyEdge;
import org.onosproject.net.topology.DefaultTopologyVertex;
import org.onosproject.net.topology.LinkWeight;
import org.onosproject.net.topology.TopologyEdge;
import org.onosproject.net.topology.TopologyVertex;
import org.onosproject.pce.pceservice.constraint.CapabilityConstraint;
import org.onosproject.pce.pceservice.constraint.CostConstraint;
import org.onosproject.pce.pceservice.constraint.SharedBandwidthConstraint;
import org.onosproject.pcep.api.DeviceCapability;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.onlab.graph.GraphPathSearch.ALL_PATHS;
import static org.onosproject.core.CoreService.CORE_PROVIDER_ID;
import static com.google.common.collect.ImmutableSet.of;
import static org.onosproject.net.resource.Resources.continuous;
import static org.onosproject.net.Link.Type.DIRECT;
import static org.onosproject.net.Link.State.ACTIVE;
import static org.onosproject.net.DeviceId.deviceId;
import static org.onosproject.pce.pceservice.constraint.CostConstraint.Type.COST;
import static org.onosproject.pce.pceservice.constraint.CostConstraint.Type.TE_COST;

/**
 * Test for CSPF path computation.
 */
public class PathComputationTest {

    private final MockPathResourceService resourceService = new MockPathResourceService();
    private final MockDeviceService deviceService = new MockDeviceService();
    private final MockNetConfigRegistryAdapter netConfigRegistry = new MockNetConfigRegistryAdapter();
    private PceManager pceManager = new PceManager();
    public static ProviderId providerId = new ProviderId("pce", "foo");
    public static final String DEVICE1 = "D001";
    public static final String DEVICE2 = "D002";
    public static final String DEVICE3 = "D003";
    public static final String DEVICE4 = "D004";
    public static final String DEVICE5 = "D005";
    public static final String PCEPDEVICE1 = "PD001";
    public static final String PCEPDEVICE2 = "PD002";
    public static final String PCEPDEVICE3 = "PD003";
    public static final String PCEPDEVICE4 = "PD004";
    public static final TopologyVertex D1 = new DefaultTopologyVertex(DeviceId.deviceId("D001"));
    public static final TopologyVertex D2 = new DefaultTopologyVertex(DeviceId.deviceId("D002"));
    public static final TopologyVertex D3 = new DefaultTopologyVertex(DeviceId.deviceId("D003"));
    public static final TopologyVertex D4 = new DefaultTopologyVertex(DeviceId.deviceId("D004"));
    public static final TopologyVertex D5 = new DefaultTopologyVertex(DeviceId.deviceId("D005"));
    private static final String ANNOTATION_COST = "cost";
    private static final String ANNOTATION_TE_COST = "teCost";
    private static final String UNKNOWN = "unknown";
    public static final String LSRID = "lsrId";
    public static final String L3 = "L3";
    public static final String PCECC_CAPABILITY = "pceccCapability";
    public static final String SR_CAPABILITY = "srCapability";
    public static final String LABEL_STACK_CAPABILITY = "labelStackCapability";

    @Before
    public void startUp() {
        pceManager.resourceService = resourceService;
        pceManager.deviceService = deviceService;
        pceManager.netCfgService = netConfigRegistry;
    }

    /**
     * Selects path computation algorithm.
     *
     * @return graph path search algorithm
     */
    public static AbstractGraphPathSearch<TopologyVertex, TopologyEdge> graphSearch() {
        return new DijkstraGraphSearch<>();
    }

    /**
     * Returns link for two devices.
     *
     * @param device source device
     * @param port source port
     * @param device2 destination device
     * @param port2 destination port
     * @return link
     */
    public static Link addLink(String device, long port, String device2, long port2, boolean setCost, int value) {
        ConnectPoint src = new ConnectPoint(DeviceId.deviceId(device), PortNumber.portNumber(port));
        ConnectPoint dst = new ConnectPoint(DeviceId.deviceId(device2), PortNumber.portNumber(port2));
        Link curLink;
        DefaultAnnotations.Builder annotationBuilder = DefaultAnnotations.builder();
        if (setCost) {
            annotationBuilder.set(ANNOTATION_COST, String.valueOf(value));
        } else {
            annotationBuilder.set(ANNOTATION_TE_COST, String.valueOf(value));
        }

        //TODO:If cost not set cost : default value case
        curLink = DefaultLink.builder().src(src).dst(dst).state(ACTIVE).type(DIRECT)
                 .providerId(PathComputationTest.providerId).annotations(annotationBuilder.build()).build();
        return curLink;
    }

    @After
    public void tearDown() {
        pceManager.resourceService = null;
        pceManager.deviceService = null;
        pceManager.netCfgService = null;
    }

    /**
     * Returns an edge-weight capable of evaluating links on the basis of the
     * specified constraints.
     *
     * @param constraints path constraints
     * @return edge-weight function
     */
    private LinkWeight weight(List<Constraint> constraints) {
        return new MockTeConstraintBasedLinkWeight(constraints);
    }

    private Set<Path> computePath(Link link1, Link link2, Link link3, Link link4, List<Constraint> constraints) {
        Graph<TopologyVertex, TopologyEdge> graph = new AdjacencyListsGraph<>(of(D1, D2, D3, D4),
                of(new DefaultTopologyEdge(D1, D2, link1),
                   new DefaultTopologyEdge(D2, D4, link2),
                   new DefaultTopologyEdge(D1, D3, link3),
                   new DefaultTopologyEdge(D3, D4, link4)));

        GraphPathSearch.Result<TopologyVertex, TopologyEdge> result =
                graphSearch().search(graph, D1, D4, weight(constraints), ALL_PATHS);
        ImmutableSet.Builder<Path> builder = ImmutableSet.builder();
        for (org.onlab.graph.Path<TopologyVertex, TopologyEdge> path : result.paths()) {
            builder.add(networkPath(path));
        }
        return builder.build();
    }

    private class MockDeviceService extends DeviceServiceAdapter {
        List<Device> devices = new LinkedList<>();

        private void addDevice(Device dev) {
            devices.add(dev);
        }

        @Override
        public Device getDevice(DeviceId deviceId) {
            for (Device dev : devices) {
                if (dev.id().equals(deviceId)) {
                    return dev;
                }
            }
            return null;
        }

        @Override
        public Iterable<Device> getAvailableDevices() {
            return devices;
        }
    }

    private class MockTeConstraintBasedLinkWeight implements LinkWeight {

        private final List<Constraint> constraints;

        /**
         * Creates a new edge-weight function capable of evaluating links
         * on the basis of the specified constraints.
         *
         * @param constraints path constraints
         */
        MockTeConstraintBasedLinkWeight(List<Constraint> constraints) {
            if (constraints == null) {
                this.constraints = Collections.emptyList();
            } else {
                this.constraints = ImmutableList.copyOf(constraints);
            }
        }

        @Override
        public double weight(TopologyEdge edge) {
            if (!constraints.iterator().hasNext()) {
                //Takes default cost/hopcount as 1 if no constraints specified
                return 1.0;
            }

            Iterator<Constraint> it = constraints.iterator();
            double cost = 1;

            //If any constraint fails return -1 also value of cost returned from cost constraint can't be negative
            while (it.hasNext() && cost > 0) {
                Constraint constraint = it.next();
                if (constraint instanceof CapabilityConstraint) {
                    cost = ((CapabilityConstraint) constraint).isValidLink(edge.link(), deviceService,
                                                                           netConfigRegistry) ? 1 : -1;
                } else {
                    cost = constraint.cost(edge.link(), resourceService::isAvailable);
                }
            }
            return cost;
        }
    }

    /**
     * Returns the path in Path object format.
     */
    public static Path networkPath(org.onlab.graph.Path<TopologyVertex, TopologyEdge> path) {
        List<Link> links = path.edges().stream().map(TopologyEdge::link).collect(Collectors.toList());
        return new DefaultPath(CORE_PROVIDER_ID, links, path.cost());
    }

    /**
     * Tests Resource service for path computation.
     */
    public class MockPathResourceService extends ResourceServiceAdapter {
        private final Map<Resource, ResourceConsumer> assignment = new HashMap<>();
        private Map<ResourceId, List<ResourceAllocation>> resourcesAllocations = new HashMap<>();

        @Override
        public Optional<ResourceAllocation> allocate(ResourceConsumer consumer, Resource resources) {
            List<ResourceAllocation> allocations = allocate(consumer, ImmutableList.of(resources));
            if (allocations.isEmpty()) {
                return Optional.empty();
            }

            assert allocations.size() == 1;
            ResourceAllocation allocation = allocations.get(0);
            assert allocation.resource().equals(resources);

            // cast is ensured by the assertions above
            return Optional.of(allocation);
        }

        @Override
        public List<ResourceAllocation> allocate(ResourceConsumer consumer, List<? extends Resource> resources) {
            for (Resource resource: resources) {
                if (resource instanceof ContinuousResource) {
                    List<ResourceAllocation> allocs = new LinkedList<>();
                    allocs.add(new ResourceAllocation(resource, consumer));
                    resourcesAllocations.put(resource.id(), allocs);
                }
            }
            return resources.stream()
                    .map(x -> new ResourceAllocation(x, consumer))
                    .collect(Collectors.toList());
        }

        @Override
        public List<ResourceAllocation> getResourceAllocations(ResourceId id) {
            if (id instanceof ContinuousResourceId) {
                return resourcesAllocations.get(id);
            }
            DiscreteResource discrete = Resources.discrete((DiscreteResourceId) id).resource();
            return Optional.ofNullable(assignment.get(discrete))
                    .map(x -> ImmutableList.of(new ResourceAllocation(discrete, x)))
                    .orElse(ImmutableList.of());
        }

        @Override
        public <T> Set<Resource> getAvailableResources(DiscreteResourceId parent, Class<T> cls) {
            return getAvailableResources(parent).stream()
                    .filter(x -> x.isTypeOf(cls))
                    .collect(Collectors.toSet());
        }

        @Override
        public <T> Set<T> getAvailableResourceValues(DiscreteResourceId parent, Class<T> cls) {
            return getAvailableResources(parent).stream()
                    .filter(x -> x.isTypeOf(cls))
                    .flatMap(x -> Tools.stream(x.valueAs(cls)))
                    .collect(Collectors.toSet());
        }

        @Override
        public boolean isAvailable(Resource resource) {
            if (resource instanceof DiscreteResource) {
                return true;
            }

            if (resource instanceof ContinuousResource) {
                List<ResourceAllocation> resalloc = resourcesAllocations.get(resource.id());

                if ((resalloc != null) && (!resalloc.isEmpty())) {
                    if (((ContinuousResource) resalloc.iterator().next().resource()).value()
                            >= ((ContinuousResource) resource).value()) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    /* Mock test for network config registry. */
    public static class MockNetConfigRegistryAdapter extends NetworkConfigRegistryAdapter {
        private ConfigFactory cfgFactory;
        private Map<DeviceId, DeviceCapability> classConfig = new HashMap<>();

        @Override
        public void registerConfigFactory(ConfigFactory configFactory) {
            cfgFactory = configFactory;
        }

        @Override
        public void unregisterConfigFactory(ConfigFactory configFactory) {
            cfgFactory = null;
        }

        @Override
        public <S, C extends Config<S>> C addConfig(S subject, Class<C> configClass) {
            if (configClass == DeviceCapability.class) {
                DeviceCapability devCap = new DeviceCapability();
                classConfig.put((DeviceId) subject, devCap);

                JsonNode node = new ObjectNode(new MockJsonNode());
                ObjectMapper mapper = new ObjectMapper();
                ConfigApplyDelegate delegate = new InternalApplyDelegate();
                devCap.init((DeviceId) subject, null, node, mapper, delegate);
                return (C) devCap;
            }

            return null;
        }

        @Override
        public <S, C extends Config<S>> void removeConfig(S subject, Class<C> configClass) {
            classConfig.remove(subject);
        }

        @Override
        public <S, C extends Config<S>> C getConfig(S subject, Class<C> configClass) {
            if (configClass == DeviceCapability.class) {
                return (C) classConfig.get(subject);
            }
            return null;
        }

        private class MockJsonNode extends JsonNodeFactory {
        }

        // Auxiliary delegate to receive notifications about changes applied to
        // the network configuration - by the apps.
        private class InternalApplyDelegate implements ConfigApplyDelegate {
            @Override
            public void onApply(Config config) {
                //configs.put(config.subject(), config.node());
            }
        }
    }

    /**
     * All links with different costs with L1-L2 as least cost path.
     */
    @Test
    public void testpathComputationCase1() {
        Link link1 = addLink(DEVICE1, 10, DEVICE2, 20, true, 50);
        Link link2 = addLink(DEVICE2, 30, DEVICE4, 40, true, 20);
        Link link3 = addLink(DEVICE1, 80, DEVICE3, 70, true, 100);
        Link link4 = addLink(DEVICE3, 60, DEVICE4, 50, true, 50);

        CostConstraint costConst = CostConstraint.of(COST);
        List<Constraint> constraints = new LinkedList<>();
        constraints.add(costConst);

        Set<Path> paths = computePath(link1, link2, link3, link4, constraints);

        List<Link> links = new LinkedList<>();

        links.add(link1);
        links.add(link2);

        assertThat(paths.iterator().next().links(), is(links));
        assertThat(paths.iterator().next().cost(), is((double) 70));
    }

    /**
     * Links with same cost 100 except link3.
     */
    @Test
    public void testpathComputationCase2() {
        Link link1 = addLink(DEVICE1, 10, DEVICE2, 20, true, 100);
        Link link2 = addLink(DEVICE2, 30, DEVICE4, 40, true, 100);
        Link link3 = addLink(DEVICE1, 80, DEVICE3, 70, true, 1000);
        Link link4 = addLink(DEVICE3, 60, DEVICE4, 50, true, 100);

        CostConstraint costConst = CostConstraint.of(COST);
        List<Constraint> constraints = new LinkedList<>();
        constraints.add(costConst);
        Set<Path> paths = computePath(link1, link2, link3, link4, constraints);

        List<Link> links = new LinkedList<>();
        links.add(link1);
        links.add(link2);

        assertThat(paths.iterator().next().links(), is(links));
        assertThat(paths.iterator().next().cost(), is((double) 200));
    }

    /**
     * Path which satisfy bandwidth as a constraint with 10bps.
     */
    @Test
    public void testpathComputationCase3() {
        Link link1 = addLink(DEVICE1, 10, DEVICE2, 20, true, 50);
        Link link2 = addLink(DEVICE2, 30, DEVICE4, 40, true, 20);
        Link link3 = addLink(DEVICE1, 80, DEVICE3, 70, true, 100);
        Link link4 = addLink(DEVICE3, 60, DEVICE4, 50, true, 50);

        List<Resource> resources = new LinkedList<>();

        resources.add(continuous(link1.src().deviceId(), link1.src().port(), Bandwidth.class)
                .resource(50));
        resources.add(continuous(link2.src().deviceId(), link2.src().port(), Bandwidth.class)
                .resource(50));
        resources.add(continuous(link3.src().deviceId(), link3.src().port(), Bandwidth.class)
                .resource(100));
        resources.add(continuous(link4.src().deviceId(), link4.src().port(), Bandwidth.class)
                .resource(100));

        resources.add(continuous(link1.dst().deviceId(), link1.dst().port(), Bandwidth.class)
                .resource(50));
        resources.add(continuous(link2.dst().deviceId(), link2.dst().port(), Bandwidth.class)
                .resource(50));
        resources.add(continuous(link3.dst().deviceId(), link3.src().port(), Bandwidth.class)
                .resource(100));
        resources.add(continuous(link4.dst().deviceId(), link4.dst().port(), Bandwidth.class)
                .resource(100));

        resourceService.allocate(IntentId.valueOf(70), resources);

        BandwidthConstraint bandwidthConst = new BandwidthConstraint(Bandwidth.bps(10.0));

        List<Constraint> constraints = new LinkedList<>();
        constraints.add(bandwidthConst);

        Set<Path> paths = computePath(link1, link2, link3, link4, constraints);

        assertThat(paths.iterator().next().cost(), is((double) 2));
    }

    /**
     * Path which satisfy bandwidth as a constraint with 60bps.
     */
    @Test
    public void testpathComputationCase4() {
        Link link1 = addLink(DEVICE1, 10, DEVICE2, 20, true, 50);
        Link link2 = addLink(DEVICE2, 30, DEVICE4, 40, true, 50);
        Link link3 = addLink(DEVICE1, 80, DEVICE3, 70, true, 100);
        Link link4 = addLink(DEVICE3, 60, DEVICE4, 50, true, 100);

        List<Resource> resources = new LinkedList<>();

        resources.add(continuous(link1.src().deviceId(), link1.src().port(), Bandwidth.class).resource(50));
        resources.add(continuous(link2.src().deviceId(), link2.src().port(), Bandwidth.class).resource(50));
        resources.add(continuous(link3.src().deviceId(), link3.src().port(), Bandwidth.class).resource(100));
        resources.add(continuous(link4.src().deviceId(), link4.src().port(), Bandwidth.class).resource(100));

        resources.add(continuous(link1.dst().deviceId(), link1.dst().port(), Bandwidth.class).resource(50));
        resources.add(continuous(link2.dst().deviceId(), link2.dst().port(), Bandwidth.class).resource(50));
        resources.add(continuous(link3.dst().deviceId(), link3.dst().port(), Bandwidth.class).resource(100));
        resources.add(continuous(link4.dst().deviceId(), link4.dst().port(), Bandwidth.class).resource(100));

        resourceService.allocate(IntentId.valueOf(70), resources);

        BandwidthConstraint bandwidthConst = new BandwidthConstraint(Bandwidth.bps(60.0));

        List<Constraint> constraints = new LinkedList<>();
        constraints.add(bandwidthConst);
        Set<Path> paths = computePath(link1, link2, link3, link4, constraints);

        assertThat(paths.iterator().next().cost(), is((double) 2));
    }

    /**
     * Shared bandwidth as L1, L2 with its value 10 bps and bandwidth constraint as 20 bps.
     */
    @Test
    public void testpathComputationCase5() {
        Link link1 = addLink(DEVICE1, 10, DEVICE2, 20, true, 50);
        Link link2 = addLink(DEVICE2, 30, DEVICE4, 40, true, 20);
        Link link3 = addLink(DEVICE1, 80, DEVICE3, 70, true, 100);
        Link link4 = addLink(DEVICE3, 60, DEVICE4, 50, true, 80);

        List<Resource> resources = new LinkedList<>();

        resources.add(continuous(link1.src().deviceId(), link1.src().port(), Bandwidth.class).resource(50));
        resources.add(continuous(link2.src().deviceId(), link2.src().port(), Bandwidth.class).resource(50));
        resources.add(continuous(link3.src().deviceId(), link3.src().port(), Bandwidth.class).resource(100));
        resources.add(continuous(link4.src().deviceId(), link4.src().port(), Bandwidth.class).resource(100));

        resources.add(continuous(link1.dst().deviceId(), link1.dst().port(), Bandwidth.class).resource(50));
        resources.add(continuous(link2.dst().deviceId(), link2.dst().port(), Bandwidth.class).resource(50));
        resources.add(continuous(link3.dst().deviceId(), link3.dst().port(), Bandwidth.class).resource(100));
        resources.add(continuous(link4.dst().deviceId(), link4.dst().port(), Bandwidth.class).resource(100));
        resourceService.allocate(IntentId.valueOf(70), resources);

        List<Constraint> constraints = new LinkedList<>();

        List<Link> sharedLinks = new LinkedList<>();

        List<Link> links = new LinkedList<>();
        links.add(link1);
        links.add(link2);

        CostConstraint costConst = CostConstraint.of(COST);
        sharedLinks.addAll(links);
        SharedBandwidthConstraint sharedBw = new SharedBandwidthConstraint(sharedLinks, Bandwidth.bps(10),
                Bandwidth.bps(20.0));
        constraints.add(sharedBw);
        constraints.add(costConst);
        Set<Path> paths = computePath(link1, link2, link3, link4, constraints);
        assertThat(paths.iterator().next().links(), is(links));
        assertThat(paths.iterator().next().cost(), is((double) 70));
    }

    /**
     * Shared bandwidth as L1, L2 with its value 20 bps and bandwidth constraint as 10 bps.
     */
    @Test
    public void testpathComputationCase6() {
        Link link1 = addLink(DEVICE1, 10, DEVICE2, 20, true, 50);
        Link link2 = addLink(DEVICE2, 30, DEVICE4, 40, true, 20);
        Link link3 = addLink(DEVICE1, 80, DEVICE3, 70, true, 100);
        Link link4 = addLink(DEVICE3, 60, DEVICE4, 50, true, 80);

        List<Resource> resources = new LinkedList<>();

        resources.add(continuous(link1.src().deviceId(), link1.src().port(), Bandwidth.class).resource(50));
        resources.add(continuous(link2.src().deviceId(), link2.src().port(), Bandwidth.class).resource(50));
        resources.add(continuous(link3.src().deviceId(), link3.src().port(), Bandwidth.class).resource(100));
        resources.add(continuous(link4.src().deviceId(), link4.src().port(), Bandwidth.class).resource(100));

        resources.add(continuous(link1.dst().deviceId(), link1.dst().port(), Bandwidth.class).resource(50));
        resources.add(continuous(link2.dst().deviceId(), link2.dst().port(), Bandwidth.class).resource(50));
        resources.add(continuous(link3.dst().deviceId(), link3.dst().port(), Bandwidth.class).resource(100));
        resources.add(continuous(link4.dst().deviceId(), link4.dst().port(), Bandwidth.class).resource(100));
        resourceService.allocate(IntentId.valueOf(70), resources);

        List<Constraint> constraints = new LinkedList<>();

        List<Link> sharedLinks = new LinkedList<>();

        List<Link> links = new LinkedList<>();
        links.add(link1);
        links.add(link2);
        CostConstraint costConst = CostConstraint.of(COST);
        sharedLinks.addAll(links);
        SharedBandwidthConstraint sharedBwConst = new SharedBandwidthConstraint(sharedLinks, Bandwidth.bps(20),
                Bandwidth.bps(10.0));
        constraints.add(sharedBwConst);
        constraints.add(costConst);
        Set<Path> paths = computePath(link1, link2, link3, link4, constraints);

        assertThat(paths.iterator().next().links(), is(links));
        assertThat(paths.iterator().next().cost(), is((double) 70));
    }

    /**
     * Path without constraints.
     */
    @Test
    public void testpathComputationCase7() {
        Link link1 = addLink(DEVICE1, 10, DEVICE2, 20, true, 50);
        Link link2 = addLink(DEVICE2, 30, DEVICE4, 40, true, 20);
        Link link3 = addLink(DEVICE1, 80, DEVICE3, 70, true, 100);
        Link link4 = addLink(DEVICE3, 60, DEVICE4, 50, true, 80);
        List<Constraint> constraints = new LinkedList<>();
        Set<Path> paths = computePath(link1, link2, link3, link4, constraints);

        assertThat(paths.iterator().next().cost(), is((double) 2));
    }

    /**
     * With TeCost as a constraints.
     */
    @Test
    public void testpathComputationCase8() {
        Link link1 = addLink(DEVICE1, 10, DEVICE2, 20, false, 50);
        Link link2 = addLink(DEVICE2, 30, DEVICE4, 40, false, 20);
        Link link3 = addLink(DEVICE1, 80, DEVICE3, 70, false, 100);
        Link link4 = addLink(DEVICE3, 60, DEVICE4, 50, false, 80);

        CostConstraint tecostConst = CostConstraint.of(TE_COST);

        List<Constraint> constraints = new LinkedList<>();
        constraints.add(tecostConst);
        Set<Path> paths = computePath(link1, link2, link3, link4, constraints);

        List<Link> links = new LinkedList<>();
        links.add(link1);
        links.add(link2);
        assertThat(paths.iterator().next().links(), is(links));
        assertThat(paths.iterator().next().cost(), is((double) 70));
    }

    /**
     * With device supporting RSVP capability as a constraints.
     */
    @Test
    public void testpathComputationCase9() {
        Link link1 = addLink(DEVICE1, 10, DEVICE2, 20, false, 50);
        Link link2 = addLink(DEVICE2, 30, DEVICE4, 40, false, 20);
        Link link3 = addLink(DEVICE1, 80, DEVICE3, 70, false, 100);
        Link link4 = addLink(DEVICE3, 60, DEVICE4, 50, false, 80);

        CostConstraint tecostConst = CostConstraint.of(TE_COST);
        CapabilityConstraint capabilityConst = CapabilityConstraint
                .of(CapabilityConstraint.CapabilityType.WITH_SIGNALLING);

        List<Constraint> constraints = new LinkedList<>();
        constraints.add(capabilityConst);
        constraints.add(tecostConst);
        //Device1
        DefaultAnnotations.Builder builder = DefaultAnnotations.builder();
        builder.set(AnnotationKeys.TYPE, L3);
        builder.set(LSRID, "1.1.1.1");
        addDevice(DEVICE1, builder);

        DeviceCapability device1Cap = netConfigRegistry.addConfig(DeviceId.deviceId("1.1.1.1"), DeviceCapability.class);
        device1Cap.setLabelStackCap(false)
            .setLocalLabelCap(false)
            .setSrCap(false)
            .apply();

        //Device2
        builder = DefaultAnnotations.builder();
        builder.set(AnnotationKeys.TYPE, L3);
        builder.set(LSRID, "2.2.2.2");
        addDevice(DEVICE2, builder);

        DeviceCapability device2Cap = netConfigRegistry.addConfig(DeviceId.deviceId("2.2.2.2"), DeviceCapability.class);
        device2Cap.setLabelStackCap(false)
            .setLocalLabelCap(false)
            .setSrCap(false)
            .apply();

        //Device3
        builder = DefaultAnnotations.builder();
        builder.set(AnnotationKeys.TYPE, L3);
        builder.set(LSRID, "3.3.3.3");
        addDevice(DEVICE3, builder);

        DeviceCapability device3Cap = netConfigRegistry.addConfig(DeviceId.deviceId("3.3.3.3"), DeviceCapability.class);
        device3Cap.setLabelStackCap(false)
            .setLocalLabelCap(false)
            .setSrCap(false)
            .apply();

        //Device4
        builder = DefaultAnnotations.builder();
        builder.set(AnnotationKeys.TYPE, L3);
        builder.set(LSRID, "4.4.4.4");
        addDevice(DEVICE4, builder);

        DeviceCapability device4Cap = netConfigRegistry.addConfig(DeviceId.deviceId("4.4.4.4"), DeviceCapability.class);
        device4Cap.setLabelStackCap(false)
            .setLocalLabelCap(false)
            .setSrCap(false)
            .apply();

        Set<Path> paths = computePath(link1, link2, link3, link4, constraints);

        List<Link> links = new LinkedList<>();
        links.add(link1);
        links.add(link2);
        assertThat(paths.iterator().next().links(), is(links));
        assertThat(paths.iterator().next().cost(), is((double) 70));
    }

    /**
     * Devices supporting CR capability.
     */
    @Test
    public void testpathComputationCase10() {
        Link link1 = addLink(DEVICE1, 10, DEVICE2, 20, true, 50);
        Link link2 = addLink(DEVICE2, 30, DEVICE4, 40, true, 20);
        Link link3 = addLink(DEVICE1, 80, DEVICE3, 70, true, 100);
        Link link4 = addLink(DEVICE3, 60, DEVICE4, 50, true, 80);

        CapabilityConstraint capabilityConst = CapabilityConstraint
                .of(CapabilityConstraint.CapabilityType.WITHOUT_SIGNALLING_AND_WITHOUT_SR);

        List<Constraint> constraints = new LinkedList<>();
        constraints.add(capabilityConst);
        CostConstraint costConst = CostConstraint.of(COST);
        constraints.add(costConst);
        //Device1
        DefaultAnnotations.Builder builder = DefaultAnnotations.builder();
        builder.set(AnnotationKeys.TYPE, L3);
        builder.set(LSRID, "1.1.1.1");
        addDevice(DEVICE1, builder);
        DeviceCapability device1Cap = netConfigRegistry.addConfig(DeviceId.deviceId("1.1.1.1"), DeviceCapability.class);
        device1Cap.setLabelStackCap(false)
            .setLocalLabelCap(true)
            .setSrCap(false)
            .apply();

        //Device2
        builder = DefaultAnnotations.builder();
        builder.set(AnnotationKeys.TYPE, L3);
        builder.set(LSRID, "2.2.2.2");
        addDevice(DEVICE2, builder);
        DeviceCapability device2Cap = netConfigRegistry.addConfig(DeviceId.deviceId("2.2.2.2"), DeviceCapability.class);
        device2Cap.setLabelStackCap(false)
            .setLocalLabelCap(true)
            .setSrCap(false)
            .apply();

        //Device3
        builder = DefaultAnnotations.builder();
        builder.set(AnnotationKeys.TYPE, L3);
        builder.set(LSRID, "3.3.3.3");
        addDevice(DEVICE3, builder);
        DeviceCapability device3Cap = netConfigRegistry.addConfig(DeviceId.deviceId("3.3.3.3"), DeviceCapability.class);
        device3Cap.setLabelStackCap(false)
            .setLocalLabelCap(true)
            .setSrCap(false)
            .apply();

        //Device4
        builder = DefaultAnnotations.builder();
        builder.set(AnnotationKeys.TYPE, L3);
        builder.set(LSRID, "4.4.4.4");
        addDevice(DEVICE4, builder);
        DeviceCapability device4Cap = netConfigRegistry.addConfig(DeviceId.deviceId("4.4.4.4"), DeviceCapability.class);
        device4Cap.setLabelStackCap(false)
            .setLocalLabelCap(true)
            .setSrCap(false)
            .apply();

        Set<Path> paths = computePath(link1, link2, link3, link4, constraints);

        List<Link> links = new LinkedList<>();
        links.add(link1);
        links.add(link2);
        assertThat(paths.iterator().next().links(), is(links));
        assertThat(paths.iterator().next().cost(), is((double) 70));
    }

    /**
     * Device supporting SR capability.
     */
    @Test
    public void testpathComputationCase11() {
        Link link1 = addLink(DEVICE1, 10, DEVICE2, 20, true, 50);
        Link link2 = addLink(DEVICE2, 30, DEVICE4, 40, true, 20);
        Link link3 = addLink(DEVICE1, 80, DEVICE3, 70, true, 100);
        Link link4 = addLink(DEVICE3, 60, DEVICE4, 50, true, 80);

        CapabilityConstraint capabilityConst = CapabilityConstraint
                .of(CapabilityConstraint.CapabilityType.SR_WITHOUT_SIGNALLING);

        List<Constraint> constraints = new LinkedList<>();
        constraints.add(capabilityConst);
        CostConstraint costConst = CostConstraint.of(COST);
        constraints.add(costConst);
        //Device1
        DefaultAnnotations.Builder builder = DefaultAnnotations.builder();
        builder.set(AnnotationKeys.TYPE, L3);
        builder.set(LSRID, "1.1.1.1");
        addDevice(DEVICE1, builder);
        DeviceCapability device1Cap = netConfigRegistry.addConfig(DeviceId.deviceId("1.1.1.1"), DeviceCapability.class);
        device1Cap.setLabelStackCap(true)
            .setLocalLabelCap(false)
            .setSrCap(true)
            .apply();

        //Device2
        builder = DefaultAnnotations.builder();
        builder.set(AnnotationKeys.TYPE, L3);
        builder.set(LSRID, "2.2.2.2");
        addDevice(DEVICE2, builder);
        DeviceCapability device2Cap = netConfigRegistry.addConfig(DeviceId.deviceId("2.2.2.2"), DeviceCapability.class);
        device2Cap.setLabelStackCap(true)
            .setLocalLabelCap(false)
            .setSrCap(true)
            .apply();

        //Device3
        builder = DefaultAnnotations.builder();
        builder.set(AnnotationKeys.TYPE, L3);
        builder.set(LSRID, "3.3.3.3");
        addDevice(DEVICE3, builder);
        DeviceCapability device3Cap = netConfigRegistry.addConfig(DeviceId.deviceId("3.3.3.3"), DeviceCapability.class);
        device3Cap.setLabelStackCap(true)
            .setLocalLabelCap(false)
            .setSrCap(true)
            .apply();

        //Device4
        builder = DefaultAnnotations.builder();
        builder.set(AnnotationKeys.TYPE, L3);
        builder.set(LSRID, "4.4.4.4");
        addDevice(DEVICE4, builder);
        DeviceCapability device4Cap = netConfigRegistry.addConfig(DeviceId.deviceId("4.4.4.4"), DeviceCapability.class);
        device4Cap.setLabelStackCap(true)
            .setLocalLabelCap(false)
            .setSrCap(true)
            .apply();
        Set<Path> paths = computePath(link1, link2, link3, link4, constraints);

        List<Link> links = new LinkedList<>();
        links.add(link1);
        links.add(link2);
        assertThat(paths.iterator().next().links(), is(links));
        assertThat(paths.iterator().next().cost(), is((double) 70));
    }

    /**
     * Path with TE and SR capability constraint.
     */
    @Test
    public void testpathComputationCase12() {
        Link link1 = addLink(DEVICE1, 10, DEVICE2, 20, false, 50);
        Link link2 = addLink(DEVICE2, 30, DEVICE4, 40, false, 20);
        Link link3 = addLink(DEVICE1, 80, DEVICE3, 70, false, 100);
        Link link4 = addLink(DEVICE3, 60, DEVICE4, 50, false, 80);

        CostConstraint tecostConst = CostConstraint.of(TE_COST);
        CapabilityConstraint capabilityConst = CapabilityConstraint
                .of(CapabilityConstraint.CapabilityType.SR_WITHOUT_SIGNALLING);

        List<Constraint> constraints = new LinkedList<>();

        constraints.add(capabilityConst);
        constraints.add(tecostConst);
        //Device1
        DefaultAnnotations.Builder builder = DefaultAnnotations.builder();
        builder.set(AnnotationKeys.TYPE, L3);
        builder.set(LSRID, "1.1.1.1");
        addDevice(DEVICE1, builder);
        DeviceCapability device1Cap = netConfigRegistry.addConfig(DeviceId.deviceId("1.1.1.1"), DeviceCapability.class);
        device1Cap.setLabelStackCap(true)
            .setLocalLabelCap(false)
            .setSrCap(true)
            .apply();

        //Device2
        builder = DefaultAnnotations.builder();
        builder.set(AnnotationKeys.TYPE, L3);
        builder.set(LSRID, "2.2.2.2");
        addDevice(DEVICE2, builder);
        DeviceCapability device2Cap = netConfigRegistry.addConfig(DeviceId.deviceId("2.2.2.2"), DeviceCapability.class);
        device2Cap.setLabelStackCap(true)
            .setLocalLabelCap(false)
            .setSrCap(true)
            .apply();

        //Device3
        builder = DefaultAnnotations.builder();
        builder.set(AnnotationKeys.TYPE, L3);
        builder.set(LSRID, "3.3.3.3");
        addDevice(DEVICE3, builder);
        DeviceCapability device3Cap = netConfigRegistry.addConfig(DeviceId.deviceId("3.3.3.3"), DeviceCapability.class);
        device3Cap.setLabelStackCap(true)
            .setLocalLabelCap(false)
            .setSrCap(true)
            .apply();

        //Device4
        builder = DefaultAnnotations.builder();
        builder.set(AnnotationKeys.TYPE, L3);
        builder.set(LSRID, "4.4.4.4");
        addDevice(DEVICE4, builder);
        DeviceCapability device4Cap = netConfigRegistry.addConfig(DeviceId.deviceId("4.4.4.4"), DeviceCapability.class);
        device4Cap.setLabelStackCap(true)
            .setLocalLabelCap(false)
            .setSrCap(true)
            .apply();

        Set<Path> paths = computePath(link1, link2, link3, link4, constraints);

        List<Link> links = new LinkedList<>();
        links.add(link1);
        links.add(link2);
        assertThat(paths.iterator().next().links(), is(links));
        assertThat(paths.iterator().next().cost(), is((double) 70));
    }

    /**
     * Path with capability constraint and with default cost.
     */
    @Test
    public void testpathComputationCase13() {
        Link link1 = addLink(DEVICE1, 10, DEVICE2, 20, false, 50);
        Link link2 = addLink(DEVICE2, 30, DEVICE4, 40, false, 20);
        Link link3 = addLink(DEVICE1, 80, DEVICE3, 70, false, 100);
        Link link4 = addLink(DEVICE3, 60, DEVICE4, 50, false, 80);

        CapabilityConstraint capabilityConst = CapabilityConstraint
                .of(CapabilityConstraint.CapabilityType.SR_WITHOUT_SIGNALLING);

        List<Constraint> constraints = new LinkedList<>();
        constraints.add(capabilityConst);
        //Device1
        DefaultAnnotations.Builder builder = DefaultAnnotations.builder();
        builder.set(AnnotationKeys.TYPE, L3);
        builder.set(LSRID, "1.1.1.1");
        addDevice(DEVICE1, builder);
        DeviceCapability device1Cap = netConfigRegistry.addConfig(DeviceId.deviceId("1.1.1.1"), DeviceCapability.class);
        device1Cap.setLabelStackCap(true)
            .setLocalLabelCap(false)
            .setSrCap(true)
            .apply();

        //Device2
        builder = DefaultAnnotations.builder();
        builder.set(AnnotationKeys.TYPE, L3);
        builder.set(LSRID, "2.2.2.2");
        addDevice(DEVICE2, builder);
        DeviceCapability device2Cap = netConfigRegistry.addConfig(DeviceId.deviceId("2.2.2.2"), DeviceCapability.class);
        device2Cap.setLabelStackCap(true)
            .setLocalLabelCap(false)
            .setSrCap(true)
            .apply();

        //Device3
        builder = DefaultAnnotations.builder();
        builder.set(AnnotationKeys.TYPE, L3);
        builder.set(LSRID, "3.3.3.3");
        addDevice(DEVICE3, builder);
        DeviceCapability device3Cap = netConfigRegistry.addConfig(DeviceId.deviceId("3.3.3.3"), DeviceCapability.class);
        device3Cap.setLabelStackCap(true)
            .setLocalLabelCap(false)
            .setSrCap(true)
            .apply();

        //Device4
        builder = DefaultAnnotations.builder();
        builder.set(AnnotationKeys.TYPE, L3);
        builder.set(LSRID, "4.4.4.4");
        addDevice(DEVICE4, builder);
        DeviceCapability device4Cap = netConfigRegistry.addConfig(DeviceId.deviceId("4.4.4.4"), DeviceCapability.class);
        device4Cap.setLabelStackCap(true)
            .setLocalLabelCap(false)
            .setSrCap(true)
            .apply();

        Set<Path> paths = computePath(link1, link2, link3, link4, constraints);

        List<Link> links = new LinkedList<>();
        links.add(link1);
        links.add(link2);
        assertThat(paths.iterator().next().cost(), is((double) 2));
    }

    /**
     * Test case with empty constraints.
     */
    @Test
    public void testpathComputationCase14() {
        Link link1 = addLink(DEVICE1, 10, DEVICE2, 20, false, 50);
        Link link2 = addLink(DEVICE2, 30, DEVICE4, 40, false, 20);
        Link link3 = addLink(DEVICE1, 80, DEVICE3, 70, false, 100);
        Link link4 = addLink(DEVICE3, 60, DEVICE4, 50, false, 80);

        List<Constraint> constraints = new LinkedList<>();
        Set<Path> paths = computePath(link1, link2, link3, link4, constraints);

        assertThat(paths.iterator().next().cost(), is((double) 2));
    }

    /**
     * Test case with constraints as null.
     */
    @Test
    public void testpathComputationCase15() {
        Link link1 = addLink(DEVICE1, 10, DEVICE2, 20, false, 50);
        Link link2 = addLink(DEVICE2, 30, DEVICE4, 40, false, 20);
        Link link3 = addLink(DEVICE1, 80, DEVICE3, 70, false, 100);
        Link link4 = addLink(DEVICE3, 60, DEVICE4, 50, false, 80);

        List<Constraint> constraints = null;
        Set<Path> paths = computePath(link1, link2, link3, link4, constraints);

        assertThat(paths.iterator().next().cost(), is((double) 2));
    }

    /**
     * Path with cost constraint.
     */
    @Test
    public void testpathComputationCase16() {
        Link link1 = addLink(DEVICE1, 10, DEVICE2, 20, true, 50);
        Link link2 = addLink(DEVICE2, 30, DEVICE4, 40, true, 100);
        Link link3 = addLink(DEVICE1, 80, DEVICE3, 70, true, 10);
        Link link4 = addLink(DEVICE3, 60, DEVICE4, 50, true, 10);
        Link link5 = addLink(DEVICE4, 90, DEVICE5, 100, true, 20);

        CostConstraint costConst = CostConstraint.of(COST);

        List<Constraint> constraints = new LinkedList<>();
        constraints.add(costConst);
        Graph<TopologyVertex, TopologyEdge> graph = new AdjacencyListsGraph<>(of(D1, D2, D3, D4, D5),
                of(new DefaultTopologyEdge(D1, D2, link1),
                   new DefaultTopologyEdge(D2, D4, link2),
                   new DefaultTopologyEdge(D1, D3, link3),
                   new DefaultTopologyEdge(D3, D4, link4),
                   new DefaultTopologyEdge(D4, D5, link5)));

        GraphPathSearch.Result<TopologyVertex, TopologyEdge> result =
                graphSearch().search(graph, D1, D5, weight(constraints), ALL_PATHS);
        ImmutableSet.Builder<Path> builder = ImmutableSet.builder();
        for (org.onlab.graph.Path<TopologyVertex, TopologyEdge> path : result.paths()) {
            builder.add(networkPath(path));
        }

        List<Link> links = new LinkedList<>();
        links.add(link3);
        links.add(link4);
        links.add(link5);
        assertThat(builder.build().iterator().next().links(), is(links));
        assertThat(builder.build().iterator().next().cost(), is((double) 40));
    }

    /**
     * D3 doesn't support capability constraint, so path is L1-L2.
     */
    @Test
    public void testpathComputationCase17() {
        Link link1 = addLink(DEVICE1, 10, DEVICE2, 20, false, 50);
        Link link2 = addLink(DEVICE2, 30, DEVICE4, 40, false, 20);
        Link link3 = addLink(DEVICE1, 80, DEVICE3, 70, false, 100);
        Link link4 = addLink(DEVICE3, 60, DEVICE4, 50, false, 80);

        CapabilityConstraint capabilityConst = CapabilityConstraint
                .of(CapabilityConstraint.CapabilityType.WITHOUT_SIGNALLING_AND_WITHOUT_SR);

        List<Constraint> constraints = new LinkedList<>();
        constraints.add(capabilityConst);
        //Device1
        DefaultAnnotations.Builder builder = DefaultAnnotations.builder();
        builder.set(AnnotationKeys.TYPE, L3);
        builder.set(LSRID, "1.1.1.1");
        addDevice(DEVICE1, builder);
        DeviceCapability device1Cap = netConfigRegistry.addConfig(DeviceId.deviceId("1.1.1.1"), DeviceCapability.class);
        device1Cap.setLabelStackCap(false)
            .setLocalLabelCap(true)
            .setSrCap(false)
            .apply();

        //Device2
        builder = DefaultAnnotations.builder();
        builder.set(AnnotationKeys.TYPE, L3);
        builder.set(LSRID, "2.2.2.2");
        addDevice(DEVICE2, builder);
        DeviceCapability device2Cap = netConfigRegistry.addConfig(DeviceId.deviceId("2.2.2.2"), DeviceCapability.class);
        device2Cap.setLabelStackCap(false)
            .setLocalLabelCap(true)
            .setSrCap(false)
            .apply();

        //Device4
        builder = DefaultAnnotations.builder();
        builder.set(AnnotationKeys.TYPE, L3);
        builder.set(LSRID, "4.4.4.4");
        addDevice(DEVICE4, builder);
        DeviceCapability device4Cap = netConfigRegistry.addConfig(DeviceId.deviceId("4.4.4.4"), DeviceCapability.class);
        device4Cap.setLabelStackCap(false)
            .setLocalLabelCap(true)
            .setSrCap(false)
            .apply();
        Set<Path> paths = computePath(link1, link2, link3, link4, constraints);

        List<Link> links = new LinkedList<>();
        links.add(link1);
        links.add(link2);

        assertThat(paths.iterator().next().links(), is(links));
        assertThat(paths.iterator().next().cost(), is((double) 2));
    }

    /**
     * L2 doesn't support cost constraint and D3 doesn't support capability constraint, both constraint fails hence no
     * path.
     */
    @Test
    public void testpathComputationCase18() {
        Link link1 = addLink(DEVICE1, 10, DEVICE2, 20, true, 50);
        Link link2 = addLink(DEVICE2, 30, DEVICE4, 40, false, 20);
        Link link3 = addLink(DEVICE1, 80, DEVICE3, 70, true, 10);
        Link link4 = addLink(DEVICE3, 60, DEVICE4, 50, true, 10);

        CapabilityConstraint capabilityConst = CapabilityConstraint
                .of(CapabilityConstraint.CapabilityType.WITHOUT_SIGNALLING_AND_WITHOUT_SR);
        CostConstraint costConst = CostConstraint.of(COST);
        List<Constraint> constraints = new LinkedList<>();
        constraints.add(capabilityConst);
        constraints.add(costConst);
        //Device1
        DefaultAnnotations.Builder builder = DefaultAnnotations.builder();
        builder.set(AnnotationKeys.TYPE, L3);
        builder.set(LSRID, "1.1.1.1");
        addDevice(DEVICE2, builder);
        DeviceCapability device1Cap = netConfigRegistry.addConfig(DeviceId.deviceId("1.1.1.1"), DeviceCapability.class);
        device1Cap.setLabelStackCap(false)
            .setLocalLabelCap(true)
            .setSrCap(false)
            .apply();

        //Device2
        builder = DefaultAnnotations.builder();
        builder.set(AnnotationKeys.TYPE, L3);
        builder.set(LSRID, "2.2.2.2");
        addDevice(DEVICE2, builder);
        DeviceCapability device2Cap = netConfigRegistry.addConfig(DeviceId.deviceId("2.2.2.2"), DeviceCapability.class);
        device2Cap.setLabelStackCap(false)
            .setLocalLabelCap(true)
            .setSrCap(false)
            .apply();

        //Device4
        builder = DefaultAnnotations.builder();
        builder.set(AnnotationKeys.TYPE, L3);
        builder.set(LSRID, "4.4.4.4");
        addDevice(DEVICE4, builder);
        DeviceCapability device4Cap = netConfigRegistry.addConfig(DeviceId.deviceId("4.4.4.4"), DeviceCapability.class);
        device4Cap.setLabelStackCap(false)
            .setLocalLabelCap(true)
            .setSrCap(false)
            .apply();
        Set<Path> paths = computePath(link1, link2, link3, link4, constraints);

        assertThat(paths, is(new HashSet<>()));
    }

    private void addDevice(String device, DefaultAnnotations.Builder builder) {
        deviceService.addDevice(new DefaultDevice(ProviderId.NONE, deviceId(device), Type.ROUTER,
        UNKNOWN, UNKNOWN, UNKNOWN,
        UNKNOWN, new ChassisId(), builder.build()));
    }
}
