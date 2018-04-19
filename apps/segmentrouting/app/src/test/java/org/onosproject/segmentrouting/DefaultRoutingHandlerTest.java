/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.segmentrouting;

import com.google.common.collect.Sets;
import org.junit.Before;
import org.junit.Test;
import org.onlab.packet.IpAddress;
import org.onosproject.cluster.ClusterService;
import org.onosproject.cluster.DefaultControllerNode;
import org.onosproject.cluster.NodeId;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceService;
import org.onosproject.segmentrouting.config.DeviceConfiguration;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.TestConsistentMap;

import java.util.Optional;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.junit.Assert.*;

public class DefaultRoutingHandlerTest {
    private SegmentRoutingManager srManager;
    private DefaultRoutingHandler dfh;

    private static final DeviceId DEV1A = DeviceId.deviceId("of:1a");
    private static final DeviceId DEV1B = DeviceId.deviceId("of:1b");
    private static final DeviceId DEV2 = DeviceId.deviceId("of:2");

    private static final NodeId NODE1 = NodeId.nodeId("192.168.1.1");
    private static final NodeId NODE2 = NodeId.nodeId("192.168.1.2");
    private static final NodeId NODE3 = NodeId.nodeId("192.168.1.3");
    private static final IpAddress IP1 = IpAddress.valueOf("192.168.1.1");
    private static final IpAddress IP2 = IpAddress.valueOf("192.168.1.2");
    private static final IpAddress IP3 = IpAddress.valueOf("192.168.1.3");

    @Before
    public void setUp() {
        srManager = createMock(SegmentRoutingManager.class);
        srManager.storageService = createMock(StorageService.class);
        expect(srManager.storageService.consistentMapBuilder()).andReturn(new TestConsistentMap.Builder<>()).anyTimes();
        replay(srManager.storageService);
        srManager.routingRulePopulator = createMock(RoutingRulePopulator.class);
        srManager.deviceService = createMock(DeviceService.class);
        srManager.deviceConfiguration = createMock(DeviceConfiguration.class);
        srManager.mastershipService = createMock(MastershipService.class);
        srManager.clusterService = createMock(ClusterService.class);
        dfh = new DefaultRoutingHandler(srManager);
    }

    private void clearCache() {
        dfh.invalidateShouldProgramCache(DEV1A);
        dfh.invalidateShouldProgramCache(DEV1B);
        dfh.invalidateShouldProgramCache(DEV2);
    }

    // Node 1 is the master of switch 1A, 1B, and 2
    @Test
    public void testShouldHandleRoutingCase1() {
        expect(srManager.mastershipService.getMasterFor(DEV1A)).andReturn(NODE1).anyTimes();
        expect(srManager.mastershipService.getMasterFor(DEV1B)).andReturn(NODE1).anyTimes();
        expect(srManager.mastershipService.getMasterFor(DEV2)).andReturn(NODE1).anyTimes();
        replay(srManager.mastershipService);

        expect(srManager.getPairDeviceId(DEV1A)).andReturn(Optional.of(DEV1B)).anyTimes();
        expect(srManager.getPairDeviceId(DEV1B)).andReturn(Optional.of(DEV1A)).anyTimes();
        expect(srManager.getPairDeviceId(DEV2)).andReturn(Optional.empty()).anyTimes();
        replay(srManager);

        // Node 1 should program every device
        expect(srManager.clusterService.getLocalNode()).andReturn(new DefaultControllerNode(NODE1, IP1)).anyTimes();
        replay(srManager.clusterService);
        assertTrue(dfh.shouldProgram(DEV1A));
        assertTrue(dfh.shouldProgram(DEV1B));
        assertTrue(dfh.shouldProgram(DEV2));

        reset(srManager.clusterService);
        clearCache();

        // Node 2 should program no device
        expect(srManager.clusterService.getLocalNode()).andReturn(new DefaultControllerNode(NODE2, IP2)).anyTimes();
        replay(srManager.clusterService);
        assertFalse(dfh.shouldProgram(DEV1A));
        assertFalse(dfh.shouldProgram(DEV1B));
        assertFalse(dfh.shouldProgram(DEV2));

        reset(srManager.clusterService);
        clearCache();

        // Node 3 should program no device
        expect(srManager.clusterService.getLocalNode()).andReturn(new DefaultControllerNode(NODE3, IP3)).anyTimes();
        replay(srManager.clusterService);
        assertFalse(dfh.shouldProgram(DEV1A));
        assertFalse(dfh.shouldProgram(DEV1B));
        assertFalse(dfh.shouldProgram(DEV2));
    }

