/*
 * Copyright 2017-present Open Networking Laboratory
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
package org.onosproject.net.config.basics;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.HashMap;
import java.util.Map;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.config.BaseConfig;
import org.slf4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Configuration to add extra annotations to a port via netcfg subsystem.
 */
public class PortAnnotationConfig
    extends BaseConfig<ConnectPoint> {

    /**
     * {@value #CONFIG_KEY} : a netcfg ConfigKey for {@link PortAnnotationConfig}.
     */
    public static final String CONFIG_KEY = "annotations";

    /**
     * JSON key for annotation entries.
     * Value is a JSON object.
     */
    private static final String ENTRIES = "entries";

    private final Logger log = getLogger(getClass());

    @Override
    public boolean isValid() {
        return hasField(ENTRIES) && object.get(ENTRIES).isObject();
    }

    /**
     * Returns annotations to add to a Port.
     *
     * @return annotations as a map. null value represent key removal request
     */
    public Map<String, String> annotations() {
        Map<String, String> map = new HashMap<>();

        JsonNode jsonNode = object.path(ENTRIES);
        if (!jsonNode.isObject()) {
            return map;
        }

        jsonNode.fields().forEachRemaining(entry -> {
            String key = entry.getKey();
            JsonNode value = entry.getValue();
            if (value.isTextual()) {
                map.put(key, value.asText());
            } else if (value.isNull()) {
                map.put(key, null);
            } else {
                try {
                    map.put(key, mapper().writeValueAsString(value));
                } catch (JsonProcessingException e) {
                    log.warn("Error processing JSON value for {}.", key, e);
                }
            }
        });
        return map;
    }

    /**
     * Sets annotations to add to a Port.
     *
     * @param replace annotations to be added by this configuration.
     *                null value represent key removal request
     * @return self
     */
    public PortAnnotationConfig annotations(Map<String, String> replace) {
        ObjectNode anns = object.objectNode();
        if (replace != null) {
            replace.forEach((k, v) -> {
                anns.put(k, v);
            });
        }
        object.set(ENTRIES, anns);
        return this;
    }

    /**
     * Add configuration to set or remove annotation entry.
     *
     * @param key annotations key
     * @param value annotations value. specifying null removes the entry.
     * @return self
     */
    public PortAnnotationConfig annotation(String key, String value) {
        JsonNode ent = object.path(ENTRIES);
        ObjectNode obj = (ent.isObject()) ? (ObjectNode) ent : object.objectNode();

        obj.put(key, value);

        object.set(ENTRIES, obj);
        return this;
    }

    /**
     * Remove configuration about specified key.
     *
     * @param key annotations key
     * @return self
     */
    public PortAnnotationConfig annotation(String key) {
        JsonNode ent = object.path(ENTRIES);
        ObjectNode obj = (ent.isObject()) ? (ObjectNode) ent : object.objectNode();

        obj.remove(key);

        object.set(ENTRIES, obj);
        return this;
    }

    /**
     * Create a detached {@link PortAnnotationConfig}.
     * <p>
     * Note: created instance needs to be initialized by #init(..) before using.
     */
    public PortAnnotationConfig() {
        super();
    }

    /**
     * Create a detached {@link PortAnnotationConfig} for specified port.
     * <p>
     * Note: created instance is not bound to NetworkConfigService,
     * thus cannot use {@link #apply()}. Must be passed to the service
     * using NetworkConfigService#applyConfig
     *
     * @param cp ConnectPoint
     */
    public PortAnnotationConfig(ConnectPoint cp) {
        ObjectMapper mapper = new ObjectMapper();
        init(cp, CONFIG_KEY, mapper.createObjectNode(), mapper, null);
    }
}
