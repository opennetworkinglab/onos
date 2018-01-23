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
import org.onosproject.grpc.net.device.models.PortEnumsProto;
import org.onosproject.grpc.net.device.models.PortStatisticsProtoOuterClass.PortStatisticsProto;
import org.onosproject.grpc.nb.net.device.DeviceServiceGrpc.DeviceServiceImplBase;
import org.onosproject.grpc.net.models.MastershipRoleProtoOuterClass;
import org.onosproject.grpc.net.models.PortProtoOuterClass.PortProto;
import org.onosproject.grpc.net.device.models.DeviceEnumsProto;
import org.onosproject.protobuf.api.GrpcServiceRegistry;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.MastershipRole;
import org.onosproject.net.PortNumber;
import org.onosproject.net.device.DeviceService;
import org.onosproject.incubator.protobuf.models.net.device.DeviceProtoTranslator;
import org.slf4j.Logger;

import static org.onosproject.grpc.nb.net.device.DeviceServiceNb.*;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * A server that provides access to the methods exposed by {@link DeviceService}.
 * TODO this requires major refactoring, translation should be delegated to calls to
 * TODO{@link DeviceProtoTranslator}.
 */
@Beta
@Component(immediate = true)
public class GrpcNbDeviceService {

    private final Logger log = getLogger(getClass());

    private DeviceServiceNbServerInternal instance = null;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected GrpcServiceRegistry registry;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

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
     * Register Device Service, Used for unit testing purposes.
     *
     * @return An instance of binding Device service
     */
    public InProcessServer<BindableService> registerInProcessServer() {
        InProcessServer<BindableService> inprocessServer =
                new InProcessServer(GrpcNbDeviceService.DeviceServiceNbServerInternal.class);
        inprocessServer.addServiceToBind(getInnerInstance());

        return inprocessServer;
    }

    private final class DeviceServiceNbServerInternal extends DeviceServiceImplBase {

        private DeviceServiceNbServerInternal() {
            super();
        }

        @Override
        public void getDeviceCount(
                getDeviceCountRequest request,
                StreamObserver<getDeviceCountReply> responseObserver) {
            responseObserver
                    .onNext(getDeviceCountReply
                            .newBuilder()
                            .setDeviceCount(
                                    deviceService.getDeviceCount())
                            .build());
            responseObserver.onCompleted();
        }

        //FIXME NOTE: this will be switched to a streaming version.
        @Override
        public void getDevices(getDevicesRequest request,
                               StreamObserver<getDevicesReply> responseObserver) {
            getDevicesReply.Builder replyBuilder = getDevicesReply.newBuilder();
            deviceService.getDevices().forEach(d -> {
                replyBuilder.addDevice(
                        org.onosproject.grpc.net.models.DeviceProtoOuterClass.DeviceProto
                                .newBuilder()
                                .setDeviceId(d.id().toString())
                                .setType(
                                        DeviceEnumsProto.DeviceTypeProto
                                                .valueOf(d.type().toString()))
                                .setManufacturer(d.manufacturer())
                                .setHwVersion(d.hwVersion())
                                .setSwVersion(d.swVersion())
                                .setSerialNumber(d.serialNumber())
                                .setChassisId(d.chassisId().toString())
                                .build());
            });
            responseObserver.onNext(replyBuilder.build());
            responseObserver.onCompleted();
        }

        //FIXME NOTE: this will be switched to a streaming version.
        @Override
        public void getAvailableDevices(getAvailableDevicesRequest request,
                                        StreamObserver
                                                <getAvailableDevicesReply> responseObserver) {
            getAvailableDevicesReply.Builder replyBuilder = getAvailableDevicesReply.newBuilder();
            deviceService.getAvailableDevices().forEach(d -> {
                replyBuilder.addDevice(
                        org.onosproject.grpc.net.models.DeviceProtoOuterClass.DeviceProto
                                .newBuilder()
                                .setDeviceId(d.id().toString())
                                .setType(DeviceEnumsProto.DeviceTypeProto.valueOf(
                                        d.type().toString()))
                                .setManufacturer(d.manufacturer())
                                .setHwVersion(d.hwVersion())
                                .setSwVersion(d.swVersion())
                                .setSerialNumber(d.serialNumber())
                                .setChassisId(d.chassisId().toString())
                                .build());
            });
            responseObserver.onNext(replyBuilder.build());
            responseObserver.onCompleted();
        }

