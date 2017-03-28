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

package org.onosproject.yms.app.ytb;

import org.onosproject.yangutils.datamodel.YangAugment;
import org.onosproject.yangutils.datamodel.YangAugmentableNode;
import org.onosproject.yangutils.datamodel.YangCase;
import org.onosproject.yangutils.datamodel.YangChoice;
import org.onosproject.yangutils.datamodel.YangDerivedInfo;
import org.onosproject.yangutils.datamodel.YangLeaf;
import org.onosproject.yangutils.datamodel.YangLeafList;
import org.onosproject.yangutils.datamodel.YangLeafRef;
import org.onosproject.yangutils.datamodel.YangLeavesHolder;
import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.datamodel.YangSchemaNode;
import org.onosproject.yangutils.datamodel.YangSchemaNodeIdentifier;
import org.onosproject.yangutils.datamodel.YangType;
import org.onosproject.yangutils.datamodel.exceptions.DataModelException;
import org.onosproject.yangutils.datamodel.utils.builtindatatype.YangDataTypes;
import org.onosproject.yms.app.utils.TraversalType;
import org.onosproject.yms.app.ydt.YdtExtendedBuilder;
import org.onosproject.yms.app.ydt.YdtExtendedContext;
import org.onosproject.yms.app.ysr.YangSchemaRegistry;
import org.onosproject.yms.ydt.YdtContextOperationType;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.onosproject.yangutils.datamodel.utils.builtindatatype.YangDataTypes.EMPTY;
import static org.onosproject.yangutils.utils.io.impl.YangIoUtils.getCapitalCase;
import static org.onosproject.yms.app.utils.TraversalType.CHILD;
import static org.onosproject.yms.app.utils.TraversalType.PARENT;
import static org.onosproject.yms.app.utils.TraversalType.ROOT;
import static org.onosproject.yms.app.utils.TraversalType.SIBLING;
import static org.onosproject.yms.app.ydt.AppType.YTB;
import static org.onosproject.yms.app.ytb.YtbUtil.PERIOD;
import static org.onosproject.yms.app.ytb.YtbUtil.STR_NULL;
import static org.onosproject.yms.app.ytb.YtbUtil.getAttributeFromInheritance;
import static org.onosproject.yms.app.ytb.YtbUtil.getAttributeOfObject;
import static org.onosproject.yms.app.ytb.YtbUtil.getClassLoaderForAugment;
import static org.onosproject.yms.app.ytb.YtbUtil.getInterfaceClassFromImplClass;
import static org.onosproject.yms.app.ytb.YtbUtil.getJavaName;
import static org.onosproject.yms.app.ytb.YtbUtil.getNodeOpType;
import static org.onosproject.yms.app.ytb.YtbUtil.getOpTypeName;
import static org.onosproject.yms.app.ytb.YtbUtil.getParentObjectOfNode;
import static org.onosproject.yms.app.ytb.YtbUtil.getStringFromType;
import static org.onosproject.yms.app.ytb.YtbUtil.isAugmentNode;
import static org.onosproject.yms.app.ytb.YtbUtil.isMultiInstanceNode;
import static org.onosproject.yms.app.ytb.YtbUtil.isNodeProcessCompleted;
import static org.onosproject.yms.app.ytb.YtbUtil.isNonProcessableNode;
import static org.onosproject.yms.app.ytb.YtbUtil.isTypePrimitive;
import static org.onosproject.yms.app.ytb.YtbUtil.isValueOrSelectLeafSet;
import static org.onosproject.yms.app.ytb.YtbUtil.nonEmpty;
import static org.onosproject.yms.ydt.YdtContextOperationType.NONE;

/**
 * Implements traversal of YANG node and its corresponding object, resulting
 * in building of the YDT tree.
 */
public class YdtBuilderFromYo {

    private static final String STR_TYPE = "type";
    private static final String STR_SUBJECT = "subject";
    private static final String TRUE = "true";
    private static final String IS_LEAF_VALUE_SET_METHOD = "isLeafValueSet";
    private static final String IS_SELECT_LEAF_SET_METHOD = "isSelectLeaf";
    private static final String OUTPUT = "output";
    private static final String YANG_AUGMENTED_INFO_MAP =
            "yangAugmentedInfoMap";
    private static final String FALSE = "false";

    /**
     * Application YANG schema registry.
     */
    private final YangSchemaRegistry registry;

    /**
     * Current instance of the YDT builder where the tree is built.
     */
    private final YdtExtendedBuilder extBuilder;

    /**
     * YANG root object that is required for walking along with the YANG node.
     */
    private Object rootObj;

