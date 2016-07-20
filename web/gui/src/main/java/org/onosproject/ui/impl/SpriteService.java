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
package org.onosproject.ui.impl;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Set;

/**
 * Provisional service to keep track of the topology view sprite definitions.
 */
public interface SpriteService {

    /**
     * Returns set of registered sprite definition names.
     *
     * @return set of sprite definition names
     */
    Set<String> getNames();

    /**
     * Registers sprite data under the specified name.
     *
     * @param name       sprite definition name
     * @param spriteData sprite data
     */
    void put(String name, JsonNode spriteData);

    /**
     * Returns the sprite definition registered under the given name.
     *
     * @param name sprite definition name
     * @return sprite data
     */
    JsonNode get(String name);

}