    // Node 1 is the master of switch 1A, 1B
    // Node 2 is the master of switch 2
    @Test
    public void testShouldHandleRoutingCase2() {
        expect(srManager.mastershipService.getMasterFor(DEV1A)).andReturn(NODE1).anyTimes();
        expect(srManager.mastershipService.getMasterFor(DEV1B)).andReturn(NODE1).anyTimes();
        expect(srManager.mastershipService.getMasterFor(DEV2)).andReturn(NODE2).anyTimes();
        replay(srManager.mastershipService);

        expect(srManager.getPairDeviceId(DEV1A)).andReturn(Optional.of(DEV1B)).anyTimes();
        expect(srManager.getPairDeviceId(DEV1B)).andReturn(Optional.of(DEV1A)).anyTimes();
        expect(srManager.getPairDeviceId(DEV2)).andReturn(Optional.empty()).anyTimes();
        replay(srManager);

        // Node 1 should program 1A, 1B
        expect(srManager.clusterService.getLocalNode()).andReturn(new DefaultControllerNode(NODE1, IP1)).anyTimes();
        replay(srManager.clusterService);
        assertTrue(dfh.shouldProgram(DEV1A));
        assertTrue(dfh.shouldProgram(DEV1B));
        assertFalse(dfh.shouldProgram(DEV2));

        reset(srManager.clusterService);
        clearCache();

        // Node 2 should program 2
        expect(srManager.clusterService.getLocalNode()).andReturn(new DefaultControllerNode(NODE2, IP2)).anyTimes();
        replay(srManager.clusterService);
        assertFalse(dfh.shouldProgram(DEV1A));
        assertFalse(dfh.shouldProgram(DEV1B));
        assertTrue(dfh.shouldProgram(DEV2));

        reset(srManager.clusterService);
        clearCache();

        // Node 3 should program no device
        expect(srManager.clusterService.getLocalNode()).andReturn(new DefaultControllerNode(NODE3, IP3)).anyTimes();
        replay(srManager.clusterService);
        assertFalse(dfh.shouldProgram(DEV1A));
        assertFalse(dfh.shouldProgram(DEV1B));
        assertFalse(dfh.shouldProgram(DEV2));
    }

    // Node 1 is the master of switch 1A
    // Node 2 is the master of switch 1B
    // Node 3 is the master of switch 2
    @Test
    public void testShouldHandleRoutingCase3() {
        expect(srManager.mastershipService.getMasterFor(DEV1A)).andReturn(NODE1).anyTimes();
        expect(srManager.mastershipService.getMasterFor(DEV1B)).andReturn(NODE2).anyTimes();
        expect(srManager.mastershipService.getMasterFor(DEV2)).andReturn(NODE3).anyTimes();
        replay(srManager.mastershipService);

        expect(srManager.getPairDeviceId(DEV1A)).andReturn(Optional.of(DEV1B)).anyTimes();
        expect(srManager.getPairDeviceId(DEV1B)).andReturn(Optional.of(DEV1A)).anyTimes();
        expect(srManager.getPairDeviceId(DEV2)).andReturn(Optional.empty()).anyTimes();
        replay(srManager);

        // Node 1 should program 1A, 1B
        expect(srManager.clusterService.getLocalNode()).andReturn(new DefaultControllerNode(NODE1, IP1)).anyTimes();
        replay(srManager.clusterService);
        assertTrue(dfh.shouldProgram(DEV1A));
        assertTrue(dfh.shouldProgram(DEV1B));
        assertFalse(dfh.shouldProgram(DEV2));

        reset(srManager.clusterService);
        clearCache();

        // Node 2 should program no device
        expect(srManager.clusterService.getLocalNode()).andReturn(new DefaultControllerNode(NODE2, IP2)).anyTimes();
        replay(srManager.clusterService);
        assertFalse(dfh.shouldProgram(DEV1A));
        assertFalse(dfh.shouldProgram(DEV1B));
        assertFalse(dfh.shouldProgram(DEV2));

        reset(srManager.clusterService);
        clearCache();

        // Node 3 should program 2
        expect(srManager.clusterService.getLocalNode()).andReturn(new DefaultControllerNode(NODE3, IP3)).anyTimes();
        replay(srManager.clusterService);
        assertFalse(dfh.shouldProgram(DEV1A));
        assertFalse(dfh.shouldProgram(DEV1B));
        assertTrue(dfh.shouldProgram(DEV2));
    }

