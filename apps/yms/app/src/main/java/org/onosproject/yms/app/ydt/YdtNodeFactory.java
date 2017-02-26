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
import org.onosproject.yangutils.datamodel.YangSchemaNodeType;
import org.onosproject.yangutils.datamodel.exceptions.DataModelException;
import org.onosproject.yms.app.ydt.exceptions.YdtException;

import static org.onosproject.yangutils.datamodel.YangSchemaNodeType.YANG_MULTI_INSTANCE_LEAF_NODE;
import static org.onosproject.yangutils.datamodel.YangSchemaNodeType.YANG_MULTI_INSTANCE_NODE;
import static org.onosproject.yangutils.datamodel.YangSchemaNodeType.YANG_SINGLE_INSTANCE_LEAF_NODE;
import static org.onosproject.yangutils.datamodel.YangSchemaNodeType.YANG_SINGLE_INSTANCE_NODE;
import static org.onosproject.yms.app.ydt.YdtConstants.errorMsg;

/**
 * Represents an YANG node factory to create different types of YANG data tree
 * node.
 */
final class YdtNodeFactory {

    // YDT formatted error string
    private static final String FMT_NOT_EXIST =
            "Schema node with name %s doesn't exist.";
    //TODO need to handle later
    private static final String E_MULTI_INS =
            "Requested interface adds an instance of type list or " +
                    "leaf-list node only.";

    // No instantiation
    private YdtNodeFactory() {
    }

    /**
     * Returns a YANG data tree node for a given name, set of values and
     * instance type.
     *
     * @param node        data node as per YANG schema metadata
     * @param cardinality requested cardinality of node
     * @param callType    identify the call type
     * @return YANG data tree node
     * @throws YdtException when user requested node type doesn't exist
     */
    static YdtNode getNode(
            YangSchemaNode node, RequestedCardinality cardinality,
            RequestedCallType callType) throws YdtException {

        YdtNode newNode;
        YangSchemaNodeType type = node.getYangSchemaNodeType();

        try {
            switch (cardinality) {

                case UNKNOWN:
                    /*
                     * if requested node type is UNKNOWN, check corresponding
                     * yang data node type and create respective type node.
                     */
                    newNode = getYangSchemaNodeTypeSpecificContext(node, type,
                                                                   callType);
                    break;

                /*
                 * if requested node type is specified and it exist as node of
                 * some other type in data model then throw exception
                 */
                case SINGLE_INSTANCE:
                    validateNodeType(node, type, YANG_SINGLE_INSTANCE_NODE);
                    newNode = new YdtSingleInstanceNode(node);
                    break;

                case MULTI_INSTANCE:

                    validateNodeType(node, type, YANG_MULTI_INSTANCE_NODE);
                    newNode = new YdtMultiInstanceNode(node);
                    break;

                case SINGLE_INSTANCE_LEAF:

                    validateNodeType(node, type, YANG_SINGLE_INSTANCE_LEAF_NODE);
                    newNode = new YdtSingleInstanceLeafNode(node);
                    break;

                case MULTI_INSTANCE_LEAF:

                    validateNodeType(node, type, YANG_MULTI_INSTANCE_LEAF_NODE);
                    newNode = new YdtMultiInstanceLeafNode(node);
                    break;

                default:
                    newNode = null;
            }
        } catch (DataModelException | YdtException e) {
            throw new YdtException(e.getLocalizedMessage());
        }

        if (newNode == null) {
            throw new YdtException(errorMsg(FMT_NOT_EXIST, node.getName()));
        }

        return newNode;
    }

    /**
     * Validates the requested ydt node type against the schema node type,
     * if it is not equal then it will throw warning.
     *
     * @param node          schema node
     * @param nodeType      actual node type
     * @param requestedType user requested node type
     * @throws YdtException when user requested node type doesn't exist
     */
    private static void validateNodeType(
            YangSchemaNode node, YangSchemaNodeType nodeType,
            YangSchemaNodeType requestedType) throws YdtException {

        if (nodeType != requestedType) {
            throw new YdtException(errorMsg(FMT_NOT_EXIST, node.getName()));
        }
    }

    /**
     * Creates Yang data tree node of YangSchemaNode type specific for
     * requestedCardinality of type UNKNOWN and returns the same.
     *
     * @param node     schema node
     * @param nodeType schema node type as per YANG schema metadata
     * @param callType identify the call type
     * @return YANG data tree node
     * @throws YdtException when user requested node type doesn't exist
     */
    private static YdtNode getYangSchemaNodeTypeSpecificContext(
            YangSchemaNode node, YangSchemaNodeType nodeType,
            RequestedCallType callType) throws YdtException, DataModelException {
        switch (callType) {
            case LEAF:
                switch (nodeType) {

                    case YANG_SINGLE_INSTANCE_LEAF_NODE:
                        return new YdtSingleInstanceLeafNode(node);

                    case YANG_MULTI_INSTANCE_LEAF_NODE:
                        return new YdtMultiInstanceLeafNode(node);

                    default:
                        return null;
                }

            case NON_LEAF:
                switch (nodeType) {

                    case YANG_SINGLE_INSTANCE_NODE:
                        return new YdtSingleInstanceNode(node);

                    case YANG_MULTI_INSTANCE_NODE:
                        return new YdtMultiInstanceNode(node);

                    default:
                        return null;
                }

            case MULTI_INSTANCE:
                switch (nodeType) {

                    case YANG_MULTI_INSTANCE_LEAF_NODE:
                        return new YdtMultiInstanceLeafNode(node);

                    case YANG_MULTI_INSTANCE_NODE:
                        return new YdtMultiInstanceNode(node);

                    default:
                        throw new YdtException(E_MULTI_INS);
                }

            case EMPTY_CONTAINER:
                switch (nodeType) {

                case YANG_SINGLE_INSTANCE_NODE:
                    return new YdtSingleInstanceNode(node);

                case YANG_SINGLE_INSTANCE_LEAF_NODE:
                    return new YdtSingleInstanceLeafNode(node);

                default:
                    return null;
            }

            default:
                return null;
        }
    }

    /**
     * Create Yang data tree node of YangSchemaNode type specific and
     * returns the same.
     *
     * @param node schema node
     * @return YANG data tree node
     * @throws YdtException when user requested node type doesn't exist
     */
    static YdtNode getYangSchemaNodeTypeSpecificContext(YangSchemaNode node)
            throws YdtException {

        switch (node.getYangSchemaNodeType()) {

            case YANG_SINGLE_INSTANCE_LEAF_NODE:
                return new YdtSingleInstanceLeafNode(node);

            case YANG_MULTI_INSTANCE_LEAF_NODE:
                return new YdtMultiInstanceLeafNode(node);

            case YANG_SINGLE_INSTANCE_NODE:
                return new YdtSingleInstanceNode(node);

            case YANG_MULTI_INSTANCE_NODE:
                return new YdtMultiInstanceNode(node);

            default:
                throw new YdtException(errorMsg(FMT_NOT_EXIST, node.getName()));
        }
    }
}