    /**
     * YANG root node that is required for walking along with the YANG object.
     */
    private YangSchemaNode rootSchema;

    /**
     * Creates YDT builder from YANG object by assigning the mandatory values.
     *
     * @param rootBuilder root node builder
     * @param rootObj     root node object
     * @param registry    application schema registry
     */
    public YdtBuilderFromYo(YdtExtendedBuilder rootBuilder, Object rootObj,
                            YangSchemaRegistry registry) {
        extBuilder = rootBuilder;
        this.rootObj = rootObj;
        this.registry = registry;
    }

    /**
     * Returns schema root node, received from YSR, which searches based on
     * the object received from YAB or YCH.
     *
     * @param object root node object
     */
    public void getModuleNodeFromYsr(Object object) {
        Class interfaceClass = getInterfaceClassFromImplClass(object);
        rootSchema = registry
                .getYangSchemaNodeUsingGeneratedRootNodeInterfaceFileName(
                        interfaceClass.getName());
    }

    /**
     * Returns schema root node, received from YSR, which searches based on
     * the object received from YNH.
     *
     * @param object notification event object
     */
    public void getRootNodeWithNotificationFromYsr(Object object) {
        rootSchema = registry.getRootYangSchemaNodeForNotification(
                object.getClass().getName());
    }

    /**
     * Creates the module node for in YDT before beginning with notification
     * root node traversal. Collects sufficient information to fill YDT with
     * notification root node in the traversal.
     */
    public void createModuleInYdt() {
        extBuilder.addChild(NONE, rootSchema);
        rootSchema = getSchemaNodeOfNotification();
        rootObj = getObjOfNotification();
    }

    /**
     * Creates the module and RPC node, in YDT tree, from the logical root
     * node received from request workbench. The output schema node is taken
     * from the child schema of RPC YANG node.
     *
     * @param rootNode logical root node
     */
    public void createModuleAndRpcInYdt(YdtExtendedContext rootNode) {

        YdtExtendedContext moduleNode =
                (YdtExtendedContext) rootNode.getFirstChild();
        extBuilder.addChild(NONE, moduleNode.getYangSchemaNode());

        YdtExtendedContext rpcNode =
                (YdtExtendedContext) moduleNode.getFirstChild();
        YangSchemaNode rpcSchemaNode = rpcNode.getYangSchemaNode();
        extBuilder.addChild(NONE, rpcSchemaNode);

        // Defines a schema identifier for output node.
        YangSchemaNodeIdentifier schemaId = new YangSchemaNodeIdentifier();
        schemaId.setName(OUTPUT);
        schemaId.setNameSpace(rpcSchemaNode.getNameSpace());
        try {
            // Gets the output schema node from RPC child schema.
            rootSchema = rpcSchemaNode.getChildSchema(schemaId).getSchemaNode();
        } catch (DataModelException e) {
            throw new YtbException(e);
        }
    }

