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

package org.onosproject.yms.app.ypm;

import org.onosproject.yangutils.datamodel.YangSchemaNode;
import org.onosproject.yangutils.datamodel.YangSchemaNodeIdentifier;
import org.onosproject.yms.app.ydt.AppType;
import org.onosproject.yms.app.ydt.YdtExtendedContext;
import org.onosproject.yms.app.ydt.YdtNode;
import org.onosproject.yms.ydt.YdtContext;
import org.onosproject.yms.ydt.YdtContextOperationType;
import org.onosproject.yms.ydt.YdtExtendedInfoType;
import org.onosproject.yms.ydt.YdtType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents implementation of interfaces to build and obtain YANG data tree.
 */
public class YdtNodeAdapter<T> implements YdtExtendedContext {

    /**
     * Parent reference.
     */
    private YdtNodeAdapter parent;

    /**
     * First child reference.
     */
    private YdtNodeAdapter child;

    /**
     * Next sibling reference.
     */
    private YdtNodeAdapter nextSibling;

    /**
     * Previous sibling reference.
     */
    private YdtNodeAdapter previousSibling;

    /**
     * Last child reference.
     */
    private YdtNode lastChild;

    /**
     * Type of node.
     */
    private YdtType ydtType;

    /**
     * Flag to keep the track of context switch.
     */
    private boolean isContextSwitch;

    private T ydtExtendedInfo;

    /**
     * YDT extended information type.
     */
    YdtExtendedInfoType ydtExtendedInfoType;

    /**
     * Ydt map to keep the track of node added in YDT.
     */
    private Map<YangSchemaNodeIdentifier, List<YdtNode<T>>> ydtNodeMap = new HashMap<>();

    /**
     * Reference for data-model schema node.
     */
    private YangSchemaNode yangSchemaNode;

    /**
     * Reference for ydt node operation type.
     */
    private YdtContextOperationType ydtContextOperationType;

    /**
     * Key object for ydtNodeMap.
     */
    protected YangSchemaNodeIdentifier nodeIdentifier;

    /**
     * Ydt map to keep the track of application information object with respective type.
     */
    Map<AppType, Object> ydtAppInfoMap = new HashMap<>();


    /**
     * Creation of YANG node object.
     */
    public YdtNodeAdapter() {
    }

    /**
     * Creates a specific type of node.
     *
     * @param type of YDT node
     * @param name name of the YDT node
     */
    public YdtNodeAdapter(YdtType type, String name) {
        setYdtType(type);
    }

    @Override
    public String getName() {
        return this.nodeIdentifier.getName();
    }

    @Override
    public String getNamespace() {
        return yangSchemaNode.getNameSpace().getModuleNamespace();
    }

    @Override
    public String getModuleNameAsNameSpace() {
        return null;
    }

    @Override
    public <T> T getYdtContextExtendedInfo() {
        return (T) ydtExtendedInfo;
    }

    @Override
    public YdtExtendedInfoType getYdtExtendedInfoType() {
        return ydtExtendedInfoType;
    }

    @Override
    public YdtType getYdtType() {
        return ydtType;
    }

    /**
     * Sets the node type.
     *
     * @param ydtType type of YDT attribute
     */
    public void setYdtType(YdtType ydtType) {
        this.ydtType = ydtType;
    }

    @Override
    public YdtNodeAdapter getParent() {
        return parent;
    }

    /**
     * Sets the parent of node.
     *
     * @param parent node
     */
    public void setParent(YdtNodeAdapter parent) {
        this.parent = parent;
    }

    @Override
    public YdtNodeAdapter getFirstChild() {
        return child;
    }

    @Override
    public YdtNodeAdapter getNextSibling() {
        return nextSibling;
    }

    /**
     * Sets the next sibling of node.
     *
     * @param sibling YANG node
     */
    public void setNextSibling(YdtNodeAdapter sibling) {
        nextSibling = sibling;
    }

    @Override
    public YdtNodeAdapter getPreviousSibling() {
        return previousSibling;
    }

    /**
     * Sets the previous sibling.
     *
     * @param previousSibling points to predecessor sibling
     */
    public void setPreviousSibling(YdtNodeAdapter previousSibling) {
        this.previousSibling = previousSibling;
    }

    /**
     * Returns data-model node reference for of a given node.
     *
     * @return yang schema data node of a data-model.
     */
    public YangSchemaNode getYangSchemaNode() {
        return yangSchemaNode;
    }

    /**
     * Sets the data-model node reference for of a given node..
     *
     * @param yangSchemaNode YANG data node.
     */
    public void setYangSchemaNode(YangSchemaNode yangSchemaNode) {
        this.yangSchemaNode = yangSchemaNode;
    }

    @Override
    public YdtNode getLastChild() {
        return lastChild;
    }

    /**
     * Sets the last instance of a child node.
     *
     * @param child is last child to be set
     */
    public void setLastChild(YdtNode child) {
        this.lastChild = child;
    }

    public void setNodeIdentifier(YangSchemaNodeIdentifier nodeIdentifier) {
        this.nodeIdentifier = nodeIdentifier;
    }

    /**
     * Adds a child node.
     *
     * @param newChild refers to a child to be added
     */
    public void addChild(YdtContext newChild) {

        ((YdtNodeAdapter) newChild).setParent(this);

        if (this.child == null) {
            this.child = (YdtNodeAdapter) newChild;
            return;
        }

        YdtNodeAdapter currNode = this.child;
        while (currNode.getNextSibling() != null) {
            currNode = currNode.getNextSibling();
        }
        currNode.setNextSibling((YdtNodeAdapter) newChild);
        ((YdtNodeAdapter) newChild).setPreviousSibling(currNode);
    }

    /**
     * Adds a sibling to YANG data tree.
     *
     * @param newSibling context of sibling to be added
     */
    public void addSibling(YdtContext newSibling) {

        ((YdtNodeAdapter) newSibling).setParent(this.getParent());

        YdtNodeAdapter currNode = this;

        while (currNode.getNextSibling() != null) {
            currNode = currNode.getNextSibling();
        }
        currNode.setNextSibling((YdtNodeAdapter) newSibling);
        ((YdtNodeAdapter) newSibling).setPreviousSibling(currNode);
    }

    /**
     * Gets the flag for node if context switch.
     *
     * @return isContextSwitch flag of a node.
     */
    public boolean getContextSwitch() {
        return isContextSwitch;
    }

    /**
     * Sets the flag to keep the track of context switch.
     *
     * @param contextSwitch boolean flag.
     */
    public void setContextSwitch(boolean contextSwitch) {
        isContextSwitch = contextSwitch;
    }

    @Override
    public String getValue() {
        return null;
    }

    @Override
    public Set<String> getValueSet() {
        return null;
    }

    @Override
    public Object getAppInfo(AppType appType) {
        return null;
    }

    @Override
    public void addAppInfo(AppType appType, Object object) {

    }

    @Override
    public YdtContextOperationType getYdtContextOperationType() {
        return ydtContextOperationType;
    }
}
