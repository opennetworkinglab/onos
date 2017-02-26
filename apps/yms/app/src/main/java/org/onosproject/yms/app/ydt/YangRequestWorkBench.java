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

import com.google.common.collect.ImmutableMap;
import org.onosproject.yangutils.datamodel.YangAugment;
import org.onosproject.yangutils.datamodel.YangLeaf;
import org.onosproject.yangutils.datamodel.YangList;
import org.onosproject.yangutils.datamodel.YangSchemaNode;
import org.onosproject.yangutils.datamodel.YangSchemaNodeContextInfo;
import org.onosproject.yangutils.datamodel.YangSchemaNodeIdentifier;
import org.onosproject.yms.app.ydt.exceptions.YdtException;
import org.onosproject.yms.app.ysr.YangSchemaRegistry;
import org.onosproject.yms.ydt.YdtContext;
import org.onosproject.yms.ydt.YdtContextOperationType;
import org.onosproject.yms.ydt.YdtType;
import org.onosproject.yms.ydt.YmsOperationType;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.onosproject.yms.app.ydt.AppNodeFactory.getAppContext;
import static org.onosproject.yms.app.ydt.RequestedCallType.LEAF;
import static org.onosproject.yms.app.ydt.RequestedCallType.NON_LEAF;
import static org.onosproject.yms.app.ydt.RequestedCardinality.MULTI_INSTANCE;
import static org.onosproject.yms.app.ydt.RequestedCardinality.MULTI_INSTANCE_LEAF;
import static org.onosproject.yms.app.ydt.RequestedCardinality.SINGLE_INSTANCE;
import static org.onosproject.yms.app.ydt.RequestedCardinality.UNKNOWN;
import static org.onosproject.yms.app.ydt.YdtConstants.errorMsg;
import static org.onosproject.yms.app.ydt.YdtNodeFactory.getNode;
import static org.onosproject.yms.app.ydt.YdtNodeFactory.getYangSchemaNodeTypeSpecificContext;
import static org.onosproject.yms.app.ydt.YdtUtils.checkElementCount;
import static org.onosproject.yms.app.ydt.YdtUtils.freeRestResources;
import static org.onosproject.yms.app.ydt.YdtUtils.getAppOpTypeFromYdtOpType;
import static org.onosproject.yms.app.ydt.YdtUtils.getAugmentingSchemaNode;
import static org.onosproject.yms.app.ydt.YdtUtils.getNodeIdentifier;
import static org.onosproject.yms.app.ydt.YdtUtils.getValidOpType;
import static org.onosproject.yms.ydt.YdtContextOperationType.DELETE;
import static org.onosproject.yms.ydt.YdtContextOperationType.REMOVE;
import static org.onosproject.yms.ydt.YdtType.MULTI_INSTANCE_LEAF_VALUE_NODE;
import static org.onosproject.yms.ydt.YdtType.MULTI_INSTANCE_NODE;

/**
 * Represents YANG request work bench which contains all parameters for
 * request handling and methods to build and obtain YANG data tree
 * which is data (sub)instance representation, abstract of protocol.
 */
public class YangRequestWorkBench implements YdtExtendedBuilder {

    // Ydt formatted error string
    private static final String FMT_NOT_EXIST =
            "Application with name \"%s\" doesn't exist.";

    // Ydt error strings.
    private static final String E_USE_ADD_LEAF =
            "Requested Node should be created using addLeaf interface.";

    private static final String E_INVOKE_PARENT =
            "Can't invoke get parent at logical root node.";

    /*
     * Reference for the current context node in YANG data tree.
     */
    private YdtNode curNode;

    /*
     * Reference for the logical root node in YANG data tree.
     */
    private YdtNode rootNode;

    /*
     * Reference for the current context in ydt application tree.
     */
    private YdtAppContext appCurNode;

    /*
     * Reference for the logical root node context in ydt application tree.
     */
    private YdtAppContext appRootNode;

