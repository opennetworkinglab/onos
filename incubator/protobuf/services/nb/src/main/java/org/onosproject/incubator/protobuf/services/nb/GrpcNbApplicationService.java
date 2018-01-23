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
import org.onosproject.app.ApplicationService;
import org.onosproject.app.ApplicationState;
import org.onosproject.core.Application;
import org.onosproject.core.ApplicationId;
import org.onosproject.grpc.nb.app.ApplicationServiceGrpc.ApplicationServiceImplBase;
import org.onosproject.grpc.nb.app.ApplicationServiceNb;
import org.onosproject.grpc.nb.app.ApplicationServiceNb.getApplicationReply;
import org.onosproject.grpc.nb.app.ApplicationServiceNb.getApplicationsReply;
import org.onosproject.grpc.nb.app.ApplicationServiceNb.getIdReply;
import org.onosproject.grpc.nb.app.ApplicationServiceNb.getPermissionsReply;
import org.onosproject.grpc.nb.app.ApplicationServiceNb.getStateReply;
import org.onosproject.incubator.protobuf.models.core.ApplicationEnumsProtoTranslator;
import org.onosproject.incubator.protobuf.models.core.ApplicationIdProtoTranslator;
import org.onosproject.incubator.protobuf.models.core.ApplicationProtoTranslator;
import org.onosproject.incubator.protobuf.models.security.PermissionProtoTranslator;
import org.onosproject.protobuf.api.GrpcServiceRegistry;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * A server that provides access to the methods exposed by {@link ApplicationService}.
 */

@Beta
@Component(immediate = true)
public class GrpcNbApplicationService {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected GrpcServiceRegistry registry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ApplicationService applicationService;

    private ApplicationServiceNbServerInternal instance = null;


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
     * Register Application Service, Used for unit testing purposes.
     *
     * @return An instance of binding Application service
     */
    public InProcessServer<BindableService> registerInProcessServer() {
        InProcessServer<BindableService> inprocessServer =
                new InProcessServer(GrpcNbApplicationService.ApplicationServiceNbServerInternal.class);
        inprocessServer.addServiceToBind(getInnerInstance());

        return inprocessServer;
    }

    private class ApplicationServiceNbServerInternal extends ApplicationServiceImplBase {

        public ApplicationServiceNbServerInternal() {
            super();
        }

        @Override
        public void getApplications(ApplicationServiceNb.getApplicationsRequest request,
                                    StreamObserver<getApplicationsReply> responseObserver) {
            getApplicationsReply.Builder replyBuilder = getApplicationsReply.newBuilder();

            applicationService.getApplications().forEach(a ->
                    replyBuilder.addApplication(ApplicationProtoTranslator.translate(a)));
            responseObserver.onNext(replyBuilder.build());
            responseObserver.onCompleted();
        }

        @Override
        public void getId(ApplicationServiceNb.getIdRequest request,
                          StreamObserver<getIdReply> responseObserver) {
            ApplicationId appId = applicationService.getId(request.getName());

            responseObserver.onNext(getIdReply.newBuilder()
                    .setApplicationId(ApplicationIdProtoTranslator.translate(appId)).build());
            responseObserver.onCompleted();
        }

        @Override
        public void getApplication(ApplicationServiceNb.getApplicationRequest request,
                                   StreamObserver<getApplicationReply> responseObserver) {

            Application application = applicationService.getApplication(
                    ApplicationIdProtoTranslator.translate(request.getApplicationId()));

            responseObserver.onNext(getApplicationReply.newBuilder()
                    .setApplication(ApplicationProtoTranslator
                            .translate(application)).build());
            responseObserver.onCompleted();
        }

        @Override
        public void getState(ApplicationServiceNb.getStateRequest request,
                             StreamObserver<getStateReply> responseObserver) {
            ApplicationState state = applicationService.getState(
                    ApplicationIdProtoTranslator.translate(request.getApplicationId()));

            responseObserver.onNext(getStateReply
                    .newBuilder().setState(ApplicationEnumsProtoTranslator
                            .translate(state)).build());
            responseObserver.onCompleted();
        }

        @Override
        public void getPermissions(ApplicationServiceNb.getPermissionsRequest request,
                                   StreamObserver<getPermissionsReply> responseObserver) {
            getPermissionsReply.Builder replyBuilder = getPermissionsReply.newBuilder();

            applicationService.getPermissions(ApplicationIdProtoTranslator
                    .translate(request.getApplicationId()))
                    .forEach(p -> replyBuilder.addPermission(
                            PermissionProtoTranslator.translate(p)));
            responseObserver.onNext(replyBuilder.build());
            responseObserver.onCompleted();
        }
    }

    private ApplicationServiceNbServerInternal getInnerInstance() {
        if (instance == null) {
            instance = new ApplicationServiceNbServerInternal();
        }
        return instance;
    }
}
