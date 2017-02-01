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

package org.onosproject.yms.app.yab;

import org.onosproject.yangutils.datamodel.YangAugment;
import org.onosproject.yangutils.datamodel.YangAugmentableNode;
import org.onosproject.yangutils.datamodel.YangInput;
import org.onosproject.yangutils.datamodel.YangModule;
import org.onosproject.yangutils.datamodel.YangNode;
import org.onosproject.yangutils.datamodel.YangRpc;
import org.onosproject.yangutils.datamodel.YangSchemaNode;
import org.onosproject.yms.app.utils.TraversalType;
import org.onosproject.yms.app.yab.exceptions.YabException;
import org.onosproject.yms.app.ydt.DefaultYdtAppContext;
import org.onosproject.yms.app.ydt.YangRequestWorkBench;
import org.onosproject.yms.app.ydt.YangResponseWorkBench;
import org.onosproject.yms.app.ydt.YdtAppContext;
import org.onosproject.yms.app.ydt.YdtExtendedContext;
import org.onosproject.yms.app.ydt.YdtMultiInstanceNode;
import org.onosproject.yms.app.ydt.YdtNode;
import org.onosproject.yms.app.yob.DefaultYobBuilder;
import org.onosproject.yms.app.ysr.YangSchemaRegistry;
import org.onosproject.yms.app.ytb.DefaultYangTreeBuilder;
import org.onosproject.yms.ydt.YdtBuilder;
import org.onosproject.yms.ydt.YdtContext;
import org.onosproject.yms.ydt.YdtResponse;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.yms.app.utils.TraversalType.CHILD;
import static org.onosproject.yms.app.utils.TraversalType.PARENT;
import static org.onosproject.yms.app.utils.TraversalType.ROOT;
import static org.onosproject.yms.app.utils.TraversalType.SIBLING;
import static org.onosproject.yms.app.ydt.AppNodeFactory.getAppContext;
import static org.onosproject.yms.app.ydt.YdtAppNodeOperationType.DELETE_ONLY;
import static org.onosproject.yms.app.ydt.YdtAppNodeOperationType.OTHER_EDIT;
import static org.onosproject.yms.ydt.YdtContextOperationType.DELETE;
import static org.onosproject.yms.ydt.YmsOperationExecutionStatus.EXECUTION_SUCCESS;

/**
 * Represents YANG application broker. It acts as a broker between Protocol and
 * YANG based application.
 */
public class YangApplicationBroker {

    private static final String GET = "get";
    private static final String SET = "set";
    private static final String AUGMENTED = "Augmented";
    private static final String VOID = "void";
    private final YangSchemaRegistry schemaRegistry;
    private Set<String> augGenMethodSet;

    /**
     * Creates a new YANG application broker.
     *
     * @param schemaRegistry YANG schema registry
     */
    public YangApplicationBroker(YangSchemaRegistry schemaRegistry) {
        this.schemaRegistry = schemaRegistry;
    }

    /**
     * Processes query request of a NBI protocol.
     *
     * @param ydtWorkBench YANG request work bench
     * @return YANG response data tree node context
     * @throws YabException violation in execution of YAB
     */
    public YdtResponse processQuery(YdtBuilder ydtWorkBench)
            throws YabException {
        List<Object> responseObjects = new LinkedList<>();
        YangRequestWorkBench workBench = (YangRequestWorkBench) ydtWorkBench;
        augGenMethodSet = ((YangRequestWorkBench) ydtWorkBench).getAugGenMethodSet();

        for (YdtAppContext appContext = workBench.getAppRootNode().getFirstChild();
             appContext != null; appContext = appContext.getNextSibling()) {
            Object responseObject = processQueryOfApplication(appContext);
            if (responseObject != null) {
                responseObjects.add(responseObject);
            }
        }

        YdtContext rootYdtContext = workBench.getRootNode();
        YdtBuilder responseYdt = buildResponseYdt(responseObjects,
                                                  rootYdtContext.getName(),
                                                  rootYdtContext.getNamespace());

        return new YangResponseWorkBench(responseYdt.getRootNode(),
                                         EXECUTION_SUCCESS,
                                         ydtWorkBench.getYmsOperationType());
    }

