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
import com.fasterxml.jackson.databind.node.JsonNodeType;
import org.onosproject.protocol.restconf.server.utils.exceptions.JsonParseException;
import org.onosproject.protocol.restconf.server.utils.parser.api.JsonListener;
import org.onosproject.yms.ydt.YdtBuilder;
import org.onosproject.yms.ydt.YdtContext;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import static com.fasterxml.jackson.databind.node.JsonNodeType.ARRAY;
import static com.google.common.base.Strings.isNullOrEmpty;
import static org.onosproject.yms.ydt.YdtType.MULTI_INSTANCE_NODE;
import static org.onosproject.yms.ydt.YdtType.SINGLE_INSTANCE_NODE;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Represents default implementation of codec JSON listener.
 */
public class JsonToYdtListener implements JsonListener {

    private static final String INPUT_FIELD_NAME = "input";
    private static final String COLON = ":";
    private static final int INPUT_FIELD_LENGTH = 2;
    private static final String E_UNSUP_TYPE = "Unsupported node type %s " +
            "field name is %s fieldName";

    private Logger log = getLogger(getClass());

    private YdtBuilder ydtBuilder;
    private String defaultMultiInsNodeName;
    private YdtContext rpcModule;

    /**
     * Creates a listener for the process of a Json Object, the listener will
     * try to transfer the JSON object to a Ydt builder.
     *
     * @param ydtBuilder the YDT builder to build a YDT object
     */
    public JsonToYdtListener(YdtBuilder ydtBuilder) {
        this.ydtBuilder = ydtBuilder;
    }

    @Override
    public void enterJsonNode(String fieldName, JsonNode node) {
        if (isNullOrEmpty(fieldName)) {
            if (!isNullOrEmpty(defaultMultiInsNodeName)) {
                ydtBuilder.addChild(defaultMultiInsNodeName, null,
                                    MULTI_INSTANCE_NODE);
            }
            return;
        }
        JsonNodeType nodeType = node.getNodeType();
        switch (nodeType) {
            case OBJECT:
                processObjectNode(fieldName);
                break;

            case ARRAY:
                processArrayNode(fieldName, node);
                break;

            //TODO for now, just process the following three node type
            case STRING:
            case NUMBER:
            case BOOLEAN:
                ydtBuilder.addLeaf(fieldName, null, node.asText());
                break;

            case BINARY:
            case MISSING:
            case NULL:
            case POJO:
                log.debug("Unimplemented node type {}", nodeType);
                break;

            default:
                throw new JsonParseException(String.format(E_UNSUP_TYPE,
                                                           nodeType, fieldName));
        }
    }

    @Override
    public void exitJsonNode(JsonNode jsonNode) {
        if (jsonNode.getNodeType() == ARRAY) {
            return;
        }
        ydtBuilder.traverseToParent();
        YdtContext curNode = ydtBuilder.getCurNode();
        //if the current node is the RPC node, then should go to the father
        //for we have enter the RPC node and Input node at the same time
        //and the input is the only child of RPC node.

        if (curNode == null) {
            return;
        }
        String name = curNode.getName();
        if (rpcModule != null && name.equals(rpcModule.getName())) {
            ydtBuilder.traverseToParent();
        }
    }

    private void processObjectNode(String fieldName) {
        String[] segments = fieldName.split(COLON);
        Boolean isLastInput = segments.length == INPUT_FIELD_LENGTH &&
                segments[INPUT_FIELD_LENGTH - 1].equals(INPUT_FIELD_NAME);
        int first = 0;
        int second = 1;
        if (isLastInput) {
            ydtBuilder.addChild(segments[first], null, SINGLE_INSTANCE_NODE);
            rpcModule = ydtBuilder.getCurNode();
            ydtBuilder.addChild(segments[second], null, SINGLE_INSTANCE_NODE);
        } else {
            ydtBuilder.addChild(fieldName, null, SINGLE_INSTANCE_NODE);
        }
    }

    private void processArrayNode(String fieldName, JsonNode node) {
        ArrayNode arrayNode = (ArrayNode) node;
        Set<String> sets = new HashSet<>();
        Iterator<JsonNode> elements = arrayNode.elements();
        boolean isLeafList = true;
        while (elements.hasNext()) {
            JsonNode element = elements.next();
            JsonNodeType eleType = element.getNodeType();

            if (eleType == JsonNodeType.STRING ||
                    eleType == JsonNodeType.NUMBER ||
                    eleType == JsonNodeType.BOOLEAN) {
                sets.add(element.asText());
            } else {
                isLeafList = false;
            }
        }
        if (isLeafList) {
            //leaf-list
            ydtBuilder.addLeaf(fieldName, null, sets);
        } else {
            this.defaultMultiInsNodeName = fieldName;
        }
    }
}