    /**
     * Creates YDT tree from the root object, by traversing through YANG data
     * model node, and simultaneously checking the object nodes presence and
     * walking the object.
     */
    public void createYdtFromRootObject() {
        YangNode curNode = (YangNode) rootSchema;
        TraversalType curTraversal = ROOT;
        YtbNodeInfo listNodeInfo = null;
        YtbNodeInfo augmentNodeInfo = null;

        while (curNode != null) {
            /*
             * Processes the node, if it is being visited for the first time in
             * the schema, also if the schema node is being retraced in a multi
             * instance node.
             */
            if (curTraversal != PARENT || isMultiInstanceNode(curNode)) {

                if (curTraversal == PARENT && isMultiInstanceNode(curNode)) {
                    /*
                     * If the schema is being retraced for a multi-instance
                     * node, it has already entered for this multi-instance
                     * node. Now this re-processes the same schema node for
                     * any additional list object.
                     */
                    listNodeInfo = getCurNodeInfoAndTraverseBack();
                }

                if (curTraversal == ROOT && !isAugmentNode(curNode)) {
                    /*
                     * In case of RPC output, the root node is augmentative,
                     * so when the root traversal is coming for augment this
                     * flow is skipped. This adds only the root node in the YDT.
                     */
                    processApplicationRootNode();
                } else {
                    /*
                     * Gets the object corresponding to current schema node.
                     * If object exists, this adds the corresponding YDT node
                     * to the tree and returns the object. Else returns null.
                     */
                    Object processedObject = processCurSchemaNodeAndAddToYdt(
                            curNode, listNodeInfo);
                    /*
                     * Clears the list info of processed node. The next time
                     * list info is taken newly and accordingly.
                     */
                    listNodeInfo = null;
                    if (processedObject == null && !isAugmentNode(curNode)) {
                        /*
                         * Checks the presence of next sibling of the node, by
                         * breaking the complete chain of the current node,
                         * when the object value is not present, or when the
                         * list entries are completely retraced. The augment
                         * may have sibling, so this doesn't process for
                         * augment.
                         */
                        YtbTraversalInfo traverseInfo =
                                getProcessableInfo(curNode);
                        curNode = traverseInfo.getYangNode();
                        curTraversal = traverseInfo.getTraverseType();
                        continue;
                        /*
                         * Irrespective of root or parent, sets the traversal
                         * type as parent, when augment node doesn't have any
                         * value. So, the other sibling augments can be
                         * processed, if present.
                         */
                    } else if (processedObject == null &&
                            isAugmentNode(curNode)) {
                        curTraversal = PARENT;
                        /*
                         * The second content in the list will be having
                         * parent traversal, in such case it cannot go to its
                         * child in the flow, so it is made as child
                         * traversal and proceeded to continue.
                         */
                    } else if (curTraversal == PARENT &&
                            isMultiInstanceNode(curNode)) {
                        curTraversal = CHILD;
                    }
                }
            }
            /*
             * Checks for the sibling augment when the first augment node is
             * getting completed. From the current augment node the previous
             * node info is taken for augment and the traversal is changed to
             * child, so as to check for the presence of sibling augment.
             */
            if (curTraversal == PARENT && isAugmentNode(curNode)) {
                curNode = ((YangAugment) curNode).getAugmentedNode();
                augmentNodeInfo = getParentYtbInfo();
                curTraversal = CHILD;
            }
            /*
             * Creates an augment iterator for the first time or takes the
             * previous augment iterator for more than one time, whenever an
             * augmentative node arrives. If augment is present it goes back
             * for processing. If its null, the augmentative nodes process is
             * continued.
             */
            if (curTraversal != PARENT &&
                    curNode instanceof YangAugmentableNode) {
                YangNode augmentNode = getAugmentInsideSchemaNode(
                        curNode, augmentNodeInfo);
                if (augmentNode != null) {
                    curNode = augmentNode;
                    continue;
                }
            }
            /*
             * Processes the child, after processing the node. If complete
             * child depth is over, it takes up sibling and processes it.
             * Once child and sibling is over, it is traversed back to the
             * parent, without processing. In multi instance case, before
             * going to parent or schema sibling, its own list sibling is
             * processed. Skips the processing of RPC,notification and
             * augment, as these nodes are dealt in a different flow.
             */
            if (curTraversal != PARENT && curNode.getChild() != null) {
                augmentNodeInfo = null;
                listNodeInfo = null;
                curTraversal = CHILD;
                curNode = curNode.getChild();
                if (isNonProcessableNode(curNode)) {
                    YtbTraversalInfo traverseInfo = getProcessableInfo(curNode);
                    curNode = traverseInfo.getYangNode();
                    curTraversal = traverseInfo.getTraverseType();
                }
            } else if (curNode.getNextSibling() != null) {
                if (isNodeProcessCompleted(curNode, curTraversal)) {
                    break;
                }
                if (isMultiInstanceNode(curNode)) {
                    listNodeInfo = getCurNodeInfoAndTraverseBack();
                    augmentNodeInfo = null;
                    continue;
                }
                curTraversal = SIBLING;
                augmentNodeInfo = null;
                traverseToParent(curNode);
                curNode = curNode.getNextSibling();
                if (isNonProcessableNode(curNode)) {
                    YtbTraversalInfo traverseInfo = getProcessableInfo(curNode);
                    curNode = traverseInfo.getYangNode();
                    curTraversal = traverseInfo.getTraverseType();
                }
            } else {
                if (isNodeProcessCompleted(curNode, curTraversal)) {
                    break;
                }
                if (isMultiInstanceNode(curNode)) {
                    listNodeInfo = getCurNodeInfoAndTraverseBack();
                    augmentNodeInfo = null;
                    continue;
                }
                curTraversal = PARENT;
                traverseToParent(curNode);
                curNode = getParentSchemaNode(curNode);
            }
        }
    }

    /**
     * Returns parent schema node of current node.
     *
     * @param curNode current schema node
     * @return parent schema node
     */
    private YangNode getParentSchemaNode(YangNode curNode) {
        if (curNode instanceof YangAugment) {
            /*
             * If curNode is augment, either next augment or augmented node
             * has to be processed. So traversal type is changed to parent,
             * but node is not changed.
             */
            return curNode;
        }
        return curNode.getParent();
    }

