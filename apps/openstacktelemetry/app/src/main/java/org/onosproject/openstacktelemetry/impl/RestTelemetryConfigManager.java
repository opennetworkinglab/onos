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
import org.onosproject.openstacktelemetry.api.RestTelemetryAdminService;
import org.onosproject.openstacktelemetry.api.RestTelemetryConfigService;
import org.onosproject.openstacktelemetry.api.config.TelemetryConfig;
import org.onosproject.openstacktelemetry.config.DefaultRestTelemetryConfig;
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

import static org.onosproject.openstacktelemetry.impl.OsgiPropertyConstants.PROP_REST_SERVER_ADDRESS;
import static org.onosproject.openstacktelemetry.impl.OsgiPropertyConstants.PROP_REST_ENABLE_SERVICE;
import static org.onosproject.openstacktelemetry.impl.OsgiPropertyConstants.PROP_REST_ENABLE_SERVICE_DEFAULT;
import static org.onosproject.openstacktelemetry.impl.OsgiPropertyConstants.PROP_REST_ENDPOINT;
import static org.onosproject.openstacktelemetry.impl.OsgiPropertyConstants.PROP_REST_METHOD;
import static org.onosproject.openstacktelemetry.impl.OsgiPropertyConstants.PROP_REST_SERVER_PORT;
import static org.onosproject.openstacktelemetry.impl.OsgiPropertyConstants.PROP_REST_REQUEST_MEDIA_TYPE;
import static org.onosproject.openstacktelemetry.impl.OsgiPropertyConstants.PROP_REST_RESPONSE_MEDIA_TYPE;
import static org.onosproject.openstacktelemetry.impl.OsgiPropertyConstants.PROP_REST_ENDPOINT_DEFAULT;
import static org.onosproject.openstacktelemetry.impl.OsgiPropertyConstants.PROP_REST_METHOD_DEFAULT;
import static org.onosproject.openstacktelemetry.impl.OsgiPropertyConstants.PROP_REST_REQUEST_MEDIA_TYPE_DEFAULT;
import static org.onosproject.openstacktelemetry.impl.OsgiPropertyConstants.PROP_REST_RESPONSE_MEDIA_TYPE_DEFAULT;
import static org.onosproject.openstacktelemetry.impl.OsgiPropertyConstants.PROP_REST_SERVER_ADDRESS_DEFAULT;
import static org.onosproject.openstacktelemetry.impl.OsgiPropertyConstants.PROP_REST_SERVER_PORT_DEFAULT;
import static org.onosproject.openstacktelemetry.util.OpenstackTelemetryUtil.getBooleanProperty;
import static org.onosproject.openstacktelemetry.util.OpenstackTelemetryUtil.initTelemetryService;

/**
 * REST server configuration manager for publishing openstack telemetry.
 */
@Component(
    immediate = true,
    service = RestTelemetryConfigService.class,
    property = {
        PROP_REST_ENABLE_SERVICE + ":Boolean=" + PROP_REST_ENABLE_SERVICE_DEFAULT,
        PROP_REST_SERVER_ADDRESS + "=" + PROP_REST_SERVER_ADDRESS_DEFAULT,
        PROP_REST_SERVER_PORT + ":Integer=" + PROP_REST_SERVER_PORT_DEFAULT,
        PROP_REST_ENDPOINT + "=" + PROP_REST_ENDPOINT_DEFAULT,
        PROP_REST_METHOD + "=" + PROP_REST_METHOD_DEFAULT,
        PROP_REST_REQUEST_MEDIA_TYPE + "=" + PROP_REST_REQUEST_MEDIA_TYPE_DEFAULT,
        PROP_REST_RESPONSE_MEDIA_TYPE + "=" + PROP_REST_RESPONSE_MEDIA_TYPE_DEFAULT
    }
)
public class RestTelemetryConfigManager implements RestTelemetryConfigService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ComponentConfigService componentConfigService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected RestTelemetryAdminService restTelemetryAdminService;

    /** Default IP address to establish initial connection to REST server. */
    protected String address = PROP_REST_SERVER_ADDRESS_DEFAULT;

    /** Default port number to establish initial connection to REST server. */
    protected Integer port = PROP_REST_SERVER_PORT_DEFAULT;

    /** Endpoint of REST server. */
    protected String endpoint = PROP_REST_ENDPOINT_DEFAULT;

    /** HTTP method of REST server. */
    protected String method = PROP_REST_METHOD_DEFAULT;

    /** Request media type of REST server. */
    protected String requestMediaType = PROP_REST_REQUEST_MEDIA_TYPE_DEFAULT;

    /** Response media type of REST server. */
    protected String responseMediaType = PROP_REST_RESPONSE_MEDIA_TYPE_DEFAULT;

    /** Specify the default behavior of telemetry service. */
    protected Boolean enableService = PROP_REST_ENABLE_SERVICE_DEFAULT;

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
        initTelemetryService(restTelemetryAdminService, getConfig(), enableService);
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

        String addressStr = Tools.get(properties, PROP_REST_SERVER_ADDRESS);
        address = addressStr != null ? addressStr : PROP_REST_SERVER_ADDRESS_DEFAULT;
        log.info("Configured. REST server address is {}", address);

        Integer portConfigured = Tools.getIntegerProperty(properties, PROP_REST_SERVER_PORT);
        if (portConfigured == null) {
            port = PROP_REST_SERVER_PORT_DEFAULT;
            log.info("REST server port is NOT configured, default value is {}", port);
        } else {
            port = portConfigured;
            log.info("Configured. REST server port is {}", port);
        }

        String endpointStr = Tools.get(properties, PROP_REST_ENDPOINT);
        endpoint = endpointStr != null ? endpointStr : PROP_REST_ENDPOINT_DEFAULT;
        log.info("Configured. REST server endpoint is {}", endpoint);

        String methodStr = Tools.get(properties, PROP_REST_METHOD);
        method = methodStr != null ? methodStr : PROP_REST_METHOD_DEFAULT;
        log.info("Configured. REST server default HTTP method is {}", method);

        String requestMediaTypeStr = Tools.get(properties, PROP_REST_REQUEST_MEDIA_TYPE);
        requestMediaType = requestMediaTypeStr != null ?
                requestMediaTypeStr : PROP_REST_REQUEST_MEDIA_TYPE_DEFAULT;
        log.info("Configured. REST server request media type is {}", requestMediaType);

        String responseMediaTypeStr = Tools.get(properties, PROP_REST_RESPONSE_MEDIA_TYPE);
        responseMediaType = responseMediaTypeStr != null ?
                responseMediaTypeStr : PROP_REST_RESPONSE_MEDIA_TYPE_DEFAULT;
        log.info("Configured. REST server response media type is {}", responseMediaType);

        Boolean enableServiceConfigured =
                getBooleanProperty(properties, PROP_REST_ENABLE_SERVICE);
        if (enableServiceConfigured == null) {
            enableService = PROP_REST_ENABLE_SERVICE_DEFAULT;
            log.info("REST service enable flag is NOT " +
                    "configured, default value is {}", enableService);
        } else {
            enableService = enableServiceConfigured;
            log.info("Configured. REST service enable flag is {}", enableService);
        }
    }
}
