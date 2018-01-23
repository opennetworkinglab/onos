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
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.HostId;
import org.onosproject.net.Host;
import org.onosproject.net.host.HostService;
import org.onosproject.protobuf.api.GrpcServiceRegistry;
import org.slf4j.Logger;
import org.onosproject.grpc.nb.net.host.HostServiceGrpc.HostServiceImplBase;
import org.onosproject.incubator.protobuf.models.net.ConnectPointProtoTranslator;
import org.onosproject.incubator.protobuf.models.net.HostProtoTranslator;

import static org.onosproject.grpc.nb.net.host.HostServiceNb.getHostCountRequest;
import static org.onosproject.grpc.nb.net.host.HostServiceNb.getHostCountReply;
import static org.onosproject.grpc.nb.net.host.HostServiceNb.getHostsRequest;
import static org.onosproject.grpc.nb.net.host.HostServiceNb.getHostsReply;
import static org.onosproject.grpc.nb.net.host.HostServiceNb.getHostRequest;
import static org.onosproject.grpc.nb.net.host.HostServiceNb.getHostReply;
import static org.onosproject.grpc.nb.net.host.HostServiceNb.getHostsByVlanRequest;
import static org.onosproject.grpc.nb.net.host.HostServiceNb.getHostsByVlanReply;
import static org.onosproject.grpc.nb.net.host.HostServiceNb.getHostsByMacRequest;
import static org.onosproject.grpc.nb.net.host.HostServiceNb.getHostsByMacReply;
import static org.onosproject.grpc.nb.net.host.HostServiceNb.getHostsByIpRequest;
import static org.onosproject.grpc.nb.net.host.HostServiceNb.getHostsByIpReply;
import static org.onosproject.grpc.nb.net.host.HostServiceNb.getConnectedHostsRequest;
import static org.onosproject.grpc.nb.net.host.HostServiceNb.getConnectedHostsReply;
import static org.onosproject.grpc.nb.net.host.HostServiceNb.startMonitoringIpRequest;
import static org.onosproject.grpc.nb.net.host.HostServiceNb.startMonitoringIpReply;
import static org.onosproject.grpc.nb.net.host.HostServiceNb.stopMonitoringIpRequest;
import static org.onosproject.grpc.nb.net.host.HostServiceNb.stopMonitoringIpReply;
import static org.onosproject.grpc.nb.net.host.HostServiceNb.requestMacRequest;
import static org.onosproject.grpc.nb.net.host.HostServiceNb.requestMacReply;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * A server that provides access to the methods exposed by {@link HostService}.
 */
@Beta
@Component(immediate = true)
public class GrpcNbHostService {

    private final Logger log = getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected GrpcServiceRegistry registry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostService hostService;

    private HostServiceNBServerInternal instance = null;

