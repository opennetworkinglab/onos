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
package org.onosproject.protocol.restconf.server.utils.parser.api;

import com.fasterxml.jackson.databind.node.JsonNodeType;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.Set;

/**
 * Abstraction of an entity which provides interfaces to build and obtain JSON
 * data tree.
 */
public interface JsonBuilder {

    /**
     * Adds a to half (a left brace/bracket and the field name) of a JSON
     * object/array to the JSON tree. This method is used by protocols which
     * knows the nature (object/array) of node.
     *
     * @param fieldName name of child to be added
     * @param nodeType  the type of the child
     */
    void addNodeTopHalf(String fieldName, JsonNodeType nodeType);

    /**
     * Adds a child with value and a comma to the JSON tree.
     * Protocols unaware of nature of node (single/multiple) will use it to add
     * both single instance and multi instance node. Protocols aware of nature
     * of node will use it for single instance value node addition.
     *
     * @param fieldName name of child to be added
     * @param value     the type of the child
     */
    void addNodeWithValueTopHalf(String fieldName, String value);

    /**
     * Adds a child with list of values to JSON data tree. This method is
     * used by protocols which knows the nature (object/array) of node for
     * ArrayNode addition.
     *
     * @param fieldName name of child to be added
     * @param sets      the value list of the child
     */
    void addNodeWithSetTopHalf(String fieldName, Set<String> sets);

    /**
     * Adds the bottom half(a right brace/bracket) of  a JSON object/array to
     * the JSON tree. for the text, a comma should be taken out.
     *
     * @param nodeType the type of the child
     */
    void addNodeBottomHalf(JsonNodeType nodeType);

    /**
     * Returns the JSON tree after build operations in the format of string.
     *
     * @return the final string JSON tree after build operations
     */
    String getTreeString();

    /**
     * Returns the JSON tree after build operations in the format of ObjectNode.
     *
     * @return the final ObjectNode JSON tree after build operations
     */
    ObjectNode getTreeNode();
}
