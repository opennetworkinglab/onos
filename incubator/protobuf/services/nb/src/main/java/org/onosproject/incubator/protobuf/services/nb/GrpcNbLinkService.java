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
import org.onosproject.grpc.nb.net.link.LinkServiceGrpc.LinkServiceImplBase;
import org.onosproject.grpc.nb.net.link.LinkServiceNb;
import org.onosproject.grpc.nb.net.link.LinkServiceNb.getActiveLinksReply;
import org.onosproject.grpc.nb.net.link.LinkServiceNb.getActiveLinksRequest;
import org.onosproject.grpc.nb.net.link.LinkServiceNb.getDeviceEgressLinksReply;
import org.onosproject.grpc.nb.net.link.LinkServiceNb.getDeviceEgressLinksRequest;
import org.onosproject.grpc.nb.net.link.LinkServiceNb.getDeviceIngressLinksReply;
import org.onosproject.grpc.nb.net.link.LinkServiceNb.getDeviceIngressLinksRequest;
import org.onosproject.grpc.nb.net.link.LinkServiceNb.getDeviceLinksReply;
import org.onosproject.grpc.nb.net.link.LinkServiceNb.getDeviceLinksRequest;
import org.onosproject.grpc.nb.net.link.LinkServiceNb.getEgressLinksReply;
import org.onosproject.grpc.nb.net.link.LinkServiceNb.getEgressLinksRequest;
import org.onosproject.grpc.nb.net.link.LinkServiceNb.getIngressLinksReply;
import org.onosproject.grpc.nb.net.link.LinkServiceNb.getIngressLinksRequest;
import org.onosproject.grpc.nb.net.link.LinkServiceNb.getLinkCountReply;
import org.onosproject.grpc.nb.net.link.LinkServiceNb.getLinkCountRequest;
import org.onosproject.grpc.nb.net.link.LinkServiceNb.getLinkReply;
import org.onosproject.grpc.nb.net.link.LinkServiceNb.getLinkRequest;
import org.onosproject.grpc.nb.net.link.LinkServiceNb.getLinksReply;
import org.onosproject.grpc.nb.net.link.LinkServiceNb.getLinksRequest;
import org.onosproject.incubator.protobuf.models.net.ConnectPointProtoTranslator;
import org.onosproject.incubator.protobuf.models.net.LinkProtoTranslator;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.link.LinkService;
import org.onosproject.protobuf.api.GrpcServiceRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A server that provides access to the methods exposed by {@link org.onosproject.net.link.LinkService}.
 */
@Beta
@Component(immediate = true)
public class GrpcNbLinkService {

    private static final Logger log = LoggerFactory.getLogger(GrpcNbLinkService.class);

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected GrpcServiceRegistry registry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected LinkService linkService;

    private LinkServiceNbServerInternal innerClassInstance;

    @Activate
    public void activate() {

        registry.register(getInnerInstance());
        log.info("Started.");
    }

    @Deactivate
    public void deactivate() {

        registry.unregister(getInnerInstance());
        log.info("Stopped.");
    }

    /**
     * Register Link Service, used for unit testing purposes.
     *
     * @return An instance of binding Link service
     */
    public InProcessServer<BindableService> registerInProcessServer() {
        InProcessServer<BindableService> inprocessServer =
                new InProcessServer(LinkServiceNb.class);
        inprocessServer.addServiceToBind(getInnerInstance());

        return inprocessServer;
    }

    private class LinkServiceNbServerInternal extends LinkServiceImplBase {

        public LinkServiceNbServerInternal() {
            super();
        }

        @Override
        public void getLinkCount(getLinkCountRequest request,
                                 StreamObserver<getLinkCountReply> responseObserver) {
            responseObserver
                    .onNext(getLinkCountReply
                                    .newBuilder()
                                    .setLinkCount(linkService.getLinkCount())
                                    .build());
            responseObserver.onCompleted();
        }

