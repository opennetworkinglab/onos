/*
 * Copyright 2014-2015 Open Networking Laboratory
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

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/*
 * JSON file example
 *

{
  "deviceId" : [ "of:2222000000000000" ],
  "deviceType" : [ "ROADM" ],
  "annotation" : { "no-lldp" : null, "sendLLDP" : "false" }
}
 */

/**
 * Allows for reading and writing LLDP suppression definition as a JSON file.
 */
public class SuppressionRulesStore {

    private static final String DEVICE_ID = "deviceId";
    private static final String DEVICE_TYPE = "deviceType";
    private static final String ANNOTATION = "annotation";

    private final Logger log = getLogger(getClass());

    private final File file;

    /**
     * Creates a reader/writer of the LLDP suppression definition file.
     *
     * @param filePath location of the definition file
     */
    public SuppressionRulesStore(String filePath) {
        file = new File(filePath);
    }

    /**
     * Creates a reader/writer of the LLDP suppression definition file.
     *
     * @param file definition file
     */
    public SuppressionRulesStore(File file) {
        this.file = checkNotNull(file);
    }

    /**
     * Returns SuppressionRules.
     *
     * @return SuppressionRules
     * @throws IOException if error occurred while reading the data
     */
    public SuppressionRules read() throws IOException {
        final Set<DeviceId> suppressedDevice = new HashSet<>();
        final EnumSet<Device.Type> suppressedDeviceType = EnumSet.noneOf(Device.Type.class);
        final Map<String, String> suppressedAnnotation = new HashMap<>();

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = (ObjectNode) mapper.readTree(file);

        for (JsonNode deviceId : root.get(DEVICE_ID)) {
            if (deviceId.isTextual()) {
                suppressedDevice.add(DeviceId.deviceId(deviceId.asText()));
            } else {
                log.warn("Encountered unexpected JSONNode {} for deviceId", deviceId);
            }
        }

        for (JsonNode deviceType : root.get(DEVICE_TYPE)) {
            if (deviceType.isTextual()) {
                suppressedDeviceType.add(Device.Type.valueOf(deviceType.asText()));
            } else {
                log.warn("Encountered unexpected JSONNode {} for deviceType", deviceType);
            }
        }

        JsonNode annotation = root.get(ANNOTATION);
        if (annotation.isObject()) {
            ObjectNode obj = (ObjectNode) annotation;
            Iterator<Entry<String, JsonNode>> it = obj.fields();
            while (it.hasNext()) {
                Entry<String, JsonNode> entry = it.next();
                final String key = entry.getKey();
                final JsonNode value = entry.getValue();

                if (value.isValueNode()) {
                    if (value.isNull()) {
                        suppressedAnnotation.put(key, SuppressionRules.ANY_VALUE);
                    } else {
                        suppressedAnnotation.put(key, value.asText());
                    }
                } else {
                    log.warn("Encountered unexpected JSON field {} for annotation", entry);
                }
            }
        } else {
            log.warn("Encountered unexpected JSONNode {} for annotation", annotation);
        }

        return new SuppressionRules(suppressedDevice,
                                    suppressedDeviceType,
                                    suppressedAnnotation);
    }

    /**
     * Writes the given SuppressionRules.
     *
     * @param rules SuppressionRules
     * @throws IOException if error occurred while writing the data
     */
    public void write(SuppressionRules rules) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = mapper.createObjectNode();
        ArrayNode deviceIds = mapper.createArrayNode();
        ArrayNode deviceTypes = mapper.createArrayNode();
        ObjectNode annotations = mapper.createObjectNode();
        root.set(DEVICE_ID, deviceIds);
        root.set(DEVICE_TYPE, deviceTypes);
        root.set(ANNOTATION, annotations);

        rules.getSuppressedDevice()
            .forEach(deviceId -> deviceIds.add(deviceId.toString()));

        rules.getSuppressedDeviceType()
            .forEach(type -> deviceTypes.add(type.toString()));

        rules.getSuppressedAnnotation().forEach((key, value) -> {
            if (value == SuppressionRules.ANY_VALUE) {
                annotations.putNull(key);
            } else {
                annotations.put(key, value);
            }
        });
        mapper.writeTree(new JsonFactory().createGenerator(file, JsonEncoding.UTF8),
                         root);
    }
}
