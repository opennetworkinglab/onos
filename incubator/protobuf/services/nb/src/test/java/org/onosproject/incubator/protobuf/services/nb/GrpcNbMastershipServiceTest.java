/*
 * Copyright 2017-present Open Networking Foundation
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
package org.onosproject.incubator.protobuf.services.nb;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import io.grpc.BindableService;
import io.grpc.ManagedChannel;
import io.grpc.inprocess.InProcessChannelBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onosproject.cluster.NodeId;
import org.onosproject.cluster.RoleInfo;
import org.onosproject.grpc.nb.mastership.MastershipServiceGrpc;
import org.onosproject.grpc.nb.mastership.MastershipServiceNb.getLocalRoleReply;
import org.onosproject.grpc.nb.mastership.MastershipServiceNb.getLocalRoleRequest;
import org.onosproject.grpc.nb.mastership.MastershipServiceNb.getMasterForReply;
import org.onosproject.grpc.nb.mastership.MastershipServiceNb.getMasterForRequest;
import org.onosproject.grpc.nb.mastership.MastershipServiceNb.getNodesForReply;
import org.onosproject.grpc.nb.mastership.MastershipServiceNb.getNodesForRequest;
import org.onosproject.grpc.nb.mastership.MastershipServiceNb.requestRoleForSyncReply;
import org.onosproject.grpc.nb.mastership.MastershipServiceNb.requestRoleForSyncRequest;
import org.onosproject.incubator.protobuf.models.cluster.NodeIdProtoTranslator;
import org.onosproject.incubator.protobuf.models.net.MastershipRoleProtoTranslator;
import org.onosproject.mastership.MastershipService;
import org.onosproject.mastership.MastershipServiceAdapter;
import org.onosproject.net.DeviceId;
import org.onosproject.net.MastershipRole;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for mastership gRPC NB service.
 */
public class GrpcNbMastershipServiceTest {

    private static InProcessServer<BindableService> inprocessServer;
    private static MastershipServiceGrpc.MastershipServiceBlockingStub blockingStub;
    private static ManagedChannel channel;

    private final MastershipService mastershipService = new MockMastershipService();

    private Map<DeviceId, MastershipRole> mastershipMap = Maps.newHashMap();
    private Map<DeviceId, NodeId> nodeIdMap = Maps.newHashMap();
    private Map<DeviceId, RoleInfo> roleInfoMap = Maps.newHashMap();

    private static final String DEVICE_ID_1 = "1";
    private static final String DEVICE_ID_2 = "2";
    private static final String DEVICE_ID_3 = "3";

    private DeviceId did1 = DeviceId.deviceId(DEVICE_ID_1);
    private DeviceId did2 = DeviceId.deviceId(DEVICE_ID_2);
    private DeviceId did3 = DeviceId.deviceId(DEVICE_ID_3);

    private NodeId nid1 = NodeId.nodeId("1");
    private NodeId nid2 = NodeId.nodeId("2");
    private NodeId nid3 = NodeId.nodeId("3");

    /**
     * Initializes the test environment.
     */
    @Before
    public void setUp() throws IllegalAccessException, IOException, InstantiationException {
        GrpcNbMastershipService grpcMastershipService = new GrpcNbMastershipService();
        grpcMastershipService.mastershipService = mastershipService;
        inprocessServer = grpcMastershipService.registerInProcessServer();
        inprocessServer.start();

        channel = InProcessChannelBuilder.forName("test").directExecutor()
                .usePlaintext(true).build();

        blockingStub = MastershipServiceGrpc.newBlockingStub(channel);

        initMastershipMap();
        initNodeIdMap();
        initRoleInfoMap();
    }

    /**
     * Finalizes the test setup.
     */
    @After
    public void tearDown() {
        channel.shutdownNow();
        inprocessServer.stop();
    }

    private void initMastershipMap() {
        mastershipMap.put(did1, MastershipRole.MASTER);
        mastershipMap.put(did2, MastershipRole.STANDBY);
        mastershipMap.put(did3, MastershipRole.NONE);
    }

    private void initNodeIdMap() {
        nodeIdMap.put(did1, nid1);
        nodeIdMap.put(did2, nid2);
        nodeIdMap.put(did3, nid3);
    }

    private void initRoleInfoMap() {
        RoleInfo roleInfo1 = new RoleInfo(nid1, ImmutableList.of(nid2, nid3));
        RoleInfo roleInfo2 = new RoleInfo(nid2, ImmutableList.of(nid1, nid3));
        RoleInfo roleInfo3 = new RoleInfo(nid3, ImmutableList.of(nid2, nid3));
        roleInfoMap.put(did1, roleInfo1);
        roleInfoMap.put(did2, roleInfo2);
        roleInfoMap.put(did3, roleInfo3);
    }

    /**
     * Tests the invocation result of getLocalRole method.
     *
     * @throws InterruptedException
     */
    @Test
    public void testGetLocalRole() throws InterruptedException {
        getLocalRoleRequest request = getLocalRoleRequest.newBuilder()
                .setDeviceId(DEVICE_ID_1)
                .build();
        getLocalRoleReply reply;

        reply = blockingStub.getLocalRole(request);
        assertEquals(Optional.of(MastershipRole.MASTER),
                MastershipRoleProtoTranslator.translate(reply.getMastershipRole()));
    }

    /**
     * Tests the invocation result of requestRoleForSync method.
     *
     * @throws InterruptedException
     */
    @Test
    public void testRequestRoleForSync() throws InterruptedException {
        requestRoleForSyncRequest request = requestRoleForSyncRequest.newBuilder()
                .setDeviceId(DEVICE_ID_2)
                .build();
        requestRoleForSyncReply reply;

        reply = blockingStub.requestRoleForSync(request);
        assertEquals(Optional.of(MastershipRole.STANDBY),
                MastershipRoleProtoTranslator.translate(reply.getMastershipRole()));
    }

    /**
     * Tests the invocation result of getMasterFor method.
     *
     * @throws InterruptedException
     */
    @Test
    public void testGetMasterFor() {
        getMasterForRequest request = getMasterForRequest.newBuilder()
                .setDeviceId(DEVICE_ID_1)
                .build();
        getMasterForReply reply;

        reply = blockingStub.getMasterFor(request);
        assertEquals(nid1, NodeIdProtoTranslator.translate(reply.getNodeId()));
    }

    /**
     * Tests the invocation result of getNodesFor method.
     *
     * @throws InterruptedException
     */
    @Test
    public void testGetNodesFor() {
        getNodesForRequest request = getNodesForRequest.newBuilder()
                .setDeviceId(DEVICE_ID_3)
                .build();
        getNodesForReply reply;

        reply = blockingStub.getNodesFor(request);
        assertEquals(nid3, NodeIdProtoTranslator.translate(reply.getRoleInfo().getMaster()));
    }

    private class MockMastershipService extends MastershipServiceAdapter {

        @Override
        public MastershipRole getLocalRole(DeviceId deviceId) {
            return mastershipMap.get(deviceId);
        }

        @Override
        public MastershipRole requestRoleForSync(DeviceId deviceId) {
            return mastershipMap.get(deviceId);
        }

        @Override
        public NodeId getMasterFor(DeviceId deviceId) {
            return nodeIdMap.get(deviceId);
        }

        @Override
        public RoleInfo getNodesFor(DeviceId deviceId) {
            return roleInfoMap.get(deviceId);
        }
    }
}
