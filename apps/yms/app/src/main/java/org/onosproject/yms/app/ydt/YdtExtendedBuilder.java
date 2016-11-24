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
import org.onosproject.yms.ydt.YdtBuilder;
import org.onosproject.yms.ydt.YdtContextOperationType;

import java.util.Set;

/**
 * Abstraction of an entity which represents extension of YDT builder
 * required by internal sub modules.
 */
public interface YdtExtendedBuilder extends YdtBuilder {

    /**
     * Adds a last child to YANG data tree; this method is to be used by
     * YANG object builder.
     *
     * @param yangSchemaNode schema node from YANG metadata
     * @param opType         type of requested operation over a node
     * @return YDT context
     */
    YdtExtendedContext addChild(YdtContextOperationType opType,
                                YangSchemaNode yangSchemaNode);

    /**
     * Adds a last leaf list to YANG data tree; this method is to be used by
     * YANG object builder.
     *
     * @param valueSet       list of value of the child
     * @param yangSchemaNode schema node from YANG metadata
     * @return YDT context
     */
    YdtExtendedContext addLeafList(Set<String> valueSet,
                                   YangSchemaNode yangSchemaNode);

    /**
     * Adds a last leaf to YANG data tree; this method is to be used by
     * YANG object builder.
     *
     * @param value          value of the child
     * @param yangSchemaNode schema node from YANG metadata
     * @return YDT context
     */
    YdtExtendedContext addLeaf(String value, YangSchemaNode yangSchemaNode);

    /**
     * Traverses up in YANG data tree to the parent node, it is to be used when
     * protocol is using extended context type and wanted to traverse
     * up the tree without doing any validation.
     *
     * @throws IllegalStateException when user request for traverse to logical
     *                               root node parent
     */
    void traverseToParentWithoutValidation() throws IllegalStateException;

    @Override
    YdtExtendedContext getRootNode();

    @Override
    YdtExtendedContext getCurNode();
}
