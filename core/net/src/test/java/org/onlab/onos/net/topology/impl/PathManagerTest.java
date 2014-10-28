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
package org.onlab.onos.net.topology.impl;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.ElementId;
import org.onlab.onos.net.Host;
import org.onlab.onos.net.HostId;
import org.onlab.onos.net.Path;
import org.onlab.onos.net.host.HostService;
import org.onlab.onos.net.host.HostServiceAdapter;
import org.onlab.onos.net.provider.ProviderId;
import org.onlab.onos.net.topology.LinkWeight;
import org.onlab.onos.net.topology.PathService;
import org.onlab.onos.net.topology.Topology;
import org.onlab.onos.net.topology.TopologyService;
import org.onlab.onos.net.topology.TopologyServiceAdapter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.onlab.onos.net.NetTestTools.*;

/**
 * Test of the path selection subsystem.
 */
public class PathManagerTest {

    private static final ProviderId PID = new ProviderId("of", "foo");

    private PathManager mgr;
    private PathService service;

    private FakeTopoMgr fakeTopoMgr = new FakeTopoMgr();
    private FakeHostMgr fakeHostMgr = new FakeHostMgr();

    @Before
    public void setUp() {
        mgr = new PathManager();
        service = mgr;
        mgr.topologyService = fakeTopoMgr;
        mgr.hostService = fakeHostMgr;
        mgr.activate();
    }

    @After
    public void tearDown() {
        mgr.deactivate();
    }

    @Test
    public void infraToInfra() {
        DeviceId src = did("src");
        DeviceId dst = did("dst");
        fakeTopoMgr.paths.add(createPath("src", "middle", "dst"));
        Set<Path> paths = service.getPaths(src, dst);
        validatePaths(paths, 1, 2, src, dst);
    }

    @Test
    public void infraToEdge() {
        DeviceId src = did("src");
        HostId dst = hid("12:34:56:78:90:ab/1");
        fakeTopoMgr.paths.add(createPath("src", "middle", "edge"));
        fakeHostMgr.hosts.put(dst, host("12:34:56:78:90:ab/1", "edge"));
        Set<Path> paths = service.getPaths(src, dst);
        validatePaths(paths, 1, 3, src, dst);
    }

    @Test
    public void edgeToInfra() {
        HostId src = hid("12:34:56:78:90:ab/1");
        DeviceId dst = did("dst");
        fakeTopoMgr.paths.add(createPath("edge", "middle", "dst"));
        fakeHostMgr.hosts.put(src, host("12:34:56:78:90:ab/1", "edge"));
        Set<Path> paths = service.getPaths(src, dst);
        validatePaths(paths, 1, 3, src, dst);
    }

    @Test
    public void edgeToEdge() {
        HostId src = hid("12:34:56:78:90:ab/1");
        HostId dst = hid("12:34:56:78:90:ef/1");
        fakeTopoMgr.paths.add(createPath("srcEdge", "middle", "dstEdge"));
        fakeHostMgr.hosts.put(src, host("12:34:56:78:90:ab/1", "srcEdge"));
        fakeHostMgr.hosts.put(dst, host("12:34:56:78:90:ef/1", "dstEdge"));
        Set<Path> paths = service.getPaths(src, dst);
        validatePaths(paths, 1, 4, src, dst);
    }

    @Test
    public void edgeToEdgeDirect() {
        HostId src = hid("12:34:56:78:90:ab/1");
        HostId dst = hid("12:34:56:78:90:ef/1");
        fakeHostMgr.hosts.put(src, host("12:34:56:78:90:ab/1", "edge"));
        fakeHostMgr.hosts.put(dst, host("12:34:56:78:90:ef/1", "edge"));
        Set<Path> paths = service.getPaths(src, dst);
        validatePaths(paths, 1, 2, src, dst);
    }

    @Test
    public void noEdge() {
        Set<Path> paths = service.getPaths(hid("12:34:56:78:90:ab/1"),
                                           hid("12:34:56:78:90:ef/1"));
        assertTrue("there should be no paths", paths.isEmpty());
    }

    // Makes sure the set of paths meets basic expectations.
    private void validatePaths(Set<Path> paths, int count, int length,
                               ElementId src, ElementId dst) {
        assertEquals("incorrect path count", count, paths.size());
        for (Path path : paths) {
            assertEquals("incorrect length", length, path.links().size());
            assertEquals("incorrect source", src, path.src().elementId());
            assertEquals("incorrect destination", dst, path.dst().elementId());
        }
    }

    // Fake entity to give out paths.
    private class FakeTopoMgr extends TopologyServiceAdapter implements TopologyService {
        Set<Path> paths = new HashSet<>();

        @Override
        public Set<Path> getPaths(Topology topology, DeviceId src, DeviceId dst) {
            return paths;
        }

        @Override
        public Set<Path> getPaths(Topology topology, DeviceId src, DeviceId dst, LinkWeight weight) {
            return paths;
        }
    }

    // Fake entity to give out hosts.
    private class FakeHostMgr extends HostServiceAdapter implements HostService {
        private Map<HostId, Host> hosts = new HashMap<>();

        @Override
        public Host getHost(HostId hostId) {
            return hosts.get(hostId);
        }
    }

}