    /**
     * Processes root YANG node and adds it as a child to the YDT
     * extended builder which is created earlier.
     */
    private void processApplicationRootNode() {

        YtbNodeInfo nodeInfo = new YtbNodeInfo();
        YangNode rootYang = (YangNode) rootSchema;
        addChildNodeInYdt(rootObj, rootYang, nodeInfo);
        // If root node has leaf or leaf-list those will be processed.
        processLeaves(rootYang);
        processLeavesList(rootYang);
    }

    /**
     * Traverses to parent, based on the schema node that requires to be
     * traversed. Skips traversal of parent for choice and case node, as they
     * don't get added to the YDT tree.
     *
     * @param curNode current YANG node
     */
    private void traverseToParent(YangNode curNode) {
        if (curNode instanceof YangCase || curNode instanceof YangChoice
                || curNode instanceof YangAugment) {
            return;
        }
        extBuilder.traverseToParentWithoutValidation();
    }

    /**
     * Returns the current YTB info of the YDT builder, and then traverses back
     * to parent. In case of multi instance node the previous node info is
     * used for iterating through the list.
     *
     * @return current YTB app info
     */
    private YtbNodeInfo getCurNodeInfoAndTraverseBack() {
        YtbNodeInfo appInfo = getParentYtbInfo();
        extBuilder.traverseToParentWithoutValidation();
        return appInfo;
    }

    /**
     * Returns augment node for an augmented node. From the list of augment
     * nodes it has, one of the nodes is taken and provided linearly. If the
     * node is not augmented or the all the augment nodes are processed, then
     * it returns null.
     *
     * @param curNode         current YANG node
     * @param augmentNodeInfo previous augment node info
     * @return YANG augment node
     */
    private YangNode getAugmentInsideSchemaNode(YangNode curNode,
                                                YtbNodeInfo augmentNodeInfo) {
        if (augmentNodeInfo == null) {
            List<YangAugment> augmentList = ((YangAugmentableNode) curNode)
                    .getAugmentedInfoList();
            if (nonEmpty(augmentList)) {
                YtbNodeInfo parentNodeInfo = getParentYtbInfo();
                Iterator<YangAugment> augmentItr = augmentList.listIterator();
                parentNodeInfo.setAugmentIterator(augmentItr);
                return augmentItr.next();
            }
        } else if (augmentNodeInfo.getAugmentIterator() != null) {
            if (augmentNodeInfo.getAugmentIterator().hasNext()) {
                return augmentNodeInfo.getAugmentIterator().next();
            }
        }
        return null;
    }

    /**
     * Processes the current YANG node and if necessary adds it to the YDT
     * builder tree by extracting the information from the corresponding
     * class object.
     *
     * @param curNode      current YANG node
     * @param listNodeInfo previous node info for list
     * @return object of the schema node
     */
    private Object processCurSchemaNodeAndAddToYdt(YangNode curNode,
                                                   YtbNodeInfo listNodeInfo) {
        YtbNodeInfo curNodeInfo = new YtbNodeInfo();
        Object nodeObj = null;
        YtbNodeInfo parentNodeInfo = getParentYtbInfo();

        switch (curNode.getYangSchemaNodeType()) {
            case YANG_SINGLE_INSTANCE_NODE:
                nodeObj = processSingleInstanceNode(curNode, curNodeInfo,
                                                    parentNodeInfo);
                break;
            case YANG_MULTI_INSTANCE_NODE:
                nodeObj = processMultiInstanceNode(
                        curNode, curNodeInfo, listNodeInfo, parentNodeInfo);
                break;
            case YANG_CHOICE_NODE:
                nodeObj = processChoiceNode(curNode, parentNodeInfo);
                break;
            case YANG_NON_DATA_NODE:
                if (curNode instanceof YangCase) {
                    nodeObj = processCaseNode(curNode, parentNodeInfo);
                }
                break;
            case YANG_AUGMENT_NODE:
                nodeObj = processAugmentNode(curNode, parentNodeInfo);
                break;
            default:
                throw new YtbException(
                        "Non processable schema node has arrived for adding " +
                                "it in YDT tree");
        }
        // Processes leaf/leaf-list only when object has value, else it skips.
        if (nodeObj != null) {
            processLeaves(curNode);
            processLeavesList(curNode);
        }
        return nodeObj;
    }