    /**
     * Processes edit request of a NBI protocol.
     *
     * @param ydtWorkBench YANG request work bench
     * @return YANG response data tree node context
     * @throws YabException               violation in execution of YAB
     * @throws CloneNotSupportedException clone is not supported
     */
    public YdtResponse processEdit(YdtBuilder ydtWorkBench)
            throws CloneNotSupportedException, YabException {
        YangRequestWorkBench workBench = (YangRequestWorkBench) ydtWorkBench;
        augGenMethodSet = ((YangRequestWorkBench) ydtWorkBench).getAugGenMethodSet();
        for (YdtAppContext appContext = workBench.getAppRootNode().getFirstChild();
             appContext != null; appContext = appContext.getNextSibling()) {
            processEditOfApplication(appContext);
        }

        /*
         * Since for set operation return type is void, there will not be
         * response ydt tree so returning null.
         */
        return new YangResponseWorkBench(null, EXECUTION_SUCCESS,
                                         workBench.getYmsOperationType());
    }

    /**
     * Processes operation request of a NBI protocol.
     *
     * @param ydtWorkBench YANG request work bench
     * @return YANG response data tree node context
     * @throws YabException violation in execution of YAB
     */
    public YdtResponse processOperation(YdtBuilder ydtWorkBench)
            throws YabException {
        YangRequestWorkBench workBench = (YangRequestWorkBench) ydtWorkBench;
        YdtAppContext appContext = workBench.getAppRootNode().getFirstChild();
        YdtContext ydtNode = appContext.getModuleContext();
        while (ydtNode != null) {
            YdtContext childYdtNode = ydtNode.getFirstChild();
            YangSchemaNode yangNode = ((YdtNode) childYdtNode).getYangSchemaNode();
            if (yangNode instanceof YangRpc) {
                return processRpcOperationOfApplication(childYdtNode,
                                                        appContext, yangNode,
                                                        workBench);
            }
            ydtNode = ydtNode.getNextSibling();
        }
        return new YangResponseWorkBench(null, EXECUTION_SUCCESS,
                                         ydtWorkBench.getYmsOperationType());
    }

    /**
     * Processes rpc request of an application.
     *
     * @param appContext application context
     * @return response object from application
     */
    private YdtResponse processRpcOperationOfApplication(YdtContext rpcYdt,
                                                         YdtAppContext appContext,
                                                         YangSchemaNode yangRpc,
                                                         YangRequestWorkBench workBench)
            throws YabException {
        Object inputObject = null;
        YdtContext inputYdtNode = getInputYdtNode(rpcYdt);
        if (inputYdtNode != null) {
            inputObject = getYangObject(inputYdtNode);
        }

        Object appObject = getApplicationObjectForRpc(appContext);

        String methodName = yangRpc.getJavaClassNameOrBuiltInType();
        Object outputObject = invokeRpcApplicationsMethod(appObject,
                                                          inputObject,
                                                          methodName);

        String returnType = getReturnTypeOfRpcResponse(appObject,
                                                       inputObject, yangRpc);

        if (!returnType.equals(VOID)) {
            YdtBuilder responseYdt = buildRpcResponseYdt(outputObject,
                                                         workBench);
            return new YangResponseWorkBench(responseYdt.getRootNode(),
                                             EXECUTION_SUCCESS,
                                             workBench.getYmsOperationType());
        }

        return new YangResponseWorkBench(null, EXECUTION_SUCCESS,
                                         workBench.getYmsOperationType());
    }

