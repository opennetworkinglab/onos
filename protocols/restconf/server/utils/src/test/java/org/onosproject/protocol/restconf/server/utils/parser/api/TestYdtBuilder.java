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

import org.onosproject.yms.ydt.YdtBuilder;
import org.onosproject.yms.ydt.YdtContext;
import org.onosproject.yms.ydt.YdtContextOperationType;
import org.onosproject.yms.ydt.YdtType;
import org.onosproject.yms.ydt.YmsOperationType;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.onosproject.yms.ydt.YdtContextOperationType.NONE;
import static org.onosproject.yms.ydt.YdtType.MULTI_INSTANCE_LEAF_VALUE_NODE;
import static org.onosproject.yms.ydt.YdtType.SINGLE_INSTANCE_LEAF_VALUE_NODE;
import static org.onosproject.yms.ydt.YdtType.SINGLE_INSTANCE_NODE;

/**
 * A test class which represents YANG request work bench which contains all
 * parameters for request handling and methods to build and obtain YANG data
 * tree which is data (sub)instance representation, abstract of protocol.
 */
public class TestYdtBuilder implements YdtBuilder {

    private TestYdtNode curNode;

    private TestYdtNode rootNode;

    private final YmsOperationType ymsOperationType;

    private YdtContextOperationType ydtDefaultOpType;

    /**
     * Creates an instance of YANG request work bench which is use to initialize
     * logical rootNode and and schema registry.
     *
     * @param name      name of logical container of a protocol
     *                  which is a holder of the complete tree
     * @param namespace namespace of logical container
     * @param opType    type of operation done by using YANG
     *                  interface
     */
    public TestYdtBuilder(String name, String namespace,
                          YmsOperationType opType) {
        TestYangSchemaId nodeId = new TestYangSchemaId();
        nodeId.setName(name);
        nodeId.setNameSpace(namespace);
        setRootNode(new TestYdtNode(nodeId, SINGLE_INSTANCE_NODE));
        ymsOperationType = opType;
    }

    private void setRootNode(TestYdtNode node) {
        rootNode = node;
        curNode = node;
    }

    @Override
    public YdtContext getRootNode() {
        return rootNode;
    }

    @Override
    public YmsOperationType getYmsOperationType() {
        return ymsOperationType;
    }

    @Override
    public void setRootTagAttributeMap(Map<String, String> attributeTag) {

    }

    @Override
    public Map<String, String> getRootTagAttributeMap() {
        return null;
    }

    @Override
    public void addChild(String name, String namespace) {
        addChild(name, namespace, SINGLE_INSTANCE_NODE, NONE);
    }

    @Override
    public void addChild(String name, String namespace, YdtType ydtType) {
        addChild(name, namespace, ydtType, NONE);
    }

    @Override
    public void addChild(String name, String namespace,
                         YdtContextOperationType opType) {
        addChild(name, namespace, SINGLE_INSTANCE_NODE, opType);
    }

    @Override
    public void addChild(String name, String namespace, YdtType ydtType,
                         YdtContextOperationType opType) {
        TestYangSchemaId id = new TestYangSchemaId();
        id.setName(name);
        String ns = namespace != null ? namespace :
                curNode.getYdtNodeIdentifier().getNameSpace();
        id.setNameSpace(ns);
        TestYdtNode childNode = new TestYdtNode(id, ydtType);

        YdtContextOperationType type = opType == null ? NONE : opType;
        childNode.setYdtContextOperationType(type);

        curNode.addChild(childNode);

        // Updating the curNode.
        curNode = childNode;
    }

    @Override
    public void addLeaf(String name, String namespace, String value) {
        addLeaf(name, namespace, value, null, SINGLE_INSTANCE_LEAF_VALUE_NODE);
    }

    @Override
    public void addLeaf(String name, String namespace, Set<String> valueSet) {
        addLeaf(name, namespace, null, valueSet, MULTI_INSTANCE_LEAF_VALUE_NODE);
    }

    @Override
    public void addMultiInstanceChild(String name, String namespace,
                                      List<String> valueList,
                                      YdtContextOperationType operationType) {
        addChild(name, namespace, MULTI_INSTANCE_LEAF_VALUE_NODE,
                 operationType);
        if (curNode.getYdtType() == MULTI_INSTANCE_LEAF_VALUE_NODE) {
            for (String value : valueList) {
                curNode.addValue(value);
            }
        }
    }

    private void addLeaf(String name, String namespace, String value,
                         Set<String> valueSet, YdtType ydtType) {
        addChild(name, namespace, ydtType, NONE);

        if (value != null) {
            curNode.addValue(value);
        } else if (valueSet != null) {
            curNode.addValueSet(valueSet);
        }
    }

    @Override
    public void traverseToParent() {
        curNode = curNode.getParent();
    }

    @Override
    public YdtContext getCurNode() {
        return curNode;
    }

    @Override
    public void setDefaultEditOperationType(YdtContextOperationType opType) {
        ydtDefaultOpType = opType;
    }

    /**
     * Returns the default context operation type of a YDT builder.
     *
     * @return default context operation type
     */
    public YdtContextOperationType getYdtDefaultOpType() {
        return ydtDefaultOpType;
    }
}