    /**
     * Root Node Tag attribute in YANG data tree, kept to maintain the root
     * tag attributes in YDT.
     * <p>
     * First key param of map represent tagName  name of tag attribute.
     * Second param of map represent tagValue value of tag attribute
     */
    private Map<String, String> rootTagAttributeMap;

    /*
     * YANG schema registry reference.
     */
    private YangSchemaRegistry registry = null;

    /*
     * YMS operation type.
     */
    private final YmsOperationType ymsOperationType;

    /*
     * YDT default operation type.
     */
    private YdtContextOperationType ydtDefaultOpType;

    /*
     * Flag to identify data validation need to be done by YDT or not.
     */
    private boolean validate = false;
    // TODO validate need to be handle later with interaction type basis in
    // future when it will be supported

    /*
     * Reference for application tree node set.
     * This set contains the method name's generated for an augmented
     * target node to avoid the duplicate entries in YDT application tree for
     * multiple augmented nodes under a single XPATH.
     */
    private Set<String> augGenMethodSet;

    /**
     * Creates an instance of YANG request work bench which is use to initialize
     * logical rootNode and and schema registry.
     *
     * @param name       name of logical container of a protocol
     *                   which is a holder of the complete tree
     * @param namespace  namespace of logical container
     * @param opType     type of operation done by using YANG
     *                   interface
     * @param reg        Yang schema registry
     * @param isValidate Flag to identify data validation need to be
     *                   done by YDT or not
     */
    public YangRequestWorkBench(String name, String namespace,
                                YmsOperationType opType,
                                YangSchemaRegistry reg,
                                boolean isValidate) {

        setRootNode(new YdtLogicalNode(name, namespace));
        registry = reg;
        ymsOperationType = opType;
        validate = isValidate;

        setAppRootNode(getAppContext(true));
    }

    /**
     * Sets the logical root node for ydt.
     *
     * @param node ydt logical root node
     */
    private void setRootNode(YdtNode node) {
        rootNode = node;
        curNode = node;
    }

    /**
     * Sets the logical root node for ydt application tree.
     *
     * @param node ydt application context logical root node
     */
    private void setAppRootNode(YdtAppContext node) {
        appRootNode = node;
        appCurNode = node;
    }

    /**
     * Returns the YANG schema registry of Ydt.
     * This method will be used by ytb.
     *
     * @return YANG schema registry
     */
    public YangSchemaRegistry getYangSchemaRegistry() {
        return registry;
    }

    /**
     * Returns the ydt app context tree logical root node.
     * This method will be used by yab and ytb.
     *
     * @return YdtAppContext app tree logical root node
     */
    public YdtAppContext getAppRootNode() {
        return appRootNode;
    }

    /**
     * Returns the ydt module node with requested node identifier.
     *
     * @param id module/application node identifier
     * @return YANG data tree node
     * @throws YdtException when user requested node schema doesn't exist or
     *                      requested node is already part of the tree
     */
    private YdtNode moduleHandler(YangSchemaNodeIdentifier id)
            throws YdtException {

        YangSchemaNode node =
                registry.getYangSchemaNodeUsingSchemaName(id.getName());

        String namespace = id.getNameSpace().getModuleNamespace();

        /*
         * Checking received schema node is having same namespace as
         * requested by user or not.
         */
        if (node == null || namespace != null &&
                !namespace.equals(node.getYangSchemaNodeIdentifier()
                                          .getNameSpace()
                                          .getModuleNamespace())) {
            throw new YdtException(errorMsg(FMT_NOT_EXIST, id.getName()));
        }

        /*
         * If yms operation is for query then no validation need to be
         * performed.
         */
        if (ymsOperationType != YmsOperationType.QUERY_REQUEST) {
            // Checking whether module node is already exits in YDT or not.
            try {
                curNode.getCollidingChild(id);
            } catch (YdtException e) {
                throw new YdtException(e.getLocalizedMessage());
            }
        }

        YdtNode newNode = new YdtSingleInstanceNode(node);
        newNode.setYangSchemaNode(node);
        return newNode;
    }

    @Override
    public void setRootTagAttributeMap(Map<String, String> attributeTag) {
        rootTagAttributeMap = attributeTag;
    }

