/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.provider.lldp.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;

import org.onosproject.core.ApplicationId;
import org.onosproject.net.Device;
import org.onosproject.net.config.Config;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static org.onosproject.provider.lldp.impl.LldpLinkProvider.DEFAULT_RULES;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * LinkDiscovery suppression config class.
 */
public class SuppressionConfig extends Config<ApplicationId> {

    private static final String DEVICE_TYPES = "deviceTypes";
    private static final String ANNOTATION = "annotation";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final List<Device.Type>  DEFAULT_DEVICE_TYPES
                = ImmutableList.copyOf(DEFAULT_RULES.getSuppressedDeviceType());

    private final Logger log = getLogger(getClass());

    /**
     * Returns types of devices on which LinkDiscovery is suppressed.
     *
     * @return set of device types
     */
    public Set<Device.Type> deviceTypes() {
        return ImmutableSet.copyOf(getList(DEVICE_TYPES, Device.Type::valueOf, DEFAULT_DEVICE_TYPES));
    }

    /**
     * Sets types of devices on which LinkDiscovery is suppressed.
     *
     * @param deviceTypes new set of device types; null to clear
     * @return self
     */
    public SuppressionConfig deviceTypes(Set<Device.Type> deviceTypes) {
        return (SuppressionConfig) setOrClear(DEVICE_TYPES, deviceTypes);
    }

    /**
     * Returns annotation of Ports on which LinkDiscovery is suppressed.
     *
     * @return key-value pairs of annotation
     */
    public Map<String, String> annotation() {
        ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();

        String jsonAnnotation = get(ANNOTATION, null);
        if (jsonAnnotation == null || jsonAnnotation.isEmpty()) {
            return ImmutableMap.of();
        }

        JsonNode annotationNode;
        try {
            annotationNode = MAPPER.readTree(jsonAnnotation);
        } catch (IOException e) {
            log.error("Failed to read JSON tree from: {}", jsonAnnotation);
            return ImmutableMap.of();
        }

        if (annotationNode.isObject()) {
            ObjectNode obj = (ObjectNode) annotationNode;
            Iterator<Map.Entry<String, JsonNode>> it = obj.fields();
            while (it.hasNext()) {
                Map.Entry<String, JsonNode> entry = it.next();
                final String key = entry.getKey();
                final JsonNode value = entry.getValue();

                if (value.isValueNode()) {
                    if (value.isNull()) {
                        builder.put(key, SuppressionRules.ANY_VALUE);
                    } else {
                        builder.put(key, value.asText());
                    }
                } else {
                    log.warn("Encountered unexpected JSON field {} for annotation", entry);
                }
            }
        } else {
            log.error("Encountered unexpected JSONNode {} for annotation", annotationNode);
            return ImmutableMap.of();
        }

        return builder.build();
    }

    /**
     * Sets annotation of Ports on which LinkDiscovery is suppressed.
     *
     * @param annotation new key-value pair of annotation; null to clear
     * @return self
     */
    public SuppressionConfig annotation(Map<String, String> annotation) {

        // ANY_VALUE should be null in JSON
        Map<String, String> config = Maps.transformValues(annotation,
                      v -> (v == SuppressionRules.ANY_VALUE) ? null : v);

        String jsonAnnotation = null;

        try {
            // TODO Store annotation as a Map instead of a String (which needs NetworkConfigRegistry modification)
            jsonAnnotation = MAPPER.writeValueAsString(config);
        } catch (JsonProcessingException e) {
            log.error("Failed to write JSON from: {}", annotation);
        }

        return (SuppressionConfig) setOrClear(ANNOTATION, jsonAnnotation);
    }
}
