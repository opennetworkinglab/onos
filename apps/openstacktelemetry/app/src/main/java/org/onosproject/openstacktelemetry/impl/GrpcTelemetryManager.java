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

import com.google.common.collect.Maps;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.onosproject.openstacktelemetry.api.GrpcTelemetryAdminService;
import org.onosproject.openstacktelemetry.api.OpenstackTelemetryService;
import org.onosproject.openstacktelemetry.api.TelemetryConfigService;
import org.onosproject.openstacktelemetry.api.config.GrpcTelemetryConfig;
import org.onosproject.openstacktelemetry.api.config.TelemetryConfig;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static org.onosproject.openstacktelemetry.api.Constants.GRPC_SCHEME;
import static org.onosproject.openstacktelemetry.api.config.TelemetryConfig.ConfigType.GRPC;
import static org.onosproject.openstacktelemetry.api.config.TelemetryConfig.Status.ENABLED;
import static org.onosproject.openstacktelemetry.config.DefaultGrpcTelemetryConfig.fromTelemetryConfig;
import static org.onosproject.openstacktelemetry.util.OpenstackTelemetryUtil.testConnectivity;

/**
 * gRPC telemetry manager.
 */
@Component(immediate = true, service = GrpcTelemetryAdminService.class)
public class GrpcTelemetryManager implements GrpcTelemetryAdminService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected OpenstackTelemetryService openstackTelemetryService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected TelemetryConfigService telemetryConfigService;

    private Map<String, ManagedChannel> channels = Maps.newConcurrentMap();

    @Activate
    protected void activate() {

        openstackTelemetryService.addTelemetryService(this);

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        stopAll();

        openstackTelemetryService.removeTelemetryService(this);

        log.info("Stopped");
    }

    @Override
    public boolean start(String name) {
        boolean success = false;
        TelemetryConfig config = telemetryConfigService.getConfig(name);
        GrpcTelemetryConfig grpcConfig = fromTelemetryConfig(config);

        if (grpcConfig != null && !config.name().equals(GRPC_SCHEME) &&
                config.status() == ENABLED) {
            ManagedChannel channel = ManagedChannelBuilder
                    .forAddress(grpcConfig.address(), grpcConfig.port())
                    .maxInboundMessageSize(grpcConfig.maxInboundMsgSize())
                    .usePlaintext(grpcConfig.usePlaintext())
                    .build();

            if (testConnectivity(grpcConfig.address(), grpcConfig.port())) {
                channels.put(name, channel);
                success = true;
            } else {
                log.warn("Unable to connect to {}:{}, " +
                                "please check the connectivity manually",
                                grpcConfig.address(), grpcConfig.port());
            }
        }
        return success;
    }

    @Override
    public void stop(String name) {
        ManagedChannel channel = channels.get(name);

        if (channel != null) {
            channel.shutdown();
            channels.remove(name);
        }
    }

    @Override
    public boolean restart(String name) {
        stop(name);
        return start(name);
    }

    @Override
    public void startAll() {
        telemetryConfigService.getConfigsByType(GRPC).forEach(c -> start(c.name()));
        log.info("gRPC producer has Started");
    }

    @Override
    public void stopAll() {
        channels.values().forEach(ManagedChannel::shutdown);
        log.info("gRPC producer has Stopped");
    }

    @Override
    public void restartAll() {
        stopAll();
        startAll();
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