    @Override
    public Map<String, String> getRootTagAttributeMap() {
        if (rootTagAttributeMap != null) {
            return ImmutableMap.copyOf(rootTagAttributeMap);
        }
        return null;
    }

    @Override
    public void addChild(String name, String namespace)
            throws IllegalArgumentException {
        addChild(name, namespace, UNKNOWN, null, NON_LEAF);
    }

    @Override
    public void addChild(String name, String namespace, YdtType ydtType)
            throws IllegalArgumentException {
        addChild(name, namespace, ydtType, null);
    }

    @Override
    public void addChild(String name, String namespace,
                         YdtContextOperationType opType)
            throws IllegalArgumentException {
        addChild(name, namespace, UNKNOWN, opType, NON_LEAF);
    }

    @Override
    public void addChild(String name, String namespace, YdtType ydtType,
                         YdtContextOperationType opType)
            throws IllegalArgumentException {
        RequestedCardinality cardinality;
        switch (ydtType) {
            case MULTI_INSTANCE_NODE:
                cardinality = MULTI_INSTANCE;
                break;
            case SINGLE_INSTANCE_NODE:
                cardinality = SINGLE_INSTANCE;
                break;
            default:
                throw new IllegalArgumentException(E_USE_ADD_LEAF);
        }
        addChild(name, namespace, cardinality, opType, NON_LEAF);
    }

    /**
     * Adds a last child to YANG data tree; this method is to be used by all
     * protocols internally which are aware or unaware of the nature
     * (single/multiple) of node.
     *
     * @param name        name of child to be added
     * @param namespace   namespace of child to be added
     * @param cardinality type of YANG data tree node operation
     * @param opType      type of requested operation over a node
     * @param callType    to identify the whether its a leaf or other node
     * @throws IllegalArgumentException when method has been passed an illegal
     *                                  or inappropriate argument.
     */
    private void addChild(String name, String namespace,
                          RequestedCardinality cardinality,
                          YdtContextOperationType opType,
                          RequestedCallType callType)
            throws IllegalArgumentException {

        YdtNode newNode;
        boolean contextSwitch = false;
        YangSchemaNode augmentingSchema = null;
        YangSchemaNodeIdentifier id = getNodeIdentifier(name, namespace);
        if (name == null) {
            if (!curNode.equals(rootNode)) {
                throw new YdtException("Name is null for node other than module");
            }

            /*
             * Since XML will not have module name, id.name will be null. In
             * that case get schema node by using namespace. In NBI flow,
             * name will never be null.
             */
            YangSchemaNode node = registry
                    .getSchemaWrtNameSpace(id.getNameSpace().getModuleNamespace());
            id.setName(node.getName());
        }

        try {
            // Module/sub-module node handler.
            if (curNode.equals(rootNode)) {
                newNode = moduleHandler(id);
            } else {

                YangSchemaNode schemaNode;
                YangSchemaNodeContextInfo contextInfo;

                // If namespace given by user null, then take namespace from parent.
                if (namespace == null) {
                    id.setNameSpace(curNode.getYangSchemaNode().getNameSpace());
                }

                /*
                 * Get the already exiting YDT node in YDT tree with same
                 * nodeIdentifier
                 */
                newNode = curNode.getCollidingChild(id);

                /*
                 * If colliding child doesn't exist ,
                 * then query yang data model for schema of given node.
                 */
                if (newNode == null) {
                    /*
                     * Get Yang Schema node context info which is having
                     * YangSchemaNode and ContextSwitchedNode.
                     */
                    contextInfo = curNode.getSchemaNodeContextInfo(id);

                    if (contextInfo.getContextSwitchedNode() != null) {
                        augmentingSchema = getAugmentingSchemaNode(
                                id, contextInfo);
                        if (augmentingSchema != null) {
                            /*
                             * As two tree(YDT and YDT Application Tree) are getting
                             * prepared in parallel, So  setting context switch
                             * flag it will help ydt to keep the track whether
                             * ydtApp tree also need to be traversed back to parent
                             * or not with YDT tree traverse to parent call.
                             */
                            contextSwitch = true;
                        }
                    }
                    schemaNode = contextInfo.getSchemaNode();
                } else {
                    /*
                     * If colliding child exist , then it will be leaf-list or list.
                     * If its leaf-list then return and add new requested
                     * value/valueSet in same node else take yang data model
                     * information from colliding child.
                     */
                    if (newNode.getYdtType() == MULTI_INSTANCE_LEAF_VALUE_NODE) {
                        curNode = newNode;
                        return;
                    }
                    schemaNode = newNode.getYangSchemaNode();
                }

                /*
                 * For yms query request node specific validation are not
                 * required as rest-conf can call addChild api for leaf/leaf-list
                 * node addition also in ydt.
                 */
                if (ymsOperationType == YmsOperationType.QUERY_REQUEST) {
                    newNode = getYangSchemaNodeTypeSpecificContext(schemaNode);
                } else {
                    newNode = getNode(schemaNode, cardinality, callType);
                }
            }

            opType = getValidOpType(opType, ydtDefaultOpType, newNode, curNode);

            newNode.setYdtContextOperationType(opType);

            curNode.addChild(newNode, true);
        } catch (YdtException e) {
            freeRestResources(rootNode);
            throw new IllegalArgumentException(e.getLocalizedMessage());
        }

        // Update parent ydt node map.
        curNode.updateYdtMap(newNode);

        processAppTree(opType, newNode, augmentingSchema, contextSwitch);

        curNode = newNode;
    }

