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
import org.onosproject.openstacktelemetry.api.config.TelemetryConfig.ConfigType;
import org.onosproject.store.Store;

import java.util.Set;

/**
 * Manages inventory of telemetry config; not intended for direct use.
 */
public interface TelemetryConfigStore
        extends Store<TelemetryConfigEvent, TelemetryConfigStoreDelegate> {

    /**
     * Creates a new telemetry config.
     *
     * @param config a telemetry config
     */
    void createTelemetryConfig(TelemetryConfig config);

    /**
     * Updates the existing telemetry config.
     *
     * @param config the existing telemetry config
     */
    void updateTelemetryConfig(TelemetryConfig config);

    /**
     * Removes the existing telemetry config.
     *
     * @param name telemetry config name
     * @return the removed telemetry config
     */
    TelemetryConfig removeTelemetryConfig(String name);

    /**
     * Obtains the existing telemetry config.
     *
     * @param name telemetry config name
     * @return queried telemetry config
     */
    TelemetryConfig telemetryConfig(String name);

    /**
     * Obtains a collection of all of telemetry configs.
     *
     * @return a collection of all of telemetry configs
     */
    Set<TelemetryConfig> telemetryConfigs();

    /**
     * Obtains a collection of telemetry configs by config type.
     *
     * @param type config type
     * @return a collection of telemetry configs by config type
     */
    Set<TelemetryConfig> telemetryConfigsByType(ConfigType type);

    /**
     * Removes all telemetry configs.
     */
    void clear();
}
