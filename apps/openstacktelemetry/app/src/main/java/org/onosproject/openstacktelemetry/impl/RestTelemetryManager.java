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
import com.google.common.collect.Sets;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.openstacktelemetry.api.OpenstackTelemetryService;
import org.onosproject.openstacktelemetry.api.RestTelemetryAdminService;
import org.onosproject.openstacktelemetry.api.TelemetryConfigService;
import org.onosproject.openstacktelemetry.api.config.RestTelemetryConfig;
import org.onosproject.openstacktelemetry.api.config.TelemetryConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.util.Map;
import java.util.Set;

import static org.onosproject.openstacktelemetry.api.Constants.REST_SCHEME;
import static org.onosproject.openstacktelemetry.api.config.TelemetryConfig.ConfigType.REST;
import static org.onosproject.openstacktelemetry.config.DefaultRestTelemetryConfig.fromTelemetryConfig;

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

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected TelemetryConfigService telemetryConfigService;

    private Map<String, WebTarget> targets = Maps.newConcurrentMap();

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

        telemetryConfigService.getConfigsByType(REST).forEach(c -> {
            RestTelemetryConfig restConfig = fromTelemetryConfig(c);

            if (restConfig != null && !c.name().equals(REST_SCHEME) && c.enabled()) {
                StringBuilder restServerBuilder = new StringBuilder();
                restServerBuilder.append(PROTOCOL);
                restServerBuilder.append(":");
                restServerBuilder.append("//");
                restServerBuilder.append(restConfig.address());
                restServerBuilder.append(":");
                restServerBuilder.append(restConfig.port());
                restServerBuilder.append("/");

                Client client = ClientBuilder.newBuilder().build();

                WebTarget target = client.target(restServerBuilder.toString()).path(restConfig.endpoint());

                targets.put(c.name(), target);
            }
        });

        log.info("REST producer has Started");
    }

    @Override
    public void stop() {
        targets.values().forEach(t -> t = null);
        log.info("REST producer has Stopped");
    }

    @Override
    public void restart() {
        stop();
        start();
    }

    @Override
    public Set<Response> publish(String record) {

        Set<Response> responses = Sets.newConcurrentHashSet();

        targets.forEach((k, v) -> {
            TelemetryConfig config = telemetryConfigService.getConfig(k);
            RestTelemetryConfig restConfig = fromTelemetryConfig(config);

            switch (restConfig.method()) {
                case POST_METHOD:
                    responses.add(v.request(restConfig.requestMediaType())
                            .post(Entity.json(record)));
                    break;
                case GET_METHOD:
                    responses.add(v.request(restConfig.requestMediaType()).get());
                    break;
                default:
                    break;
            }
        });

        return responses;
    }

    @Override
    public boolean isRunning() {
        return !targets.isEmpty();
    }
}