    /**
     * Processes query request of an application.
     *
     * @param appContext application context
     * @return response object from application
     */
    private Object processQueryOfApplication(YdtAppContext appContext)
            throws YabException {
        YdtContext ydtNode = appContext.getModuleContext();

        // Update application context tree if any node is augmented
        YangNode yangNode = (YangNode) appContext.getYangSchemaNode();
        if (yangNode.isDescendantNodeAugmented()) {
            processAugmentForChildNode(appContext, yangNode);
        }

        String appName = getCapitalCase(((YdtNode) appContext.getModuleContext())
                                                .getYangSchemaNode()
                                                .getJavaClassNameOrBuiltInType());

        // get YangObject of YdtContext from YOB
        Object outputObject = getYangObject(ydtNode);

        TraversalType curTraversal = ROOT;
        do {
            if (curTraversal != PARENT) {

                // find application and get application's object using YSR
                Object appManagerObject = getApplicationObject(appContext);

                // find which method to invoke
                String methodName = getApplicationMethodName(appContext,
                                                             appName, GET);

                String moduleName = appContext.getAppData()
                        .getRootSchemaNode().getName();

                // invoke application's getter method
                outputObject = invokeApplicationsMethod(appManagerObject,
                                                        outputObject,
                                                        methodName, moduleName);
            }

            /*
             * AppContext may contain other nodes if it is augmented, so
             * traverse the appContext tree
             */
            if (curTraversal != PARENT && appContext.getFirstChild() != null) {
                curTraversal = CHILD;
                appContext = appContext.getFirstChild();
            } else if (appContext.getNextSibling() != null) {
                curTraversal = SIBLING;
                appContext = appContext.getNextSibling();
            } else {
                curTraversal = PARENT;
                if (appContext.getParent().getParent() != null) {
                    appContext = appContext.getParent();
                }
            }
            // no need to do any operation for logical root node
        } while (appContext.getParent().getParent() != null);
        return outputObject;
    }

    /**
     * Processes edit request of an application.
     *
     * @param appContext application context
     * @throws YabException               violation in execution of YAB
     * @throws CloneNotSupportedException clone is not supported
     */
    private void processEditOfApplication(YdtAppContext appContext)
            throws CloneNotSupportedException, YabException {

        // process delete request if operation type is delete and both
        if (appContext.getOperationType() != OTHER_EDIT) {
            processDeleteRequestOfApplication(appContext);
        }

        // process edit request if operation type is other edit and both
        if (appContext.getOperationType() != DELETE_ONLY) {
            YdtContext ydtNode = appContext.getModuleContext();

            String appName = getCapitalCase(((YdtNode) appContext.getModuleContext())
                                                    .getYangSchemaNode()
                                                    .getJavaClassNameOrBuiltInType());

            // get YO from YOB
            Object outputObject = getYangObject(ydtNode);

            TraversalType curTraversal = ROOT;
            do {
                if (curTraversal != PARENT) {

                    // find application and get application's object using YSR
                    Object appManagerObject = getApplicationObject(appContext);

                    // find which method to invoke
                    String methodName = getApplicationMethodName(appContext,
                                                                 appName, SET);

                    String moduleName = appContext.getAppData()
                            .getRootSchemaNode().getName();

                    // invoke application's setter method
                    invokeApplicationsMethod(appManagerObject, outputObject,
                                             methodName, moduleName);
                }

                /*
                 * AppContext may contain other nodes if it is augmented,
                 * so traverse the appContext tree
                 */
                if (curTraversal != PARENT && appContext.getFirstChild() != null) {
                    curTraversal = CHILD;
                    appContext = appContext.getFirstChild();
                } else if (appContext.getNextSibling() != null) {
                    curTraversal = SIBLING;
                    appContext = appContext.getNextSibling();
                } else {
                    curTraversal = PARENT;
                    if (appContext.getParent().getParent() != null) {
                        appContext = appContext.getParent();
                    }
                }
                // no need to do any operation for logical root node
            } while (appContext.getParent().getParent() != null);
        }
    }

