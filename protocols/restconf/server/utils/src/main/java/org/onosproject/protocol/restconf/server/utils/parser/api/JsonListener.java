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


import com.fasterxml.jackson.databind.JsonNode;

/**
 * Abstraction of an entity which provide call back methods which are called
 * by JSON walker while walking the JSON data tree. This interface needs to be
 * implemented by protocol implementing listener's based call backs while JSON
 * walk.
 */
public interface JsonListener {

    /**
     * Callback invoked during a node entry.
     * All the related information about the node can be obtain from the JSON
     * object.
     *
     * @param fieldName the field name of the JSON Node value
     * @param node      the JsonNode which is walked through
     */
    void enterJsonNode(String fieldName, JsonNode node);

    /**
     * Callback invoked during a node exit.
     * All the related information about the node can be obtain from the JSON
     * node.
     *
     * @param jsonNode JSON node which has been walked through
     */
    void exitJsonNode(JsonNode jsonNode);

}
