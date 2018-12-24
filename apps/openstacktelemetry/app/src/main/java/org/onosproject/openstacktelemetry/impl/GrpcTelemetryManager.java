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

import com.google.common.collect.Sets;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.openstacktelemetry.api.GrpcTelemetryAdminService;
import org.onosproject.openstacktelemetry.api.OpenstackTelemetryService;
import org.onosproject.openstacktelemetry.api.TelemetryConfigService;
import org.onosproject.openstacktelemetry.api.config.GrpcTelemetryConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

import static org.onosproject.openstacktelemetry.api.Constants.GRPC_SCHEME;
import static org.onosproject.openstacktelemetry.api.config.TelemetryConfig.ConfigType.GRPC;
import static org.onosproject.openstacktelemetry.config.DefaultGrpcTelemetryConfig.fromTelemetryConfig;

/**
 * gRPC telemetry manager.
 */
@Component(immediate = true)
@Service
public class GrpcTelemetryManager implements GrpcTelemetryAdminService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OpenstackTelemetryService openstackTelemetryService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TelemetryConfigService telemetryConfigService;

    private Set<ManagedChannel> channels = Sets.newConcurrentHashSet();

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
    public void start() {
        telemetryConfigService.getConfigsByType(GRPC).forEach(c -> {
            GrpcTelemetryConfig grpcConfig = fromTelemetryConfig(c);

            if (grpcConfig != null && !c.name().equals(GRPC_SCHEME) && c.enabled()) {
                ManagedChannel channel = ManagedChannelBuilder
                        .forAddress(grpcConfig.address(), grpcConfig.port())
                        .maxInboundMessageSize(grpcConfig.maxInboundMsgSize())
                        .usePlaintext(grpcConfig.usePlaintext())
                        .build();

                channels.add(channel);
            }
        });

        log.info("gRPC producer has Started");
    }

    @Override
    public void stop() {
        channels.forEach(ManagedChannel::shutdown);
        log.info("gRPC producer has Stopped");
    }

    @Override
    public void restart() {
        stop();
        start();
    }

    @Override
    public Object publish(Object record) {
        // TODO: need to find a way to invoke gRPC endpoint using channel

        if (channels.isEmpty()) {
            log.debug("gRPC telemetry service has not been enabled!");
        }

        return null;
    }

    @Override
    public boolean isRunning() {
        return !channels.isEmpty();
    }
}