        @Override
        public void getDevice(getDeviceRequest request,
                              io.grpc.stub.StreamObserver<getDeviceReply> responseObserver) {
            org.onosproject.net.Device device = deviceService.getDevice(
                    DeviceId.deviceId(request.getDeviceId()));
            responseObserver.onNext(
                    getDeviceReply.newBuilder().setDevice(
                            org.onosproject.grpc.net.models.DeviceProtoOuterClass.DeviceProto
                                    .newBuilder()
                                    .setDeviceId(device.id().toString())
                                    .setType(
                                            //TODO check for better approach to mapping between enum varieties
                                            DeviceEnumsProto.DeviceTypeProto.valueOf(device.type().toString()))
                                    .setManufacturer(device.manufacturer())
                                    .setHwVersion(device.hwVersion())
                                    .setSwVersion(device.swVersion())
                                    .setSerialNumber(device.serialNumber())
                                    .setChassisId(device.chassisId().toString())
                                    .build()).build());
            responseObserver.onCompleted();
        }

        @Override
        public void getRole(getRoleRequest request,
                            StreamObserver<getRoleReply> responseObserver) {
            DeviceId deviceId = DeviceId.deviceId(request.getDeviceId());
            MastershipRole role = deviceService.getRole(deviceId);
            MastershipRoleProtoOuterClass.MastershipRoleProto mastershipRole =
                    MastershipRoleProtoOuterClass.MastershipRoleProto.valueOf(role.toString());
            responseObserver.onNext(getRoleReply.newBuilder()
                    .setRole(mastershipRole).build());
            responseObserver.onCompleted();
        }

        //FIXME NOTE: this may be switched to a streaming version.
        @Override
        public void getPorts(getPortsRequest request, StreamObserver<getPortsReply> responseObserver) {
            getPortsReply.Builder replyBuilder = getPortsReply.newBuilder();
            deviceService.getPorts(
                    DeviceId.deviceId(request.getDeviceId()))
                    .forEach(port -> {
                        PortProto.Builder portBuilder = PortProto
                                .newBuilder()
                                .setPortNumber(port.number().toString())
                                .setIsEnabled(port.isEnabled())
                                .setType(PortEnumsProto.PortTypeProto.valueOf(port.type().toString()))
                                .setPortSpeed(port.portSpeed());
                        port.annotations().keys().forEach(key -> portBuilder
                                .putAnnotations(key, port.annotations().value(key)));

                        replyBuilder.addPort(portBuilder.build());
                    });
            responseObserver.onNext(replyBuilder.build());
            responseObserver.onCompleted();
        }

        //FIXME NOTE: this may be switched to a streaming version.
        @Override
        public void getPortStatistics(getPortStatisticsRequest request,
                                      StreamObserver<getPortStatisticsReply> responseObserver) {
            getPortStatisticsReply.Builder replyBuilder = getPortStatisticsReply.newBuilder();
            deviceService.getPortStatistics(DeviceId.deviceId(request.getDeviceId()))
                    .forEach(statistic -> {
                        replyBuilder.addPortStatistics(
                                PortStatisticsProto
                                        .newBuilder()
                                        .setPort(statistic.port())
                                        .setPacketsReceived(statistic.packetsReceived())
                                        .setPacketsSent(statistic.packetsSent())
                                        .setBytesReceived(statistic.bytesReceived())
                                        .setBytesSent(statistic.bytesSent())
                                        .setPacketsRxDropped(statistic.packetsRxDropped())
                                        .setPacketsTxDropped(statistic.packetsTxDropped())
                                        .setPacketsRxErrors(statistic.packetsRxErrors())
                                        .setPacketsTxErrors(statistic.packetsTxErrors())
                                        .setDurationSec(statistic.durationSec())
                                        .setDurationNano(statistic.durationNano())
                                        .setIsZero(statistic.isZero())
                                        .build());
                    });
            responseObserver.onNext(replyBuilder.build());
            responseObserver.onCompleted();
        }

        //FIXME NOTE: this may be switched to a streaming version.
        @Override
        public void getPortDeltaStatistics(getPortDeltaStatisticsRequest request,
                                           StreamObserver<getPortDeltaStatisticsReply> responseObserver) {
            getPortDeltaStatisticsReply.Builder replyBuilder = getPortDeltaStatisticsReply.newBuilder();
            deviceService.getPortDeltaStatistics(DeviceId.deviceId(request.getDeviceId()))
                    .forEach(statistic -> {
                        replyBuilder.addPortStatistics(
                                PortStatisticsProto
                                        .newBuilder()
                                        .setPort(statistic.port())
                                        .setPacketsReceived(statistic.packetsReceived())
                                        .setPacketsSent(statistic.packetsSent())
                                        .setBytesReceived(statistic.bytesReceived())
                                        .setBytesSent(statistic.bytesSent())
                                        .setPacketsRxDropped(statistic.packetsRxDropped())
                                        .setPacketsTxDropped(statistic.packetsTxDropped())
                                        .setPacketsRxErrors(statistic.packetsRxErrors())
                                        .setPacketsTxErrors(statistic.packetsTxErrors())
                                        .setDurationSec(statistic.durationSec())
                                        .setDurationNano(statistic.durationNano())
                                        .setIsZero(statistic.isZero())
                                        .build());
                    });
            responseObserver.onNext(replyBuilder.build());
            responseObserver.onCompleted();
        }