        @Override
        public void getLink(getLinkRequest request,
                            StreamObserver<getLinkReply> responseObserver) {
            ConnectPoint src = ConnectPointProtoTranslator.translate(request.getSrc()).get();
            ConnectPoint dst = ConnectPointProtoTranslator.translate(request.getDst()).get();

            org.onosproject.net.Link link = linkService.getLink(src, dst);
            getLinkReply reply = getLinkReply.newBuilder()
                    .setLink(LinkProtoTranslator.translate(link)).build();

            responseObserver.onNext(reply);
            responseObserver.onCompleted();
        }

        @Override
        public void getActiveLinks(getActiveLinksRequest request,
                                   StreamObserver<getActiveLinksReply> responseObserver) {
            getActiveLinksReply.Builder builder = getActiveLinksReply.newBuilder();
            linkService.getActiveLinks().forEach(l -> {
                builder.addLink(LinkProtoTranslator.translate(l));
            });
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        }

        @Override
        public void getDeviceLinks(getDeviceLinksRequest request,
                                   StreamObserver<getDeviceLinksReply> responseObserver) {
            DeviceId deviceId = DeviceId.deviceId(request.getDeviceId());
            getDeviceLinksReply.Builder builder = getDeviceLinksReply.newBuilder();
            linkService.getDeviceLinks(deviceId).forEach(l -> {
                builder.addLink(LinkProtoTranslator.translate(l));
            });
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        }

        @Override
        public void getDeviceEgressLinks(getDeviceEgressLinksRequest request,
                                         StreamObserver<getDeviceEgressLinksReply> responseObserver) {
            DeviceId deviceId = DeviceId.deviceId(request.getDeviceId());
            getDeviceEgressLinksReply.Builder builder = getDeviceEgressLinksReply.newBuilder();
            linkService.getDeviceEgressLinks(deviceId).forEach(l -> {
                builder.addLink(LinkProtoTranslator.translate(l));
            });
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        }

        @Override
        public void getDeviceIngressLinks(getDeviceIngressLinksRequest request,
                                          StreamObserver<getDeviceIngressLinksReply> responseObserver) {
            DeviceId deviceId = DeviceId.deviceId(request.getDeviceId());
            getDeviceIngressLinksReply.Builder builder = getDeviceIngressLinksReply.newBuilder();
            linkService.getDeviceIngressLinks(deviceId).forEach(l -> {
                builder.addLink(LinkProtoTranslator.translate(l));
            });
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        }

        @Override
        public void getEgressLinks(getEgressLinksRequest request,
                                   StreamObserver<getEgressLinksReply> responseObserver) {
            ConnectPoint connectPoint = ConnectPointProtoTranslator.translate(request.getConnectPoint()).get();
            getEgressLinksReply.Builder builder = getEgressLinksReply.newBuilder();
            linkService.getEgressLinks(connectPoint).forEach(l -> {
                builder.addLink(LinkProtoTranslator.translate(l));
            });
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        }

        @Override
        public void getIngressLinks(getIngressLinksRequest request,
                                    StreamObserver<getIngressLinksReply> responseObserver) {
            ConnectPoint connectPoint = ConnectPointProtoTranslator.translate(request.getConnectPoint()).get();

            getIngressLinksReply.Builder builder = getIngressLinksReply.newBuilder();
            linkService.getIngressLinks(connectPoint).forEach(l -> {
                builder.addLink(LinkProtoTranslator.translate(l));
            });
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        }

        @Override
        public void getLinks(getLinksRequest request,
                             StreamObserver<getLinksReply> responseObserver) {
            ConnectPoint connectPoint = ConnectPointProtoTranslator.translate(request.getConnectPoint()).get();

            getLinksReply.Builder builder = getLinksReply.newBuilder();
            linkService.getLinks(connectPoint).forEach(l -> {
                builder.addLink(LinkProtoTranslator.translate(l));
            });
            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();
        }
    }

    private LinkServiceNbServerInternal getInnerInstance() {
        if (innerClassInstance == null) {
            innerClassInstance = new LinkServiceNbServerInternal();
        }
        return innerClassInstance;
    }
}