    /**
     * Processes delete request of an application.
     *
     * @param appContext application context
     * @throws YabException               violation in execution of YAB
     * @throws CloneNotSupportedException clone is not supported
     */
    private void processDeleteRequestOfApplication(YdtAppContext appContext)
            throws CloneNotSupportedException, YabException {
        TraversalType curTraversal = ROOT;
        List<YdtContext> deleteNodes = appContext.getDeleteNodes();

        if (deleteNodes != null && !deleteNodes.isEmpty()) {

            /*
             * Split the current Ydt tree into two trees.
             * Delete Tree with all nodes with delete operation and other
             * tree with other edit operation
             */
            YdtContext deleteTree = buildDeleteTree(deleteNodes);

            /*
             * If any of nodes in ydt delete tree is augmented then add
             * augmented nodes to current ydt tree
             */
            processAugmentedNodesForDelete(deleteTree.getFirstChild(), appContext);

            Object inputObject = getYangObject(deleteTree.getFirstChild());

            String appName = getCapitalCase(((YdtNode) appContext.getModuleContext())
                                                    .getYangSchemaNode()
                                                    .getJavaClassNameOrBuiltInType());

            do {
                if (curTraversal == ROOT || curTraversal == SIBLING) {
                    while (appContext.getLastChild() != null) {
                        appContext = appContext.getLastChild();
                    }
                }

                // getAugmentApplication manager object
                Object appManagerObject = getApplicationObject(appContext);

                // find which method to invoke
                String methodName = getApplicationMethodName(appContext,
                                                             appName, SET);

                String moduleName = appContext.getAppData().getRootSchemaNode()
                        .getName();

                // invoke application's setter method
                invokeApplicationsMethod(appManagerObject, inputObject,
                                         methodName, moduleName);

                if (appContext.getPreviousSibling() != null) {
                    curTraversal = SIBLING;
                    appContext = appContext.getPreviousSibling();
                } else if (appContext.getParent() != null) {
                    curTraversal = PARENT;
                    appContext = appContext.getParent();
                }
            } while (appContext.getParent() != null);
        }
    }

    /**
     * Traverses data model tree and if any node is augmented, then
     * adds child to current application context.
     *
     * @param curAppContext current application context
     * @param schemaNode    YANG data model node, either module or augment
     */
    protected void processAugmentForChildNode(YdtAppContext curAppContext,
                                              YangNode schemaNode) {
        YangNode yangNode = schemaNode.getChild();
        if (yangNode == null) {
            return;
        }

        TraversalType curTraversal = CHILD;
        while (!yangNode.equals(schemaNode)) {
            if (curTraversal != PARENT && yangNode instanceof YangAugmentableNode
                    && !((YangAugmentableNode) yangNode).getAugmentedInfoList()
                    .isEmpty()) {
                updateAppTreeWithAugmentNodes(yangNode, curAppContext);
            }

            if (curTraversal != PARENT && yangNode.getChild() != null
                    && yangNode.isDescendantNodeAugmented()) {
                curTraversal = CHILD;
                yangNode = yangNode.getChild();
            } else if (yangNode.getNextSibling() != null) {
                curTraversal = SIBLING;
                yangNode = yangNode.getNextSibling();
            } else {
                curTraversal = PARENT;
                yangNode = yangNode.getParent();
            }
        }
    }

