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

import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onosproject.incubator.protobuf.models.net.meter.MeterRequestProtoTranslator;
import org.onosproject.net.meter.MeterService;
import org.onosproject.incubator.protobuf.models.net.meter.MeterProtoTranslator;

import org.onosproject.grpc.nb.net.meter.MeterServiceGrpc.MeterServiceImplBase;
import org.onosproject.grpc.nb.net.meter.MeterServiceNbProto.submitRequest;
import org.onosproject.grpc.nb.net.meter.MeterServiceNbProto.submitReply;
import org.onosproject.grpc.nb.net.meter.MeterServiceNbProto.withdrawRequest;
import org.onosproject.grpc.nb.net.meter.MeterServiceNbProto.withdrawReply;
import org.onosproject.grpc.nb.net.meter.MeterServiceNbProto.getMeterRequest;
import org.onosproject.grpc.nb.net.meter.MeterServiceNbProto.getMeterReply;
import org.onosproject.grpc.nb.net.meter.MeterServiceNbProto.getAllMetersRequest;
import org.onosproject.grpc.nb.net.meter.MeterServiceNbProto.getAllMetersReply;
import org.onosproject.grpc.nb.net.meter.MeterServiceNbProto.getMetersRequest;
import org.onosproject.grpc.nb.net.meter.MeterServiceNbProto.getMetersReply;

import io.grpc.stub.StreamObserver;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Deactivate;
import com.google.common.annotations.Beta;
import org.apache.felix.scr.annotations.Component;

import org.onosproject.net.meter.Meter;
import org.onosproject.net.meter.MeterId;
import org.onosproject.net.DeviceId;
import org.onosproject.protobuf.api.GrpcServiceRegistry;

/**
 * A server that provides access to the methods exposed by {@link MeterService}.
 * TODO this requires major refactoring, translation should be delegated to calls to
 * TODO{@link MeterProtoTranslator}.
 */
@Beta
@Component(immediate = true)
public class GrpcNbMeterService extends MeterServiceImplBase {

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MeterService meterService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected GrpcServiceRegistry grpcServiceRegistry;

    @Activate
    public void activate() {
        grpcServiceRegistry.register(this);
    }

    @Deactivate
    public void deactivate() {
        grpcServiceRegistry.unregister(this);
    }

    @Override
    public void submit(submitRequest request,
                       StreamObserver<submitReply> responseObserver) {
        submitReply.Builder replyBuilder = submitReply.newBuilder();
        Meter meter = meterService.submit(MeterRequestProtoTranslator.translate(request.getMeter()));
        responseObserver.onNext(replyBuilder.setSubmitMeter(MeterProtoTranslator.translate(meter)).build());
        responseObserver.onCompleted();
    }

    @Override
    public void withdraw(withdrawRequest request,
                         StreamObserver<withdrawReply> responseObserver) {
        withdrawReply.Builder replyBuilder = withdrawReply.newBuilder();
        meterService.withdraw(MeterRequestProtoTranslator.translate(request.getMeter()),
                MeterId.meterId(request.getMeterId()));
        responseObserver.onNext(replyBuilder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void getMeter(getMeterRequest request,
                         StreamObserver<getMeterReply> responseObserver) {
        getMeterReply.Builder replyBuilder = getMeterReply.newBuilder();
        Meter meter = meterService.getMeter(DeviceId.deviceId(request.getDeviceId()),
                MeterId.meterId(request.getMeterId()));
        responseObserver.onNext(replyBuilder.setMeter(MeterProtoTranslator.translate(meter)).build());
        responseObserver.onCompleted();
    }

    @Override
    public void getAllMeters(getAllMetersRequest request,
                             StreamObserver<getAllMetersReply> responseObserver) {
        getAllMetersReply.Builder replyBuilder = getAllMetersReply.newBuilder();
        meterService.getAllMeters().forEach(d -> {
            replyBuilder.addMeters(MeterProtoTranslator.translate(d));
        });
        responseObserver.onNext(replyBuilder.build());
        responseObserver.onCompleted();
    }

    @Override
    public void getMeters(getMetersRequest request,
                          StreamObserver<getMetersReply> responseObserver) {
        getMetersReply.Builder replyBuilder = getMetersReply.newBuilder();
        meterService.getMeters(DeviceId.deviceId(request.getDeviceId())).forEach(d -> {
            replyBuilder.addMeters(MeterProtoTranslator.translate(d));
        });
        responseObserver.onNext(replyBuilder.build());
        responseObserver.onCompleted();
    }

}

