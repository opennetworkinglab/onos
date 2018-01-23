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
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.incubator.protobuf.models.cfg.ConfigPropertyProtoTranslator;
import org.onosproject.protobuf.api.GrpcServiceRegistry;
import org.slf4j.Logger;

import static org.onosproject.grpc.nb.cfg.ComponentConfigServiceGrpc.ComponentConfigServiceImplBase;
import static org.onosproject.grpc.nb.cfg.ComponentConfigServiceNb.getComponentNamesRequest;
import static org.onosproject.grpc.nb.cfg.ComponentConfigServiceNb.getComponentNamesReply;
import static org.onosproject.grpc.nb.cfg.ComponentConfigServiceNb.registerPropertiesRequest;
import static org.onosproject.grpc.nb.cfg.ComponentConfigServiceNb.registerPropertiesReply;
import static org.onosproject.grpc.nb.cfg.ComponentConfigServiceNb.unregisterPropertiesRequest;
import static org.onosproject.grpc.nb.cfg.ComponentConfigServiceNb.unregisterPropertiesReply;
import static org.onosproject.grpc.nb.cfg.ComponentConfigServiceNb.getPropertiesRequest;
import static org.onosproject.grpc.nb.cfg.ComponentConfigServiceNb.getPropertiesReply;
import static org.onosproject.grpc.nb.cfg.ComponentConfigServiceNb.setPropertyRequest;
import static org.onosproject.grpc.nb.cfg.ComponentConfigServiceNb.setPropertyReply;
import static org.onosproject.grpc.nb.cfg.ComponentConfigServiceNb.preSetPropertyRequest;
import static org.onosproject.grpc.nb.cfg.ComponentConfigServiceNb.preSetPropertyReply;
import static org.onosproject.grpc.nb.cfg.ComponentConfigServiceNb.unsetPropertyRequest;
import static org.onosproject.grpc.nb.cfg.ComponentConfigServiceNb.unsetPropertyReply;
import static org.slf4j.LoggerFactory.getLogger;


/**
 * A server that provides access to the methods exposed by {@link ComponentConfigService}.
 */
@Beta
@Component(immediate = true)
public class GrpcNbComponentConfigService {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected GrpcServiceRegistry registry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ComponentConfigService componentConfigService;

    private ComponentConfigServiceNbServerInternal instance = null;

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
     * Register ComponentConfig Service, Used for unit testing purposes.
     *
     * @return An instance of binding ComponentConfig service
     */
    public InProcessServer<BindableService> registerInProcessServer() {
        InProcessServer<BindableService> inprocessServer =
                new InProcessServer(ComponentConfigServiceNbServerInternal.class);
        inprocessServer.addServiceToBind(getInnerInstance());

        return inprocessServer;
    }

    private final class ComponentConfigServiceNbServerInternal extends ComponentConfigServiceImplBase {

        private ComponentConfigServiceNbServerInternal() {
            super();
        }

        @Override
        public void getComponentNames(getComponentNamesRequest request,
                                      StreamObserver<getComponentNamesReply> responseObserver) {

            getComponentNamesReply.Builder replyBuilder = getComponentNamesReply.newBuilder();

            componentConfigService.getComponentNames().forEach(n -> replyBuilder.addNames(n));
            responseObserver.onNext(replyBuilder.build());
            responseObserver.onCompleted();
        }

        @Override
        public void registerProperties(registerPropertiesRequest request,
                                       StreamObserver<registerPropertiesReply> responseObserver) {

            try {
                componentConfigService.registerProperties(Class.forName(request.getComponentClass()));
                responseObserver.onNext(registerPropertiesReply.getDefaultInstance());
                responseObserver.onCompleted();
            } catch (ClassNotFoundException e) {
                responseObserver.onError(e);
            }
        }

        @Override
        public void unregisterProperties(unregisterPropertiesRequest request,
                                         StreamObserver<unregisterPropertiesReply> responseObserver) {

            try {
                componentConfigService.unregisterProperties(Class.forName(request.getComponentClass()),
                                                            request.getClear());
                responseObserver.onNext(unregisterPropertiesReply.getDefaultInstance());
                responseObserver.onCompleted();
            } catch (ClassNotFoundException e) {
                responseObserver.onError(e);
            }
        }

        @Override
        public void getProperties(getPropertiesRequest request, StreamObserver<getPropertiesReply> responseObserver) {

            getPropertiesReply.Builder replyBuilder = getPropertiesReply.newBuilder();

            componentConfigService.getProperties(request.getComponentName())
                    .forEach(n -> replyBuilder.addConfigProperties(ConfigPropertyProtoTranslator.translate(n)));
            responseObserver.onNext(replyBuilder.build());
            responseObserver.onCompleted();
        }

        @Override
        public void setProperty(setPropertyRequest request, StreamObserver<setPropertyReply> responseObserver) {

            componentConfigService.setProperty(request.getComponentName(), request.getName(), request.getValue());
            responseObserver.onNext(setPropertyReply.getDefaultInstance());
            responseObserver.onCompleted();
        }

        @Override
        public void preSetProperty(preSetPropertyRequest request,
                                   StreamObserver<preSetPropertyReply> responseObserver) {

            componentConfigService.preSetProperty(request.getComponentName(), request.getName(), request.getValue());
            responseObserver.onNext(preSetPropertyReply.getDefaultInstance());
            responseObserver.onCompleted();
        }

        @Override
        public void unsetProperty(unsetPropertyRequest request, StreamObserver<unsetPropertyReply> responseObserver) {

            componentConfigService.unsetProperty(request.getComponentName(), request.getName());
            responseObserver.onNext(unsetPropertyReply.getDefaultInstance());
            responseObserver.onCompleted();
        }
    }

    private ComponentConfigServiceNbServerInternal getInnerInstance() {
        if (instance == null) {
            instance = new ComponentConfigServiceNbServerInternal();
        }
        return instance;
    }
}
