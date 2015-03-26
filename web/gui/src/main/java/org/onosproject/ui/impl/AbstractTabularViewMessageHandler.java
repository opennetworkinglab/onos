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
 */
package org.onosproject.ui.impl;

import com.fasterxml.jackson.databind.node.ArrayNode;
import org.onosproject.ui.UiMessageHandler;

import java.util.Set;

/**
 * Base message handler for tabular views.
 */
public abstract class AbstractTabularViewMessageHandler extends UiMessageHandler {

    /**
     * Creates a new tabular view message handler.
     *
     * @param messageTypes set of message types
     */
    protected AbstractTabularViewMessageHandler(Set<String> messageTypes) {
        super(messageTypes);
    }

    /**
     * Produces JSON from the specified array of rows.
     *
     * @param rows table rows
     * @return JSON array
     */
    protected ArrayNode generateArrayNode(TableRow[] rows) {
        ArrayNode array = mapper.createArrayNode();
        for (TableRow r : rows) {
            array.add(r.toJsonNode());
        }
        return array;
    }

    // TODO: possibly convert this into just a toolbox class
    // TODO: extract and generalize other table constructs
}