    /**
     * Processes single instance node which is added to the YDT tree.
     *
     * @param curNode        current YANG node
     * @param curNodeInfo    current YDT node info
     * @param parentNodeInfo parent YDT node info
     * @return object of the current node
     */
    private Object processSingleInstanceNode(YangNode curNode,
                                             YtbNodeInfo curNodeInfo,
                                             YtbNodeInfo parentNodeInfo) {
        Object childObj = getChildObject(curNode, parentNodeInfo);
        if (childObj != null) {
            addChildNodeInYdt(childObj, curNode, curNodeInfo);
        }
        return childObj;
    }

    /**
     * Processes multi instance node which has to be added to the YDT tree.
     * For the first instance in the list, iterator is created and added to
     * the list. For second instance or more the iterator from first instance
     * is taken and iterated through to get the object of parent.
     *
     * @param curNode        current list node
     * @param curNodeInfo    current node info for list
     * @param listNodeInfo   previous instance node info of list
     * @param parentNodeInfo parent node info of list
     * @return object of the current instance
     */
    private Object processMultiInstanceNode(YangNode curNode,
                                            YtbNodeInfo curNodeInfo,
                                            YtbNodeInfo listNodeInfo,
                                            YtbNodeInfo parentNodeInfo) {
        Object childObj = null;
        /*
         * When YANG list comes to this flow for first time, its YTB node
         * will be null. When it comes for the second or more content, then
         * the list would have been already set for that node. According to
         * set or not set this flow will be proceeded.
         */
        if (listNodeInfo == null) {
            List<Object> childObjList = (List<Object>) getChildObject(
                    curNode, parentNodeInfo);
            if (nonEmpty(childObjList)) {
                Iterator<Object> listItr = childObjList.iterator();
                if (!listItr.hasNext()) {
                    return null;
                    //TODO: Handle the subtree filtering with no list entries.
                }
                childObj = listItr.next();
                /*
                 * For that node the iterator is set. So the next time for
                 * the list this iterator will be taken.
                 */
                curNodeInfo.setListIterator(listItr);
            }
        } else {
            /*
             * If the list value comes for second or more time, that list
             * node will be having YTB node info, where iterator can be
             * retrieved and check if any more contents are present. If
             * present those will be processed.
             */
            curNodeInfo.setListIterator(listNodeInfo.getListIterator());
            if (listNodeInfo.getListIterator().hasNext()) {
                childObj = listNodeInfo.getListIterator().next();
            }
        }
        if (childObj != null) {
            addChildNodeInYdt(childObj, curNode, curNodeInfo);
        }
        return childObj;
    }

    /**
     * Processes choice node which adds a map to the parent node info of
     * choice name and the case object. The object taken for choice node is
     * of case object with choice name. Also, this Skips the addition of choice
     * to YDT.
     *
     * @param curNode        current choice node
     * @param parentNodeInfo parent YTB node info
     * @return object of the choice node
     */
    private Object processChoiceNode(YangNode curNode,
                                     YtbNodeInfo parentNodeInfo) {
        /*
         * Retrieves the parent YTB info, to take the object of parent, so as
         * to check the child attribute from the object.
         */
        Object childObj = getChildObject(curNode, parentNodeInfo);
        if (childObj != null) {
            Map<String, Object> choiceCaseMap = parentNodeInfo
                    .getChoiceCaseMap();
            if (choiceCaseMap == null) {
                choiceCaseMap = new HashMap<>();
                parentNodeInfo.setChoiceCaseMap(choiceCaseMap);
            }
            choiceCaseMap.put(curNode.getName(), childObj);
        }
        return childObj;
    }

    /**
     * Processes case node from the map contents that is filled by choice
     * nodes. Object of choice is taken when choice name and case class name
     * matches. When the case node is not present in the map it returns null.
     *
     * @param curNode        current case node
     * @param parentNodeInfo choice parent node info
     * @return object of the case node
     */
    private Object processCaseNode(YangNode curNode,
                                   YtbNodeInfo parentNodeInfo) {
        Object childObj = null;
        if (parentNodeInfo.getChoiceCaseMap() != null) {
            childObj = getCaseObjectFromChoice(parentNodeInfo,
                                               curNode);
        }
        if (childObj != null) {
            /*
             * Sets the case object in parent info, so that rest of the case
             * children can use it as parent. Case is not added in YDT.
             */
            parentNodeInfo.setCaseObject(childObj);
        }
        return childObj;
    }

