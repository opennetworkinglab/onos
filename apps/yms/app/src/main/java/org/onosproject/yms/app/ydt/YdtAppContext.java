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

/**
 * Abstraction of an entity which represents YANG application data tree context
 * information. This context information will be used by protocol to obtain
 * the information associated with YDT application node. This is used when
 * protocol is walking the application data tree in both visitor and listener
 * mechanism.
 */
public interface YdtAppContext {

    /**
     * Returns the context of parent node.
     *
     * @return context of parent node
     */
    YdtAppContext getParent();

    /**
     * Sets the context of parent node.
     *
     * @param parent node
     */
    void setParent(YdtAppContext parent);

    /**
     * Returns the context of first child.
     *
     * @return context of first child
     */
    YdtAppContext getFirstChild();

    /**
     * Returns the context of last child.
     *
     * @return context of last child
     */
    YdtAppContext getLastChild();

    /**
     * Returns the context of next sibling.
     *
     * @return context of next sibling
     */
    YdtAppContext getNextSibling();

    /**
     * Sets the context of next sibling.
     *
     * @param nextSibling node
     */
    void setNextSibling(YdtAppContext nextSibling);

    /**
     * Returns the context of previous sibling.
     *
     * @return context of previous sibling
     */
    YdtAppContext getPreviousSibling();

    /**
     * Sets the context of previous sibling.
     *
     * @param preSibling node
     */
    void setPreviousSibling(YdtAppContext preSibling);

    /**
     * Returns the app tree operation type.
     *
     * @return app tree operation type
     */
    YdtAppNodeOperationType getOperationType();

    /**
     * Set the app tree operation type.
     *
     * @param opType app tree operation type
     */
    void setOperationType(YdtAppNodeOperationType opType);

    /**
     * Returns the list of nodes with operation type delete.
     *
     * @return list of nodes with operation type delete
     */
    List<YdtContext> getDeleteNodes();

    /**
     * Adds the ydt node with operation type delete in module delete node list.
     *
     * @param node ydt node with operation type delete/remove
     */
    void addDeleteNode(YdtNode node);

    /**
     * Returns application's root ydtContext.
     *
     * @return YdtContext of application root node
     */
    YdtContext getModuleContext();

    /**
     * Returns the YangSchemaNode of augmenting application.
     *
     * @return YangSchemaNode of augmenting application
     */
    YangSchemaNode getAugmentingSchemaNode();

    /**
     * Sets the YangSchemaNode of augmenting application root node.
     *
     * @param schemaNode YangSchemaNode of augmenting application module
     */
    void setAugmentingSchemaNode(YangSchemaNode schemaNode);

    /**
     * Adds a last child to ydt application data tree.
     *
     * @param newChild name of child to be added
     */
    void addChild(YdtAppContext newChild);

    /**
     * Updates the app tree operation type.
     * <p>
     * If earlier operation type was OTHER_EDIT and now operation type came as
     * DELETE_ONLY or vice-versa, then update operation type to BOTH.
     *
     * @param opType ydt current context operation type
     */
    void updateAppOperationType(YdtContextOperationType opType);

    /**
     * Sets the application data for given request. If in requested parameters
     * schemaNode is not null then appData will be set with
     * augmentedSchemaData else with moduleSchemaData object.
     *
     * @param moduleNode module node of requested app
     * @param schemaNode augmented schema node of requested context
     */
    void setAppData(YdtNode moduleNode, YangSchemaNode schemaNode);

    /**
     * Returns the app data for current context.
     *
     * @return app data
     */
    AppData getAppData();

    /**
     * Returns the yang schema for requested node.
     *
     * @return schema node
     */
    YangSchemaNode getYangSchemaNode();
}