    // Node 3 is the master of switch 1A, 1B, 2
    // Later on, node 1 becomes the master of 1A; Node 2 becomes the master of 1B.
    @Test
    public void testShouldHandleRoutingCase4() {
        expect(srManager.mastershipService.getMasterFor(DEV1A)).andReturn(NODE3).anyTimes();
        expect(srManager.mastershipService.getMasterFor(DEV1B)).andReturn(NODE3).anyTimes();
        expect(srManager.mastershipService.getMasterFor(DEV2)).andReturn(NODE3).anyTimes();
        replay(srManager.mastershipService);

        expect(srManager.getPairDeviceId(DEV1A)).andReturn(Optional.of(DEV1B)).anyTimes();
        expect(srManager.getPairDeviceId(DEV1B)).andReturn(Optional.of(DEV1A)).anyTimes();
        expect(srManager.getPairDeviceId(DEV2)).andReturn(Optional.empty()).anyTimes();
        replay(srManager);

        // Node 1 should program no device
        expect(srManager.clusterService.getLocalNode()).andReturn(new DefaultControllerNode(NODE1, IP1)).anyTimes();
        replay(srManager.clusterService);
        assertFalse(dfh.shouldProgram(DEV1A));
        assertFalse(dfh.shouldProgram(DEV1B));
        assertFalse(dfh.shouldProgram(DEV2));

        reset(srManager.clusterService);
        clearCache();

        // Node 2 should program no device
        expect(srManager.clusterService.getLocalNode()).andReturn(new DefaultControllerNode(NODE2, IP2)).anyTimes();
        replay(srManager.clusterService);
        assertFalse(dfh.shouldProgram(DEV1A));
        assertFalse(dfh.shouldProgram(DEV1B));
        assertFalse(dfh.shouldProgram(DEV2));

        reset(srManager.clusterService);
        clearCache();

        // Node 3 should program 1A, 1B and 2
        expect(srManager.clusterService.getLocalNode()).andReturn(new DefaultControllerNode(NODE3, IP3)).anyTimes();
        replay(srManager.clusterService);
        assertTrue(dfh.shouldProgram(DEV1A));
        assertTrue(dfh.shouldProgram(DEV1B));
        assertTrue(dfh.shouldProgram(DEV2));

        // Mastership of switch 1A moves to Node 1
        reset(srManager.mastershipService);
        expect(srManager.mastershipService.getMasterFor(DEV1A)).andReturn(NODE1).anyTimes();
        expect(srManager.mastershipService.getMasterFor(DEV1B)).andReturn(NODE2).anyTimes();
        expect(srManager.mastershipService.getMasterFor(DEV2)).andReturn(NODE3).anyTimes();
        replay(srManager.mastershipService);

        reset(srManager.clusterService);
        clearCache();

        // Node 1 should program 1A, 1B
        expect(srManager.clusterService.getLocalNode()).andReturn(new DefaultControllerNode(NODE1, IP1)).anyTimes();
        replay(srManager.clusterService);
        assertTrue(dfh.shouldProgram(DEV1A));
        assertTrue(dfh.shouldProgram(DEV1B));
        assertFalse(dfh.shouldProgram(DEV2));

        reset(srManager.clusterService);
        clearCache();

        // Node 2 should program no device
        expect(srManager.clusterService.getLocalNode()).andReturn(new DefaultControllerNode(NODE2, IP2)).anyTimes();
        replay(srManager.clusterService);
        assertFalse(dfh.shouldProgram(DEV1A));
        assertFalse(dfh.shouldProgram(DEV1B));
        assertFalse(dfh.shouldProgram(DEV2));

        reset(srManager.clusterService);
        clearCache();

        // Node 3 should program 2
        expect(srManager.clusterService.getLocalNode()).andReturn(new DefaultControllerNode(NODE3, IP3)).anyTimes();
        replay(srManager.clusterService);
        assertFalse(dfh.shouldProgram(DEV1A));
        assertFalse(dfh.shouldProgram(DEV1B));
        assertTrue(dfh.shouldProgram(DEV2));
    }

