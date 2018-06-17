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
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.onosproject.openstacktelemetry.api.InfluxDbTelemetryAdminService;
import org.onosproject.openstacktelemetry.api.InfluxRecord;
import org.onosproject.openstacktelemetry.api.OpenstackTelemetryService;
import org.onosproject.openstacktelemetry.api.config.InfluxDbTelemetryConfig;
import org.onosproject.openstacktelemetry.api.config.TelemetryConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * InfluxDB telemetry manager.
 */
@Component(immediate = true)
@Service
public class InfluxDbTelemetryManager implements InfluxDbTelemetryAdminService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected OpenstackTelemetryService openstackTelemetryService;

    private static final String PROTOCOL = "http";
    private InfluxDB producer = null;

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
        if (producer != null) {
            log.info("InfluxDB producer has already been started");
            return;
        }

        InfluxDbTelemetryConfig influxDbConfig = (InfluxDbTelemetryConfig) config;

        StringBuilder influxDbServerBuilder = new StringBuilder();
        influxDbServerBuilder.append(PROTOCOL);
        influxDbServerBuilder.append(":");
        influxDbServerBuilder.append("//");
        influxDbServerBuilder.append(influxDbConfig.address());
        influxDbServerBuilder.append(":");
        influxDbServerBuilder.append(influxDbConfig.port());

        producer = InfluxDBFactory.connect(influxDbServerBuilder.toString(),
                influxDbConfig.username(), influxDbConfig.password());
        log.info("InfluxDB producer has Started");
    }

    @Override
    public void stop() {
        if (producer != null) {
            producer = null;
        }

        log.info("InfluxDB producer has Stopped");
    }

    @Override
    public void restart(TelemetryConfig config) {
        stop();
        start(config);
    }

    @Override
    public void publish(InfluxRecord<String, Object> record) {
        // TODO: need to find a way to invoke InfluxDB endpoint using producer

        if (producer == null) {
            log.warn("InfluxDB telemetry service has not been enabled!");
        }
    }

    @Override
    public boolean isRunning() {
        return producer != null;
    }
}
