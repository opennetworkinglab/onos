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

package org.onosproject.yms.ydt;

import java.util.Set;

/**
 * Abstraction of an entity which represents YANG data tree context
 * information. This context information will be used protocol to obtain
 * the information associated with YDT node. This is used when protocol is
 * walking the data tree in both visitor and listener mechanism.
 */
public interface YdtContext {

    /**
     * Returns the node name.
     *
     * @return node name
     */
    String getName();

    /**
     * Returns the node namespace.
     *
     * @return node  namespace
     */
    String getNamespace();

    /**
     * Returns module name as namespace.
     *
     * @return module name
     */
    String getModuleNameAsNameSpace();

    /**
     * Returns the YDT node extended context information corresponding to YDT
     * node.
     *
     * @param <T> specifies YMS operation specific extended information
     *            associated with YDT context. It will be
     *            YdtContextOperationType in case extended information type
     *            is EDIT_REQUEST and will be YdtContextResponseInfo in case
     *            extended information type is RESPONSE.
     * @return YdtContextOperationType  YDT node operation type
     */
    <T> T getYdtContextExtendedInfo();

    /**
     * Returns YANG data tree extended information type. This is used to
     * identify the type of extended information applicable for YDT node.
     *
     * @return type of extended information
     */
    YdtExtendedInfoType getYdtExtendedInfoType();

    /**
     * Returns the type of YDT entity. This type will be used by protocols to
     * identify the nature of node and can implement it accordingly.
     *
     * @return YDT entity type
     */
    YdtType getYdtType();

    /**
     * Returns the context of parent node.
     *
     * @return context of parent node
     */
    YdtContext getParent();

    /**
     * Returns the context of first child.
     *
     * @return context of first child
     */
    YdtContext getFirstChild();

    /**
     * Returns the context of last child.
     *
     * @return context of last child
     */
    YdtContext getLastChild();

    /**
     * Returns the context of next sibling.
     *
     * @return context of next sibling
     */
    YdtContext getNextSibling();

    /**
     * Returns the context of previous sibling.
     *
     * @return context of previous sibling
     */
    YdtContext getPreviousSibling();

    /**
     * Returns value of node, this is only valid for single instance leaf
     * node, to obtain the nature of the node protocols need to use
     * getYdtType().
     *
     * @return value of node
     */
    String getValue();

    /**
     * Returns set of values of a node, this is only valid for multi instance
     * leaf node, to obtain the nature of the node protocols need to use
     * getYdtType().
     *
     * @return value of YDT leaf
     */
    Set<String> getValueSet();
}
