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
import org.onosproject.protocol.restconf.server.utils.parser.api.NormalizedYangNode;
import org.onosproject.yms.ydt.YdtBuilder;
import org.onosproject.yms.ydt.YdtContext;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Stack;

import static com.fasterxml.jackson.databind.node.JsonNodeType.ARRAY;
import static com.google.common.base.Strings.isNullOrEmpty;
import static org.onosproject.protocol.restconf.server.utils.parser.json.ParserUtils.buildNormalizedNode;
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
    private NormalizedYangNode defaultMultiInsNode;
    private Stack<NormalizedYangNode> nameStack = new Stack<>();
    private YdtContext rpcModule;
    private boolean isListArray = false;

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
            if (defaultMultiInsNode != null) {
                ydtBuilder.addChild(defaultMultiInsNode.getName(),
                                    defaultMultiInsNode.getNamespace(),
                                    MULTI_INSTANCE_NODE);
            }
            return;
        }
        NormalizedYangNode normalizedNode = buildNormalizedNode(fieldName);
        JsonNodeType nodeType = node.getNodeType();
        switch (nodeType) {
            case OBJECT:
                processObjectNode(normalizedNode);
                break;

            case ARRAY:
                processArrayNode(normalizedNode, node);
                break;

            //TODO for now, just process the following three node type
            case STRING:
            case NUMBER:
            case BOOLEAN:
                processLeafNode(normalizedNode, node.asText());
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
        if (jsonNode.getNodeType() == ARRAY && nameStack.empty()) {
            return;
        }

        if (jsonNode.getNodeType() == ARRAY && !isListArray) {
            nameStack.pop();
            if (nameStack.empty()) {
                return;
            }
            defaultMultiInsNode = nameStack.get(nameStack.size() - 1);
            return;
        }
        if (isListArray) {
            isListArray = false;
        }

        ydtBuilder.traverseToParent();
    }

    private void processObjectNode(NormalizedYangNode node) {
        ydtBuilder.addChild(node.getName(), node.getNamespace(),
                            SINGLE_INSTANCE_NODE);
    }

    private void processLeafNode(NormalizedYangNode node, String value) {
        ydtBuilder.addLeaf(node.getName(), node.getNamespace(), value);
    }

    private void processArrayNode(NormalizedYangNode normalizedNode,
                                  JsonNode node) {
        ArrayNode arrayNode = (ArrayNode) node;
        if (arrayNode.size() == 0) {
            return;
        }
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
            ydtBuilder.addLeaf(normalizedNode.getName(),
                               normalizedNode.getNamespace(), sets);
            isListArray = true;
        } else {
            defaultMultiInsNode = normalizedNode;
            nameStack.push(defaultMultiInsNode);
        }
    }
}
