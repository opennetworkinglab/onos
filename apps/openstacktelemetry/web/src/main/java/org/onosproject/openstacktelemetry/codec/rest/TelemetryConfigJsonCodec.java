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
package org.onosproject.openstacktelemetry.codec.rest;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.openstacktelemetry.api.DefaultTelemetryConfig;
import org.onosproject.openstacktelemetry.api.config.TelemetryConfig;

import java.util.Map;
import java.util.stream.IntStream;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.nullIsIllegal;

/**
 * Openstack telemetry config codec used for serializing and de-serializing JSON string.
 */
public final class TelemetryConfigJsonCodec extends JsonCodec<TelemetryConfig> {

    private static final String NAME = "name";
    private static final String TYPE = "type";
    private static final String MANUFACTURER = "manufacturer";
    private static final String SW_VERSION = "swVersion";
    private static final String ENABLED = "enabled";
    private static final String PROPS = "props";
    private static final String KEY = "key";
    private static final String VALUE = "value";

    private static final String MISSING_MESSAGE = " is required in TelemetryConfig";

    @Override
    public ObjectNode encode(TelemetryConfig config, CodecContext context) {
        checkNotNull(config, "TelemetryConfig cannot be null");

        ObjectNode node = context.mapper().createObjectNode()
                .put(NAME, config.name())
                .put(TYPE, config.type().name())
                .put(MANUFACTURER, config.manufacturer())
                .put(SW_VERSION, config.swVersion())
                .put(ENABLED, config.enabled());

        Map<String, String> props = config.properties();
        ArrayNode propsJson = context.mapper().createArrayNode();
        props.forEach((k, v) -> {
            ObjectNode propNode = context.mapper().createObjectNode();
            propNode.put(KEY, k);
            propNode.put(VALUE, v);
            propsJson.add(propNode);
        });
        node.set(PROPS, propsJson);
        return node;
    }

    @Override
    public TelemetryConfig decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        // parse name
        String name = nullIsIllegal(json.get(NAME),
                NAME + MISSING_MESSAGE).asText();

        // parse type
        String type = nullIsIllegal(json.get(TYPE),
                TYPE + MISSING_MESSAGE).asText();

        TelemetryConfig.ConfigType configType = configType(type);

        // parse manufacturer
        String manufacturer = nullIsIllegal(json.get(MANUFACTURER).asText(),
                MANUFACTURER + MISSING_MESSAGE);

        // parse software version
        String swVersion = nullIsIllegal(json.get(SW_VERSION),
                SW_VERSION + MISSING_MESSAGE).asText();

        // parse enabled flag
        boolean enabled = nullIsIllegal(json.get(ENABLED),
                ENABLED + MISSING_MESSAGE).asBoolean();

        JsonNode propertiesJson = json.get(PROPS);
        Map<String, String> properties = Maps.newConcurrentMap();
        if (propertiesJson != null) {
            IntStream.range(0, propertiesJson.size()).forEach(i -> {
                ObjectNode propertyJson = get(propertiesJson, i);
                properties.put(propertyJson.get(KEY).asText(),
                        propertyJson.get(VALUE).asText());
            });
        }

        return new DefaultTelemetryConfig(name, configType,
                ImmutableList.of(), manufacturer, swVersion, enabled, properties);
    }

    private TelemetryConfig.ConfigType configType(String type) {
        switch (type.toUpperCase()) {
            case "KAFKA" :
                return TelemetryConfig.ConfigType.KAFKA;
            case "GRPC" :
                return TelemetryConfig.ConfigType.GRPC;
            case "INFLUXDB" :
                return TelemetryConfig.ConfigType.INFLUXDB;
            case "PROMETHEUS" :
                return TelemetryConfig.ConfigType.PROMETHEUS;
            case "REST" :
                return TelemetryConfig.ConfigType.REST;
            default:
                return TelemetryConfig.ConfigType.UNKNOWN;
        }
    }
}