    @Activate
    public void activate() {

        registry.register(getInnerInstance());
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {

        registry.unregister(getInnerInstance());
        log.info("Stoped");
    }

    /**
     * Register Host Service, Used for unit testing purposes.
     *
     * @return An instance of binding Host service
     */
    public InProcessServer<BindableService> registerInProcessServer() {
        InProcessServer<BindableService> inprocessServer =
                new InProcessServer(HostServiceNBServerInternal.class);
        inprocessServer.addServiceToBind(getInnerInstance());

        return inprocessServer;
    }

    /**
     * Host Service NorthBound implementation.
     */
    private final class HostServiceNBServerInternal extends HostServiceImplBase {

        private HostServiceNBServerInternal() {
            super();
        }

        @Override
        public void getHostCount(getHostCountRequest request,
                                 StreamObserver<getHostCountReply> responseObserver) {

            responseObserver.onNext(getHostCountReply.newBuilder()
                                            .setHostCount(hostService.getHostCount())
                                            .build());
            responseObserver.onCompleted();
        }

        @Override
        public void getHosts(getHostsRequest request,
                             StreamObserver<getHostsReply> responseObserver) {

            getHostsReply.Builder replyBuilder = getHostsReply.newBuilder();

            hostService.getHosts().forEach(d -> replyBuilder.addHost(HostProtoTranslator.translate(d)));
            responseObserver.onNext(replyBuilder.build());
            responseObserver.onCompleted();
        }

        @Override
        public void getHost(getHostRequest request,
                            StreamObserver<getHostReply> responseObserver) {

            Host host = hostService.getHost(HostId.hostId(request.getHostId().toString()));

            responseObserver.onNext(getHostReply.newBuilder().setHost(HostProtoTranslator.translate(host)).build());
            responseObserver.onCompleted();
        }

        @Override
        public void getHostsByVlan(getHostsByVlanRequest request,
                                   StreamObserver<getHostsByVlanReply> responseObserver) {

            getHostsByVlanReply.Builder replyBuilder = getHostsByVlanReply.newBuilder();

            hostService.getHostsByVlan(VlanId.vlanId(request.getVlanId())).forEach(
                    d -> replyBuilder.addHost(HostProtoTranslator.translate(d)));
            responseObserver.onNext(replyBuilder.build());
            responseObserver.onCompleted();
        }

        @Override
        public void getHostsByMac(getHostsByMacRequest request,
                                  StreamObserver<getHostsByMacReply> responseObserver) {

            getHostsByMacReply.Builder replyBuilder = getHostsByMacReply.newBuilder();

            hostService.getHostsByMac(MacAddress.valueOf(request.getMac())).forEach(
                    d -> replyBuilder.addHost(HostProtoTranslator.translate(d)));
            responseObserver.onNext(replyBuilder.build());
            responseObserver.onCompleted();
        }

        @Override
        public void getHostsByIp(getHostsByIpRequest request,
                                 StreamObserver<getHostsByIpReply> responseObserver) {

            getHostsByIpReply.Builder replyBuilder = getHostsByIpReply.newBuilder();

            hostService.getHostsByIp(IpAddress.valueOf(request.getIpAddress())).forEach(
                    d -> replyBuilder.addHost(HostProtoTranslator.translate(d)));
            responseObserver.onNext(replyBuilder.build());
            responseObserver.onCompleted();
        }

        @Override
        public void getConnectedHosts(getConnectedHostsRequest request,
                                      StreamObserver<getConnectedHostsReply> responseObserver) {

            getConnectedHostsReply.Builder replyBuilder = getConnectedHostsReply.newBuilder();

            if (getConnectedHostsRequest.ConnectedHostCase.DEVICEID == request.getConnectedHostCase()) {
                hostService.getConnectedHosts(DeviceId.deviceId(request.getDeviceId()))
                        .forEach(d -> replyBuilder.addHost(HostProtoTranslator.translate(d)));
            } else if (getConnectedHostsRequest.ConnectedHostCase.CONNECT_POINT == request.getConnectedHostCase()) {
                hostService.getConnectedHosts(ConnectPointProtoTranslator.translate(request.getConnectPoint()).get())
                        .forEach(d -> replyBuilder.addHost(HostProtoTranslator.translate(d)));
            } else {
                log.warn("Both DeviceId and ConnectPoint are not set.");
            }

            responseObserver.onNext(replyBuilder.build());
            responseObserver.onCompleted();
        }

        @Override
        public void startMonitoringIp(startMonitoringIpRequest request,
                                      StreamObserver<startMonitoringIpReply> responseObserver) {

            hostService.startMonitoringIp(IpAddress.valueOf(request.getIpAddress()));
            responseObserver.onNext(startMonitoringIpReply.getDefaultInstance());
            responseObserver.onCompleted();
        }

        @Override
        public void stopMonitoringIp(stopMonitoringIpRequest request,
                                     StreamObserver<stopMonitoringIpReply> responseObserver) {

            hostService.stopMonitoringIp(IpAddress.valueOf(request.getIpAddress()));
            responseObserver.onNext(stopMonitoringIpReply.getDefaultInstance());
            responseObserver.onCompleted();
        }

        @Override
        public void requestMac(requestMacRequest request,
                               StreamObserver<requestMacReply> responseObserver) {

            hostService.requestMac(IpAddress.valueOf(request.getIpAddress()));
            responseObserver.onNext(requestMacReply.getDefaultInstance());
            responseObserver.onCompleted();
        }
    }

    private HostServiceNBServerInternal getInnerInstance() {
        if (instance == null) {
            instance = new HostServiceNBServerInternal();
        }
        return instance;
    }
}