    /**
     * Processes application tree on the bases of requested ydt node.
     *
     * @param opType           user requested operation type
     * @param childNode        requested ydt node
     * @param augmentingSchema schema of last augmenting node
     * @param contextSwitch    true, for module node call; false for modules
     *                         sub-node calls
     */
    private void processAppTree(
            YdtContextOperationType opType, YdtNode childNode,
            YangSchemaNode augmentingSchema, boolean contextSwitch) {

        if (curNode == rootNode) {
            augGenMethodSet = new HashSet<>();
        }

        if (opType == null) {
            opType = curNode.getYdtContextOperationType();
        } else {
            // Updating operation type for parent nodes
            appCurNode.updateAppOperationType(opType);
        }

        /*
         * This is to avoid multiple entries of single augmented target.
         */
        if (augmentingSchema != null) {
            if (!augGenMethodSet.add(((YangAugment) augmentingSchema)
                                             .getSetterMethodName())) {
                return;
            }
        }

        /*
         * Create entry of module node in ydt app tree.
         * Or if context switch happened then also add entry for same
         * augmented ydt node in the ydt application tree.
         */
        if (curNode.equals(rootNode) || contextSwitch) {
            addChildInAppTree(childNode, augmentingSchema, opType,
                              contextSwitch);

            // Setting app tree node operation.
            appCurNode.setOperationType(getAppOpTypeFromYdtOpType(opType));
        }

        // Updating the delete operation list in app tree.
        if (opType == DELETE || opType == REMOVE) {
            appCurNode.addDeleteNode(childNode);
        }
    }


    /**
     * Adds a last child to YANG app data tree.this method is to be used
     * internally by other ydt interfaces.
     *
     * @param childNode       node to be added in tree
     * @param schemaNode      last augmenting module node
     * @param childOpType     operation type of node
     * @param isContextSwitch true, for module node call; false for modules
     *                        sub-node calls
     */
    private void addChildInAppTree(YdtNode childNode,
                                   YangSchemaNode schemaNode,
                                   YdtContextOperationType childOpType,
                                   boolean isContextSwitch) {

        YdtAppNodeOperationType opType;

        DefaultYdtAppContext appContext = getAppContext(isContextSwitch);

        // Add context switched child in ydt App tree.
        appCurNode.addChild(appContext);

        appCurNode = appContext;

        opType = getAppOpTypeFromYdtOpType(childOpType);

        appCurNode.setAppData(childNode, schemaNode);

        appCurNode.setOperationType(opType);

        childNode.setAppContextSwitch();
    }

