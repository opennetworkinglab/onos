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

import org.onlab.util.Tools;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.openstacktelemetry.api.GrpcTelemetryAdminService;
import org.onosproject.openstacktelemetry.api.GrpcTelemetryConfigService;
import org.onosproject.openstacktelemetry.api.config.TelemetryConfig;
import org.onosproject.openstacktelemetry.config.DefaultGrpcTelemetryConfig;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;

import static org.onlab.util.Tools.get;
import static org.onlab.util.Tools.getIntegerProperty;
import static org.onosproject.openstacktelemetry.impl.OsgiPropertyConstants.GRPC_ENABLE_SERVICE_DEFAULT;
import static org.onosproject.openstacktelemetry.impl.OsgiPropertyConstants.GRPC_MAX_INBOUND_MSG_SIZE_DEFAULT;
import static org.onosproject.openstacktelemetry.impl.OsgiPropertyConstants.GRPC_SERVER_ADDRESS_DEFAULT;
import static org.onosproject.openstacktelemetry.impl.OsgiPropertyConstants.GRPC_SERVER_PORT_DEFAULT;
import static org.onosproject.openstacktelemetry.impl.OsgiPropertyConstants.GRPC_USE_PLAINTEXT_DEFAULT;
import static org.onosproject.openstacktelemetry.impl.OsgiPropertyConstants.PROP_GRPC_ENABLE_SERVICE;
import static org.onosproject.openstacktelemetry.impl.OsgiPropertyConstants.PROP_GRPC_MAX_INBOUND_MSG_SIZE;
import static org.onosproject.openstacktelemetry.impl.OsgiPropertyConstants.PROP_GRPC_SERVER_ADDRESS;
import static org.onosproject.openstacktelemetry.impl.OsgiPropertyConstants.PROP_GRPC_SERVER_PORT;
import static org.onosproject.openstacktelemetry.impl.OsgiPropertyConstants.PROP_GRPC_USE_PLAINTEXT;
import static org.onosproject.openstacktelemetry.util.OpenstackTelemetryUtil.getBooleanProperty;
import static org.onosproject.openstacktelemetry.util.OpenstackTelemetryUtil.initTelemetryService;

/**
 * gRPC server configuration manager for publishing openstack telemetry.
 */
@Component(
    immediate = true,
    service = GrpcTelemetryConfigService.class,
    property = {
        PROP_GRPC_ENABLE_SERVICE + ":Boolean=" + GRPC_ENABLE_SERVICE_DEFAULT,
        PROP_GRPC_SERVER_ADDRESS  + "=" + GRPC_SERVER_ADDRESS_DEFAULT,
        PROP_GRPC_SERVER_PORT + ":Integer=" + GRPC_SERVER_PORT_DEFAULT,
        PROP_GRPC_USE_PLAINTEXT + ":Boolean=" + GRPC_USE_PLAINTEXT_DEFAULT,
        PROP_GRPC_MAX_INBOUND_MSG_SIZE + ":Integer=" + GRPC_MAX_INBOUND_MSG_SIZE_DEFAULT
    }
)
public class GrpcTelemetryConfigManager implements GrpcTelemetryConfigService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ComponentConfigService componentConfigService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected GrpcTelemetryAdminService grpcTelemetryAdminService;

    /** Default IP address to establish initial connection to gRPC server. */
    protected String address = GRPC_SERVER_ADDRESS_DEFAULT;

    /** Default port number to establish initial connection to gRPC server. */
    protected Integer port = GRPC_SERVER_PORT_DEFAULT;

    /** UsePlaintext flag value used for connecting to gRPC server. */
    protected Boolean usePlaintext = GRPC_USE_PLAINTEXT_DEFAULT;

    /** Maximum inbound message size used for communicating with gRPC server. */
    protected Integer maxInboundMsgSize = GRPC_MAX_INBOUND_MSG_SIZE_DEFAULT;

    /** Specify the default behavior of telemetry service. */
    protected Boolean enableService = GRPC_ENABLE_SERVICE_DEFAULT;

    @Activate
    protected void activate(ComponentContext context) {
        componentConfigService.registerProperties(getClass());

        if (enableService) {
            grpcTelemetryAdminService.start(getConfig());
        }
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        componentConfigService.unregisterProperties(getClass(), false);

        if (enableService) {
            grpcTelemetryAdminService.stop();
        }
        log.info("Stopped");
    }

    @Modified
    private void modified(ComponentContext context) {
        readComponentConfiguration(context);
        initTelemetryService(grpcTelemetryAdminService, getConfig(), enableService);
        log.info("Modified");
    }

    @Override
    public TelemetryConfig getConfig() {
        return new DefaultGrpcTelemetryConfig.DefaultBuilder()
                .withAddress(address)
                .withPort(port)
                .withUsePlaintext(usePlaintext)
                .withMaxInboundMsgSize(maxInboundMsgSize)
                .build();
    }

    /**
     * Extracts properties from the component configuration context.
     *
     * @param context the component context
     */
    private void readComponentConfiguration(ComponentContext context) {
        Dictionary<?, ?> properties = context.getProperties();

        String addressStr = get(properties, PROP_GRPC_SERVER_ADDRESS);
        address = addressStr != null ? addressStr : GRPC_SERVER_ADDRESS_DEFAULT;
        log.info("Configured. gRPC server address is {}", address);

        Integer portConfigured = Tools.getIntegerProperty(properties, PROP_GRPC_SERVER_PORT);
        if (portConfigured == null) {
            port = GRPC_SERVER_PORT_DEFAULT;
            log.info("gRPC server port is NOT configured, default value is {}", port);
        } else {
            port = portConfigured;
            log.info("Configured. gRPC server port is {}", port);
        }

        Boolean usePlaintextConfigured =
                getBooleanProperty(properties, PROP_GRPC_USE_PLAINTEXT);
        if (usePlaintextConfigured == null) {
            usePlaintext = GRPC_USE_PLAINTEXT_DEFAULT;
            log.info("gRPC server use plaintext flag is NOT " +
                    "configured, default value is {}", usePlaintext);
        } else {
            usePlaintext = usePlaintextConfigured;
            log.info("Configured. gRPC server use plaintext flag is {}", usePlaintext);
        }

        Integer maxInboundMsgSizeConfigured =
                getIntegerProperty(properties, PROP_GRPC_MAX_INBOUND_MSG_SIZE);
        if (maxInboundMsgSizeConfigured == null) {
            maxInboundMsgSize = GRPC_MAX_INBOUND_MSG_SIZE_DEFAULT;
            log.info("gRPC server max inbound message size is NOT " +
                    "configured, default value is {}", maxInboundMsgSize);
        } else {
            maxInboundMsgSize = maxInboundMsgSizeConfigured;
            log.info("Configured. gRPC server max inbound message size is {}", maxInboundMsgSize);
        }

        Boolean enableServiceConfigured =
                getBooleanProperty(properties, PROP_GRPC_ENABLE_SERVICE);
        if (enableServiceConfigured == null) {
            enableService = GRPC_ENABLE_SERVICE_DEFAULT;
            log.info("gRPC service enable flag is NOT " +
                    "configured, default value is {}", enableService);
        } else {
            enableService = enableServiceConfigured;
            log.info("Configured. gRPC service enable flag is {}", enableService);
        }
    }

}
