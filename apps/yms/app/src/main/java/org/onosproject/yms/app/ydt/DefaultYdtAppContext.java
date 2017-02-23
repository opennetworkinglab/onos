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

package org.onosproject.yms.app.ydt;

import org.onosproject.yangutils.datamodel.YangSchemaNode;
import org.onosproject.yms.ydt.YdtContext;
import org.onosproject.yms.ydt.YdtContextOperationType;

import java.util.List;

import static org.onosproject.yms.app.ydt.YdtAppNodeOperationType.BOTH;
import static org.onosproject.yms.app.ydt.YdtUtils.getAppOpTypeFromYdtOpType;

/**
 * Abstraction of an entity which represents YDT application context
 * information. This context information will be used by protocol to obtain
 * the information associated with YDT application tree node.
 */
public final class DefaultYdtAppContext<T extends AppData>
        implements YdtAppContext {

    /*
     * Parent reference.
     */
    private YdtAppContext parent;

    /*
     * First child reference.
     */
    private YdtAppContext child;

    /*
     * Next sibling reference.
     */
    private YdtAppContext nextSibling;

    /*
     * Previous sibling reference.
     */
    private YdtAppContext previousSibling;

    /*
     * Last child reference.
     */
    private YdtAppContext lastChild;

    /*
     * YDT application tree extended information.
     */
    private final T appData;

    /*
     * Reference for operation type for application root node.
     */
    private YdtAppNodeOperationType operationType;

    /**
     * Creates an instance of YANG application tree which is used by all node
     * needs delete list.
     *
     * @param data application data
     */
    DefaultYdtAppContext(T data) {
        appData = data;
    }

    @Override
    public void updateAppOperationType(YdtContextOperationType ydtOpType) {
        if (parent == null) {
            return;
        }
        YdtAppNodeOperationType opType = getAppOpTypeFromYdtOpType(ydtOpType);
        YdtAppContext curNode = this;
        YdtAppNodeOperationType parentOpType = operationType;
        if (opType != parentOpType) {
            if (parentOpType != null) {
                while (curNode.getOperationType() != BOTH &&
                        curNode.getParent() != null) {
                    curNode.setOperationType(BOTH);
                    curNode = curNode.getParent();
                }
            } else {
                // If operation type for ydt node is "NONE" then in that
                // case operation type for module node in app tree set as null.
                // Once the target node operation type received by ydt then
                // operation type for module node will be updated with the
                // same target node operation type in app tree.
                while (curNode.getParent() != null && curNode
                        .getOperationType() == null) {
                    curNode.setOperationType(opType);
                    curNode = curNode.getParent();
                }
            }
        }
    }

    @Override
    public void setAppData(YdtNode moduleNode, YangSchemaNode augmentNode) {
        if (augmentNode != null) {
            ((AugmentAppData) appData).setAugmentingSchemaNode(augmentNode);
        } else {
            ((ModuleAppData) appData).setModuleContext(moduleNode);
        }
    }

    @Override
    public AppData getAppData() {
        return appData;
    }

    @Override
    public YdtAppContext getParent() {
        return parent;
    }

    @Override
    public void setParent(YdtAppContext parent) {
        this.parent = parent;
    }

    @Override
    public YdtAppContext getFirstChild() {
        return child;
    }

    /**
     * Sets the context of first child.
     *
     * @param child node
     */
    private void setChild(YdtAppContext child) {
        this.child = child;
    }

    @Override
    public YdtAppContext getNextSibling() {
        return nextSibling;
    }

    @Override
    public void setNextSibling(YdtAppContext context) {
        this.nextSibling = context;
    }

    @Override
    public YdtAppContext getPreviousSibling() {
        return previousSibling;
    }

    @Override
    public void setPreviousSibling(YdtAppContext context) {
        this.previousSibling = context;
    }

    @Override
    public YdtAppNodeOperationType getOperationType() {
        return operationType;
    }

    @Override
    public void setOperationType(YdtAppNodeOperationType opType) {
        operationType = opType;
    }

    @Override
    public List<YdtContext> getDeleteNodes() {
        return ((ModuleAppData) appData).getDeleteNodes();
    }

    @Override
    public void addDeleteNode(YdtNode node) {
        DefaultYdtAppContext<?> curNode = this;
        while (curNode.getParent().getParent() != null) {
            curNode = (DefaultYdtAppContext<?>) curNode.getParent();
        }
        ((ModuleAppData) curNode.appData).addDeleteNodes(node);
    }

    @Override
    public YdtContext getModuleContext() {
        return ((ModuleAppData) appData).getModuleContext();
    }

    @Override
    public YangSchemaNode getAugmentingSchemaNode() {
        return ((AugmentAppData) appData).getAugmentingSchemaNode();
    }

    @Override
    public void setAugmentingSchemaNode(YangSchemaNode schemaNode) {
        ((AugmentAppData) appData).setAugmentingSchemaNode(schemaNode);
    }

    @Override
    public YdtAppContext getLastChild() {
        return lastChild;
    }

    /**
     * Sets the context of last child.
     *
     * @param child node
     */
    private void setLastChild(YdtAppContext child) {
        lastChild = child;
    }

    @Override
    public void addChild(YdtAppContext newChild) {

        if (newChild.getParent() == null) {
            newChild.setParent(this);
        }

        // First child to be added.
        if (getFirstChild() == null) {
            setChild(newChild);
            // Update last child.
            setLastChild(newChild);
            return;
        }

        // If the new node needs to be add as last child.
        YdtAppContext curNode = getLastChild();
        curNode.setNextSibling(newChild);
        newChild.setPreviousSibling(curNode);
        setLastChild(newChild);
    }

    @Override
    public YangSchemaNode getYangSchemaNode() {
        return appData.getSchemaNode();
    }
}
