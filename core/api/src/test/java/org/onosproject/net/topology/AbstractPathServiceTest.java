/*
 * Copyright 2017-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onosproject.net.topology;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.onlab.graph.ScalarWeight;
import org.onlab.graph.Weight;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DefaultDisjointPath;
import org.onosproject.net.DefaultLink;
import org.onosproject.net.DefaultPath;
import org.onosproject.net.DeviceId;
import org.onosproject.net.DisjointPath;
import org.onosproject.net.ElementId;
import org.onosproject.net.HostId;
import org.onosproject.net.Link;
import org.onosproject.net.NetTestTools;
import org.onosproject.net.Path;
import org.onosproject.net.host.HostServiceAdapter;
import org.onosproject.net.provider.ProviderId;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.onosproject.net.NetTestTools.did;
import static org.onosproject.net.NetTestTools.hid;

public class AbstractPathServiceTest {

    private TestPathService service;
    private FakeTopoMgr topoMgr;

    private ConnectPoint cpA = NetTestTools.connectPoint("A", 1);
    private ConnectPoint cpB = NetTestTools.connectPoint("B", 2);
    private ConnectPoint cpC = NetTestTools.connectPoint("C", 3);
    private ProviderId pid = ProviderId.NONE;
    private Link link1 = DefaultLink.builder()
            .providerId(pid)
            .src(cpA)
            .dst(cpB)
            .type(Link.Type.DIRECT)
            .state(Link.State.ACTIVE)
            .build();
    private Link link2 = DefaultLink.builder()
            .providerId(pid)
            .src(cpB)
            .dst(cpC)
            .type(Link.Type.DIRECT)
            .state(Link.State.ACTIVE)
            .build();
    private List<Link> links1 = ImmutableList.of(link1, link2);
    private Path path1 = new DefaultPath(pid, links1, new ScalarWeight(1.0));



    private class TestPathService extends AbstractPathService {
        Set<Path> paths = null;
        Set<DisjointPath> disjointPaths = null;

        @Override
        public Set<Path> getPaths(ElementId src, ElementId dst) {
            return paths;
        }

        @Override
        public Set<DisjointPath> getDisjointPaths(ElementId src, ElementId dst) {
            return disjointPaths;
        }

        @Override
        public Set<DisjointPath> getDisjointPaths(ElementId src, ElementId dst, Map<Link, Object> riskProfile) {
            return disjointPaths;
        }
    }

    class TestWeigher implements LinkWeigher {
        @Override
        public Weight weight(TopologyEdge edge) {
            return new ScalarWeight(1.0);
        }

        @Override
        public Weight getInitialWeight() {
            return new ScalarWeight(1.0);
        }

        @Override
        public Weight getNonViableWeight() {
            return new ScalarWeight(0.0);
        }
    }

    // Fake entity to give out paths.
    private class FakeTopoMgr extends TopologyServiceAdapter {

        Set<Path> paths = new HashSet<>();
        Set<DisjointPath> disjointPaths = new HashSet<>();

        void definePaths(Set<Path> paths) {
            this.paths = paths;
            this.disjointPaths = paths.stream()
                    .map(path ->
                        new DefaultDisjointPath(path.providerId(),
                                                (DefaultPath) path))
                    .collect(Collectors.toSet());
        }

        @Override
        public Set<Path> getPaths(Topology topology, DeviceId src,
                                  DeviceId dst) {
            return paths;
        }

        @Override
        public Set<Path> getPaths(Topology topology, DeviceId src,
                                  DeviceId dst, LinkWeigher weight) {
            return paths;
        }

        @Override
        public Set<DisjointPath> getDisjointPaths(Topology topology, DeviceId src,
                                                  DeviceId dst,
                                                  LinkWeigher weigher) {
            return disjointPaths;
        }

        @Override
        public Set<DisjointPath> getDisjointPaths(Topology topology, DeviceId src,
                                                  DeviceId dst,
                                                  LinkWeigher weigher,
                                                  Map<Link, Object> riskProfile) {
            return disjointPaths;
        }
    }

    @Before
    public void setUp() {
        service = new TestPathService();
        topoMgr = new FakeTopoMgr();
        service.topologyService = topoMgr;
        service.hostService = new HostServiceAdapter();
    }

    private void checkPathValues(Path path) {
        assertThat(path, notNullValue());
        assertThat(path.links(), hasSize(2));
        assertThat(path.links().get(0).src(), is(cpA));
        assertThat(path.links().get(0).dst(), is(cpB));
        assertThat(path.links().get(1).src(), is(cpB));
        assertThat(path.links().get(1).dst(), is(cpC));
    }

    private void checkDisjointPaths(Set<DisjointPath> paths) {
        assertThat(paths, notNullValue());
        assertThat(paths, hasSize(1));
        Path path = paths.iterator().next();
        checkPathValues(path);
    }

    private void checkPaths(Collection<Path> paths) {
        assertThat(paths, notNullValue());
        assertThat(paths, hasSize(1));
        Path path = paths.iterator().next();
        checkPathValues(path);
    }

    /**
     * Tests no paths being set up.
     */
    @Test
    public void testNoPaths() {
        Set<Path> noPaths = service.getPaths(did("A"), did("B"), new TestWeigher());
        assertThat(noPaths, empty());
    }

    /**
     * Tests paths from a host.
     */
    @Test
    public void testSelfPaths() {
        HostId host = hid("12:34:56:78:90:ab/1");
        Set<Path> paths = service.getPaths(host, host, new TestWeigher());
        assertThat(paths, hasSize(1));
        Path path = paths.iterator().next();
        assertThat(path, not(nullValue()));
        assertThat(path.links(), hasSize(2));
        Link link1 = path.links().get(0);
        Link link2 = path.links().get(1);
        assertThat(link1.src(), is(link2.dst()));
        assertThat(link2.src(), is(link1.dst()));
        assertThat(link1.src().hostId(), is(host));
        assertThat(link2.dst().hostId(), is(host));
    }

    /**
     * Tests paths from a device to a device.
     */
    @Test
    public void testDevicePaths() {
        topoMgr.definePaths(ImmutableSet.of(path1));
        Set<Path> pathsAC = service.getPaths(did("A"), did("C"), new TestWeigher());
        checkPaths(pathsAC);
    }

    /**
     * Tests K Shortest Path computation.
     */
    @Test
    public void testKShortestPath() {
        topoMgr.definePaths(ImmutableSet.of(path1));
        List<Path> paths = service.getKShortestPaths(did("A"), did("C"), new TestWeigher())
                .collect(Collectors.toList());
        checkPaths(paths);
    }

    /**
     * Tests disjoint paths.
     */
    @Test
    public void testDisjointPaths() {
        topoMgr.definePaths(ImmutableSet.of(path1));
        Set<DisjointPath> paths = service.getDisjointPaths(did("A"), did("C"), new TestWeigher());
        checkDisjointPaths(paths);
    }

    /**
     * Tests disjoint paths with a risk profile.
     */
    @Test
    public void testDisjointPathsWithRiskProfile() {
        topoMgr.definePaths(ImmutableSet.of(path1));
        Map<Link, Object> riskProfile = ImmutableMap.of();

        Set<DisjointPath> paths =
                service.getDisjointPaths(did("A"), did("C"), new TestWeigher(),
                                         riskProfile);

        checkDisjointPaths(paths);
    }
}