    /**
     * Traverses YDT delete tree and if any YDT node is augmented then
     * updates the YDT delete tree with augment nodes.
     *
     * @param deleteTree YDT delete tree
     * @param appContext application context
     */
    protected void processAugmentedNodesForDelete(YdtContext deleteTree,
                                                  YdtAppContext appContext) {
        TraversalType curTraversal = ROOT;
        YdtContext ydtContext = deleteTree.getFirstChild();

        if (ydtContext == null) {
            /*
             * Delete request is for module, so check all the nodes under
             * module whether it is augmented.
             */
            YangNode yangNode = ((YangNode) ((YdtNode) deleteTree)
                    .getYangSchemaNode());
            if (yangNode.isDescendantNodeAugmented()) {
                processAugmentForChildNode(appContext, yangNode);
            }
            return;
        }

        while (!ydtContext.equals(deleteTree)) {
            if (curTraversal != PARENT && ((YdtNode) ydtContext)
                    .getYdtContextOperationType() == DELETE) {
                YangNode yangNode = ((YangNode) ((YdtNode) ydtContext)
                        .getYangSchemaNode());
                if (yangNode instanceof YangAugmentableNode) {
                    updateAppTreeWithAugmentNodes(yangNode, appContext);
                }
                if (yangNode.isDescendantNodeAugmented()) {
                    processAugmentForChildNode(appContext, yangNode);
                }
            }

            if (curTraversal != PARENT && ydtContext.getFirstChild() != null) {
                curTraversal = CHILD;
                ydtContext = ydtContext.getFirstChild();
            } else if (ydtContext.getNextSibling() != null) {
                curTraversal = SIBLING;
                ydtContext = ydtContext.getNextSibling();
            } else {
                curTraversal = PARENT;
                ydtContext = ydtContext.getParent();
            }
        }
    }

    /**
     * Returns response YANG data tree using YTB.
     *
     * @param responseObjects list of application's response objects
     * @param name            application YANG name
     * @param namespace       application YANG namespace
     * @return response YANG data tree
     */
    private YdtBuilder buildResponseYdt(List<Object> responseObjects,
                                        String name, String namespace) {
        DefaultYangTreeBuilder treeBuilder = new DefaultYangTreeBuilder();
        return treeBuilder.getYdtBuilderForYo(responseObjects,
                                              name, namespace, null, schemaRegistry);
    }

    private YdtBuilder buildRpcResponseYdt(Object responseObject,
                                           YangRequestWorkBench requestWorkBench) {
        DefaultYangTreeBuilder treeBuilder = new DefaultYangTreeBuilder();
        return treeBuilder.getYdtForRpcResponse(responseObject, requestWorkBench);
    }

    /**
     * Builds delete tree for list of delete nodes.
     *
     * @param deleteNodes list of delete nodes
     * @return deleteTree YANG data tree for delete operation
     * @throws CloneNotSupportedException clone is not supported
     */
    protected YdtContext buildDeleteTree(List<YdtContext> deleteNodes) throws
            CloneNotSupportedException {
        Iterator<YdtContext> iterator = deleteNodes.iterator();
        YdtContext deleteTree = null;
        while (iterator.hasNext()) {
            YdtContext deleteNode = iterator.next();
            if (((YdtExtendedContext) deleteNode.getParent())
                    .getYdtContextOperationType() != DELETE) {
                cloneAncestorsOfDeleteNode(deleteNode);
                deleteTree = unlinkDeleteNodeFromCurrentTree((YdtNode) deleteNode);
            }
        }

        if (deleteTree != null) {
            while (deleteTree.getParent() != null) {
                deleteTree = deleteTree.getParent();
            }
        }
        return deleteTree;
    }

    /**
     * Clones ancestor nodes of delete node.
     *
     * @param deleteNode node to be deleted
     * @throws CloneNotSupportedException clone not supported
     */
    private void cloneAncestorsOfDeleteNode(YdtContext deleteNode)
            throws CloneNotSupportedException {
        YdtNode clonedNode;
        YdtNode previousNode = null;

        // Clone the parents of delete node to form delete tree
        YdtNode nodeToClone = (YdtNode) deleteNode.getParent();
        while (nodeToClone != null) {
            // If node is not cloned yet
            if (nodeToClone.getClonedNode() == null) {
                clonedNode = nodeToClone.clone();
                unlinkCurrentYdtNode(clonedNode);
                if (nodeToClone instanceof YdtMultiInstanceNode) {
                    addKeyLeavesToClonedNode(nodeToClone, clonedNode);
                }
                nodeToClone.setClonedNode(clonedNode);
            } else {
                // already node is cloned
                clonedNode = (YdtNode) nodeToClone.getClonedNode();
            }

            if (previousNode != null) {
                /*
                 * add previous cloned node as child of current cloned node
                 * so that tree will be formed from delete node parent to
                 * logical root node.
                 */
                clonedNode.addChild(previousNode, false);
            }
            previousNode = clonedNode;
            nodeToClone = nodeToClone.getParent();
        }
    }

