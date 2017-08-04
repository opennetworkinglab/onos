/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.codec.impl;

import java.util.Map;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.onosproject.net.driver.Driver;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Hamcrest matcher for drivers.
 */
public final class DriverJsonMatcher extends TypeSafeDiagnosingMatcher<JsonNode> {
    private final Driver driver;

    private DriverJsonMatcher(Driver driver) {
        this.driver = driver;
    }

    @Override
    public boolean matchesSafely(JsonNode jsonDriver, Description description) {
        // check id
        String jsonDriverName = jsonDriver.get("name").asText();
        String driverName = driver.name();
        if (!jsonDriverName.equals(driverName)) {
            description.appendText("name was " + jsonDriverName);
            return false;
        }


        // check parent
        String jsonParent = jsonDriver.get("parent").asText();
        String parent = driver.parent().name();
        if (!jsonParent.equals(parent)) {
            description.appendText("parent was " + jsonParent);
            return false;
        }

        // check manufacturer
        String jsonManufacturer = jsonDriver.get("manufacturer").asText();
        String manufacturer = driver.manufacturer();
        if (!jsonManufacturer.equals(manufacturer)) {
            description.appendText("manufacturer was " + jsonManufacturer);
            return false;
        }

        // check HW version
        String jsonHWVersion = jsonDriver.get("hwVersion").asText();
        String hwVersion = driver.hwVersion();
        if (!jsonHWVersion.equals(hwVersion)) {
            description.appendText("HW version was " + jsonHWVersion);
            return false;
        }

        // check SW version
        String jsonSWVersion = jsonDriver.get("swVersion").asText();
        String swVersion = driver.swVersion();
        if (!jsonSWVersion.equals(swVersion)) {
            description.appendText("SW version was " + jsonSWVersion);
            return false;
        }

        // Check properties
        JsonNode jsonProperties = jsonDriver.get("properties");
        if (driver.properties().size() != jsonProperties.size()) {
            description.appendText("properties map size was was " + jsonProperties.size());
            return false;
        }
        for (Map.Entry<String, String> entry : driver.properties().entrySet()) {
            boolean propertyFound = false;
            for (int propertyIndex = 0; propertyIndex < jsonProperties.size(); propertyIndex++) {
                String jsonName = jsonProperties.get(propertyIndex).get("name").asText();
                String jsonValue = jsonProperties.get(propertyIndex).get("value").asText();
                if (!jsonName.equals(entry.getKey()) ||
                        !jsonValue.equals(entry.getValue())) {
                    propertyFound = true;
                    break;
                }
            }
            if (!propertyFound) {
                description.appendText("property not found " + entry.getKey());
                return false;
            }
        }
        return true;
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(driver.toString());
    }

    /**
     * Factory to allocate a driver matcher.
     *
     * @param driver driver object we are looking for
     * @return matcher
     */
    public static DriverJsonMatcher matchesDriver(Driver driver) {
        return new DriverJsonMatcher(driver);
    }
}