    @Override
    public void addLeaf(String name, String namespace, String value)
            throws IllegalArgumentException {
        addLeaf(name, namespace, value, null, UNKNOWN);
    }

    @Override
    public void addLeaf(String name, String namespace, Set<String> valueSet)
            throws IllegalArgumentException {
        addLeaf(name, namespace, null, valueSet, MULTI_INSTANCE_LEAF);
    }

    @Override
    public void addNode(String name, String namespace)
            throws IllegalArgumentException {
        addChild(name, namespace, RequestedCardinality.UNKNOWN,
                null, RequestedCallType.EMPTY_CONTAINER);
    }


    /**
     * Adds a last leaf with list of values/single value to YANG data tree.
     * This method is used by all protocols which knows the nature
     * (single/multiple) or not.
     * Value of leaf can be null which indicates selection node in get
     * operation.
     *
     * @param name        name of child to be added
     * @param namespace   namespace of child to be added, if it's
     *                    null, parent's
     *                    namespace will be applied to child
     * @param value       value of the child
     * @param valueSet    list of value of the child
     * @param cardinality type of YANG data tree node operation
     * @throws IllegalArgumentException when method has been passed an illegal
     *                                  or inappropriate argument.
     */
    private void addLeaf(String name, String namespace, String value,
                         Set<String> valueSet,
                         RequestedCardinality cardinality)
            throws IllegalArgumentException {
        try {
            addChild(name, namespace, cardinality, null, LEAF);

            // After successful addition of child node updating the values in same.
            if (value != null) {
                curNode.addValue(value);
            } else if (valueSet != null) {
                curNode.addValueSet(valueSet);
            }
        } catch (YdtException e) {
            freeRestResources(rootNode);
            throw new IllegalArgumentException(e.getLocalizedMessage());
        }
    }

    @Override
    public void traverseToParent() throws IllegalStateException {
        // If traverse back to parent for logical root node comes
        if (curNode.equals(rootNode)) {
            freeRestResources(rootNode);
            throw new IllegalStateException(E_INVOKE_PARENT);
        }

        try {

            // If node is of multiInstanceNode type then check key uniqueness.
            if (curNode.getYdtType() == MULTI_INSTANCE_NODE) {
                List<YdtContext> keyList = ((YdtMultiInstanceNode) curNode).getKeyNodeList();
                if (keyList == null || keyList.isEmpty()) {
                    curNode.createKeyNodeList();
                }
            }

            /*
             * Check application switch for curNode if set,
             * then traverseToParent in YDT application tree.
             */
            if (curNode.getParent().equals(rootNode) ||
                    curNode.getAppContextSwitch()) {
                traverseToAppTreeParent();
            }

            /*
             * Validate all multi Instance inside current context,
             * This is not valid for leaf and leaf-list node.
             */
            if (curNode instanceof YdtMultiInstanceNode ||
                    curNode instanceof YdtSingleInstanceNode) {
                curNode.validateMultiInstanceNode();
            }

            curNode = curNode.getParent();
        } catch (YdtException e) {
            freeRestResources(rootNode);
            throw new IllegalStateException(e.getLocalizedMessage());
        }
    }

    /**
     * Traverses up in YANG application tree to the parent node,
     * This will be used when Ydt current context switch flag is set.
     */
    private void traverseToAppTreeParent() {
        appCurNode = appCurNode.getParent();
    }

    @Override
    public YdtExtendedContext getCurNode() {
        return curNode;
    }

    @Override
    public void setDefaultEditOperationType(
            YdtContextOperationType opType) {
        ydtDefaultOpType = opType;
    }

    @Override
    public YdtExtendedContext getRootNode() {
        return rootNode;
    }

    @Override
    public YmsOperationType getYmsOperationType() {
        return ymsOperationType;
    }