    /**
     * Unlinks delete node from current YANG data tree of application
     * and links it to cloned delete tree.
     *
     * @param deleteNode node to be unlinked
     * @return deleteNode delete node linked to cloned delete tree
     */
    private YdtNode unlinkDeleteNodeFromCurrentTree(YdtNode deleteNode) {
        YdtNode parentClonedNode = (YdtNode) deleteNode.getParent().getClonedNode();
        unlinkNodeFromParent(deleteNode);
        unlinkNodeFromSibling(deleteNode);

        /*
         * Set all the pointers of node to null before adding as child
         * to parent's cloned node.
         */
        deleteNode.setParent(null);
        deleteNode.setPreviousSibling(null);
        deleteNode.setNextSibling(null);

        parentClonedNode.addChild(deleteNode, false);
        return deleteNode;
    }

    /**
     * Adds key leaf nodes to cloned YDT node from current Ydt node.
     *
     * @param curNode    current YDT node
     * @param clonedNode cloned YDT node
     */
    private void addKeyLeavesToClonedNode(YdtNode curNode, YdtNode clonedNode)
            throws CloneNotSupportedException {
        YdtNode keyClonedLeaf;
        List<YdtContext> keyList = ((YdtMultiInstanceNode) curNode)
                .getKeyNodeList();
        if (keyList != null && !keyList.isEmpty()) {
            for (YdtContext keyLeaf : keyList) {
                keyClonedLeaf = ((YdtNode) keyLeaf).clone();
                unlinkCurrentYdtNode(keyClonedLeaf);
                clonedNode.addChild(keyClonedLeaf, true);
            }
        }
    }

    /**
     * Updates application context tree if any of the nodes in current
     * application context tree is augmented.
     *
     * @param yangNode      YANG schema node which is augmented
     * @param curAppContext current application context tree
     */
    private void updateAppTreeWithAugmentNodes(YangNode yangNode,
                                               YdtAppContext curAppContext) {
        YdtAppContext childAppContext;
        for (YangAugment yangAugment : ((YangAugmentableNode) yangNode)
                .getAugmentedInfoList()) {
            Object appManagerObject = schemaRegistry
                    .getRegisteredApplication(yangAugment.getParent());
            if (appManagerObject != null
                    && augGenMethodSet.add(yangAugment.getSetterMethodName())) {
                childAppContext = addChildToYdtAppTree(curAppContext,
                                                       yangAugment);
                processAugmentForChildNode(childAppContext, yangAugment);
            }
        }
    }

    /**
     * Adds child node to current application context tree.
     *
     * @param curAppContext current application context
     * @param augment       augment data model node
     * @return childAppContext child node added
     */
    private YdtAppContext addChildToYdtAppTree(YdtAppContext curAppContext,
                                               YangNode augment) {
        DefaultYdtAppContext childAppContext = getAppContext(true);
        childAppContext.setParent(curAppContext);
        childAppContext.setOperationType(curAppContext.getOperationType());
        childAppContext.setAugmentingSchemaNode(augment);
        curAppContext.addChild(childAppContext);
        return childAppContext;
    }

