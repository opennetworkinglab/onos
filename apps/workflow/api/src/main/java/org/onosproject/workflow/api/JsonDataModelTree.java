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
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.MoreObjects;

import java.util.Objects;

/**
 * Class for json data model tree.
 */
public final class JsonDataModelTree implements DataModelTree {

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
        JsonNode node = root.at(ptr);
        if (!(node instanceof MissingNode)) {
            throw new WorkflowException("Path(" + path + ") has already subtree(" + node + ")");
        }

        if (!(tree instanceof JsonDataModelTree)) {
            throw new WorkflowException("Invalid subTree(" + tree + ")");
        }
        JsonNode attachingNode = ((JsonDataModelTree) tree).root();

        alloc(ptr.head(), Nodetype.MAP);
        JsonNode parentNode = root.at(ptr.head());

        if (!parentNode.isObject()) {
            throw new WorkflowException("Invalid parentNode type(" + parentNode.getNodeType() + ")");
        }

        String key = ptr.last().getMatchingProperty();
        ((ObjectNode) parentNode).put(key, attachingNode);
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
     * @param ptr json pointer to allocate
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
     * @return root json node
     * @throws WorkflowException workflow exception
     */
    public JsonNode root() throws WorkflowException {
        return nodeAt("");
    }

    /**
     * Gets root json node as ObjectNode (MAP type).
     * @return root json node as ObjectNode
     * @throws WorkflowException workflow exception
     */
    public ObjectNode rootObject() throws WorkflowException {
        return objectAt("");
    }

    /**
     * Gets root json node as ArrayNode (Array type).
     * @return root json node as ArrayNode
     * @throws WorkflowException workflow exception
     */
    public ArrayNode rootArray() throws WorkflowException {
        return arrayAt("");
    }

    /**
     * Gets json node on specific path.
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
     * @param ptr json pointer
     * @return ObjectNode type json node on specific json pointer.
     * @throws WorkflowException workflow exception
     */
    public ObjectNode objectAt(JsonPointer ptr) throws WorkflowException {
        if (root == null || root instanceof MissingNode) {
            throw new WorkflowException("Invalid root node");
        }
        JsonNode node = root.at(ptr);
        if (!(node instanceof ObjectNode)) {
            throw new WorkflowException("Invalid node(" + node + ") at " + ptr);
        }
        return (ObjectNode) node;
    }

    /**
     * Gets json node on specific path as ArrayNode.
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
     * @param ptr json pointer
     * @return ArrayNode type json node on specific json pointer.
     * @throws WorkflowException workflow exception
     */
    public ArrayNode arrayAt(JsonPointer ptr) throws WorkflowException {
        if (root == null || root instanceof MissingNode) {
            throw new WorkflowException("Invalid root node");
        }
        JsonNode node = root.at(ptr);
        if (!(node instanceof ArrayNode)) {
            throw new WorkflowException("Invalid node(" + node + ") at " + ptr);
        }
        return (ArrayNode) node;
    }

    /**
     * Allocates json data model tree on json pointer path with specific leaf type.
     * @param node current json node in the json tree path
     * @param ptr json pointer
     * @param leaftype leaf type to be allocated
     * @return allocated json node
     * @throws WorkflowException workflow exception
     */
    private JsonNode alloc(JsonNode node, JsonPointer ptr, JsonNodeType leaftype) throws WorkflowException {

        if (ptr.matches()) {
            if (node instanceof MissingNode) {
                node = createEmpty(leaftype);
            } else {
                //TODO: checking existing node type is matched with leaftype
                if (Objects.equals(node.getNodeType(), leaftype)) {
                    throw new WorkflowException("Requesting leaftype(" + leaftype + ") is not matched with "
                            + "existing nodetype(" + node.getNodeType() + ") for " + ptr);
                }
            }
            return node;
        }

        if (ptr.getMatchingIndex() != -1) {
            if (node instanceof MissingNode) {
                node = createEmpty(JsonNodeType.ARRAY);
            }
            JsonNode child = alloc(node.get(ptr.getMatchingIndex()), ptr.tail(), leaftype);
            if (!node.has(ptr.getMatchingIndex())) {
                ((ArrayNode) node).insert(ptr.getMatchingIndex(), child);
            }
        } else if (ptr.getMatchingProperty() != null) {
            if (node instanceof MissingNode) {
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
     * @return pretty json formatted string of this json data model tree
     */
    public String formattedRootString() {
        String str = "";
        try {
            str = (new ObjectMapper()).writerWithDefaultPrettyPrinter().writeValueAsString(root);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return str;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("json", formattedRootString())
                .toString();
    }
}

