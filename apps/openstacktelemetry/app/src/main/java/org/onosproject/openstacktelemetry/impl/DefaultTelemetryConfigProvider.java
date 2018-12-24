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

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import org.onosproject.openstacktelemetry.api.TelemetryConfigProvider;
import org.onosproject.openstacktelemetry.api.config.TelemetryConfig;

import java.util.Map;
import java.util.Set;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Default telemetry configuration provider implementation.
 */
public class DefaultTelemetryConfigProvider implements TelemetryConfigProvider {

    protected final Map<String, TelemetryConfig> configs = Maps.newConcurrentMap();

    @Override
    public Set<TelemetryConfig> getTelemetryConfigs() {
        return ImmutableSet.copyOf(configs.values());
    }

    /**
     * Adds the specified configuration to the provider. If a configuration with
     * the name does not exist yet, the specified one will be added. Otherwise,
     * the existing configuration will be merged with the new one and the result will
     * be registered.
     *
     * @param config telemetry configuration to be provided
     * @return registered configuration
     */
    public TelemetryConfig addConfig(TelemetryConfig config) {
        return configs.compute(config.name(), (name, oldConfig) ->
                oldConfig == null ? config : oldConfig.merge(config));
    }

    /**
     * Removes the specified configuration from the provider.
     *
     * @param config telemetry configuration
     */
    public void removeConfig(TelemetryConfig config) {
        configs.remove(config.name());
    }

    @Override
    public String toString() {
        return toStringHelper(this).add("configs", configs).toString();
    }
}
