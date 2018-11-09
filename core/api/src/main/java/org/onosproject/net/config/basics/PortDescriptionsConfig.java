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

package org.onosproject.net.config.basics;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.annotations.Beta;
import com.google.common.collect.ImmutableList;
import org.onosproject.net.DefaultAnnotations;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Port;
import org.onosproject.net.PortNumber;
import org.onosproject.net.config.Config;
import org.onosproject.net.device.DefaultPortDescription;
import org.onosproject.net.device.PortDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Configuration for Ports. Creates a list of PortDescription based on the given Json.
 */
@Beta
public class PortDescriptionsConfig extends Config<DeviceId> {
    private static Logger log = LoggerFactory.getLogger(PortDescriptionsConfig.class);

    private static final String NUMBER = "number";
    private static final String NAME = "name";
    private static final String ENABLED = "enabled";
    private static final String REMOVED = "removed";
    private static final String TYPE = "type";
    private static final String SPEED = "speed";
    private static final String ANNOTATIONS = "annotations";

    private static final String CONFIG_VALUE_ERROR = "Error parsing config value";

    @Override
    public boolean isValid() {
        for (Iterator<Map.Entry<String, JsonNode>> it = node.fields(); it.hasNext();) {
            JsonNode nodePort = it.next().getValue();
            if (!hasOnlyFields((ObjectNode) nodePort, NUMBER, NAME, ENABLED, REMOVED, TYPE,
                    SPEED, ANNOTATIONS)) {
                return false;
            }
            ObjectNode obj = (ObjectNode) nodePort;

            if (!(isNumber(obj, NUMBER, FieldPresence.MANDATORY) &&
                    isString(obj, NAME, FieldPresence.OPTIONAL) &&
                    isBoolean(obj, ENABLED, FieldPresence.OPTIONAL) &&
                    isBoolean(obj, REMOVED, FieldPresence.OPTIONAL) &&
                    isString(obj, TYPE, FieldPresence.OPTIONAL) &&
                    isIntegralNumber(obj, SPEED, FieldPresence.OPTIONAL))) {
                return false;
            }

            if (node.has(ANNOTATIONS) && !node.get(ANNOTATIONS).isObject()) {
                log.error("Annotations must be an inner json node");
                return false;
            }

        }
        return true;
    }

    /**
     * Retrieves all port descriptions.
     *
     * @return set of port descriptions
     */
    public List<PortDescription> portDescriptions() {

        try {
            ImmutableList.Builder<PortDescription> portDescriptions = ImmutableList.builder();
            for (Iterator<Map.Entry<String, JsonNode>> it = node.fields(); it.hasNext();) {
                JsonNode portNode = it.next().getValue();
                long number = portNode.path(NUMBER).asLong();

                String name = portNode.path(NAME).asText(null);

                PortNumber portNumber = createPortNumber(number, name);

                DefaultPortDescription.Builder builder = DefaultPortDescription.builder()
                        .withPortNumber(portNumber);
                if (portNode.has(ENABLED)) {
                    builder.isEnabled(portNode.path(ENABLED).asBoolean());
                }

                if (portNode.has(REMOVED)) {
                    builder.isRemoved(portNode.path(REMOVED).asBoolean());
                }

                if (portNode.has(TYPE)) {
                    builder.type(Port.Type.valueOf(portNode.path(TYPE).asText().toUpperCase()));
                }

                if (portNode.has(SPEED)) {
                    builder.portSpeed(portNode.path(SPEED).asLong());
                }

                if (portNode.has(ANNOTATIONS)) {
                    DefaultAnnotations.Builder annotationsBuilder = DefaultAnnotations.builder();
                    Iterator<Map.Entry<String, JsonNode>> annotationsIt = portNode.get(ANNOTATIONS).fields();
                    while (annotationsIt.hasNext()) {
                        Map.Entry<String, JsonNode> entry = annotationsIt.next();
                        annotationsBuilder.set(entry.getKey(), entry.getValue().asText());
                    }
                    builder.annotations(annotationsBuilder.build());
                }

                portDescriptions.add(builder.build());
            }

            return portDescriptions.build();

        } catch (IllegalArgumentException e) {
            log.error(CONFIG_VALUE_ERROR, e);
            return ImmutableList.of();
        }
    }

    private PortNumber createPortNumber(long number, String name) {
        if (name == null) {
            return PortNumber.portNumber(number);
        }
        return PortNumber.portNumber(number, name);
    }


}
