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
package org.onosproject.yangutils.datamodel;

import java.io.Serializable;

import org.onosproject.yangutils.datamodel.exceptions.DataModelException;

/**
 * Represents base class of a node in data model tree.
 */
public abstract class YangNode
        implements Cloneable, Serializable {

    private static final long serialVersionUID = 806201601L;

    /**
     * Type of node.
     */
    private YangNodeType nodeType;

    /**
     * Parent reference.
     */
    private YangNode parent;

    /**
     * First child reference.
     */
    private YangNode child;

    /**
     * Next sibling reference.
     */
    private YangNode nextSibling;

    /**
     * Previous sibling reference.
     */
    private YangNode previousSibling;

    /**
     * Returns the nodes name.
     *
     * @return nodes name
     */
    public abstract String getName();

    /**
     * Sets the nodes name.
     *
     * @param name nodes name
     */
    public abstract void setName(String name);

    /**
     * Creates a YANG node object.
     */
    @SuppressWarnings("unused")
    private YangNode() {

    }

    /**
     * Creates a specific type of node.
     *
     * @param type of YANG node
     */
    protected YangNode(YangNodeType type) {
        setNodeType(type);
    }

    /**
     * Returns the node type.
     *
     * @return node type
     */
    public YangNodeType getNodeType() {
        return nodeType;
    }

    /**
     * Sets the node type.
     *
     * @param nodeType type of node
     */
    private void setNodeType(YangNodeType nodeType) {
        this.nodeType = nodeType;
    }

    /**
     * Returns the parent of node.
     *
     * @return parent of node
     */
    public YangNode getParent() {
        return parent;
    }

    /**
     * Sets the parent of node.
     *
     * @param parent node
     */
    public void setParent(YangNode parent) {
        this.parent = parent;
    }

    /**
     * Returns the first child of node.
     *
     * @return first child of node
     */
    public YangNode getChild() {
        return child;
    }

    /**
     * Sets the first instance of a child node.
     *
     * @param child is only child to be set
     */
    public void setChild(YangNode child) {
        this.child = child;
    }

    /**
     * Returns the next sibling of node.
     *
     * @return next sibling of node
     */
    public YangNode getNextSibling() {
        return nextSibling;
    }

    /**
     * Sets the next sibling of node.
     *
     * @param sibling YANG node
     */
    private void setNextSibling(YangNode sibling) {
        nextSibling = sibling;
    }

    /**
     * Returns the previous sibling.
     *
     * @return previous sibling node
     */
    public YangNode getPreviousSibling() {
        return previousSibling;
    }

    /**
     * Sets the previous sibling.
     *
     * @param previousSibling points to predecessor sibling
     */
    private void setPreviousSibling(YangNode previousSibling) {
        this.previousSibling = previousSibling;
    }

    /**
     * Adds a child node, the children sibling list will be sorted based on node
     * type.
     *
     * @param newChild refers to a child to be added
     * @throws DataModelException due to violation in data model rules
     */
    public void addChild(YangNode newChild)
            throws DataModelException {
        if (newChild.getNodeType() == null) {
            throw new DataModelException("Abstract node cannot be inserted into a tree");
        }

        if (newChild.getParent() == null) {
            newChild.setParent(this);
        } else if (newChild.getParent() != this) {
            throw new DataModelException("Node is already part of a tree");
        }

        if (newChild.getChild() != null) {
            throw new DataModelException("Child to be added is not atomic, it already has a child");
        }

        if (newChild.getNextSibling() != null) {
            throw new DataModelException("Child to be added is not atomic, it already has a next sibling");
        }

        if (newChild.getPreviousSibling() != null) {
            throw new DataModelException("Child to be added is not atomic, it already has a previous sibling");
        }

        /* First child to be added */
        if (getChild() == null) {
            setChild(newChild);
            return;
        }

        YangNode curNode;
        curNode = getChild();

        /*
         * Get the predecessor child of new child
         */
        while (curNode.getNextSibling() != null) {

            curNode = curNode.getNextSibling();
        }

        /* If the new node needs to be the last child */
        if (curNode.getNextSibling() == null) {
            curNode.setNextSibling(newChild);
            newChild.setPreviousSibling(curNode);
        }
    }
}
