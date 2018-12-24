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

import org.onosproject.openstacktelemetry.api.TelemetryConfigAdminService;
import org.onosproject.openstacktelemetry.api.TelemetryConfigProvider;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract bootstrapper for loading and registering telemetry configurations
 * that are independent from the default telemetry configurations.
 */
public abstract class AbstractTelemetryConfigLoader {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private TelemetryConfigProvider provider;
    private final String path;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected TelemetryConfigAdminService configAdminService;

    /**
     * Creates a new loader for resource with the specified path.
     *
     * @param path configurations definition XML resource path
     */
    protected AbstractTelemetryConfigLoader(String path) {
        this.path = path;
    }

    @Activate
    protected void activate() {
        try {
            provider = new XmlTelemetryConfigLoader().loadTelemetryConfigs(
                                            getClass().getResourceAsStream(path));
            configAdminService.registerProvider(provider);
        } catch (Exception e) {
            log.error("Unable to load {} telemetry configuration definitions", path, e);
        }
        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        configAdminService.unregisterProvider(provider);
        log.info("Stopped");
    }
}
