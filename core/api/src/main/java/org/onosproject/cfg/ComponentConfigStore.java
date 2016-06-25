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
package org.onosproject.cfg;

import org.onosproject.store.Store;

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
     * Clears the value of the specified configuration property thus making
     * the property take on its default value.
     *
     * @param componentName component name
     * @param name          property name
     */
    void unsetProperty(String componentName, String name);

}
