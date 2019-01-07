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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.onosproject.openstacktelemetry.api.config.TelemetryConfig;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableMap.copyOf;

/**
 * Implementation of TelemetryConfig.
 */
public final class DefaultTelemetryConfig implements TelemetryConfig {
    private final String name;
    private final ConfigType type;
    private final List<TelemetryConfig> parents;

    private final String manufacturer;
    private final String swVersion;
    private final Status status;

    private final Map<String, String> properties;

    /**
     * Creates a configuration with the specified name.
     *
     * @param name          configuration name
     * @param type          configuration type
     * @param parents       optional parent configurations
     * @param manufacturer  off-platform application manufacturer
     * @param swVersion     off-platform application software version
     * @param status        service status
     * @param properties    properties for telemetry configuration
     */
    public DefaultTelemetryConfig(String name, ConfigType type,
                                  List<TelemetryConfig> parents,
                                  String manufacturer, String swVersion,
                                  Status status, Map<String, String> properties) {
        this.name = checkNotNull(name, "Name cannot be null");
        this.type = checkNotNull(type, "type cannot be null");
        this.parents = parents == null ? ImmutableList.of() : ImmutableList.copyOf(parents);
        this.manufacturer = checkNotNull(manufacturer, "Manufacturer cannot be null");
        this.swVersion = checkNotNull(swVersion, "SW version cannot be null");
        this.properties = copyOf(checkNotNull(properties, "Properties cannot be null"));
        this.status = checkNotNull(status, "status cannot be null");
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public ConfigType type() {
        return type;
    }

    @Override
    public List<TelemetryConfig> parents() {
        if (parents == null) {
            return ImmutableList.of();
        } else {
            return ImmutableList.copyOf(parents);
        }
    }

    @Override
    public String manufacturer() {
        return manufacturer;
    }

    @Override
    public String swVersion() {
        return swVersion;
    }

    @Override
    public Status status() {
        return status;
    }

    @Override
    public Map<String, String> properties() {
        if (properties == null) {
            return ImmutableMap.of();
        } else {
            return ImmutableMap.copyOf(properties);
        }
    }

    @Override
    public String getProperty(String name) {
        Queue<TelemetryConfig> queue = new LinkedList<>();
        queue.add(this);
        while (!queue.isEmpty()) {
            TelemetryConfig config = queue.remove();
            String property = config.properties().get(name);
            if (property != null) {
                return property;
            } else if (config.parents() != null) {
                queue.addAll(config.parents());
            }
        }
        return null;
    }

    @Override
    public TelemetryConfig merge(TelemetryConfig other) {
        // merge the properties
        ImmutableMap.Builder<String, String> properties = ImmutableMap.builder();
        properties.putAll(other.properties());

        // remove duplicated properties from this configuration and merge
        this.properties().entrySet().stream()
                .filter(e -> !other.properties().containsKey(e.getKey()))
                .forEach(properties::put);

        List<TelemetryConfig> completeParents = new ArrayList<>();

        if (parents != null) {
            parents.forEach(parent -> other.parents().forEach(otherParent -> {
                if (otherParent.name().equals(parent.name())) {
                    completeParents.add(parent.merge(otherParent));
                } else if (!completeParents.contains(otherParent)) {
                    completeParents.add(otherParent);
                } else if (!completeParents.contains(parent)) {
                    completeParents.add(parent);
                }
            }));
        }

        return new DefaultTelemetryConfig(name, type,
                !completeParents.isEmpty() ? completeParents : other.parents(),
                manufacturer, swVersion, status, properties.build());
    }

    @Override
    public TelemetryConfig updateProperties(Map<String, String> properties) {

        return new DefaultTelemetryConfig(name, type, parents, manufacturer,
                swVersion, status, properties);
    }

    @Override
    public TelemetryConfig updateStatus(Status status) {
        return new DefaultTelemetryConfig(name, type, parents, manufacturer,
                swVersion, status, properties);
    }

    @Override
    public Set<String> keys() {
        return properties.keySet();
    }

    @Override
    public String value(String key) {
        return properties.get(key);
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("name", name)
                .add("type", type)
                .add("parents", parents)
                .add("manufacturer", manufacturer)
                .add("swVersion", swVersion)
                .add("status", status)
                .add("properties", properties)
                .toString();
    }

    @Override
    public boolean equals(Object configToBeCompared) {
        if (this == configToBeCompared) {
            return true;
        }

        if (configToBeCompared == null || getClass() != configToBeCompared.getClass()) {
            return false;
        }

        DefaultTelemetryConfig telemetryConfig =
                                    (DefaultTelemetryConfig) configToBeCompared;
        return name.equals(telemetryConfig.name());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name);
    }
}
