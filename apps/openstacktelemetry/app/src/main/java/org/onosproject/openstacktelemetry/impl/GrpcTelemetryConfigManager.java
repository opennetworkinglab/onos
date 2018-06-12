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

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.util.Tools;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.openstacktelemetry.api.GrpcTelemetryAdminService;
import org.onosproject.openstacktelemetry.api.GrpcTelemetryConfigService;
import org.onosproject.openstacktelemetry.api.config.TelemetryConfig;
import org.onosproject.openstacktelemetry.config.DefaultGrpcTelemetryConfig;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;

import static org.onlab.util.Tools.get;
import static org.onlab.util.Tools.getIntegerProperty;
import static org.onosproject.openstacktelemetry.api.Constants.DEFAULT_DISABLE;
import static org.onosproject.openstacktelemetry.api.Constants.DEFAULT_GRPC_MAX_INBOUND_MSG_SIZE;
import static org.onosproject.openstacktelemetry.api.Constants.DEFAULT_GRPC_SERVER_IP;
import static org.onosproject.openstacktelemetry.api.Constants.DEFAULT_GRPC_SERVER_PORT;
import static org.onosproject.openstacktelemetry.api.Constants.DEFAULT_GRPC_USE_PLAINTEXT;
import static org.onosproject.openstacktelemetry.util.OpenstackTelemetryUtil.getBooleanProperty;

/**
 * gRPC server configuration manager for publishing openstack telemetry.
 */
@Component(immediate = true)
@Service
public class GrpcTelemetryConfigManager implements GrpcTelemetryConfigService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String ENABLE_SERVICE = "enableService";
    private static final String ADDRESS = "address";
    private static final String PORT = "port";
    private static final String USE_PLAINTEXT = "usePlaintext";
    private static final String MAX_INBOUND_MSG_SIZE = "maxInboundMsgSize";

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ComponentConfigService componentConfigService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected GrpcTelemetryAdminService grpcTelemetryAdminService;

    @Property(name = ADDRESS, value = DEFAULT_GRPC_SERVER_IP,
            label = "Default IP address to establish initial connection to gRPC server")
    protected String address = DEFAULT_GRPC_SERVER_IP;

    @Property(name = PORT, intValue = DEFAULT_GRPC_SERVER_PORT,
            label = "Default port number to establish initial connection to gRPC server")
    protected Integer port = DEFAULT_GRPC_SERVER_PORT;

    @Property(name = USE_PLAINTEXT, boolValue = DEFAULT_GRPC_USE_PLAINTEXT,
            label = "UsePlaintext flag value used for connecting to gRPC server")
    protected Boolean usePlaintext = DEFAULT_GRPC_USE_PLAINTEXT;

    @Property(name = MAX_INBOUND_MSG_SIZE, intValue = DEFAULT_GRPC_MAX_INBOUND_MSG_SIZE,
            label = "Maximum inbound message size used for communicating with gRPC server")
    protected Integer maxInboundMsgSize = DEFAULT_GRPC_MAX_INBOUND_MSG_SIZE;

    @Property(name = ENABLE_SERVICE, boolValue = DEFAULT_DISABLE,
            label = "Specify the default behavior of telemetry service")
    protected Boolean enableService = DEFAULT_DISABLE;

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

        if (enableService) {
            if (grpcTelemetryAdminService.isRunning()) {
                grpcTelemetryAdminService.restart(getConfig());
            } else {
                grpcTelemetryAdminService.start(getConfig());
            }
        } else {
            if (grpcTelemetryAdminService.isRunning()) {
                grpcTelemetryAdminService.stop();
            }
        }
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

        String addressStr = get(properties, ADDRESS);
        address = addressStr != null ? addressStr : DEFAULT_GRPC_SERVER_IP;
        log.info("Configured. gRPC server address is {}", address);

        Integer portConfigured = Tools.getIntegerProperty(properties, PORT);
        if (portConfigured == null) {
            port = DEFAULT_GRPC_SERVER_PORT;
            log.info("gRPC server port is NOT configured, default value is {}", port);
        } else {
            port = portConfigured;
            log.info("Configured. gRPC server port is {}", port);
        }

        Boolean usePlaintextConfigured =
                getBooleanProperty(properties, USE_PLAINTEXT);
        if (usePlaintextConfigured == null) {
            usePlaintext = DEFAULT_GRPC_USE_PLAINTEXT;
            log.info("gRPC server use plaintext flag is NOT " +
                    "configured, default value is {}", usePlaintext);
        } else {
            usePlaintext = usePlaintextConfigured;
            log.info("Configured. gRPC server use plaintext flag is {}", usePlaintext);
        }

        Integer maxInboundMsgSizeConfigured =
                getIntegerProperty(properties, MAX_INBOUND_MSG_SIZE);
        if (maxInboundMsgSizeConfigured == null) {
            maxInboundMsgSize = DEFAULT_GRPC_MAX_INBOUND_MSG_SIZE;
            log.info("gRPC server max inbound message size is NOT " +
                    "configured, default value is {}", maxInboundMsgSize);
        } else {
            maxInboundMsgSize = maxInboundMsgSizeConfigured;
            log.info("Configured. gRPC server max inbound message size is {}", maxInboundMsgSize);
        }

        Boolean enableServiceConfigured =
                getBooleanProperty(properties, ENABLE_SERVICE);
        if (enableServiceConfigured == null) {
            enableService = DEFAULT_DISABLE;
            log.info("gRPC service enable flag is NOT " +
                    "configured, default value is {}", enableService);
        } else {
            enableService = enableServiceConfigured;
            log.info("Configured. gRPC service enable flag is {}", enableService);
        }
    }

}
