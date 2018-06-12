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
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.openstacktelemetry.api.OpenstackTelemetryService;
import org.onosproject.openstacktelemetry.api.RestTelemetryAdminService;
import org.onosproject.openstacktelemetry.api.config.RestTelemetryConfig;
import org.onosproject.openstacktelemetry.api.config.TelemetryConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

/**
 * REST telemetry manager.
 */
@Component(immediate = true)
@Service
public class RestTelemetryManager implements RestTelemetryAdminService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String PROTOCOL = "http";
    private static final String POST_METHOD = "POST";
    private static final String GET_METHOD  = "GET";

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OpenstackTelemetryService openstackTelemetryService;

    private WebTarget target = null;
    private RestTelemetryConfig restConfig = null;

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
        if (target != null) {
            log.info("REST producer has already been started");
            return;
        }

        restConfig = (RestTelemetryConfig) config;

        StringBuilder restServerBuilder = new StringBuilder();
        restServerBuilder.append(PROTOCOL);
        restServerBuilder.append(":");
        restServerBuilder.append("//");
        restServerBuilder.append(restConfig.address());
        restServerBuilder.append(":");
        restServerBuilder.append(restConfig.port());
        restServerBuilder.append("/");

        Client client = ClientBuilder.newBuilder().build();

        target = client.target(restServerBuilder.toString()).path(restConfig.endpoint());

        log.info("REST producer has Started");
    }

    @Override
    public void stop() {
        if (target != null) {
            target = null;
        }

        log.info("REST producer has Stopped");
    }

    @Override
    public void restart(TelemetryConfig config) {
        stop();
        start(config);
    }

    @Override
    public Response publish(String endpoint, String method, String record) {
        // TODO: need to find a way to invoke REST endpoint using target
        return null;
    }

    @Override
    public Response publish(String method, String record) {
        switch (method) {
            case POST_METHOD:
                return target.request(restConfig.requestMediaType())
                        .post(Entity.json(record));
            case GET_METHOD:
                return target.request(restConfig.requestMediaType()).get();
            default:
                return null;
        }
    }

    @Override
    public Response publish(String record) {

        if (target == null) {
            log.warn("REST telemetry service has not been enabled!");
            return null;
        }

        switch (restConfig.method()) {
            case POST_METHOD:
                return target.request(restConfig.requestMediaType())
                        .post(Entity.json(record));
            case GET_METHOD:
                return target.request(restConfig.requestMediaType()).get();
            default:
                return null;
        }
    }

    @Override
    public boolean isRunning() {
        return target != null;
    }
}
