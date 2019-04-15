/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.workflow.api;


import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.NumericNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.base.MoreObjects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Class for json data model tree.
 */
public final class JsonDataModelTree implements DataModelTree {

    private static final Logger log = LoggerFactory.getLogger(JsonDataModelTree.class);

    /**
     * Root node of json data model tree.
     */
    private JsonNode root;

    /**
     * Constructor of JsonDataModelTree.
     */
    public JsonDataModelTree() {
        this.root = JsonNodeFactory.instance.objectNode();
    }

    /**
     * Constructor of JsonDataModelTree.
     *
     * @param root root node of json data model tree
     */
    public JsonDataModelTree(JsonNode root) {
        this.root = root;
    }

    @Override
    public DataModelTree subtree(String path) {
        JsonNode node = root.at(path);
        if (Objects.isNull(node) || node.isMissingNode()) {
            return null;
        }
        return new JsonDataModelTree(node);
    }

    @Override
    public void attach(String path, DataModelTree tree) throws WorkflowException {

        if (root == null || root instanceof MissingNode) {
            throw new WorkflowException("Invalid root node");
        }

        JsonPointer ptr = JsonPointer.compile(path);

        if (!(tree instanceof JsonDataModelTree)) {
            throw new WorkflowException("Invalid subTree(" + tree + ")");
        }
        JsonNode attachingNode = ((JsonDataModelTree) tree).root();

        attach(ptr, attachingNode);
    }

    private void attach(JsonPointer ptr, JsonNode attachingNode) throws WorkflowException {

        JsonNode node = root.at(ptr);
        if (!(node instanceof MissingNode)) {
            throw new WorkflowException("Path(" + ptr + ") has already subtree(" + node + ")");
        }

        if (ptr.last().getMatchingIndex() != -1) {

            alloc(ptr.head(), Nodetype.ARRAY);
            JsonNode parentNode = root.at(ptr.head());
            if (!parentNode.isArray()) {
                throw new WorkflowException("Invalid parentNode type(" + parentNode.getNodeType() + " != Array)");
            }
            int index = ptr.last().getMatchingIndex();
            ((ArrayNode) parentNode).insert(index, attachingNode);

        } else if (ptr.last().getMatchingProperty() != null) {

            alloc(ptr.head(), Nodetype.MAP);
            JsonNode parentNode = root.at(ptr.head());
            if (!parentNode.isObject()) {
                throw new WorkflowException("Invalid parentNode type(" + parentNode.getNodeType() + " != Object)");
            }
            String key = ptr.last().getMatchingProperty();
            ((ObjectNode) parentNode).put(key, attachingNode);

        } else {
            throw new WorkflowException("Invalid path(" + ptr + ")");
        }
    }

    @Override
    public void remove(String path) throws WorkflowException {
        JsonPointer ptr = JsonPointer.compile(path);
        remove(ptr);
    }

    private void remove(JsonPointer ptr) throws WorkflowException {

        JsonNode node = root.at(ptr);
        if (node instanceof MissingNode) {
            log.warn("{} does not have valid node", ptr);
            return;
        }

        if (ptr.last().getMatchingIndex() != -1) {

            JsonNode parentNode = root.at(ptr.head());
            if (!parentNode.isArray()) {
                throw new WorkflowException("Invalid parentNode type(" + parentNode.getNodeType() + " != Array)");
            }
            int index = ptr.last().getMatchingIndex();
            ((ArrayNode) parentNode).remove(index);

        } else if (ptr.last().getMatchingProperty() != null) {

            JsonNode parentNode = root.at(ptr.head());
            if (!parentNode.isObject()) {
                throw new WorkflowException("Invalid parentNode type(" + parentNode.getNodeType() + " != Object)");
            }
            String key = ptr.last().getMatchingProperty();
            ((ObjectNode) parentNode).remove(key);

        } else {
            throw new WorkflowException("Invalid path(" + ptr + ")");
        }
    }

    @Override
    public JsonDataModelTree alloc(String path, Nodetype leaftype) throws WorkflowException {
        if (root == null || root instanceof MissingNode) {
            throw new WorkflowException("Invalid root node");
        }

        JsonPointer ptr = JsonPointer.compile(path);
        return alloc(ptr, leaftype);
    }

