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

import com.google.common.collect.ImmutableSet;
import org.onosproject.store.Store;

import java.util.Set;

/**
 * Service for storing and distributing system-wide configurations for various
 * software components.
 */
public interface ComponentConfigStore
        extends Store<ComponentConfigEvent, ComponentConfigStoreDelegate> {

    /**
     * Sets the value of the specified configuration property.
     *
     * @param componentName component name
     * @param name          property name
     * @param value         new property value
     */
    void setProperty(String componentName, String name, String value);

    /**
     * Sets the value of the specified configuration property.
     *
     * @param componentName component name
     * @param name          property name
     * @param value         new property value
     * @param override      true to override even if the property has been set to other value
     */
    void setProperty(String componentName, String name, String value, boolean override);

    /**
     * Clears the value of the specified configuration property thus making
     * the property take on its default value.
     *
     * @param componentName component name
     * @param name          property name
     */
    void unsetProperty(String componentName, String name);


    /**
     * Returns set of component configuration property names. This includes
     * only the names of properties whose values depart from their default.
     *
     * @param component component name
     * @return set of property names whose values are set to non-default values
     */
    default Set<String> getProperties(String component) {
        return ImmutableSet.of();
    }

    /**
     * Returns the string value of the given component configuration property.
     * For properties whose values are set to their default this may return null.
     *
     * @param component component name
     * @param name      property name; null if no property found or if value
     *                  is default
     * @return set of property names
     */
    default String getProperty(String component, String name) {
        return null;
    }

}
