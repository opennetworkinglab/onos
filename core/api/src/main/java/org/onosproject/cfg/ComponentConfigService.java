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

import java.util.Set;

/**
 * Service for tracking system-wide configurations for various software components.
 */
public interface ComponentConfigService {

    /**
     * Returns names of all components that have registered their
     * configuration properties.
     *
     * @return set of component names
     */
    Set<String> getComponentNames();

    /**
     * Registers configuration properties for the specified component.
     *
     * @param componentClass class of configurable component
     */
    void registerProperties(Class<?> componentClass);

    /**
     * Unregisters configuration properties for the specified component.
     *
     * @param componentClass class of configurable component
     * @param clear          true indicates any settings should be cleared
     */
    void unregisterProperties(Class<?> componentClass, boolean clear);

    /**
     * Returns configuration properties of the named components.
     *
     * @param componentName component name
     * @return set of configuration properties
     */
    Set<ConfigProperty> getProperties(String componentName);

    /**
     * Sets the value of the specified configuration property.
     *
     * @param componentName component name
     * @param name          property name
     * @param value         new property value
     */
    void setProperty(String componentName, String name, String value);

    /**
     * Presets the value of the specified configuration property, regardless
     * of the component's state.
     *
     * @param componentName component name
     * @param name          property name
     * @param value         new property value
     */
    void preSetProperty(String componentName, String name, String value);

    /**
     * Presets the value of the specified configuration property, regardless
     * of the component's state.
     *
     * @param componentName component name
     * @param name          property name
     * @param value         new property value
     * @param override      true to override even if the property has been set to other value
     */
    void preSetProperty(String componentName, String name, String value, boolean override);

    /**
     * Clears the value of the specified configuration property thus making
     * the property take on its default value.
     *
     * @param componentName component name
     * @param name          property name
     */
    void unsetProperty(String componentName, String name);

    /**
     * Returns configuration property of the named components.
     *
     * @param componentName component name
     * @param attribute component attribute
     * @return configuration property
     */
    ConfigProperty getProperty(String componentName, String attribute);

}

