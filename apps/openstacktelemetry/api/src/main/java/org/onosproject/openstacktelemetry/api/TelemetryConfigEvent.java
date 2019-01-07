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

import org.onosproject.event.AbstractEvent;
import org.onosproject.openstacktelemetry.api.config.TelemetryConfig;

/**
 * Describes telemetry config event.
 */
public class TelemetryConfigEvent
        extends AbstractEvent<TelemetryConfigEvent.Type, TelemetryConfig> {

    /**
     * Telemetry config event type.
     */
    public enum Type {
        /**
         * Signifies that a new telemetry config is added.
         */
        CONFIG_ADDED,
        /**
         * Signifies that an existing telemetry config is updated.
         */
        CONFIG_UPDATED,
        /**
         * Signifies that an existing telemetry config is removed.
         */
        CONFIG_DELETED,
        /**
         * Signifies that a telemetry service is enabled.
         */
        SERVICE_ENABLED,
        /**
         * Signifies that a telemetry service is disabled.
         */
        SERVICE_DISABLED,
        /**
         * Signifies that a telemetry service in a pending status due to previous error.
         */
        SERVICE_PENDING
    }

    /**
     * Creates an event of a given type for the specified telemetry config.
     *
     * @param type     telemetry config type
     * @param config   telemetry config
     */
    public TelemetryConfigEvent(Type type, TelemetryConfig config) {
        super(type, config);
    }
}
