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
import org.onosproject.yangutils.datamodel.YangSchemaNodeIdentifier;
import org.onosproject.yangutils.datamodel.YangSchemaNodeType;
import org.onosproject.yms.app.ydt.exceptions.YdtException;
import org.onosproject.yms.ydt.YdtContextOperationType;

import static org.onosproject.yangutils.datamodel.YangSchemaNodeType.YANG_MULTI_INSTANCE_LEAF_NODE;
import static org.onosproject.yangutils.datamodel.YangSchemaNodeType.YANG_MULTI_INSTANCE_NODE;
import static org.onosproject.yangutils.datamodel.YangSchemaNodeType.YANG_SINGLE_INSTANCE_LEAF_NODE;
import static org.onosproject.yangutils.datamodel.YangSchemaNodeType.YANG_SINGLE_INSTANCE_NODE;
import static org.onosproject.yms.app.ydt.YdtAppNodeOperationType.DELETE_ONLY;
import static org.onosproject.yms.app.ydt.YdtAppNodeOperationType.OTHER_EDIT;
import static org.onosproject.yms.app.ydt.YdtConstants.errorMsg;

/**
 * Represents an YANG node factory to create different types of YANG data tree
 * node.
 */
final class YdtNodeFactory {

    // ydt formatted error string
    private static final String FMT_NOT_EXIST =
            "Schema node with name %s doesn't exist.";

    // No instantiation
    private YdtNodeFactory() {
    }

    /**
     * Returns a YANG data tree node for a given name, set of values and
     * instance type.
     *
     * @param id          dataNodeIdentifier of data tree node
     * @param schemaNode  data node as per YANG schema metadata
     * @param cardinality requested cardinality of node
     * @param callType    identify the call type
     * @return YANG data tree node
     */
    protected static YdtNode getNode(YangSchemaNodeIdentifier id,
                                     YangSchemaNode schemaNode,
                                     RequestedCardinality cardinality,
                                     RequestedCallType callType) {

        YdtNode newNode = null;
        YangSchemaNodeType nodeType = schemaNode.getYangSchemaNodeType();

        switch (cardinality) {

            case UNKNOWN:
                /*
                 * if requested node type is UNKNOWN, check corresponding
                 * yang data node type and create respective type node.
                 */
                newNode = getYangSchemaNodeTypeSpecificContext(id, nodeType,
                                                               callType);
                break;

            /*
             * if requested node type is specified and it exist as node of some
             * other type in data model then throw exception
             */
            case SINGLE_INSTANCE:
                validateNodeType(id, nodeType, YANG_SINGLE_INSTANCE_NODE);
                newNode = new YdtSingleInstanceNode(id);
                break;

            case MULTI_INSTANCE:

                validateNodeType(id, nodeType, YANG_MULTI_INSTANCE_NODE);
                newNode = new YdtMultiInstanceNode(id);
                break;

            case SINGLE_INSTANCE_LEAF:

                validateNodeType(id, nodeType, YANG_SINGLE_INSTANCE_LEAF_NODE);
                newNode = new YdtSingleInstanceLeafNode(id);
                break;

            case MULTI_INSTANCE_LEAF:

                validateNodeType(id, nodeType, YANG_MULTI_INSTANCE_LEAF_NODE);
                newNode = new YdtMultiInstanceLeafNode(id);
                break;

            default:
                throwNotExistError(id);
        }

        // set reference of yang data node in the requested node.
        newNode.setYangSchemaNode(schemaNode);

        return newNode;
    }

    /**
     * Validates the requested ydt node type against the schema node type,
     * if it is not equal then it will throw warning.
     *
     * @param id            dataNodeIdentifier of data tree node
     * @param nodeType      actual node type
     * @param requestedType user requested node type
     */
    private static void validateNodeType(YangSchemaNodeIdentifier id,
                                         YangSchemaNodeType nodeType,
                                         YangSchemaNodeType requestedType) {
        if (nodeType != requestedType) {
            throwNotExistError(id);
        }
    }

    /**
     * Creates Yang data tree node of YangSchemaNode type specific for
     * requestedCardinality of type UNKNOWN and returns the same.
     *
     * @param id       node identifier of data tree node
     * @param nodeType schema node type as per YANG schema metadata
     * @param callType identify the call type
     * @return YANG data tree node
     */
    private static YdtNode getYangSchemaNodeTypeSpecificContext(
            YangSchemaNodeIdentifier id,
            YangSchemaNodeType nodeType,
            RequestedCallType callType) {
        switch (callType) {
            case LEAF:
                switch (nodeType) {

                    case YANG_SINGLE_INSTANCE_LEAF_NODE:
                        return new YdtSingleInstanceLeafNode(id);

                    case YANG_MULTI_INSTANCE_LEAF_NODE:
                        return new YdtMultiInstanceLeafNode(id);

                    default:
                        throwNotExistError(id);
                }

            case OTHER:
                switch (nodeType) {

                    case YANG_SINGLE_INSTANCE_NODE:
                        return new YdtSingleInstanceNode(id);

                    case YANG_MULTI_INSTANCE_NODE:
                        return new YdtMultiInstanceNode(id);

                    default:
                        throwNotExistError(id);
                }

            case MULTI_INSTANCE:
                switch (nodeType) {

                    case YANG_MULTI_INSTANCE_LEAF_NODE:
                        return new YdtMultiInstanceLeafNode(id);

                    case YANG_MULTI_INSTANCE_NODE:
                        return new YdtMultiInstanceNode(id);

                    default:
                        throwNotExistError(id);
                }

            default:
                throwNotExistError(id);
        }

        return null;
    }

    /**
     * Create Yang data tree node of YangSchemaNode type specific and
     * returns the same.
     *
     * @param id       node identifier of data tree node
     * @param nodeType schema node type as per YANG schema metadata
     * @return YANG data tree node
     */
    protected static YdtNode getYangSchemaNodeTypeSpecificContext(
            YangSchemaNodeIdentifier id,
            YangSchemaNodeType nodeType) {

        switch (nodeType) {

            case YANG_SINGLE_INSTANCE_LEAF_NODE:
                return new YdtSingleInstanceLeafNode(id);

            case YANG_MULTI_INSTANCE_LEAF_NODE:
                return new YdtMultiInstanceLeafNode(id);

            case YANG_SINGLE_INSTANCE_NODE:
                return new YdtSingleInstanceNode(id);

            case YANG_MULTI_INSTANCE_NODE:
                return new YdtMultiInstanceNode(id);

            default:
                throwNotExistError(id);
        }

        return null;
    }

    /**
     * Returns the app tree operation type with the help of YdtOperation type.
     *
     * @param opType ydt operation type
     * @return app tree operation type
     */
    protected static YdtAppNodeOperationType getAppOpTypeFromYdtOpType(
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
     * Throws exception for requested ydt node by preparing error message with
     * given node identifier.
     *
     * @param id node identifier
     */
    private static void throwNotExistError(YangSchemaNodeIdentifier id) {
        throw new YdtException(errorMsg(FMT_NOT_EXIST, id.getName()));
    }
}
