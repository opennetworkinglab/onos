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
package org.onosproject.openstacktelemetry.impl;

import io.grpc.ManagedChannel;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.openstacktelemetry.api.GrpcTelemetryAdminService;
import org.onosproject.openstacktelemetry.api.OpenstackTelemetryService;
import org.onosproject.openstacktelemetry.api.config.TelemetryConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * gRPC telemetry manager.
 */
@Component(immediate = true)
@Service
public class GrpcTelemetryManager implements GrpcTelemetryAdminService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OpenstackTelemetryService openstackTelemetryService;

    private ManagedChannel channel = null;

    @Activate
    protected void activate() {

        openstackTelemetryService.addTelemetryService(this);

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        stop();

        openstackTelemetryService.removeTelemetryService(this);

        log.info("Stopped");
    }

    @Override
    public void start(TelemetryConfig config) {
        if (channel != null) {
            log.info("gRPC producer has already been started");
            return;
        }

        // FIXME do not activate grpc service for now due to deps conflict
//        GrpcTelemetryConfig grpcConfig = (GrpcTelemetryConfig) config;
//        channel = ManagedChannelBuilder
//                .forAddress(grpcConfig.address(), grpcConfig.port())
//                .maxInboundMessageSize(grpcConfig.maxInboundMsgSize())
//                .usePlaintext(grpcConfig.usePlaintext())
//                .build();

        log.info("gRPC producer has Started");
    }

    @Override
    public void stop() {
        // FIXME do not activate grpc service for now due to deps conflict
//        if (channel != null) {
//            channel.shutdown();
//            channel = null;
//        }

        log.info("gRPC producer has Stopped");
    }

    @Override
    public void restart(TelemetryConfig config) {
        stop();
        start(config);
    }

    @Override
    public Object publish(Object record) {
        // TODO: need to find a way to invoke gRPC endpoint using channel

        if (channel == null) {
            log.warn("gRPC telemetry service has not been enabled!");
        }

        return null;
    }

    @Override
    public boolean isRunning() {
        return channel != null;
    }
}
