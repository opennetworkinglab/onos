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

import org.onosproject.yangutils.datamodel.YangAugment;
import org.onosproject.yangutils.datamodel.YangSchemaNode;
import org.onosproject.yangutils.datamodel.YangSchemaNodeContextInfo;
import org.onosproject.yangutils.datamodel.YangSchemaNodeIdentifier;
import org.onosproject.yangutils.datamodel.exceptions.DataModelException;
import org.onosproject.yms.app.ydt.exceptions.YdtException;
import org.onosproject.yms.ydt.YdtContextOperationType;

import static org.onosproject.yms.app.ydt.YdtAppNodeOperationType.DELETE_ONLY;
import static org.onosproject.yms.app.ydt.YdtAppNodeOperationType.OTHER_EDIT;
import static org.onosproject.yms.app.ydt.YdtConstants.errorMsg;
import static org.onosproject.yms.ydt.YdtContextOperationType.CREATE;
import static org.onosproject.yms.ydt.YdtContextOperationType.DELETE;
import static org.onosproject.yms.ydt.YdtContextOperationType.MERGE;

/**
 * Utils to support yang data tree node creation.
 */
final class YdtUtils {

    // YDT formatted error string
    private static final String E_CREATE_OP =
            "Create request is not allowed under delete operation.";
    private static final String E_DELETE_OP =
            "Delete request is not allowed under create operation.";
    private static final String FMT_TOO_FEW =
            "Too few key parameters in %s. Expected %d; actual %d.";
    private static final String FMT_TOO_MANY =
            "Too many key parameters in %s. Expected %d; actual %d.";

    //No instantiation.
    private YdtUtils() {
    }

    /**
     * Returns the app tree operation type with the help of YdtOperation type.
     *
     * @param opType ydt operation type
     * @return app tree operation type
     */
    static YdtAppNodeOperationType getAppOpTypeFromYdtOpType(
            YdtContextOperationType opType) {
        // Get the app tree operation type.
        switch (opType) {
            case CREATE:
            case MERGE:
            case REPLACE:
                return OTHER_EDIT;

            case DELETE:
            case REMOVE:
                return DELETE_ONLY;

            default:
                return null;
            //TODO handle the default data type.
        }
    }

    /**
     * Validates the various combination of operation type.
     *
     * @param parentOpType Reference for parent node operation type
     * @param childOpType  type of YANG data tree node operation
     * @throws YdtException when user requested node operation type is
     *                      not valid as per parent node operation type
     */
    private static void validateOperationType(YdtContextOperationType parentOpType,
                                              YdtContextOperationType childOpType)
            throws YdtException {

        switch (parentOpType) {
            case CREATE:
                // Inside the create operation delete operation should not come.
                if (childOpType == DELETE) {
                    throw new YdtException(E_CREATE_OP);
                }
                break;
            case DELETE:
                // Inside the delete operation create operation should not come.
                if (childOpType == CREATE) {
                    throw new YdtException(E_DELETE_OP);
                }
                break;
            default:
                //TODO check all possible scenario.
        }
    }

    /**
     * Returns the operation type for non leaf node.
     * When "operation" attribute for current node is not specified or null,
     * then the operation applied to the parent data node of the
     * configuration is used. If no parent data node is available,
     * then the default-operation'value is used.
     * If default operation type is not set, merge will be taken as default
     * operation type.
     *
     * @param type    operation type of parent node
     * @param defType YDT default operation type
     * @return operation type for current non leaf node
     */
    private static YdtContextOperationType getOperationType(
            YdtContextOperationType type, YdtContextOperationType defType) {
        return type != null ? type : (defType != null ? defType : MERGE);
    }

    /**
     * Returns the yang node identifier with requested name and namespace.
     *
     * @param name      name of the node
     * @param namespace namespace of the node
     * @return yang node identifier
     */
    static YangSchemaNodeIdentifier getNodeIdentifier(String name,
                                                      String namespace) {
        YangSchemaNodeIdentifier id = new YangSchemaNodeIdentifier();
        id.setName(name);
        id.setNameSpace(new NameSpace(namespace));
        return id;
    }

