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

import com.fasterxml.jackson.databind.node.JsonNodeType;
import org.onosproject.protocol.restconf.server.utils.exceptions.YdtParseException;
import org.onosproject.protocol.restconf.server.utils.parser.api.JsonBuilder;
import org.onosproject.yms.ydt.YdtContext;
import org.onosproject.yms.ydt.YdtListener;

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * Represents implementation of codec YDT listener.
 */
public class YdtToJsonListener implements YdtListener {

    private static final String EMPTY = "";
    private JsonBuilder jsonBuilder;
    //the root name of the json
    //the input YdtContext is usually a total tree of a YANG resource
    //this property is used to mark the start of the request node.
    private String rootName;
    //the parse state
    private boolean isBegin;

    /**
     * Creates a listener for the process of a Ydt, the listener will try to
     * transfer the Ydt to a JSON Object.
     *
     * @param rootName    the name of a specific YdtContext begin to  process
     * @param jsonBuilder the JSON builder to build a JSON object
     */
    public YdtToJsonListener(String rootName, JsonBuilder jsonBuilder) {
        this.jsonBuilder = jsonBuilder;
        this.rootName = rootName;
        this.isBegin = isNullOrEmpty(rootName);
    }

    @Override
    public void enterYdtNode(YdtContext ydtContext) {
        String name = ydtContext.getName();

        if (!isBegin && name.equals(rootName)) {
            isBegin = true;
        }
        if (!isBegin) {
            return;
        }

        switch (ydtContext.getYdtType()) {

            case SINGLE_INSTANCE_NODE:
                jsonBuilder.addNodeTopHalf(name, JsonNodeType.OBJECT);
                break;
            case MULTI_INSTANCE_NODE:
                YdtContext preNode = ydtContext.getPreviousSibling();
                if (preNode == null || !preNode.getName().equals(name)) {
                    jsonBuilder.addNodeTopHalf(name, JsonNodeType.ARRAY);
                }
                jsonBuilder.addNodeTopHalf(EMPTY, JsonNodeType.OBJECT);
                break;
            case SINGLE_INSTANCE_LEAF_VALUE_NODE:
                jsonBuilder.addNodeWithValueTopHalf(name, ydtContext.getValue());
                break;
            case MULTI_INSTANCE_LEAF_VALUE_NODE:
                jsonBuilder.addNodeWithSetTopHalf(name, ydtContext.getValueSet());
                break;
            default:
                throw new YdtParseException("unknown Ydt type " +
                                                    ydtContext.getYdtType());
        }

    }

    @Override
    public void exitYdtNode(YdtContext ydtContext) {

        if (!isBegin) {
            return;
        }

        String curName = ydtContext.getName();
        YdtContext nextNode = ydtContext.getNextSibling();
        switch (ydtContext.getYdtType()) {

            case SINGLE_INSTANCE_NODE:
                jsonBuilder.addNodeBottomHalf(JsonNodeType.OBJECT);
                break;
            case MULTI_INSTANCE_NODE:
                if (nextNode == null || !nextNode.getName().equals(curName)) {
                    jsonBuilder.addNodeBottomHalf(JsonNodeType.OBJECT);
                    jsonBuilder.addNodeBottomHalf(JsonNodeType.ARRAY);
                } else {
                    jsonBuilder.addNodeBottomHalf(JsonNodeType.OBJECT);
                }
                break;
            case SINGLE_INSTANCE_LEAF_VALUE_NODE:
                jsonBuilder.addNodeBottomHalf(JsonNodeType.STRING);
                break;
            case MULTI_INSTANCE_LEAF_VALUE_NODE:
                jsonBuilder.addNodeBottomHalf(JsonNodeType.ARRAY);
                break;
            default:
                throw new YdtParseException("Unknown Ydt type " +
                                                    ydtContext.getYdtType());
        }
        if (curName.equals(rootName) &&
                (nextNode == null || !nextNode.getName().equals(rootName))) {
            isBegin = false;
        }
    }
}