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

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Abstraction of an entity which provides interfaces to build and obtain YANG
 * data tree which is data (sub)instance representation, abstract of protocol.
 *
 * NBI protocols need to translate the protocol operation request, into a
 * protocol independent abstract tree called the YANG data tree (YDT). In order
 * to enable the protocol in building these abstract data tree, YANG
 * management system provides a utility called the YANG data tree builder.
 */
public interface YdtBuilder
        extends Ydt {

    /**
     * Sets root node tag attributes. This is used by protocol
     * to specify tag attributes associated with root resource.
     *
     * @param attributeTag map of root tags attribute values indexed by root
     *                     tag name.
     */
    void setRootTagAttributeMap(Map<String, String> attributeTag);

    /**
     * Returns map of tag attribute list associated with root resource.
     *
     * @return linked hash map of tag name with value
     */
    Map<String, String> getRootTagAttributeMap();

    /**
     * Adds a last child to YANG data tree, this method is to be used by
     * protocols which are unaware of the nature (single/multiple) of node and
     * also unaware of the operation type at every node(Example: RESTCONF).
     *
     * Add child is used to add module/sub-module nodes also. Request may
     * contain revision number corresponding to Module/sub-module in that
     * case YMS expect revision number to be appended to module/sub-module
     * name in the below mentioned format.
     * module-or-submodule-name ['@' date-arg]
     * date-arg = 4DIGIT "-" 2DIGIT "-" 2DIGIT
     * Example: testModule@2016-10-27.
     *
     * If the revision date is not specified YMS first search for
     * registered module/sub-module without revision date, if still can't obtain
     * then uses registered module/sub-module with latest revision date.
     *
     * @param name      name of child to be added
     * @param namespace namespace of child to be added, if it's null, parent's
     *                  namespace will be applied to child
     */
    void addChild(String name, String namespace);

    /**
     * Adds a last child to YANG data tree, this method is to be used by
     * protocols which are aware of the nature (single/multiple) of node.
     *
     * Add child is used to add module/sub-module nodes also. Request may
     * contain revision number corresponding to Module/sub-module in that
     * case YMS expect revision number to be appended to module/sub-module
     * name in the below mentioned format.
     * module-or-submodule-name ['@' date-arg]
     * date-arg = 4DIGIT "-" 2DIGIT "-" 2DIGIT
     * Example: testModule@2016-10-27.
     *
     * If the revision date is not specified YMS first search for
     * registered module/sub-module without revision date, if still can't obtain
     * then uses registered module/sub-module with latest revision date.
     *
     * @param name      name of child to be added
     * @param namespace namespace of child to be added, if it's null, parent's
     *                  namespace will be applied to child
     * @param ydtType   type of YDT node to be added
     */
    void addChild(String name, String namespace, YdtType ydtType);

    /**
     * Adds a last child to YANG data tree, this method is to be used by
     * protocols which are unaware of the nature (single/multiple) of node.
     * This is an overloaded method with operation type. This method can
     * optionally
     * be used when protocol doesn't want to specify operation type by
     * keeping it null.
     *
     * Add child is used to add module/sub-module nodes also. Request may
     * contain revision number corresponding to Module/sub-module in that
     * case YMS expect revision number to be appended to module/sub-module
     * name in the below mentioned format.
     * module-or-submodule-name ['@' date-arg]
     * date-arg = 4DIGIT "-" 2DIGIT "-" 2DIGIT
     * Example: testModule@2016-10-27.
     *
     * If the revision date is not specified YMS first search for
     * registered module/sub-module without revision date, if still can't obtain
     * then uses registered module/sub-module with latest revision date.
     *
     * @param name      name of child to be added
     * @param namespace namespace of child to be added, if it's null, parent's
     *                  namespace will be applied to child
     * @param opType    type of requested operation over a node
     */
    void addChild(String name, String namespace,
                  YdtContextOperationType opType);

    /**
     * Adds a last child to YANG data tree, this method is to be used by
     * protocols which are aware of the nature (single/multiple) of node.
     * This is an overloaded method with operation type. This method can
     * optionally
     * be used when protocol doesn't want to specify operation type by
     * keeping it null.
     *
     * Add child is used to add module/sub-module nodes also. Request may
     * contain revision number corresponding to Module/sub-module in that
     * case YMS expect revision number to be appended to module/sub-module
     * name in the below mentioned format.
     * module-or-submodule-name ['@' date-arg]
     * date-arg = 4DIGIT "-" 2DIGIT "-" 2DIGIT
     * Example: testModule@2016-10-27.
     *
     * If the revision date is not specified YMS first search for
     * registered module/sub-module without revision date, if still can't obtain
     * then uses registered module/sub-module with latest revision date.
     *
     * @param name      name of child to be added
     * @param namespace namespace of child to be added, if it's null, parent's
     *                  namespace will be applied to child
     * @param ydtType   type of YDT node to be added
     * @param opType    type of requested operation over a node
     */
    void addChild(String name, String namespace, YdtType ydtType,
                  YdtContextOperationType opType);


    /**
     * Adds a last leaf with value to YANG data tree. Protocols unaware of
     * nature
     * of leaf (single/multiple) will use it to add both single instance and
     * multi instance node. Protocols aware of nature of node will use it for
     * single instance value node addition.
     * Value of leaf can be null which indicates selection node in get
     * operation.
     *
     * @param name      name of child to be added
     * @param namespace namespace of child to be added, if it's null, parent's
     *                  namespace will be applied to child
     * @param value     value of the child
     */
    void addLeaf(String name, String namespace, String value);

    /**
     * Adds a last leaf with list of values to YANG data tree. This method is
     * used by protocols which knows the nature (single/multiple) of node for
     * multi instance node addition.
     * Value of leaf can be null which indicates selection node in get
     * operation.
     *
     * @param name      name of child to be added
     * @param namespace namespace of child to be added, if it's null, parent's
     *                  namespace will be applied to child
     * @param valueSet  list of value of the child
     */
    void addLeaf(String name, String namespace, Set<String> valueSet);

    /**
     * Adds an instance of a child list node, or adds a child leaf list with
     * multiple instance.
     * In case the name and namespace identifies the child list node, then
     * the values for all the key leaves must be passed in the same order of
     * schema. Then the effective YANG data tree will be like adding a  list
     * node, followed by adding the key leaves as the child to the list node.
     * After this operation, the call to getCurNode will return the list node.
     * In case the name and namespace identifies the child leaf-list, then
     * the values identifies the instance of leaf list.
     * After this operation, the call to getCurNode will return the leaf-list
     * node.
     *
     * @param name      name of child to be added
     * @param namespace namespace of child to be added, if it's null, parent's
     *                  namespace will be applied to child
     * @param valueList values of the keys in URI in the same order
     *                  as defined in YANG file
     */
    void addMultiInstanceChild(String name, String namespace,
                               List<String> valueList);

    /**
     * Traverses up in YANG data tree to the parent node, it is to be used when
     * protocol is using context type "current" and wanted to traverse up the
     * tree.
     */
    void traverseToParent();

    /**
     * Returns the current context information available in YDT node.
     *
     * @return current YDT context
     */
    YdtContext getCurNode();

    /**
     * Sets default operation type. This operation type is taken if operation
     * type is not explicitly specified in request. If default operation type
     * is not set, merge will be taken as default operation type.
     *
     * @param ydtContextOperationType default edit operation type
     */
    void setDefaultEditOperationType(
            YdtContextOperationType ydtContextOperationType);
}