    /**
     * Unlinks the current node from its parent.
     *
     * @param deleteNode node which should be unlinked from YDT tree
     */
    private void unlinkNodeFromParent(YdtNode deleteNode) {
        YdtNode parentNode = deleteNode.getParent();
        if (parentNode.getFirstChild().equals(deleteNode)
                && parentNode.getLastChild().equals(deleteNode)) {
            parentNode.setChild(null);
            parentNode.setLastChild(null);
        } else if (parentNode.getFirstChild().equals(deleteNode)) {
            parentNode.setChild(deleteNode.getNextSibling());
        } else if (parentNode.getLastChild().equals(deleteNode)) {
            parentNode.setLastChild(deleteNode.getPreviousSibling());
        }
    }

    /**
     * Unlinks the current node from its sibling.
     *
     * @param deleteNode node which should be unlinked from YDT tree
     */
    private void unlinkNodeFromSibling(YdtNode deleteNode) {
        YdtNode previousSibling = deleteNode.getPreviousSibling();
        YdtNode nextSibling = deleteNode.getNextSibling();
        if (nextSibling != null && previousSibling != null) {
            previousSibling.setNextSibling(nextSibling);
            nextSibling.setPreviousSibling(previousSibling);
        } else if (nextSibling != null) {
            nextSibling.setPreviousSibling(null);
        } else if (previousSibling != null) {
            previousSibling.setNextSibling(null);
        }
    }

    /**
     * Unlinks current Ydt node from parent, sibling and child.
     *
     * @param ydtNode YANG data tree node
     */
    private void unlinkCurrentYdtNode(YdtNode ydtNode) {
        ydtNode.setParent(null);
        ydtNode.setNextSibling(null);
        ydtNode.setPreviousSibling(null);
        ydtNode.setChild(null);
        ydtNode.setLastChild(null);
    }

    /**
     * Returns YANG object for YDT node.
     *
     * @param ydtNode YANG data node
     * @return YANG object for YDT node
     */
    private Object getYangObject(YdtContext ydtNode) {
        checkNotNull(ydtNode);
        DefaultYobBuilder yobBuilder = new DefaultYobBuilder();
        return yobBuilder.getYangObject((YdtExtendedContext) ydtNode,
                                        schemaRegistry);
    }

    /**
     * Returns application manager object for YDT node.
     *
     * @param appContext YDT application context
     * @return application manager object
     */
    private Object getApplicationObjectForRpc(YdtAppContext appContext) {
        checkNotNull(appContext);
        while (appContext.getFirstChild() != null) {
            appContext = appContext.getFirstChild();
        }
        return schemaRegistry.getRegisteredApplication(appContext.getAppData()
                                                               .getRootSchemaNode());
    }

    /**
     * Returns application manager object of application.
     *
     * @param appContext application context
     * @return application manager object
     */
    private Object getApplicationObject(YdtAppContext appContext) {
        return schemaRegistry.getRegisteredApplication(appContext.getAppData()
                                                               .getRootSchemaNode());
    }

    /**
     * Converts name to capital case.
     *
     * @param yangIdentifier identifier
     * @return name to capital case
     */
    private String getCapitalCase(String yangIdentifier) {
        return yangIdentifier.substring(0, 1).toUpperCase() +
                yangIdentifier.substring(1);
    }

    /**
     * Returns get/set method name for application's request.
     *
     * @param appContext application context
     * @return get/set method name for application's query request
     */
    private String getApplicationMethodName(YdtAppContext appContext,
                                            String appName,
                                            String operation) {
        if (appContext.getYangSchemaNode() instanceof YangModule) {
            return operation + appName;
        }

        String augment = ((YangAugment) appContext
                .getAugmentingSchemaNode()).getTargetNode().get(0)
                .getResolvedNode().getJavaClassNameOrBuiltInType();
        return new StringBuilder().append(operation).append(AUGMENTED)
                .append(appName).append(getCapitalCase(augment)).toString();
    }

