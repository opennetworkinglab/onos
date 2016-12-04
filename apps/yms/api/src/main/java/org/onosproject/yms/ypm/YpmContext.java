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
 * Abstraction of an entity which represents YANG protocol metadata context
 * information.
 */
public interface YpmContext {

    /**
     * Returns the node name.
     *
     * @return node name
     */
    String getName();

    /**
     * Sets the nodes name.
     *
     * @param name nodes name
     */
    void setName(String name);

    /**
     * Returns the context of parent node.
     *
     * @return context of parent node
     */
    YpmContext getParent();

    /**
     * Sets the context of parent node.
     *
     * @param parent node parent
     */
    void setParent(YpmContext parent);

    /**
     * Retrieves the first child of a node.
     *
     * @return first child of a node
     */
    YpmContext getFirstChild();

    /**
     * Retrieves the child of a node for corresponding ydt node.
     *
     * @param name ypm node
     * @return child of a node
     */
    YpmContext getChild(String name);

    /**
     * Adds child to a node by ydt name.
     *
     * @param name ypm name
     */
    void addChild(String name);

    /**
     * Retrieves sibling of a child by (ydt) name.
     *
     * @param name ypm name
     * @return sibling of a child
     */
    YpmContext getSibling(String name);

    /**
     * Adds new sibling to a child.
     *
     * @param name ypm name
     */
    void addSibling(String name);

    /**
     * Returns the context of next sibling.
     *
     * @return context of next sibling
     */
    YpmContext getNextSibling();

    /**
     * Sets the next sibling of node.
     *
     * @param sibling ypm node
     */
    void setNextSibling(DefaultYpmNode sibling);

    /**
     * Returns the previous sibling of a node.
     *
     * @return previous sibling of a node
     */
    DefaultYpmNode getPreviousSibling();

    /**
     * Sets the previous sibling.
     *
     * @param previousSibling points to predecessor sibling
     */
    void setPreviousSibling(DefaultYpmNode previousSibling);

    /**
     * Retrieves protocol metadata.
     *
     * @return metadata
     */
    Object getMetaData();

    /**
     * Sets the protocol metadata.
     *
     * @param data metadata
     */
    void setMetaData(Object data);
}