    /**
     * Allocates json data model tree on json pointer path with specific leaf type.
     *
     * @param ptr      json pointer to allocate
     * @param leaftype type of leaf node
     * @return json data model tree
     * @throws WorkflowException workflow exception
     */
    private JsonDataModelTree alloc(JsonPointer ptr, Nodetype leaftype) throws WorkflowException {
        if (root == null || root instanceof MissingNode) {
            throw new WorkflowException("Invalid root node");
        }

        switch (leaftype) {
            case MAP:
                alloc(root, ptr, JsonNodeType.OBJECT);
                break;
            case ARRAY:
                alloc(root, ptr, JsonNodeType.ARRAY);
                break;
            default:
                throw new WorkflowException("Not supported leaftype(" + leaftype + ")");
        }
        return this;
    }

    /**
     * Gets root json node.
     *
     * @return root json node
     * @throws WorkflowException workflow exception
     */
    public JsonNode root() throws WorkflowException {
        return nodeAt("");
    }

    /**
     * Gets root json node as ObjectNode (MAP type).
     *
     * @return root json node as ObjectNode
     * @throws WorkflowException workflow exception
     */
    public ObjectNode rootObject() throws WorkflowException {
        return objectAt("");
    }

    /**
     * Gets root json node as ArrayNode (Array type).
     *
     * @return root json node as ArrayNode
     * @throws WorkflowException workflow exception
     */
    public ArrayNode rootArray() throws WorkflowException {
        return arrayAt("");
    }

    /**
     * Gets json node on specific path.
     *
     * @param path path of json node
     * @return json node on specific path
     * @throws WorkflowException workflow exception
     */
    public JsonNode nodeAt(String path) throws WorkflowException {
        JsonPointer ptr = JsonPointer.compile(path);
        return nodeAt(ptr);
    }

    /**
     * Gets json node on specific json pointer.
     *
     * @param ptr json pointer
     * @return json node on specific json pointer.
     * @throws WorkflowException workflow exception
     */
    public JsonNode nodeAt(JsonPointer ptr) throws WorkflowException {
        if (root == null || root instanceof MissingNode) {
            throw new WorkflowException("Invalid root node");
        }
        JsonNode node = root.at(ptr);
        return node;
    }

    /**
     * Gets json node on specific path as ObjectNode.
     *
     * @param path path of json node
     * @return ObjectNode type json node on specific path
     * @throws WorkflowException workflow exception
     */
    public ObjectNode objectAt(String path) throws WorkflowException {
        JsonPointer ptr = JsonPointer.compile(path);
        return objectAt(ptr);
    }

    /**
     * Gets json node on specific json pointer as ObjectNode.
     *
     * @param ptr json pointer
     * @return ObjectNode type json node on specific json pointer.
     * @throws WorkflowException workflow exception
     */
    public ObjectNode objectAt(JsonPointer ptr) throws WorkflowException {
        if (root == null || root instanceof MissingNode) {
            throw new WorkflowException("Invalid root node");
        }
        JsonNode node = root.at(ptr);
        if (node instanceof MissingNode) {
            return null;
        }
        if (!(node instanceof ObjectNode)) {
            throw new WorkflowException("Invalid node(" + node + ") at " + ptr);
        }
        return (ObjectNode) node;
    }

    /**
     * Gets json node on specific path as ArrayNode.
     *
     * @param path path of json node
     * @return ArrayNode type json node on specific path
     * @throws WorkflowException workflow exception
     */
    public ArrayNode arrayAt(String path) throws WorkflowException {
        JsonPointer ptr = JsonPointer.compile(path);
        return arrayAt(ptr);
    }

    /**
     * Gets json node on specific json pointer as ArrayNode.
     *
     * @param ptr json pointer
     * @return ArrayNode type json node on specific json pointer.
     * @throws WorkflowException workflow exception
     */
    public ArrayNode arrayAt(JsonPointer ptr) throws WorkflowException {
        if (root == null || root instanceof MissingNode) {
            throw new WorkflowException("Invalid root node");
        }
        JsonNode node = root.at(ptr);
        if (node instanceof MissingNode) {
            return null;
        }
        if (!(node instanceof ArrayNode)) {
            throw new WorkflowException("Invalid node(" + node + ") at " + ptr);
        }
        return (ArrayNode) node;
    }

    /**
     * Gets text node on specific path.
     *
     * @param path path of json node
     * @return text on specific path
     * @throws WorkflowException workflow exception
     */
    public String textAt(String path) throws WorkflowException {
        JsonPointer ptr = JsonPointer.compile(path);
        return textAt(ptr);
    }

    /**
     * Gets text on specific json pointer.
     *
     * @param ptr json pointer
     * @return text on specific json pointer
     * @throws WorkflowException workflow exception
     */
    public String textAt(JsonPointer ptr) throws WorkflowException {
        if (root == null || root instanceof MissingNode) {
            throw new WorkflowException("Invalid root node");
        }
        JsonNode node = root.at(ptr);
        if (node instanceof MissingNode) {
            return null;
        }
        if (!(node instanceof TextNode)) {
            throw new WorkflowException("Invalid node(" + node + ") at " + ptr);
        }
        return ((TextNode) node).asText();
    }

