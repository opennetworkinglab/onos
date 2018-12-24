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

import org.onosproject.event.ListenerService;
import org.onosproject.openstacktelemetry.api.config.TelemetryConfig;
import org.onosproject.openstacktelemetry.api.config.TelemetryConfig.ConfigType;

import java.util.Set;

/**
 * Telemetry configuration service interface.
 */
public interface TelemetryConfigService
        extends ListenerService<TelemetryConfigEvent, TelemetryConfigListener> {

    /**
     * Obtains the telemetry configuration with the given telemetry
     * configuration name.
     *
     * @param name telemetry configuration name
     * @return provided telemetry configuration
     */
    TelemetryConfig getConfig(String name);

    /**
     * Obtains the telemetry configuration with the given telemetry config type.
     *
     * @param type telemetry configuration type
     * @return provided telemetry configurations
     */
    Set<TelemetryConfig> getConfigsByType(ConfigType type);

    /**
     * Returns the overall set of telemetry configurations being provided.
     *
     * @return provided telemetry configurations
     */
    Set<TelemetryConfig> getConfigs();
}
