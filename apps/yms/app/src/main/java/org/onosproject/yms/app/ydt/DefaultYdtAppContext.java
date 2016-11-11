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

import org.onosproject.yangutils.datamodel.YangAugment;
import org.onosproject.yangutils.datamodel.YangSchemaNode;
import org.onosproject.yangutils.datamodel.YangSchemaNodeContextInfo;
import org.onosproject.yangutils.datamodel.YangSchemaNodeIdentifier;
import org.onosproject.yangutils.datamodel.exceptions.DataModelException;
import org.onosproject.yms.app.ydt.exceptions.YdtException;
import org.onosproject.yms.ydt.YdtContext;
import org.onosproject.yms.ydt.YdtContextOperationType;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.onosproject.yms.app.ydt.YdtAppNodeOperationType.BOTH;
import static org.onosproject.yms.app.ydt.YdtNodeFactory.getAppOpTypeFromYdtOpType;

/**
 * Represents YANG request work bench which contains all parameters for
 * request handling and methods to build and obtain YANG application data tree
 * which is data (sub)instance representation, abstract of protocol.
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
    private T appData;

    /*
     * Reference for operation type for application root node.
     */
    private YdtAppNodeOperationType operationType;

    /*
     * Reference application node set.
     */
    private Set<YangSchemaNode> appSet;

    /**
     * Creates an instance of YANG application tree which is used by all node
     * needs delete list.
     */
    private DefaultYdtAppContext() {
        appSet = new HashSet<>();
    }

    /**
     * Adds schema node of new requested augmented node in current context of
     * application tree.
     *
     * @param schemaNode schema node of requested node
     * @return addition result(true/false)
     */
    public boolean addSchemaToAppSet(YangSchemaNode schemaNode) {
        return appSet.add(schemaNode);
    }

    @Override
    public void updateAppOperationType(YdtContextOperationType ydtOpType) {
        if (parent == null) {
            return;
        }
        YdtAppNodeOperationType opType = getAppOpTypeFromYdtOpType(ydtOpType);
        YdtAppContext curNode = this;
        YdtAppNodeOperationType parentOpType = operationType;
        if (parentOpType != null && opType != parentOpType) {
            while (curNode.getOperationType() != BOTH &&
                    curNode.getParent() != null) {
                curNode.setOperationType(BOTH);
                curNode = curNode.getParent();
            }
        }
    }

    @Override
    public void setAppData(YdtNode moduleNode, YangSchemaNode augmentNode) {
        if (augmentNode != null) {
            appData.setAugmentingSchemaNode(augmentNode);
        } else {
            appData.setModuleContext(moduleNode);
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

    @Override
    public void setChild(YdtAppContext child) {
        this.child = child;
    }

    @Override
    public YdtAppContext getNextSibling() {
        return nextSibling;
    }

    @Override
    public void setNextSibling(YdtAppContext nextSibling) {
        this.nextSibling = nextSibling;
    }

    @Override
    public YdtAppContext getPreviousSibling() {
        return previousSibling;
    }

    @Override
    public void setPreviousSibling(YdtAppContext previousSibling) {
        this.previousSibling = previousSibling;
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
        return appData.getDeleteNodes();
    }


    @Override
    public void addDeleteNode(YdtNode node) {
        DefaultYdtAppContext<?> curNode = this;
        while (curNode.getParent().getParent() != null) {
            curNode = (DefaultYdtAppContext<?>) curNode.getParent();
        }

        curNode.appData.addDeleteNodes(node);
    }

    @Override
    public YdtContext getModuleContext() {
        return appData.getModuleContext();
    }

    @Override
    public void setModuleContext(YdtContext moduleNode) {
        appData.setModuleContext(moduleNode);
    }

    @Override
    public YangSchemaNode getAugmentingSchemaNode() {
        return appData.getAugmentingSchemaNode();
    }

    @Override
    public void setAugmentingSchemaNode(YangSchemaNode schemaNode) {
        appData.setAugmentingSchemaNode(schemaNode);
    }


    @Override
    public YangSchemaNode getAugmentingSchemaNode(
            YangSchemaNodeIdentifier id,
            YangSchemaNodeContextInfo contextInfo) {
        YangSchemaNode lastAugMod = null;
        YangSchemaNode switchedNode =
                contextInfo.getContextSwitchedNode();

        while (switchedNode != null) {
            if (switchedNode instanceof YangAugment) {
                lastAugMod = switchedNode;
            }
            try {
                switchedNode = switchedNode.getChildSchema(id)
                        .getContextSwitchedNode();
            } catch (DataModelException e) {
                throw new YdtException(e.getMessage());
            }
        }
        return lastAugMod;
    }

    @Override
    public YdtAppContext getLastChild() {
        return lastChild;
    }

    @Override
    public void setLastChild(YdtAppContext lastChild) {
        this.lastChild = lastChild;
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

    /**
     * Creates an instance of application tree context with module schema data.
     *
     * @return application tree context
     */
    public static DefaultYdtAppContext getModuleAppContext() {
        DefaultYdtAppContext context =
                new DefaultYdtAppContext<ModuleSchemaData>();
        context.appData = new ModuleSchemaData();
        return context;
    }

    /**
     * Creates an instance of application tree context with augment schema data.
     *
     * @return application tree context
     */
    public static DefaultYdtAppContext getAugmentAppContext() {
        DefaultYdtAppContext context =
                new DefaultYdtAppContext<AugmentedSchemaData>();
        context.appData = new AugmentedSchemaData();
        return context;
    }
}
