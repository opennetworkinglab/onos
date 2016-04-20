/*
 * Copyright 2016-present Open Networking Laboratory
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
package org.onosproject.isis.io.util;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Representation of ISIS config.
 */
public enum IsisConfig {
    INSTANCE;
    private JsonNode jsonNodes = null;

    /**
     * Returns the config value.
     *
     * @return jsonNodes json node
     */
    public JsonNode config() {
        return jsonNodes;
    }

    /**
     * Sets the config value for jsonNode.
     *
     * @param jsonNodes json node
     */
    public void setConfig(JsonNode jsonNodes) {
        this.jsonNodes = jsonNodes;
    }
}