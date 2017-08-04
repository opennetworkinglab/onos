/*
 * Copyright 2014-present Open Networking Foundation
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
package org.onosproject.net.topology.impl;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.junit.TestUtils;
import org.onosproject.net.DeviceId;
import org.onosproject.net.ElementId;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.Path;
import org.onosproject.net.host.HostServiceAdapter;
import org.onosproject.net.provider.ProviderId;
import org.onosproject.net.topology.LinkWeigher;
import org.onosproject.net.topology.PathService;
import org.onosproject.net.topology.Topology;
import org.onosproject.net.topology.TopologyServiceAdapter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.onosproject.net.NetTestTools.*;

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
    public void setUp() throws Exception {
        mgr = new PathManager();
        service = mgr;
        TestUtils.setField(mgr, "topologyService", fakeTopoMgr);
        TestUtils.setField(mgr, "hostService", fakeHostMgr);
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

        validatePaths(service.getKShortestPaths(src, dst)
                          .collect(Collectors.toSet()),
                      1, 2, src, dst);
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

        validatePaths(service.getKShortestPaths(src, dst)
                      .collect(Collectors.toSet()),
                  1, 4, src, dst);
    }

    @Test
    public void edgeToEdgeDirect() {
        HostId src = hid("12:34:56:78:90:ab/1");
        HostId dst = hid("12:34:56:78:90:ef/1");
        fakeHostMgr.hosts.put(src, host("12:34:56:78:90:ab/1", "edge"));
        fakeHostMgr.hosts.put(dst, host("12:34:56:78:90:ef/1", "edge"));
        Set<Path> paths = service.getPaths(src, dst);
        validatePaths(paths, 1, 2, src, dst);

        validatePaths(service.getKShortestPaths(src, dst)
                      .collect(Collectors.toSet()),
                  1, 2, src, dst);
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
    private class FakeTopoMgr extends TopologyServiceAdapter {
        Set<Path> paths = new HashSet<>();

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
    }

    // Fake entity to give out hosts.
    private class FakeHostMgr extends HostServiceAdapter  {
        private Map<HostId, Host> hosts = new HashMap<>();

        @Override
        public Host getHost(HostId hostId) {
            return hosts.get(hostId);
        }
    }

}
