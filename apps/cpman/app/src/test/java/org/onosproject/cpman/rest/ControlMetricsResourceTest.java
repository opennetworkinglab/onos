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
package org.onosproject.cpman.rest;

import com.google.common.collect.ImmutableSet;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Before;
import org.junit.Test;
import org.onlab.osgi.ServiceDirectory;
import org.onlab.osgi.TestServiceDirectory;
import org.onlab.packet.IpAddress;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.ControllerNode;
import org.onosproject.cluster.NodeId;
import org.onosproject.codec.CodecService;
import org.onosproject.codec.impl.CodecManager;
import org.onosproject.cpman.ControlLoad;
import org.onosproject.cpman.ControlLoadSnapshot;
import org.onosproject.cpman.ControlPlaneMonitorService;
import org.onosproject.cpman.codec.ControlLoadSnapshotCodec;
import org.onosproject.rest.resources.ResourceTest;

import javax.ws.rs.client.WebTarget;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.anyString;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * Unit test for ControlMetrics REST API.
 */
public class ControlMetricsResourceTest extends ResourceTest {

    final ControlPlaneMonitorService mockControlPlaneMonitorService =
            createMock(ControlPlaneMonitorService.class);
    final ClusterService mockClusterService = createMock(ClusterService.class);
    Set<String> resourceSet = ImmutableSet.of("resource1", "resource2");
    NodeId nodeId;
    ControlLoad mockControlLoad;

    private static final String PREFIX = "controlmetrics";

    /**
     * Constructs a control metrics resource test instance.
     */
    public ControlMetricsResourceTest() {
        super(ResourceConfig.forApplicationClass(CPManWebApplication.class));
    }

    /**
     * Mock class for a controller node.
     */
    private static class MockControllerNode implements ControllerNode {
        final NodeId id;

        public MockControllerNode(NodeId id) {
            this.id = id;
        }

        @Override
        public NodeId id() {
            return this.id;
        }

        @Override
        public IpAddress ip() {
            return null;
        }

        @Override
        public IpAddress ip(boolean resolve) {
            return null;
        }

        @Override
        public String host() {
            return null;
        }

        @Override
        public int tcpPort() {
            return 0;
        }
    }

    private static class MockControlLoad implements ControlLoad {

        @Override
        public long average(int duration, TimeUnit unit) {
            return 0;
        }

        @Override
        public long average() {
            return 10L;
        }

        @Override
        public long[] recent(int duration, TimeUnit unit) {
            return new long[0];
        }

        @Override
        public long[] all() {
            return new long[0];
        }

        @Override
        public long rate() {
            return 0;
        }

        @Override
        public long latest() {
            return 10L;
        }

        @Override
        public boolean isValid() {
            return false;
        }

        @Override
        public long time() {
            return 20L;
        }
    }

    /**
     * Sets up the global values for all the tests.
     */
    @Before
    public void setUpTest() {
        final CodecManager codecService = new CodecManager();
        codecService.activate();
        codecService.registerCodec(ControlLoadSnapshot.class, new ControlLoadSnapshotCodec());
        ServiceDirectory testDirectory =
                new TestServiceDirectory()
                        .add(ControlPlaneMonitorService.class,
                                mockControlPlaneMonitorService)
                        .add(ClusterService.class, mockClusterService)
                        .add(CodecService.class, codecService);
        setServiceDirectory(testDirectory);

        nodeId = new NodeId("1");
        mockControlLoad = new MockControlLoad();
        ControllerNode mockControllerNode = new MockControllerNode(nodeId);

        expect(mockClusterService.getLocalNode()).andReturn(mockControllerNode).anyTimes();
        replay(mockClusterService);
    }

    /**
     * Tests the results of the REST API GET when there are no active entries.
     */
    @Test
    public void testResourceEmptyArray() {
        expect(mockControlPlaneMonitorService.availableResourcesSync(anyObject(), anyObject()))
                .andReturn(ImmutableSet.of()).once();
        replay(mockControlPlaneMonitorService);
        final WebTarget wt = target();
        final String response = wt.path(PREFIX + "/disk_metrics").request().get(String.class);
        assertThat(response, is("{\"disks\":[]}"));

        verify(mockControlPlaneMonitorService);
    }

    /**
     * Tests the results of the rest api GET when there are active metrics.
     */
    @Test
    public void testResourcePopulatedArray() {
        expect(mockControlPlaneMonitorService.availableResourcesSync(anyObject(), anyObject()))
                .andReturn(resourceSet).once();
        expect(mockControlPlaneMonitorService.getLoadSync(anyObject(), anyObject(),
                anyString())).andReturn(null).times(4);
        replay(mockControlPlaneMonitorService);

        final WebTarget wt = target();
        final String response = wt.path(PREFIX + "/disk_metrics").request().get(String.class);
        assertThat(response, is("{\"disks\":[{\"name\":\"resource1\",\"value\":{\"metrics\":[]}}," +
                "{\"name\":\"resource2\",\"value\":{\"metrics\":[]}}]}"));
    }
}
