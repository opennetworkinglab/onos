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
import org.onosproject.openstacktelemetry.api.RestTelemetryAdminService;
import org.onosproject.openstacktelemetry.api.RestTelemetryConfigService;
import org.onosproject.openstacktelemetry.api.config.TelemetryConfig;
import org.onosproject.openstacktelemetry.config.DefaultRestTelemetryConfig;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;

import static org.onosproject.openstacktelemetry.api.Constants.DEFAULT_DISABLE;
import static org.onosproject.openstacktelemetry.api.Constants.DEFAULT_REST_ENDPOINT;
import static org.onosproject.openstacktelemetry.api.Constants.DEFAULT_REST_METHOD;
import static org.onosproject.openstacktelemetry.api.Constants.DEFAULT_REST_REQUEST_MEDIA_TYPE;
import static org.onosproject.openstacktelemetry.api.Constants.DEFAULT_REST_RESPONSE_MEDIA_TYPE;
import static org.onosproject.openstacktelemetry.api.Constants.DEFAULT_REST_SERVER_IP;
import static org.onosproject.openstacktelemetry.api.Constants.DEFAULT_REST_SERVER_PORT;
import static org.onosproject.openstacktelemetry.util.OpenstackTelemetryUtil.getBooleanProperty;

/**
 * REST server configuration manager for publishing openstack telemetry.
 */
@Component(immediate = true)
@Service
public class RestTelemetryConfigManager implements RestTelemetryConfigService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String ENABLE_SERVICE = "enableService";
    private static final String ADDRESS = "address";
    private static final String PORT = "port";
    private static final String ENDPOINT = "endpoint";
    private static final String METHOD = "method";
    private static final String REQUEST_MEDIA_TYPE = "requestMediaType";
    private static final String RESPONSE_MEDIA_TYPE = "responseMediaType";

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ComponentConfigService componentConfigService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected RestTelemetryAdminService restTelemetryAdminService;

    @Property(name = ADDRESS, value = DEFAULT_REST_SERVER_IP,
            label = "Default IP address to establish initial connection to REST server")
    protected String address = DEFAULT_REST_SERVER_IP;

    @Property(name = PORT, intValue = DEFAULT_REST_SERVER_PORT,
            label = "Default port number to establish initial connection to REST server")
    protected Integer port = DEFAULT_REST_SERVER_PORT;

    @Property(name = ENDPOINT, value = DEFAULT_REST_ENDPOINT,
            label = "Endpoint of REST server")
    protected String endpoint = DEFAULT_REST_ENDPOINT;

    @Property(name = METHOD, value = DEFAULT_REST_METHOD,
            label = "HTTP method of REST server")
    protected String method = DEFAULT_REST_METHOD;

    @Property(name = REQUEST_MEDIA_TYPE, value = DEFAULT_REST_REQUEST_MEDIA_TYPE,
            label = "Request media type of REST server")
    protected String requestMediaType = DEFAULT_REST_REQUEST_MEDIA_TYPE;

    @Property(name = RESPONSE_MEDIA_TYPE, value = DEFAULT_REST_RESPONSE_MEDIA_TYPE,
            label = "Response media type of REST server")
    protected String responseMediaType = DEFAULT_REST_RESPONSE_MEDIA_TYPE;

    @Property(name = ENABLE_SERVICE, boolValue = DEFAULT_DISABLE,
            label = "Specify the default behavior of telemetry service")
    protected Boolean enableService = DEFAULT_DISABLE;

    @Activate
    protected void activate(ComponentContext context) {
        componentConfigService.registerProperties(getClass());

        if (enableService) {
            restTelemetryAdminService.start(getConfig());
        }
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        componentConfigService.unregisterProperties(getClass(), false);

        if (enableService) {
            restTelemetryAdminService.stop();
        }
        log.info("Stopped");
    }

    @Modified
    private void modified(ComponentContext context) {
        readComponentConfiguration(context);

        if (enableService) {
            if (restTelemetryAdminService.isRunning()) {
                restTelemetryAdminService.restart(getConfig());
            } else {
                restTelemetryAdminService.start(getConfig());
            }
        } else {
            if (restTelemetryAdminService.isRunning()) {
                restTelemetryAdminService.stop();
            }
        }
        log.info("Modified");
    }

    @Override
    public TelemetryConfig getConfig() {
        return new DefaultRestTelemetryConfig.DefaultBuilder()
                .withAddress(address)
                .withPort(port)
                .withEndpoint(endpoint)
                .withMethod(method)
                .withRequestMediaType(requestMediaType)
                .withResponseMediaType(responseMediaType)
                .build();
    }

    /**
     * Extracts properties from the component configuration context.
     *
     * @param context the component context
     */
    private void readComponentConfiguration(ComponentContext context) {
        Dictionary<?, ?> properties = context.getProperties();

        String addressStr = Tools.get(properties, ADDRESS);
        address = addressStr != null ? addressStr : DEFAULT_REST_SERVER_IP;
        log.info("Configured. REST server address is {}", address);

        Integer portConfigured = Tools.getIntegerProperty(properties, PORT);
        if (portConfigured == null) {
            port = DEFAULT_REST_SERVER_PORT;
            log.info("REST server port is NOT configured, default value is {}", port);
        } else {
            port = portConfigured;
            log.info("Configured. REST server port is {}", port);
        }

        String endpointStr = Tools.get(properties, ENDPOINT);
        endpoint = endpointStr != null ? endpointStr : DEFAULT_REST_ENDPOINT;
        log.info("Configured. REST server endpoint is {}", endpoint);

        String methodStr = Tools.get(properties, METHOD);
        method = methodStr != null ? methodStr : DEFAULT_REST_METHOD;
        log.info("Configured. REST server default HTTP method is {}", method);

        String requestMediaTypeStr = Tools.get(properties, REQUEST_MEDIA_TYPE);
        requestMediaType = requestMediaTypeStr != null ?
                requestMediaTypeStr : DEFAULT_REST_REQUEST_MEDIA_TYPE;
        log.info("Configured. REST server request media type is {}", requestMediaType);

        String responseMediaTypeStr = Tools.get(properties, RESPONSE_MEDIA_TYPE);
        responseMediaType = responseMediaTypeStr != null ?
                responseMediaTypeStr : DEFAULT_REST_RESPONSE_MEDIA_TYPE;
        log.info("Configured. REST server response media type is {}", responseMediaType);

        Boolean enableServiceConfigured =
                getBooleanProperty(properties, ENABLE_SERVICE);
        if (enableServiceConfigured == null) {
            enableService = DEFAULT_DISABLE;
            log.info("REST service enable flag is NOT " +
                    "configured, default value is {}", enableService);
        } else {
            enableService = enableServiceConfigured;
            log.info("Configured. REST service enable flag is {}", enableService);
        }
    }
}