    /**
     * Returns rpc's input schema node.
     *
     * @param rpcNode rpc schema node
     * @return rpc's input YDT node
     */
    private YdtContext getInputYdtNode(YdtContext rpcNode) {
        YdtContext inputNode = rpcNode.getFirstChild();
        while (inputNode != null) {
            YangSchemaNode yangInputNode = ((YdtNode) inputNode)
                    .getYangSchemaNode();
            if (yangInputNode instanceof YangInput) {
                return inputNode;
            }
            inputNode = rpcNode.getNextSibling();
        }
        return null;
    }

    /**
     * Invokes application method for RPC request.
     *
     * @param appManagerObject application manager object
     * @param inputObject      input parameter object of method
     * @param methodName       method name which should be invoked
     * @return response object from application
     * @throws YabException violation in execution of YAB
     */
    private Object invokeApplicationsMethod(Object appManagerObject,
                                            Object inputObject,
                                            String methodName, String appName)
            throws YabException {
        checkNotNull(appManagerObject);
        Class<?> appClass = appManagerObject.getClass();
        try {
            Method methodObject = appClass.getDeclaredMethod(methodName,
                                                             inputObject.getClass());
            if (methodObject != null) {
                return methodObject.invoke(appManagerObject, inputObject);
            }
            throw new YabException("No such method in application");
        } catch (IllegalAccessException | NoSuchMethodException e) {
            throw new YabException(e);
        } catch (InvocationTargetException e) {
            throw new YabException("Invocation exception in service " + appName,
                                   e.getCause());
        }
    }

    /**
     * Invokes application method for RPC request.
     *
     * @param appObject   application manager object
     * @param inputObject input parameter object of method
     * @param yangNode    method name which should be invoked
     * @return response object from application
     * @throws YabException violation in execution of YAB
     */
    private String getReturnTypeOfRpcResponse(Object appObject,
                                              Object inputObject, YangSchemaNode
                                                      yangNode) throws YabException {
        Method methodObject = null;
        try {
            if (inputObject == null) {
                methodObject = appObject.getClass()
                        .getDeclaredMethod(yangNode.getJavaClassNameOrBuiltInType(),
                                           null);
            } else {
                methodObject = appObject.getClass()
                        .getDeclaredMethod(yangNode.getJavaClassNameOrBuiltInType(),
                                           inputObject.getClass().getInterfaces());
            }
            if (methodObject != null) {
                return methodObject.getReturnType().getSimpleName();
            }
            throw new YabException("No such method in application");
        } catch (NoSuchMethodException e) {
            throw new YabException(e);
        }
    }

    /**
     * Invokes application method for RPC request.
     *
     * @param appManagerObject application manager object
     * @param inputParamObject input parameter object of method
     * @param methodName       method name which should be invoked
     * @return response object from application
     * @throws YabException violation in execution of YAB
     */
    private Object invokeRpcApplicationsMethod(Object appManagerObject,
                                               Object inputParamObject,
                                               String methodName) throws YabException {
        checkNotNull(appManagerObject);
        Class<?> appClass = appManagerObject.getClass();
        try {
            Method methodObject;
            if (inputParamObject == null) {
                methodObject = appClass.getDeclaredMethod(methodName, null);
                if (methodObject != null) {
                    return methodObject.invoke(appManagerObject);
                }
            } else {
                methodObject = appClass.getDeclaredMethod(methodName,
                                                          inputParamObject
                                                                  .getClass()
                                                                  .getInterfaces());
                if (methodObject != null) {
                    return methodObject.invoke(appManagerObject, inputParamObject);
                }
            }
            throw new YabException("No such method in application");
        } catch (IllegalAccessException | NoSuchMethodException |
                InvocationTargetException e) {
            throw new YabException(e);
        }
    }

    /**
     * Sets the augment setter method name.
     *
     * @param augGenMethodSet augment setter method name
     */
    public void setAugGenMethodSet(Set<String> augGenMethodSet) {
        this.augGenMethodSet = augGenMethodSet;
    }
}
