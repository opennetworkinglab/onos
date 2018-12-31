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
package org.onosproject.openstacktelemetry.api;

import org.onosproject.openstacktelemetry.api.config.TelemetryConfig;

import java.util.Set;

/**
 * Service for mapping telemetry configurations implementations.
 */
public interface TelemetryConfigAdminService extends TelemetryConfigService {

    /**
     * Returns the set of telemetry configurations currently registered.
     *
     * @return registered telemetry configurations
     */
    Set<TelemetryConfigProvider> getProviders();

    /**
     * Registers the specified telemetry configuration provider.
     *
     * @param provider configuration provider to register
     */
    void registerProvider(TelemetryConfigProvider provider);

    /**
     * Unregisters the specified telemetry configuration provider.
     *
     * @param provider configuration provider to unregister
     */
    void unregisterProvider(TelemetryConfigProvider provider);

    /**
     * Updates an existing telemetry configuration.
     *
     * @param config telemetry configuration
     */
    void updateTelemetryConfig(TelemetryConfig config);

    /**
     * Removes an existing telemetry configuration with the given config name.
     *
     * @param name configuration name
     */
    void removeTelemetryConfig(String name);
}
