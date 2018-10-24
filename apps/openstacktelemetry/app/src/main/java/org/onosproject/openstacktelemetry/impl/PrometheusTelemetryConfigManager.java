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
import org.onosproject.openstacktelemetry.api.PrometheusTelemetryAdminService;
import org.onosproject.openstacktelemetry.api.PrometheusTelemetryConfigService;
import org.onosproject.openstacktelemetry.api.config.TelemetryConfig;
import org.onosproject.openstacktelemetry.config.DefaultPrometheusTelemetryConfig;
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

import static org.onosproject.openstacktelemetry.impl.OsgiPropertyConstants.PROMETHEUS_ENABLE_SERVICE_DEFAULT;
import static org.onosproject.openstacktelemetry.impl.OsgiPropertyConstants.PROMETHEUS_EXPORTER_ADDRESS_DEFAULT;
import static org.onosproject.openstacktelemetry.impl.OsgiPropertyConstants.PROMETHEUS_EXPORTER_PORT_DEFAULT;
import static org.onosproject.openstacktelemetry.impl.OsgiPropertyConstants.PROP_PROMETHEUS_ENABLE_SERVICE;
import static org.onosproject.openstacktelemetry.impl.OsgiPropertyConstants.PROP_PROMETHEUS_EXPORTER_ADDRESS;
import static org.onosproject.openstacktelemetry.impl.OsgiPropertyConstants.PROP_PROMETHEUS_EXPORTER_PORT;
import static org.onosproject.openstacktelemetry.util.OpenstackTelemetryUtil.getBooleanProperty;
import static org.onosproject.openstacktelemetry.util.OpenstackTelemetryUtil.initTelemetryService;

/**
 * Prometheus exporter configuration manager for publishing openstack telemetry.
 */
@Component(
    immediate = true,
    service = PrometheusTelemetryConfigService.class,
    property = {
        PROP_PROMETHEUS_ENABLE_SERVICE + ":Boolean=" + PROMETHEUS_ENABLE_SERVICE_DEFAULT,
        PROP_PROMETHEUS_EXPORTER_ADDRESS + "=" + PROMETHEUS_EXPORTER_ADDRESS_DEFAULT,
        PROP_PROMETHEUS_EXPORTER_PORT + ":Integer=" + PROMETHEUS_EXPORTER_PORT_DEFAULT
    }
)
public class PrometheusTelemetryConfigManager implements PrometheusTelemetryConfigService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ComponentConfigService componentConfigService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected PrometheusTelemetryAdminService prometheusTelemetryAdminService;

    /** Default IP address of prometheus exporter. */
    protected String address = PROMETHEUS_EXPORTER_ADDRESS_DEFAULT;

    /** Default port number of prometheus exporter. */
    protected Integer port = PROMETHEUS_EXPORTER_PORT_DEFAULT;

    /** Specify the default behavior of telemetry service. */
    protected Boolean enableService = PROMETHEUS_ENABLE_SERVICE_DEFAULT;

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

        String addressStr = Tools.get(properties, PROP_PROMETHEUS_EXPORTER_ADDRESS);
        address = addressStr != null ? addressStr : PROMETHEUS_EXPORTER_ADDRESS_DEFAULT;
        log.info("Configured. Prometheus exporter address is {}", address);

        Integer portConfigured = Tools.getIntegerProperty(properties, PROP_PROMETHEUS_EXPORTER_PORT);
        if (portConfigured == null) {
            port = PROMETHEUS_EXPORTER_PORT_DEFAULT;
            log.info("Prometheus exporter port is NOT configured, default value is {}", port);
        } else {
            port = portConfigured;
            log.info("Configured. Prometheus exporter port is {}", port);
        }

        Boolean enableServiceConfigured = getBooleanProperty(properties, PROP_PROMETHEUS_ENABLE_SERVICE);
        if (enableServiceConfigured == null) {
            enableService = PROMETHEUS_ENABLE_SERVICE_DEFAULT;
            log.info("Prometheus service enable flag is NOT " +
                             "configured, default value is {}", enableService);
        } else {
            enableService = enableServiceConfigured;
            log.info("Configured. Prometheus service enable flag is {}", enableService);
        }
    }
}