    // Node 1 is the master of 1A. 1B has no master
    // Node 2 becomes the master of 1B later
    @Test
    public void testShouldHandleRoutingCase5() {
        expect(srManager.mastershipService.getMasterFor(DEV1A)).andReturn(NODE1).anyTimes();
        expect(srManager.mastershipService.getMasterFor(DEV1B)).andReturn(null).anyTimes();
        replay(srManager.mastershipService);

        expect(srManager.getPairDeviceId(DEV1A)).andReturn(Optional.of(DEV1B)).anyTimes();
        expect(srManager.getPairDeviceId(DEV1B)).andReturn(Optional.of(DEV1A)).anyTimes();
        replay(srManager);

        // Node 1 should program both 1A and 1B
        expect(srManager.clusterService.getLocalNode()).andReturn(new DefaultControllerNode(NODE1, IP1)).anyTimes();
        replay(srManager.clusterService);
        assertTrue(dfh.shouldProgram(DEV1A));
        assertTrue(dfh.shouldProgram(DEV1B));

        reset(srManager.clusterService);
        clearCache();

        // Node 2 should program no device
        expect(srManager.clusterService.getLocalNode()).andReturn(new DefaultControllerNode(NODE2, IP2)).anyTimes();
        replay(srManager.clusterService);
        assertFalse(dfh.shouldProgram(DEV1A));
        assertFalse(dfh.shouldProgram(DEV1B));

        // Mastership of switch 1B moves to Node 2
        reset(srManager.mastershipService);
        expect(srManager.mastershipService.getMasterFor(DEV1A)).andReturn(NODE1).anyTimes();
        expect(srManager.mastershipService.getMasterFor(DEV1B)).andReturn(NODE2).anyTimes();
        replay(srManager.mastershipService);

        reset(srManager.clusterService);
        clearCache();

        // Node 1 should program 1A, 1B
        expect(srManager.clusterService.getLocalNode()).andReturn(new DefaultControllerNode(NODE1, IP1)).anyTimes();
        replay(srManager.clusterService);
        assertTrue(dfh.shouldProgram(DEV1A));
        assertTrue(dfh.shouldProgram(DEV1B));

        reset(srManager.clusterService);
        clearCache();

        // Node 2 should program no device
        expect(srManager.clusterService.getLocalNode()).andReturn(new DefaultControllerNode(NODE2, IP2)).anyTimes();
        replay(srManager.clusterService);
        assertFalse(dfh.shouldProgram(DEV1A));
        assertFalse(dfh.shouldProgram(DEV1B));
    }

    // Neither 1A or 1B has master
    @Test
    public void testShouldHandleRoutingCase6() {
        expect(srManager.mastershipService.getMasterFor(DEV1A)).andReturn(null).anyTimes();
        expect(srManager.mastershipService.getMasterFor(DEV1B)).andReturn(null).anyTimes();
        replay(srManager.mastershipService);

        expect(srManager.getPairDeviceId(DEV1A)).andReturn(Optional.of(DEV1B)).anyTimes();
        expect(srManager.getPairDeviceId(DEV1B)).andReturn(Optional.of(DEV1A)).anyTimes();
        replay(srManager);

        // Node 1 should program no device
        expect(srManager.clusterService.getLocalNode()).andReturn(new DefaultControllerNode(NODE1, IP1)).anyTimes();
        replay(srManager.clusterService);
        assertFalse(dfh.shouldProgram(DEV1A));
        assertFalse(dfh.shouldProgram(DEV1B));

        reset(srManager.clusterService);
        clearCache();

        // Node 2 should program no device
        expect(srManager.clusterService.getLocalNode()).andReturn(new DefaultControllerNode(NODE2, IP2)).anyTimes();
        replay(srManager.clusterService);
        assertFalse(dfh.shouldProgram(DEV1A));
        assertFalse(dfh.shouldProgram(DEV1B));

        assertFalse(dfh.shouldProgram.containsKey(Sets.newHashSet(DEV1A, DEV1B)));
    }
}