    /**
     * Processes augment node, which is not added in the YDT, but binds
     * itself to the parent YTB info, so rest of its child nodes can use for
     * adding themselves to the YDT tree. If there is no augment node added
     * in map or if the augment module is not registered, then it returns null.
     *
     * @param curNode        current augment node
     * @param parentNodeInfo augment parent node info
     * @return object of the augment node
     */
    private Object processAugmentNode(YangNode curNode,
                                      YtbNodeInfo parentNodeInfo) {
        String className = curNode.getJavaClassNameOrBuiltInType();
        String pkgName = curNode.getJavaPackage();
        Object parentObj = getParentObjectOfNode(parentNodeInfo,
                                                 curNode.getParent());
        Map augmentMap;
        try {
            augmentMap = (Map) getAttributeOfObject(parentObj,
                                                    YANG_AUGMENTED_INFO_MAP);
            /*
             * Gets the registered module class. Loads the class and gets the
             * augment class.
             */
            Class moduleClass = getClassLoaderForAugment(curNode, registry);
            if (moduleClass == null) {
                return null;
            }
            Class augmentClass = moduleClass.getClassLoader().loadClass(
                    pkgName + PERIOD + className);
            Object childObj = augmentMap.get(augmentClass);
            parentNodeInfo.setAugmentObject(childObj);
            return childObj;
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            throw new YtbException(e);
        }
    }

    /**
     * Returns the YTB info from the parent node, so that its own bounded
     * object can be taken out.
     *
     * @return parent node YTB node info
     */
    private YtbNodeInfo getParentYtbInfo() {
        YdtExtendedContext parentExtContext = extBuilder.getCurNode();
        return (YtbNodeInfo) parentExtContext.getAppInfo(YTB);
    }

    /**
     * Returns the child object from the parent object. Uses java name of the
     * current node to search the attribute in the parent object.
     *
     * @param curNode        current YANG node
     * @param parentNodeInfo parent YTB node info
     * @return object of the child node
     */
    private Object getChildObject(YangNode curNode,
                                  YtbNodeInfo parentNodeInfo) {
        String nodeJavaName = curNode.getJavaAttributeName();
        Object parentObj = getParentObjectOfNode(parentNodeInfo,
                                                 curNode.getParent());
        try {
            return getAttributeOfObject(parentObj, nodeJavaName);
        } catch (NoSuchMethodException e) {
            throw new YtbException(e);
        }
    }

    /**
     * Adds the child node to the YDT by taking operation type from the
     * object. Also, binds the object to the YDT node through YTB node info.
     *
     * @param childObj    node object
     * @param curNode     current YANG node
     * @param curNodeInfo current YTB info
     */
    private void addChildNodeInYdt(Object childObj, YangNode curNode,
                                   YtbNodeInfo curNodeInfo) {
        YdtContextOperationType opType =
                getNodeOpType(childObj, getOpTypeName(curNode));
        extBuilder.addChild(opType, curNode);
        YdtExtendedContext curExtContext = extBuilder.getCurNode();
        curNodeInfo.setYangObject(childObj);
        curExtContext.addAppInfo(YTB, curNodeInfo);
    }

    /**
     * Processes every leaf in a YANG node. Iterates through the leaf, takes
     * value from the leaf and adds it to the YDT with value. If value is not
     * present, and select leaf is set, adds it to the YDT without value.
     *
     * @param yangNode leaves holder node
     */
    private void processLeaves(YangNode yangNode) {
        if (yangNode instanceof YangLeavesHolder) {
            List<YangLeaf> leavesList = ((YangLeavesHolder) yangNode)
                    .getListOfLeaf();
            if (leavesList != null) {
                for (YangLeaf yangLeaf : leavesList) {
                    YtbNodeInfo parentYtbInfo = getParentYtbInfo();
                    Object parentObj = getParentObjectOfNode(parentYtbInfo,
                                                             yangNode);
                    Object leafType;
                    try {
                        leafType = getAttributeOfObject(parentObj,
                                                        getJavaName(yangLeaf));
                    } catch (NoSuchMethodException e) {
                        throw new YtbException(e);
                    }

                    addLeafWithValue(yangNode, yangLeaf, parentObj, leafType);
                    addLeafWithoutValue(yangNode, yangLeaf, parentObj);
                }
            }
        }
    }

    /**
     * Processes every leaf-list in a YANG node for adding the value in YDT.
     *
     * @param yangNode list of leaf-list holder node
     */
    private void processLeavesList(YangNode yangNode) {
        if (yangNode instanceof YangLeavesHolder) {
            List<YangLeafList> listOfLeafList =
                    ((YangLeavesHolder) yangNode).getListOfLeafList();

            if (listOfLeafList != null) {
                for (YangLeafList yangLeafList : listOfLeafList) {
                    addToBuilder(yangNode, yangLeafList);
                }
            }
        }
    }

