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
package org.onosproject.codec.impl;

import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.net.driver.Driver;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * JSON codec for the Driver class.
 */
public final class DriverCodec extends JsonCodec<Driver> {
    private static final String PARENT = "parent";
    private static final String NAME = "name";
    private static final String MANUFACTURER = "manufacturer";
    private static final String HW_VERSION = "hwVersion";
    private static final String SW_VERSION = "swVersion";
    private static final String BEHAVIOURS = "behaviours";
    private static final String BEHAVIORS_NAME = "name";
    private static final String BEHAVIORS_IMPLEMENTATION_NAME = "implementationName";
    private static final String PROPERTIES = "properties";

    @Override
    public ObjectNode encode(Driver driver, CodecContext context) {
        checkNotNull(driver, "Driver cannot be null");

        ObjectNode result = context.mapper().createObjectNode()
                .put(NAME, driver.name())
                .put(MANUFACTURER, driver.manufacturer())
                .put(HW_VERSION, driver.hwVersion())
                .put(SW_VERSION, driver.swVersion());

        if (driver.parent() != null) {
            result.put(PARENT, driver.parent().name());
        }

        ArrayNode behaviours = context.mapper().createArrayNode();
        driver.behaviours().forEach(behaviour -> {
            ObjectNode entry = context.mapper().createObjectNode()
                    .put(BEHAVIORS_NAME, behaviour.getCanonicalName())
                    .put(BEHAVIORS_IMPLEMENTATION_NAME,
                            driver.implementation(behaviour).getCanonicalName());

            behaviours.add(entry);
        });
        result.set(BEHAVIOURS, behaviours);

        ArrayNode properties = context.mapper().createArrayNode();
        driver.properties().forEach((name, value) -> {
            ObjectNode entry = context.mapper().createObjectNode()
                    .put("name", name)
                    .put("value", value);

            properties.add(entry);
        });
        result.set(PROPERTIES, properties);

        return result;
    }
}
