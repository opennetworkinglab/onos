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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.XMLConfiguration;
import org.onosproject.openstacktelemetry.api.DefaultTelemetryConfig;
import org.onosproject.openstacktelemetry.api.config.TelemetryConfig;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.onosproject.openstacktelemetry.api.config.TelemetryConfig.ConfigType.GRPC;
import static org.onosproject.openstacktelemetry.api.config.TelemetryConfig.ConfigType.INFLUXDB;
import static org.onosproject.openstacktelemetry.api.config.TelemetryConfig.ConfigType.KAFKA;
import static org.onosproject.openstacktelemetry.api.config.TelemetryConfig.ConfigType.PROMETHEUS;
import static org.onosproject.openstacktelemetry.api.config.TelemetryConfig.ConfigType.REST;

/**
 * Utility capable of reading telemetry configuration XML resources and producing
 * a telemetry config as a result.
 * <p>
 * The telemetry configurations stream structure is as follows:
 * </p>
 * <pre>
 *     &lt;configs&gt;
 *         &lt;config name="..." [manufacturer="..." swVersion="..."]&gt;
 *            [&lt;property name="key"&gt;value&lt;/key&gt;]
 *            ...
 *        &lt;/config&gt;
 *        ...
 *     &lt;/configs&gt;
 * </pre>
 */
public class XmlTelemetryConfigLoader {

    private static final String CONFIGS = "configs";
    private static final String CONFIG = "config";

    private static final String PROPERTY = "property";

    private static final String NAME = "[@name]";
    private static final String TYPE = "[@type]";
    private static final String EXTENDS = "[@extends]";
    private static final String MFG = "[@manufacturer]";
    private static final String SW = "[@swVersion]";
    private static final String STATUS = "[@status]";

    private Map<String, TelemetryConfig> configs = Maps.newHashMap();

    /**
     * Creates a new config loader capable of loading configs from the supplied
     * class loader.
     */
    public XmlTelemetryConfigLoader() {
    }

    /**
     * Loads the specified telemetry configs resource as an XML stream and parses
     * it to produce a ready-to-register config provider.
     *
     * @param configsStream stream containing the telemetry configs definition
     * @return telemetry configuration provider
     * @throws IOException if issues are encountered reading the stream
     *                     or parsing the telemetry config definition within
     */
    public DefaultTelemetryConfigProvider
            loadTelemetryConfigs(InputStream configsStream) throws IOException {
        try {
            XMLConfiguration cfg = new XMLConfiguration();
            cfg.setRootElementName(CONFIGS);
            cfg.setAttributeSplittingDisabled(true);

            cfg.load(configsStream);
            return loadTelemetryConfigs(cfg);
        } catch (ConfigurationException e) {
            throw new IOException("Unable to load telemetry configs", e);
        }
    }

    /**
     * Loads a telemetry config provider from the supplied hierarchical configuration.
     *
     * @param telemetryConfig hierarchical configuration containing the configs definition
     * @return telemetry configuration provider
     */
    public DefaultTelemetryConfigProvider
                loadTelemetryConfigs(HierarchicalConfiguration telemetryConfig) {
        DefaultTelemetryConfigProvider provider = new DefaultTelemetryConfigProvider();
        for (HierarchicalConfiguration cfg : telemetryConfig.configurationsAt(CONFIG)) {
            DefaultTelemetryConfig config = loadTelemetryConfig(cfg);
            configs.put(config.name(), config);
            provider.addConfig(config);
        }
        configs.clear();
        return provider;
    }

    /**
     * Loads a telemetry configuration from the supplied hierarchical configuration.
     *
     * @param telemetryCfg hierarchical configuration containing the telemetry config definition
     * @return telemetry configuration
     */
    public DefaultTelemetryConfig loadTelemetryConfig(HierarchicalConfiguration telemetryCfg) {
        String name = telemetryCfg.getString(NAME);
        String parentsString = telemetryCfg.getString(EXTENDS, "");
        List<TelemetryConfig> parents = Lists.newArrayList();

        if (!"".equals(parentsString)) {
            List<String> parentsNames;
            if (parentsString.contains(",")) {
                parentsNames = Arrays.asList(
                        parentsString.replace(" ", "").split(","));
            } else {
                parentsNames = Lists.newArrayList(parentsString);
            }
            parents = parentsNames.stream().map(parent -> (parent != null) ?
                    configs.get(parent) : null).collect(Collectors.toList());
        }

        String typeStr = telemetryCfg.getString(TYPE, getParentAttribute(parents, TYPE));
        String manufacturer = telemetryCfg.getString(MFG, getParentAttribute(parents, MFG));
        String swVersion = telemetryCfg.getString(SW, getParentAttribute(parents, SW));

        // note that we do not inherits enabled property from parent
        String statusStr = telemetryCfg.getString(STATUS);
        TelemetryConfig.Status status =
                statusStr == null ? TelemetryConfig.Status.UNKNOWN : status(statusStr);

        TelemetryConfig.ConfigType type = type(typeStr);

        if (type == null) {
            return null;
        }

        return new DefaultTelemetryConfig(name, type, parents, manufacturer,
                swVersion, status, parseProperties(parents, telemetryCfg));
    }

    private TelemetryConfig.ConfigType type(String typeStr) {
        switch (typeStr.toUpperCase()) {
            case "GRPC" :
                return GRPC;
            case "KAFKA":
                return KAFKA;
            case "REST":
                return REST;
            case "INFLUXDB":
                return INFLUXDB;
            case "PROMETHEUS":
                return PROMETHEUS;
            case "UNKNOWN":
            default:
                return TelemetryConfig.ConfigType.UNKNOWN;
        }
    }

    // Returns the specified property from the highest priority parent
    private String getParentAttribute(List<TelemetryConfig> parents, String attribute) {
        if (!parents.isEmpty()) {
            TelemetryConfig parent = parents.get(0);
            switch (attribute) {
                case TYPE:
                    return parent.type().name().toLowerCase();
                case MFG:
                    return parent.manufacturer();
                case SW:
                    return parent.swVersion();
                default:
                    throw new IllegalArgumentException("Unsupported attribute");
            }
        }
        return "";
    }

    // Parses the properties section.
    private Map<String, String> parseProperties(List<TelemetryConfig> parents,
                                                HierarchicalConfiguration config) {
        ImmutableMap.Builder<String, String> properties = ImmutableMap.builder();

        // note that, we only allow the inheritance from single source
        final Map<String, String> parentConfigs = Maps.newHashMap();
        if (!parents.isEmpty()) {
            TelemetryConfig parent = parents.get(0);
            parent.properties().forEach(parentConfigs::put);
        }

        for (HierarchicalConfiguration b : config.configurationsAt(PROPERTY)) {
            if (parentConfigs.keySet().contains(b.getString(NAME))) {
                parentConfigs.remove(b.getString(NAME));
            }
        }

        properties.putAll(parentConfigs);

        for (HierarchicalConfiguration b : config.configurationsAt(PROPERTY)) {
            properties.put(b.getString(NAME), (String) b.getRootNode().getValue());
        }

        return properties.build();
    }

    private TelemetryConfig.Status status(String status) {
        switch (status.toUpperCase()) {
            case "ENABLED" :
                return TelemetryConfig.Status.ENABLED;
            case "DISABLED" :
                return TelemetryConfig.Status.DISABLED;
            case "PENDING" :
                return TelemetryConfig.Status.PENDING;
            default:
                return TelemetryConfig.Status.UNKNOWN;
        }
    }
}