    /**
     * Processes the list of objects of the leaf list and adds the leaf list
     * value to the builder.
     *
     * @param yangNode YANG node
     * @param leafList YANG leaf list
     */
    private void addToBuilder(YangNode yangNode, YangLeafList leafList) {
        YtbNodeInfo ytbNodeInfo = getParentYtbInfo();
        Object parentObj = getParentObjectOfNode(ytbNodeInfo, yangNode);
        List<Object> obj;
        try {
            obj = (List<Object>) getAttributeOfObject(parentObj,
                                                      getJavaName(leafList));
        } catch (NoSuchMethodException e) {
            throw new YtbException(e);
        }
        if (obj != null) {
            addLeafListValue(yangNode, parentObj, leafList, obj);
        }
    }

    /**
     * Adds the leaf list value to the YDT builder by taking the string value
     * from the data type.
     *
     * @param yangNode  YANG node
     * @param parentObj parent object
     * @param leafList  YANG leaf list
     * @param obj       list of objects
     */
    private void addLeafListValue(YangNode yangNode, Object parentObj,
                                  YangLeafList leafList, List<Object> obj) {

        Set<String> leafListVal = new LinkedHashSet<>();
        boolean isEmpty = false;
        for (Object object : obj) {
            String val = getStringFromType(yangNode, parentObj,
                                           getJavaName(leafList), object,
                                           leafList.getDataType());
            isEmpty = isTypeEmpty(val, leafList.getDataType());
            if (isEmpty) {
                if (val.equals(TRUE)) {
                    addLeafList(leafListVal, leafList);
                }
                break;
            }
            if (!"".equals(val)) {
                leafListVal.add(val);
            }
        }
        if (!isEmpty && !leafListVal.isEmpty()) {
            addLeafList(leafListVal, leafList);
        }
    }

    /**
     * Adds set of leaf list values in the builder and traverses back to the
     * holder.
     *
     * @param leafListVal set of values
     * @param leafList    YANG leaf list
     */
    private void addLeafList(Set<String> leafListVal, YangLeafList leafList) {
        extBuilder.addLeafList(leafListVal, leafList);
        extBuilder.traverseToParentWithoutValidation();
    }

    /**
     * Returns the schema node of notification from the root node. Gets the
     * enum value from event object and gives it to the root schema node for
     * getting back the notification schema node.
     *
     * @return YANG schema node of notification
     */
    private YangSchemaNode getSchemaNodeOfNotification() {

        Object eventObjType = getAttributeFromInheritance(rootObj, STR_TYPE);
        String opTypeValue = String.valueOf(eventObjType);

        if (opTypeValue.equals(STR_NULL) || opTypeValue.isEmpty()) {
            throw new YtbException(
                    "There is no notification present for the event. Invalid " +
                            "input for notification.");
        }
        try {
            return rootSchema.getNotificationSchemaNode(opTypeValue);
        } catch (DataModelException e) {
            throw new YtbException(e);
        }
    }

    /**
     * Returns the object of the notification by retrieving the attributes
     * from the event class object.
     *
     * @return notification YANG object
     */
    private Object getObjOfNotification() {

        Object eventSubjectObj =
                getAttributeFromInheritance(rootObj, STR_SUBJECT);
        String notificationName = rootSchema.getJavaAttributeName();
        try {
            return getAttributeOfObject(eventSubjectObj, notificationName);
        } catch (NoSuchMethodException e) {
            throw new YtbException(e);
        }
    }

    /**
     * Returns case object from the map that is bound to the parent node
     * info. For any case node, only when the key and value is matched the
     * object of the case is provided. If a match is not found, null is
     * returned.
     *
     * @param parentNodeInfo parent YTB node info
     * @param caseNode       case schema node
     * @return object of the case node
     */
    private Object getCaseObjectFromChoice(YtbNodeInfo parentNodeInfo,
                                           YangSchemaNode caseNode) {
        String javaName = getCapitalCase(
                caseNode.getJavaClassNameOrBuiltInType());
        String choiceName = ((YangNode) caseNode).getParent().getName();
        Map<String, Object> mapObj = parentNodeInfo.getChoiceCaseMap();
        Object caseObj = mapObj.get(choiceName);
        Class<?> interfaceClass = getInterfaceClassFromImplClass(caseObj);
        return interfaceClass.getSimpleName().equals(javaName) ? caseObj : null;
    }

