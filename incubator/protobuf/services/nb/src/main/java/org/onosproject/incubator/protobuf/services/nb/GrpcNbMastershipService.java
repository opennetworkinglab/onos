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


import com.google.common.annotations.Beta;
import io.grpc.BindableService;
import io.grpc.stub.StreamObserver;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.cluster.NodeId;
import org.onosproject.cluster.RoleInfo;
import org.onosproject.grpc.nb.mastership.MastershipServiceGrpc;
import org.onosproject.grpc.nb.mastership.MastershipServiceNb;
import org.onosproject.grpc.nb.mastership.MastershipServiceNb.getLocalRoleReply;
import org.onosproject.grpc.nb.mastership.MastershipServiceNb.getLocalRoleRequest;
import org.onosproject.grpc.nb.mastership.MastershipServiceNb.isLocalMasterReply;
import org.onosproject.grpc.nb.mastership.MastershipServiceNb.isLocalMasterRequest;
import org.onosproject.grpc.nb.mastership.MastershipServiceNb.relinquishMastershipSyncReply;
import org.onosproject.grpc.nb.mastership.MastershipServiceNb.relinquishMastershipSyncRequest;
import org.onosproject.grpc.nb.mastership.MastershipServiceNb.requestRoleForSyncReply;
import org.onosproject.grpc.nb.mastership.MastershipServiceNb.requestRoleForSyncRequest;
import org.onosproject.incubator.protobuf.models.cluster.NodeIdProtoTranslator;
import org.onosproject.incubator.protobuf.models.cluster.RoleInfoProtoTranslator;
import org.onosproject.incubator.protobuf.models.net.MastershipRoleProtoTranslator;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.DeviceId;
import org.onosproject.net.MastershipRole;
import org.onosproject.protobuf.api.GrpcServiceRegistry;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * A server that provides access to the methods exposed by {@link MastershipService}.
 */

@Beta
@Component(immediate = true)
public class GrpcNbMastershipService {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected GrpcServiceRegistry registry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MastershipService mastershipService;

    private MastershipServiceNbServerInternal instance = null;

    @Activate
    public void activate() {
        registry.register(getInnerInstance());
        log.info("Started.");
    }

    @Deactivate
    public void deactivate() {
        registry.unregister(getInnerInstance());
        log.info("Stopped");
    }

    /**
     * Register Mastership Service, used for unit testing purposes.
     *
     * @return an instance of binding Mastership service
     */
    public InProcessServer<BindableService> registerInProcessServer() {
        InProcessServer<BindableService> inprocessServer =
                new InProcessServer(GrpcNbMastershipService.MastershipServiceNbServerInternal.class);
        inprocessServer.addServiceToBind(getInnerInstance());

        return inprocessServer;
    }

    private class MastershipServiceNbServerInternal extends MastershipServiceGrpc.MastershipServiceImplBase {

        @Override
        public void getLocalRole(getLocalRoleRequest request,
                                 StreamObserver<getLocalRoleReply> responseObserver) {
            MastershipRole mr = mastershipService.getLocalRole(DeviceId.deviceId(request.getDeviceId()));

            responseObserver.onNext(getLocalRoleReply.newBuilder()
                    .setMastershipRole(MastershipRoleProtoTranslator.translate(mr))
                    .build());
            responseObserver.onCompleted();
        }

        @Override
        public void isLocalMaster(isLocalMasterRequest request,
                                  StreamObserver<isLocalMasterReply> responseObserver) {
            boolean isLocalMaster = mastershipService.isLocalMaster(DeviceId.deviceId(request.getDeviceId()));

            responseObserver.onNext(isLocalMasterReply.newBuilder()
                    .setIsLocalMaster(isLocalMaster)
                    .build());
            responseObserver.onCompleted();
        }

        @Override
        public void requestRoleForSync(requestRoleForSyncRequest request,
                                       StreamObserver<requestRoleForSyncReply> responseObserver) {
            MastershipRole mr = mastershipService.requestRoleForSync(DeviceId.deviceId(request.getDeviceId()));

            responseObserver.onNext(requestRoleForSyncReply.newBuilder()
                    .setMastershipRole(MastershipRoleProtoTranslator.translate(mr))
                    .build());
            responseObserver.onCompleted();
        }

        @Override
        public void relinquishMastershipSync(relinquishMastershipSyncRequest request,
                                             StreamObserver<relinquishMastershipSyncReply> responseObserver) {
            relinquishMastershipSyncReply.Builder replyBuilder = relinquishMastershipSyncReply.newBuilder();
            mastershipService.relinquishMastershipSync(DeviceId.deviceId(request.getDeviceId()));

            responseObserver.onNext(replyBuilder.build());
            responseObserver.onCompleted();
        }

        @Override
        public void getMasterFor(MastershipServiceNb.getMasterForRequest request,
                                 StreamObserver<MastershipServiceNb.getMasterForReply> responseObserver) {
            NodeId nodeId = mastershipService.getMasterFor(DeviceId.deviceId(request.getDeviceId()));

            responseObserver.onNext(MastershipServiceNb.getMasterForReply.newBuilder()
                    .setNodeId(NodeIdProtoTranslator.translate(nodeId))
                    .build());
            responseObserver.onCompleted();
        }

        @Override
        public void getNodesFor(MastershipServiceNb.getNodesForRequest request,
                                StreamObserver<MastershipServiceNb.getNodesForReply> responseObserver) {
            RoleInfo roleInfo = mastershipService.getNodesFor(DeviceId.deviceId(request.getDeviceId()));

            responseObserver.onNext(MastershipServiceNb.getNodesForReply.newBuilder()
                    .setRoleInfo(RoleInfoProtoTranslator.translate(roleInfo))
                    .build());
            responseObserver.onCompleted();
        }
    }

    private MastershipServiceNbServerInternal getInnerInstance() {
        if (instance == null) {
            instance = new MastershipServiceNbServerInternal();
        }
        return instance;
    }
}