    /**
     * Gets integer node on specific path.
     *
     * @param path path of json node
     * @return integer on specific path
     * @throws WorkflowException workflow exception
     */
    public Integer intAt(String path) throws WorkflowException {
        JsonPointer ptr = JsonPointer.compile(path);
        return intAt(ptr);
    }

    /**
     * Gets integer on specific json pointer.
     *
     * @param ptr json pointer
     * @return integer on specific json pointer
     * @throws WorkflowException workflow exception
     */
    public Integer intAt(JsonPointer ptr) throws WorkflowException {
        if (root == null || root instanceof MissingNode) {
            throw new WorkflowException("Invalid root node");
        }
        JsonNode node = root.at(ptr);
        if (node instanceof MissingNode) {
            return null;
        }
        if (!(node instanceof NumericNode)) {
            throw new WorkflowException("Invalid node(" + node + ") at " + ptr);
        }
        return ((NumericNode) node).asInt();
    }

    /**
     * Gets boolean on specific path.
     *
     * @param path path of json node
     * @return boolean on specific path
     * @throws WorkflowException workflow exception
     */
    public Boolean booleanAt(String path) throws WorkflowException {
        JsonPointer ptr = JsonPointer.compile(path);
        return booleanAt(ptr);
    }

    /**
     * Gets boolean on specific json pointer.
     *
     * @param ptr json pointer
     * @return boolean on specific json pointer
     * @throws WorkflowException workflow exception
     */
    public Boolean booleanAt(JsonPointer ptr) throws WorkflowException {
        if (root == null || root instanceof MissingNode) {
            throw new WorkflowException("Invalid root node");
        }
        JsonNode node = root.at(ptr);
        if (node instanceof MissingNode) {
            return null;
        }
        if (!(node instanceof BooleanNode)) {
            throw new WorkflowException("Invalid node(" + node + ") at " + ptr);
        }
        return ((BooleanNode) node).asBoolean();
    }

    /**
     * Sets text on specific json path.
     *
     * @param path json path
     * @param text text to set
     * @throws WorkflowException workflow exception
     */
    public void setAt(String path, String text) throws WorkflowException {
        JsonPointer ptr = JsonPointer.compile(path);
        setAt(ptr, text);
    }

    /**
     * Sets text on the specific json pointer.
     *
     * @param ptr  json pointer
     * @param text text to set
     * @throws WorkflowException workflow exception
     */
    public void setAt(JsonPointer ptr, String text) throws WorkflowException {
        TextNode textNode = TextNode.valueOf(text);

        attach(ptr, textNode);
    }

    /**
     * Sets boolean on specific json path.
     *
     * @param path   json path
     * @param isTrue boolean to set
     * @throws WorkflowException workflow exception
     */
    public void setAt(String path, Boolean isTrue) throws WorkflowException {
        JsonPointer ptr = JsonPointer.compile(path);
        setAt(ptr, isTrue);
    }

    /**
     * Sets text on the specific json pointer.
     *
     * @param ptr      json pointer
     * @param jsonNode jsonNode to set
     * @throws WorkflowException workflow exception
     */
    public void setAt(JsonPointer ptr, JsonNode jsonNode) throws WorkflowException {
        JsonNode node = jsonNode;
        attach(ptr, node);
    }

    /**
     * Sets boolean on specific json path.
     *
     * @param path     json path
     * @param jsonNode jsonNode to set
     * @throws WorkflowException workflow exception
     */
    public void setAt(String path, JsonNode jsonNode) throws WorkflowException {
        JsonPointer ptr = JsonPointer.compile(path);
        setAt(ptr, jsonNode);
    }


    /**
     * Sets text on the specific json pointer.
     *
     * @param ptr       json pointer
     * @param arrayNode arrayNode to set
     * @throws WorkflowException workflow exception
     */
    public void setAt(JsonPointer ptr, ArrayNode arrayNode) throws WorkflowException {
        ArrayNode node = arrayNode;
        attach(ptr, node);
    }

    /**
     * Sets boolean on specific json path.
     *
     * @param path      json path
     * @param arrayNode arrayNode to set
     * @throws WorkflowException workflow exception
     */
    public void setAt(String path, ArrayNode arrayNode) throws WorkflowException {
        JsonPointer ptr = JsonPointer.compile(path);
        setAt(ptr, arrayNode);
    }