    /**
     * Checks the user supplied list of argument match's the expected value
     * or not.
     *
     * @param name     name of the parent list/leaf-list node
     * @param expected count suppose to be
     * @param actual   user supplied values count
     * @throws YdtException when user requested multi instance node instance's
     *                      count doesn't fit into the allowed instance limit
     */
    static void checkElementCount(String name, int expected,
                                  int actual) throws YdtException {
        if (expected < actual) {
            throw new YdtException(
                    errorMsg(FMT_TOO_MANY, name, expected, actual));
        } else if (expected > actual) {
            throw new YdtException(
                    errorMsg(FMT_TOO_FEW, name, expected, actual));
        }
    }

    /**
     * Returns the valid operation type for requested ydt node after performing
     * validation.
     *
     * @param opType     user requested operation type
     * @param newNode    new requested ydt node
     * @param parentNode parent node under which new node to be added
     * @param defOpType  YDT context operation type
     * @return operation type
     * @throws YdtException when user requested node operation type is
     *                      not valid as per parent node operation type
     */
    static YdtContextOperationType getValidOpType(
            YdtContextOperationType opType, YdtContextOperationType defOpType,
            YdtNode newNode, YdtNode parentNode)
            throws YdtException {

        switch (newNode.getYdtType()) {

            case SINGLE_INSTANCE_NODE:
            case MULTI_INSTANCE_NODE:

                // Reference for parent node operation type.
                YdtContextOperationType parentOpType =
                        parentNode.getYdtContextOperationType();

                if (opType == null) {
                    opType = getOperationType(parentOpType, defOpType);
                } else if (parentOpType != null) {
                    validateOperationType(parentOpType, opType);
                }

                return opType;

            /*
             * Nodes other then single/multi instance node does not support
             * operation type so no need of validation for those.
             */
            default:
                return null;
        }
    }

    /**
     * Returns augmenting node module yang schema node.
     *
     * @param id          schema node identifier
     * @param contextInfo Yang Schema node context info
     *                    which is having YangSchemaNode and
     *                    ContextSwitchedNode
     * @return augmenting node module yang schema node
     * @throws YdtException when user requested node schema doesn't exist
     */
    public static YangSchemaNode getAugmentingSchemaNode(
            YangSchemaNodeIdentifier id,
            YangSchemaNodeContextInfo contextInfo) throws YdtException {
        YangSchemaNode lastAugMod = null;
        YangSchemaNode switchedNode =
                contextInfo.getContextSwitchedNode();

        // Finding the last augmenting schema for case/choice scenario.
        while (switchedNode != null) {
            if (switchedNode instanceof YangAugment) {
                lastAugMod = switchedNode;
            }
            try {
                switchedNode = switchedNode.getChildSchema(id)
                        .getContextSwitchedNode();
            } catch (DataModelException e) {
                throw new YdtException(e.getMessage());
            }
        }
        return lastAugMod;
    }

    /**
     * De-reference all the tree node by walking the whole YDT from logical
     * root node.
     * This will be called only when any exception occurs while processing
     * the node in Ydt tree.
     *
     * @param rootNode ydt logical root node
     */
    public static void freeRestResources(YdtNode rootNode) {

        YdtNode currentNode = rootNode;
        while (currentNode != null) {

            // Move down to first child
            YdtNode nextNode = currentNode.getFirstChild();
            if (nextNode != null) {
                currentNode = nextNode;
                continue;
            }

            // No child nodes, so walk tree
            while (currentNode != null) {
                // To keep the track of last sibling.
                YdtNode lastSibling = currentNode;

                // Move to sibling if possible.
                nextNode = currentNode.getNextSibling();

                // free currentNode resources
                free(lastSibling);

                lastSibling.getNamespace();
                if (nextNode != null) {
                    currentNode = nextNode;
                    break;
                }

                // Move up
                if (currentNode.equals(rootNode)) {
                    currentNode = null;
                } else {
                    currentNode = currentNode.getParent();
                    lastSibling.setParent(null);
                }
            }
        }
    }

    /**
     * Free the give YDT node by de-referencing it to null.
     *
     * @param node node to be freed
     */
    private static void free(YdtNode node) {
        if (node.getParent() != null) {
            YdtNode parent = node.getParent();
            parent.setChild(null);
            parent.setLastChild(null);
            if (node.getNextSibling() != null) {
                parent.setChild(node.getNextSibling());
            }
        }
        YdtNode parentRef = node.getParent();
        node = new YdtLogicalNode(null, null);
        node.setParent(parentRef);
    }
}
