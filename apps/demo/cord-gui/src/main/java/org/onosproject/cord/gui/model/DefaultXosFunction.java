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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Default implementation of an XOS function.
 */
public class DefaultXosFunction implements XosFunction {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final XosFunctionDescriptor descriptor;

    public DefaultXosFunction(XosFunctionDescriptor xfd) {
        descriptor = xfd;
    }

    public XosFunctionDescriptor descriptor() {
        return descriptor;
    }

    public ObjectNode params() {
        return MAPPER.createObjectNode();
    }

    public String toJson() {
        return null;
    }

    public JsonNode toJsonNode() {
        return null;
    }
}
