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

package org.onosproject.protocol.restconf.server.utils.parser.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.protocol.restconf.server.utils.parser.api.JsonWalker;
import org.onosproject.protocol.restconf.server.utils.parser.api.JsonListener;

import java.util.Iterator;
import java.util.Map;

/**
 * Represents implementation of JSON walk, which walks the JSON object node.
 */
public class DefaultJsonWalker implements JsonWalker {
    @Override
    public void walk(JsonListener jsonListener, String fieldName,
                     ObjectNode objectNode) {

        //enter the object node, the original ObjectNode should have a module
        //name as fieldName.
        jsonListener.enterJsonNode(fieldName, objectNode);
        //the node has no children, then exist and return.
        if (!objectNode.isContainerNode()) {
            jsonListener.exitJsonNode(objectNode);
            return;
        }

        Iterator<Map.Entry<String, JsonNode>> fields = objectNode.fields();
        while (fields.hasNext()) {
            //get the children entry of the node
            Map.Entry<String, JsonNode> currentChild = fields.next();
            String key = currentChild.getKey();
            JsonNode value = currentChild.getValue();
            //if the entry's value has its own children, do a recursion.
            //if the entry has no children, store the key and value.
            //for we don't know the specific type of the entry's value, we
            // should give it to a method which can handle JsonNode
            if (value.isContainerNode()) {
                walkJsonNode(jsonListener, key, value);
            } else {
                jsonListener.enterJsonNode(key, value);
                jsonListener.exitJsonNode(value);
            }
        }
        jsonListener.exitJsonNode(objectNode);
    }

    /**
     * Walks the JSON data tree. This method is called when we don't know
     * the exact type of a json node.
     *
     * @param jsonListener Json listener implemented by the user
     * @param fieldName    the original object node field
     * @param rootNode     the json node which needs to be walk
     */
    private void walkJsonNode(JsonListener jsonListener, String fieldName,
                              JsonNode rootNode) {
        if (rootNode.isObject()) {
            walk(jsonListener, fieldName, (ObjectNode) rootNode);
            return;
        }

        if (rootNode.isArray()) {
            walkArrayNode(jsonListener, fieldName, (ArrayNode) rootNode);
        }
    }

    /**
     * Walks the JSON data tree. This method is called when the user knows the
     * json node type is ArrayNode.
     *
     * @param jsonListener Json listener implemented by the user
     * @param fieldName    the original object node field
     * @param rootNode     the json node which needs to be walk
     */
    private void walkArrayNode(JsonListener jsonListener, String fieldName,
                               ArrayNode rootNode) {
        if (rootNode == null) {
            return;
        }
        //enter the array node.
        jsonListener.enterJsonNode(fieldName, rootNode);
        Iterator<JsonNode> children = rootNode.elements();
        while (children.hasNext()) {
            JsonNode currentChild = children.next();
            if (currentChild.isContainerNode()) {
                walkJsonNode(jsonListener, null, currentChild);
            }
        }
        jsonListener.exitJsonNode(rootNode);
    }
}
