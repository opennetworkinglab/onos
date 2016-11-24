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

import org.onosproject.yms.ydt.YdtContext;
import org.onosproject.yms.ydt.YdtContextOperationType;
import org.onosproject.yms.ydt.YdtExtendedInfoType;
import org.onosproject.yms.ydt.YdtType;

import java.util.HashSet;
import java.util.Set;

/**
 * A test class which represents a general instance node in YANG data tree.
 */
public class TestYdtNode implements YdtContext {

    private TestYdtNode parent;
    private TestYdtNode child;
    private TestYdtNode nextSibling;
    private TestYdtNode previousSibling;
    private TestYdtNode lastChild;
    private YdtType ydtType;
    private String value;
    private final Set<String> valueSet = new HashSet<>();
    private TestYangSchemaId id;
    private YdtContextOperationType ydtContextOperationType;

    /**
     * Creates a general YANG instance node object.
     *
     * @param id      node identifier of YDT multi instance node .
     * @param ydtType type of YDT node to be added
     */
    public TestYdtNode(TestYangSchemaId id, YdtType ydtType) {
        this.id = id;
        this.ydtType = ydtType;
    }

    @Override
    public String getName() {
        return id.getName();
    }

    @Override
    public String getNamespace() {
        return id.getNameSpace();
    }

    @Override
    public String getModuleNameAsNameSpace() {
        return null;
    }

    @Override
    public <T> T getYdtContextExtendedInfo() {
        return null;
    }

    @Override
    public YdtExtendedInfoType getYdtExtendedInfoType() {
        return null;
    }

    @Override
    public YdtType getYdtType() {
        return ydtType;
    }

    @Override
    public TestYdtNode getParent() {
        return parent;
    }

    @Override
    public TestYdtNode getFirstChild() {
        return child;
    }

    @Override
    public TestYdtNode getLastChild() {
        return lastChild;
    }

    @Override
    public TestYdtNode getNextSibling() {
        return nextSibling;
    }

    @Override
    public YdtContext getPreviousSibling() {
        return previousSibling;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public Set<String> getValueSet() {
        return valueSet;
    }

    /**
     * Sets the parent of node.
     *
     * @param parent node
     */
    public void setParent(TestYdtNode parent) {
        this.parent = parent;
    }

    /**
     * Sets the first instance of a child node.
     *
     * @param child is only child to be set
     */
    public void setChild(TestYdtNode child) {
        this.child = child;
    }

    /**
     * Sets the last instance of a child node.
     *
     * @param child is last child to be set
     */
    public void setLastChild(TestYdtNode child) {
        lastChild = child;
    }

    /**
     * Sets the next sibling of node.
     *
     * @param sibling YANG node
     */
    public void setNextSibling(TestYdtNode sibling) {
        nextSibling = sibling;
    }

    /**
     * Sets the previous sibling.
     *
     * @param previousSibling points to predecessor sibling
     */
    public void setPreviousSibling(TestYdtNode previousSibling) {
        this.previousSibling = previousSibling;
    }

    /**
     * Returns object node identifier.
     *
     * @return node identifier
     */
    public TestYangSchemaId getYdtNodeIdentifier() {
        return id;
    }

    /**
     * Adds a child node.
     * The children sibling list will be sorted based on node
     * type. This will add single child or sub-tree based on isAtomic flag.
     *
     * @param newChild refers to a new child to be added
     */
    public void addChild(YdtContext newChild) {
        TestYdtNode node = (TestYdtNode) newChild;

        if (node.getParent() == null) {
            node.setParent(this);
        }

        // If new node needs to be added as first child.
        if (getFirstChild() == null) {
            setChild(node);
            setLastChild(node);
            return;
        }

        // If new node needs to be added as last child.
        TestYdtNode curNode = getLastChild();
        curNode.setNextSibling(node);
        node.setPreviousSibling(curNode);
        setLastChild(node);
    }

    /**
     * Adds the given value to the non single instance leaf node.
     *
     * @param value value in a single instance node
     */
    public void addValue(String value) {
        this.value = value;
    }

    /**
     * Adds the given valueSet to the non multi instance leaf node.
     *
     * @param values value set in a multi instance leaf node
     */
    public void addValueSet(Set<String> values) {
        valueSet.addAll(values);
    }


    /**
     * Sets the context operation type for the YDT node.
     *
     * @param opType context operation type
     */
    public void setYdtContextOperationType(YdtContextOperationType opType) {
        this.ydtContextOperationType = opType;
    }

    /**
     * Returns the context operation type for the YDT node.
     *
     * @return context operation type
     */
    public YdtContextOperationType getYdtContextOperationType() {
        return ydtContextOperationType;
    }
}