    /**
     * Sets text on the specific json pointer.
     *
     * @param ptr        json pointer
     * @param objectNode objectNode to set
     * @throws WorkflowException workflow exception
     */
    public void setAt(JsonPointer ptr, ObjectNode objectNode) throws WorkflowException {
        ObjectNode node = objectNode;
        attach(ptr, node);
    }

    /**
     * Sets boolean on specific json path.
     *
     * @param path       json path
     * @param objectNode objectNode to set
     * @throws WorkflowException workflow exception
     */
    public void setAt(String path, ObjectNode objectNode) throws WorkflowException {
        JsonPointer ptr = JsonPointer.compile(path);
        setAt(ptr, objectNode);
    }


    /**
     * Sets boolean on the specific json pointer.
     *
     * @param ptr    json pointer
     * @param isTrue boolean to set
     * @throws WorkflowException workflow exception
     */
    public void setAt(JsonPointer ptr, Boolean isTrue) throws WorkflowException {
        BooleanNode booleanNode = BooleanNode.valueOf(isTrue);
        attach(ptr, booleanNode);
    }

    /**
     * Sets integer on specific json path.
     *
     * @param path   json path
     * @param number number to set
     * @throws WorkflowException workflow exception
     */
    public void setAt(String path, Integer number) throws WorkflowException {
        JsonPointer ptr = JsonPointer.compile(path);
        setAt(ptr, number);
    }

    /**
     * Sets integer on the specific json pointer.
     *
     * @param ptr    json pointer
     * @param number number to set
     * @throws WorkflowException workflow exception
     */
    public void setAt(JsonPointer ptr, Integer number) throws WorkflowException {
        IntNode intNode = IntNode.valueOf(number);
        attach(ptr, intNode);
    }

    /**
     * Allocates json data model tree on json pointer path with specific leaf type.
     *
     * @param node     current json node in the json tree path
     * @param ptr      json pointer
     * @param leaftype leaf type to be allocated
     * @return allocated json node
     * @throws WorkflowException workflow exception
     */
    private JsonNode alloc(JsonNode node, JsonPointer ptr, JsonNodeType leaftype) throws WorkflowException {

        if (ptr.matches()) {
            if (node == null || node instanceof MissingNode) {
                node = createEmpty(leaftype);
            } else {
                //TODO: checking existing node type is matched with leaftype
                if (!Objects.equals(node.getNodeType(), leaftype)) {
                    throw new WorkflowException("Requesting leaftype(" + leaftype + ") is not matched with "
                            + "existing nodetype(" + node.getNodeType() + ") for " + ptr);
                }
            }
            return node;
        }

        if (ptr.getMatchingIndex() != -1) {
            if (node == null || node instanceof MissingNode) {
                node = createEmpty(JsonNodeType.ARRAY);
            }
            JsonNode child = alloc(node.get(ptr.getMatchingIndex()), ptr.tail(), leaftype);
            if (!node.has(ptr.getMatchingIndex())) {
                ((ArrayNode) node).insert(ptr.getMatchingIndex(), child);
            }
        } else if (ptr.getMatchingProperty() != null) {
            if (node == null || node instanceof MissingNode) {
                node = createEmpty(JsonNodeType.OBJECT);
            }
            JsonNode child = alloc(node.get(ptr.getMatchingProperty()), ptr.tail(), leaftype);
            if (!node.has(ptr.getMatchingProperty())) {
                ((ObjectNode) node).put(ptr.getMatchingProperty(), child);
            }
        }
        return node;
    }

    /**
     * Creating empty json node.
     *
     * @param type json node type to create
     * @return created json node
     * @throws WorkflowException workflow exception
     */
    private JsonNode createEmpty(JsonNodeType type) throws WorkflowException {
        if (type == JsonNodeType.OBJECT) {
            return JsonNodeFactory.instance.objectNode();
        } else if (type == JsonNodeType.ARRAY) {
            return JsonNodeFactory.instance.arrayNode();
        } else if (type == JsonNodeType.STRING) {
            return JsonNodeFactory.instance.textNode("");
        } else {
            throw new WorkflowException("Not supported JsonNodetype(" + type + ")");
        }
    }

    /**
     * Gets the pretty json formatted string of this json data model tree.
     *
     * @return pretty json formatted string of this json data model tree
     */
    public String formattedRootString() {
        String str = "";
        try {
            str = (new ObjectMapper()).writerWithDefaultPrettyPrinter().writeValueAsString(root);
        } catch (JsonProcessingException e) {
            log.error("Exception: ", e);
        }
        return str;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("json", root)
                .toString();
    }
}

