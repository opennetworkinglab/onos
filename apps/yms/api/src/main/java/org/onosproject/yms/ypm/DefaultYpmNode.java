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

package org.onosproject.yms.ypm;

/**
 * Represents implementation of interfaces to build and obtain YANG protocol metadata tree node.
 */
public class DefaultYpmNode implements YpmContext {

    /**
     * Name of the node.
     */
    private String name;

    /**
     * Parent reference.
     */
    private DefaultYpmNode parent;

    /**
     * First child reference.
     */
    private DefaultYpmNode child;

    /**
     * Next sibling reference.
     */
    private DefaultYpmNode nextSibling;

    /**
     * Previous sibling reference.
     */
    private DefaultYpmNode previousSibling;

    /**
     * Protocol metadata object.
     */
    private Object metaData;

    /**
     * Creates a specific type of node.
     *
     * @param name of ypm node
     */
    public DefaultYpmNode(String name) {
        setName(name);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public DefaultYpmNode getParent() {
        return parent;
    }

    @Override
    public void setParent(YpmContext parent) {
        this.parent = (DefaultYpmNode) parent;
    }

    @Override
    public YpmContext getFirstChild() {
        return child;
    }

    @Override
    public YpmContext getChild(String ypmName) {
        if (ypmName == null) {
            // Input is null. So, will not proceed.
            return null;
        }

        if (child == null) {
            // No children
            return null;
        }

        // Check the first child
        if (child.name.equals(ypmName)) {
            return child;
        }

        // Check its siblings
        YpmContext currentChild = child;
        while (currentChild.getNextSibling() != null) {
            currentChild = currentChild.getNextSibling();
            if (currentChild.getName().equals(ypmName)) {
                return currentChild;
            }
        }

        return null;
    }

    @Override
    public void addChild(String ypmName) {
        if (ypmName == null) {
            // Input is null. So, will not proceed.
            return;
        }

        if (getChild(ypmName) != null) {
            // Already available with the same name. So, no need to add
            return;
        }

        // Create new ypm node and attach to its parent
        DefaultYpmNode newNode = new DefaultYpmNode(ypmName);
        newNode.setParent(this);

        // Check the first child
        if (child == null) {
            child = newNode;
            return;
        }

        // Add it to its siblings
        YpmContext currentChild = child;
        // Go to the last child
        while (currentChild.getNextSibling() != null) {
            currentChild = currentChild.getNextSibling();
        }
        currentChild.setNextSibling(newNode);
        newNode.setPreviousSibling((DefaultYpmNode) currentChild);
    }

    @Override
    public YpmContext getSibling(String ypmName) {
        if (ypmName == null) {
            // Input is null. So, will not proceed.
            return null;
        }

        YpmContext sibling = getNextSibling();
        while (sibling != null) {
            if (sibling.getName().equals(ypmName)) {
                return sibling;
            }
            sibling = sibling.getNextSibling();
        }
        return null;
    }

    @Override
    public void addSibling(String ypmName) {
        if (ypmName == null) {
            // Input is null. So, will not proceed.
            return;
        }

        if (getSibling(ypmName) != null) {
            // Already available with the same name. So, no need to add
            return;
        }

        // Create new ypm node and attach to its parent
        DefaultYpmNode newSibling = new DefaultYpmNode(ypmName);
        newSibling.setParent(this.getParent());

        // Add it as its sibling
        YpmContext sibling = getNextSibling();
        if (sibling == null) {
            setNextSibling(newSibling);
            newSibling.setPreviousSibling(this);
        } else {
            // Go to the last sibling
            while (sibling.getNextSibling() != null) {
                sibling = sibling.getNextSibling();
            }
            sibling.setNextSibling(newSibling);
            newSibling.setPreviousSibling((DefaultYpmNode) sibling);
        }
    }

    @Override
    public DefaultYpmNode getNextSibling() {
        return nextSibling;
    }

    @Override
    public void setNextSibling(DefaultYpmNode sibling) {
        nextSibling = sibling;
    }

    @Override
    public DefaultYpmNode getPreviousSibling() {
        return previousSibling;
    }

    @Override
    public void setPreviousSibling(DefaultYpmNode sibling) {
        this.previousSibling = sibling;
    }

    @Override
    public Object getMetaData() {
        return this.metaData;
    }

    @Override
    public void setMetaData(Object data) {
        this.metaData = data;
    }
}
