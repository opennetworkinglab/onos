/*
 * Copyright 2015 Open Networking Laboratory
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
 *
 */

package org.onosproject.cord.gui.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Base class for factories that convert objects to JSON.
 */
public abstract class JsonFactory {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    protected static final String ID = "id";
    protected static final String NAME = "name";
    protected static final String DESC = "desc";
    protected static final String ICON_ID = "icon_id";

    /**
     * Returns a freshly minted object node.
     *
     * @return empty object node
     */
    protected static ObjectNode objectNode() {
        return MAPPER.createObjectNode();
    }

    /**
     * Returns a freshly minted array node.
     *
     * @return empty array node
     */
    protected static ArrayNode arrayNode() {
        return MAPPER.createArrayNode();
    }
}