    @Override
    public void addMultiInstanceChild(String name, String namespace,
                                      List<String> keysValueList,
                                      YdtContextOperationType opType)
            throws IllegalArgumentException {

        addChild(name, namespace, UNKNOWN, opType,
                 RequestedCallType.MULTI_INSTANCE);
        int inputCount = keysValueList.size();

        try {
            if (curNode.getYdtType() == MULTI_INSTANCE_LEAF_VALUE_NODE) {

            /*
             * Calculating the current leaf-list node array size by adding
             * existing elements count and new supplied elements by user for
             * the same.
             */
                // TODO instance count for leaf list need to be handled.
//            if (curNode.getValueSet().size() + inputCount > expectedCount) {
//                curNode.errorHandler(
//                        errorMsg(FMT_MANY_INS, name, expectedCount), rootNode);
//            }

            /*
             * After successful addition of child node updating
             * the values in same.
             */
                for (String value : keysValueList) {
                    curNode.addValue(value);
                }
            } else if (curNode.getYdtType() == MULTI_INSTANCE_NODE) {

                YangList yangListHolder = (YangList) curNode.getYangSchemaNode();
                List<String> schemaKeyList = yangListHolder.getKeyList();
                int expectedCount = schemaKeyList.size();
                checkElementCount(name, expectedCount, inputCount);

                //After validation adding the key nodes under the list node.
                Iterator<String> sklIter = schemaKeyList.iterator();
                Iterator<String> kvlIter = keysValueList.iterator();
                String keyEleName;

                while (kvlIter.hasNext()) {
                    String value = kvlIter.next();
                    keyEleName = sklIter.next();
                    addLeaf(keyEleName, namespace, value);
                    if (kvlIter.hasNext()) {
                        traverseToParentWithoutValidation();
                    }
                }

                curNode = curNode.getParent();
                curNode.createKeyNodeList();
            }
        } catch (YdtException e) {
            freeRestResources(rootNode);
            throw new IllegalArgumentException(e.getLocalizedMessage());
        }
    }

    /**
     * Adds a last child to YANG data tree, this method is to be used by
     * YANG object builder sub-calls internally.
     *
     * @param opType type of requested operation over a node
     * @return returns added ydt node in YDT tree
     */
    private YdtNode addExtendedChildNode(YdtContextOperationType opType,
                                         YangSchemaNode schemaNode) {

        YdtNode childNode = getYangSchemaNodeTypeSpecificContext(schemaNode);

        childNode.setYangSchemaNode(schemaNode);

        childNode.setYdtContextOperationType(opType);

        curNode.addChild(childNode, true);

        curNode = childNode;

        return childNode;
    }

    @Override
    public YdtExtendedContext addChild(YdtContextOperationType opType,
                                       YangSchemaNode schemaNode) {
        return addExtendedChildNode(opType, schemaNode);
    }

    @Override
    public YdtExtendedContext addLeafList(Set<String> valueSet,
                                          YangSchemaNode schemaNode) {
        YdtNode childNode = addExtendedChildNode(null, schemaNode);

        /*
         * After successful addition of child node updating the values in
         * valueSet.
         */
        childNode.addValueSetWithoutValidation(valueSet);
        return childNode;
    }

    @Override
    public YdtExtendedContext addLeaf(String value, YangSchemaNode schemaNode) {

        YdtNode childNode = addExtendedChildNode(null, schemaNode);

        // After successful addition of child node updating the values in same.
        childNode.addValueWithoutValidation(value, ((YangLeaf) schemaNode)
                .isKeyLeaf());
        return childNode;
    }

    @Override
    public void traverseToParentWithoutValidation()
            throws IllegalStateException {
        // If traverse back to parent for logical root node comes.
        if (curNode.equals(rootNode)) {
            freeRestResources(rootNode);
            throw new IllegalStateException(E_INVOKE_PARENT);
        }
        curNode = curNode.getParent();
    }

    /**
     * Returns the method name's set for an augmented target node in an
     * application tree.
     *
     * @return augGenMethodSet set of method name's
     */
    public Set<String> getAugGenMethodSet() {
        return augGenMethodSet;
    }

    /**
     * Sets the method name's set for an augmented target node in an
     * application tree.
     *
     * @param augGenMethodSet set of method name's
     */
    public void setAugGenMethodSet(Set<String> augGenMethodSet) {
        this.augGenMethodSet = augGenMethodSet;
    }
}
