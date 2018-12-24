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
 * Represents entity capable of providing telemetry configurations.
 */
public interface TelemetryConfigProvider {

    /**
     * Returns the set of telemetry configuration to be made available by this provider.
     *
     * @return set of telemetry configurations
     */
    Set<TelemetryConfig> getTelemetryConfigs();
}
