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
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.onosproject.openstacktelemetry.api.config.TelemetryConfig;

/**
 * Hamcrest matcher for TelemetryConfig.
 */
public final class TelemetryConfigJsonMatcher extends TypeSafeDiagnosingMatcher<JsonNode> {

    private final TelemetryConfig telemetryConfig;

    private static final String NAME = "name";
    private static final String TYPE = "type";
    private static final String MANUFACTURER = "manufacturer";
    private static final String SW_VERSION = "swVersion";
    private static final String STATUS = "status";
    private static final String PROPS = "props";
    private static final String KEY = "key";
    private static final String VALUE = "value";

    private TelemetryConfigJsonMatcher(TelemetryConfig telemetryConfig) {
        this.telemetryConfig = telemetryConfig;
    }

    @Override
    protected boolean matchesSafely(JsonNode jsonNode, Description description) {

        // check name
        String jsonName = jsonNode.get(NAME).asText();
        String name = telemetryConfig.name();
        if (!jsonName.equals(name)) {
            description.appendText("name was " + jsonName);
            return false;
        }

        // check type
        String jsonType = jsonNode.get(TYPE).asText();
        String type = telemetryConfig.type().name();
        if (!jsonType.equalsIgnoreCase(type)) {
            description.appendText("type was " + jsonType);
            return false;
        }

        // check manufacturer
        String jsonManufacturer = jsonNode.get(MANUFACTURER).asText();
        String manufacturer = telemetryConfig.manufacturer();
        if (!jsonManufacturer.equals(manufacturer)) {
            description.appendText("manufacturer was " + jsonManufacturer);
            return false;
        }

        // check software version
        String jsonSwVersion = jsonNode.get(SW_VERSION).asText();
        String swVersion = telemetryConfig.swVersion();
        if (!jsonSwVersion.equals(swVersion)) {
            description.appendText("SW version was " + jsonSwVersion);
            return false;
        }

        // check status
        JsonNode jsonStatus = jsonNode.get(STATUS);
        TelemetryConfig.Status status = telemetryConfig.status();
        if (jsonStatus == null || !jsonStatus.asText().equals(status.name())) {
            description.appendText("Enabled was " + jsonStatus);
            return false;
        }

        // check properties
        JsonNode jsonProperties = jsonNode.get(PROPS);
        if (jsonProperties != null) {
            if (jsonProperties.size() != telemetryConfig.properties().size()) {
                description.appendText("properties size was " + jsonProperties.size());
                return false;
            }

            for (String key : telemetryConfig.properties().keySet()) {
                boolean keyFound = false;
                boolean valueFound = false;
                String value = telemetryConfig.properties().get(key);
                for (int keyIndex = 0; keyIndex < jsonProperties.size(); keyIndex++) {
                    ObjectNode jsonProperty = get(jsonProperties, keyIndex);
                    JsonNode jsonKey = jsonProperty.get(KEY);
                    JsonNode jsonValue = jsonProperty.get(VALUE);

                    if (jsonKey != null && jsonValue != null) {
                        if (jsonKey.asText().equals(key)) {
                            keyFound = true;
                        }

                        if (jsonValue.asText().equals(value)) {
                            valueFound = true;
                        }

                        if (keyFound && valueFound) {
                            break;
                        }
                    }
                }

                if (!keyFound) {
                    description.appendText("Property key not found " + key);
                    return false;
                }

                if (!valueFound) {
                    description.appendText("Property value not found " + value);
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(telemetryConfig.toString());
    }

    /**
     * Factory to allocate an flow info matcher.
     *
     * @param telemetryConfig telemetry config object we are looking for
     * @return matcher
     */
    public static TelemetryConfigJsonMatcher
                    matchesTelemetryConfig(TelemetryConfig telemetryConfig) {
        return new TelemetryConfigJsonMatcher(telemetryConfig);
    }

    private static ObjectNode get(JsonNode parent, int childIndex) {
        JsonNode node = parent.path(childIndex);
        return node.isObject() && !node.isNull() ? (ObjectNode) node : null;
    }
}
