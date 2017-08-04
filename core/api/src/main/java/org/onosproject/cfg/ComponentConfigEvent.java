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
package org.onosproject.cfg;

import org.onosproject.event.AbstractEvent;

/**
 * Describes a component configuration event.
 */
public class ComponentConfigEvent extends AbstractEvent<ComponentConfigEvent.Type, String> {

    private final String name;
    private final String value;

    public enum Type {
        /**
         * Signifies that a configuration property has set.
         */
        PROPERTY_SET,

        /**
         * Signifies that a configuration property has been unset.
         */
        PROPERTY_UNSET
    }

    /**
     * Creates an event of a given type and for the specified app and the
     * current time.
     *
     * @param type          config property event type
     * @param componentName component name event subject
     * @param name          config property name
     * @param value         config property value
     */
    public ComponentConfigEvent(Type type, String componentName,
                                String name, String value) {
        super(type, componentName);
        this.name = name;
        this.value = value;
    }

    /**
     * Returns the property name.
     *
     * @return property name
     */
    public String name() {
        return name;
    }

    /**
     * Returns the property value as a string.
     *
     * @return string value
     */
    public String value() {
        return value;
    }

}