        @Override
        public void getStatisticsForPort(getStatisticsForPortRequest request,
                                         StreamObserver<getStatisticsForPortReply> responseObserver) {
            org.onosproject.net.device.PortStatistics statistics = deviceService
                    .getStatisticsForPort(DeviceId.deviceId(request.getDeviceId()),
                            PortNumber.portNumber(request.getPortNumber()));
            responseObserver.onNext(
                    getStatisticsForPortReply
                            .newBuilder()
                            .setPortStatistics(
                                    PortStatisticsProto
                                            .newBuilder()
                                            .setPort(statistics.port())
                                            .setPacketsReceived(statistics.packetsReceived())
                                            .setPacketsSent(statistics.packetsSent())
                                            .setBytesReceived(statistics.bytesReceived())
                                            .setBytesSent(statistics.bytesSent())
                                            .setPacketsRxDropped(statistics.packetsRxDropped())
                                            .setPacketsTxDropped(statistics.packetsTxDropped())
                                            .setPacketsRxErrors(statistics.packetsRxErrors())
                                            .setPacketsTxErrors(statistics.packetsTxErrors())
                                            .setDurationSec(statistics.durationSec())
                                            .setDurationNano(statistics.durationNano())
                                            .setIsZero(statistics.isZero())
                                            .build()).build());
            responseObserver.onCompleted();

        }

        @Override
        public void getDeltaStatisticsForPort(getDeltaStatisticsForPortRequest request,
                                              StreamObserver<getDeltaStatisticsForPortReply> responseObserver) {
            org.onosproject.net.device.PortStatistics statistics = deviceService
                    .getDeltaStatisticsForPort(DeviceId.deviceId(request.getDeviceId()),
                            PortNumber.portNumber(request.getPortNumber()));
            responseObserver.onNext(
                    getDeltaStatisticsForPortReply
                            .newBuilder()
                            .setPortStatistics(
                                    PortStatisticsProto
                                            .newBuilder()
                                            .setPort(statistics.port())
                                            .setPacketsReceived(statistics.packetsReceived())
                                            .setPacketsSent(statistics.packetsSent())
                                            .setBytesReceived(statistics.bytesReceived())
                                            .setBytesSent(statistics.bytesSent())
                                            .setPacketsRxDropped(statistics.packetsRxDropped())
                                            .setPacketsTxDropped(statistics.packetsTxDropped())
                                            .setPacketsRxErrors(statistics.packetsRxErrors())
                                            .setPacketsTxErrors(statistics.packetsTxErrors())
                                            .setDurationSec(statistics.durationSec())
                                            .setDurationNano(statistics.durationNano())
                                            .setIsZero(statistics.isZero())
                                            .build()).build());
            responseObserver.onCompleted();
        }

        @Override
        public void getPort(getPortRequest request,
                            StreamObserver<getPortReply> responseObserver) {
            //FIXME getting deviceId here is dangerous because it is not guaranteed to be populated as port of a OneOf
            org.onosproject.net.Port port = deviceService.getPort(
                    new ConnectPoint(DeviceId.deviceId(
                            request.getConnectPoint().getDeviceId()),
                            PortNumber.portNumber(
                                    request.getConnectPoint()
                                            .getPortNumber())));
            PortProto.Builder portBuilder =
                    PortProto.newBuilder()
                            .setPortNumber(port.number().toString())
                            .setIsEnabled(port.isEnabled())
                            .setType(
                                    PortEnumsProto.PortTypeProto
                                            .valueOf(port.type().toString()))
                            .setPortSpeed(port.portSpeed());

            port.annotations().keys().forEach(key -> portBuilder
                    .putAnnotations(key, port.annotations().value(key)));

            responseObserver.onNext(getPortReply
                    .newBuilder()
                    .setPort(portBuilder.build())
                    .build());
            responseObserver.onCompleted();
        }

        @Override
        public void isAvailable(isAvailableRequest request,
                                StreamObserver<isAvailableReply> responseObserver) {
            responseObserver.onNext(
                    isAvailableReply
                            .newBuilder()
                            .setIsAvailable(
                                    deviceService.isAvailable(
                                            DeviceId.deviceId(
                                                    request.getDeviceId())))
                            .build());
            responseObserver.onCompleted();
        }

        @Override
        public void localStatus(localStatusRequest request,
                                StreamObserver<localStatusReply> responseObserver) {
            responseObserver.onNext(
                    localStatusReply
                            .newBuilder()
                            .setStatus(
                                    deviceService.localStatus(
                                            DeviceId.deviceId(request.getDeviceId())))
                            .build());
            responseObserver.onCompleted();
        }
    }

    private DeviceServiceNbServerInternal getInnerInstance() {
        if (instance == null) {
            instance = new DeviceServiceNbServerInternal();
        }
        return instance;
    }
}