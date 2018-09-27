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
import org.onosproject.openstacktelemetry.api.PrometheusTelemetryAdminService;
import org.onosproject.openstacktelemetry.api.PrometheusTelemetryConfigService;
import org.onosproject.openstacktelemetry.api.config.TelemetryConfig;
import org.onosproject.openstacktelemetry.config.DefaultPrometheusTelemetryConfig;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;

import static org.onosproject.openstacktelemetry.api.Constants.DEFAULT_DISABLE;
import static org.onosproject.openstacktelemetry.api.Constants.DEFAULT_ENABLE;
import static org.onosproject.openstacktelemetry.api.Constants.DEFAULT_PROMETHEUS_EXPORTER_IP;
import static org.onosproject.openstacktelemetry.api.Constants.DEFAULT_PROMETHEUS_EXPORTER_PORT;
import static org.onosproject.openstacktelemetry.util.OpenstackTelemetryUtil.getBooleanProperty;
import static org.onosproject.openstacktelemetry.util.OpenstackTelemetryUtil.initTelemetryService;

/**
 * Prometheus exporter configuration manager for publishing openstack telemetry.
 */
@Component(immediate = true)
@Service
public class PrometheusTelemetryConfigManager implements PrometheusTelemetryConfigService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String ENABLE_SERVICE = "enableService";
    private static final String ADDRESS = "address";
    private static final String PORT = "port";

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected ComponentConfigService componentConfigService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected PrometheusTelemetryAdminService prometheusTelemetryAdminService;

    @Property(name = ADDRESS, value = DEFAULT_PROMETHEUS_EXPORTER_IP,
            label = "Default IP address of prometheus exporter")
    protected String address = DEFAULT_PROMETHEUS_EXPORTER_IP;

    @Property(name = PORT, intValue = DEFAULT_PROMETHEUS_EXPORTER_PORT,
            label = "Default port number of prometheus exporter")
    protected Integer port = DEFAULT_PROMETHEUS_EXPORTER_PORT;

    @Property(name = ENABLE_SERVICE, boolValue = DEFAULT_ENABLE,
            label = "Specify the default behavior of telemetry service")
    protected Boolean enableService = DEFAULT_ENABLE;

    @Activate
    protected void activate(ComponentContext context) {
        componentConfigService.registerProperties(getClass());
        if (enableService) {
            prometheusTelemetryAdminService.start(getConfig());
        }
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        componentConfigService.unregisterProperties(getClass(), false);
        if (enableService) {
            prometheusTelemetryAdminService.stop();
        }
        log.info("Stopped");
    }

    @Modified
    private void modified(ComponentContext context) {
        readComponentConfiguration(context);
        initTelemetryService(prometheusTelemetryAdminService, getConfig(), enableService);
        log.info("Modified");
    }

    @Override
    public TelemetryConfig getConfig() {
        return new DefaultPrometheusTelemetryConfig.DefaultBuilder()
                .withAddress(address)
                .withPort(port)
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
        address = addressStr != null ? addressStr : DEFAULT_PROMETHEUS_EXPORTER_IP;
        log.info("Configured. Prometheus exporter address is {}", address);

        Integer portConfigured = Tools.getIntegerProperty(properties, PORT);
        if (portConfigured == null) {
            port = DEFAULT_PROMETHEUS_EXPORTER_PORT;
            log.info("Prometheus exporter port is NOT configured, default value is {}", port);
        } else {
            port = portConfigured;
            log.info("Configured. Prometheus exporter port is {}", port);
        }

        Boolean enableServiceConfigured = getBooleanProperty(properties, ENABLE_SERVICE);
        if (enableServiceConfigured == null) {
            enableService = DEFAULT_DISABLE;
            log.info("Prometheus service enable flag is NOT " +
                             "configured, default value is {}", enableService);
        } else {
            enableService = enableServiceConfigured;
            log.info("Configured. Prometheus service enable flag is {}", enableService);
        }
    }
}