    /**
     * Adds leaf to YDT when value is present. For primitive types, in order
     * to avoid default values, the value select is set or not is checked and
     * then added.
     *
     * @param holder    leaf holder
     * @param yangLeaf  YANG leaf node
     * @param parentObj leaf holder object
     * @param leafType  object of leaf type
     */
    private void addLeafWithValue(YangSchemaNode holder, YangLeaf yangLeaf,
                                  Object parentObj, Object leafType) {
        String fieldValue = null;
        if (isTypePrimitive(yangLeaf.getDataType())) {
            fieldValue = getLeafValueFromValueSetFlag(holder, parentObj,
                                                      yangLeaf, leafType);
            /*
             * Checks the object is present or not, when type is
             * non-primitive. And adds the value from the respective data type.
             */
        } else if (leafType != null) {
            fieldValue = getStringFromType(holder, parentObj,
                                           getJavaName(yangLeaf), leafType,
                                           yangLeaf.getDataType());
        }

        if (nonEmpty(fieldValue)) {
            boolean isEmpty = isTypeEmpty(fieldValue,
                                          yangLeaf.getDataType());
            if (isEmpty) {
                if (!fieldValue.equals(TRUE)) {
                    return;
                }
                fieldValue = null;
            }
            extBuilder.addLeaf(fieldValue, yangLeaf);
            extBuilder.traverseToParentWithoutValidation();
        }
    }

    /**
     * Returns the value as true if direct or referred type from leafref or
     * derived points to empty data type; false otherwise.
     *
     * @param fieldValue value of the leaf
     * @param dataType   type of the leaf
     * @return true if type is empty; false otherwise.
     */
    private boolean isTypeEmpty(String fieldValue, YangType<?> dataType) {
        if (fieldValue.equals(TRUE) || fieldValue.equals(FALSE)) {
            switch (dataType.getDataType()) {
                case EMPTY:
                    return true;

                case LEAFREF:
                    YangLeafRef leafRef =
                            (YangLeafRef) dataType.getDataTypeExtendedInfo();
                    return isTypeEmpty(fieldValue,
                                       leafRef.getEffectiveDataType());
                case DERIVED:
                    YangDerivedInfo info =
                            (YangDerivedInfo) dataType
                                    .getDataTypeExtendedInfo();
                    YangDataTypes type = info.getEffectiveBuiltInType();
                    return type == EMPTY;

                default:
                    return false;
            }
        }
        return false;
    }

    /**
     * Adds leaf without value, when the select leaf bit is set.
     *
     * @param holder    leaf holder
     * @param yangLeaf  YANG leaf node
     * @param parentObj leaf holder object
     */
    private void addLeafWithoutValue(YangSchemaNode holder, YangLeaf yangLeaf,
                                     Object parentObj) {

        String selectLeaf;
        try {
            selectLeaf = isValueOrSelectLeafSet(holder, parentObj,
                                                getJavaName(yangLeaf),
                                                IS_SELECT_LEAF_SET_METHOD);
        } catch (NoSuchMethodException e) {
            selectLeaf = FALSE;
        }
        if (selectLeaf.equals(TRUE)) {
            extBuilder.addLeaf(null, yangLeaf);
            extBuilder.traverseToParentWithoutValidation();
        }
    }

    /**
     * Returns the value of type, after checking the value leaf flag. If the
     * flag is set, then it takes the value else returns null.
     *
     * @param holder    leaf holder
     * @param parentObj parent object
     * @param yangLeaf  YANG leaf node
     * @param leafType  object of leaf type
     * @return value of type
     */
    private String getLeafValueFromValueSetFlag(YangSchemaNode holder, Object parentObj,
                                                YangLeaf yangLeaf, Object leafType) {

        String valueOfLeaf;
        try {
            valueOfLeaf = isValueOrSelectLeafSet(holder, parentObj,
                                                 getJavaName(yangLeaf),
                                                 IS_LEAF_VALUE_SET_METHOD);
        } catch (NoSuchMethodException e) {
            throw new YtbException(e);
        }
        if (valueOfLeaf.equals(TRUE)) {
            return getStringFromType(holder, parentObj,
                                     getJavaName(yangLeaf), leafType,
                                     yangLeaf.getDataType());
        }
        return null;
    }

    /**
     * Returns the node info which can be processed, by eliminating the nodes
     * which need not to be processed at normal conditions such as RPC,
     * notification and augment.
     *
     * @param curNode current node
     * @return info of node which needs processing
     */
    private YtbTraversalInfo getProcessableInfo(YangNode curNode) {
        if (curNode.getNextSibling() != null) {
            YangNode sibling = curNode.getNextSibling();
            while (isNonProcessableNode(sibling)) {
                sibling = sibling.getNextSibling();
            }
            if (sibling != null) {
                return new YtbTraversalInfo(sibling, SIBLING);
            }
        }
        return new YtbTraversalInfo(curNode.getParent(), PARENT);
    }

}
