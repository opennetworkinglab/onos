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

package org.onosproject.yangutils.datamodel;

import org.onosproject.yangutils.datamodel.exceptions.DataModelException;
import org.onosproject.yangutils.datamodel.utils.Parsable;
import org.onosproject.yangutils.datamodel.utils.YangConstructType;

import static org.onosproject.yangutils.datamodel.utils.DataModelUtils.detectCollidingChildUtil;

/*
 * Reference RFC 6020.
 *
 * The "rpc" statement is used to define a NETCONF RPC operation.  It
 * takes one argument, which is an identifier, followed by a block of
 * substatements that holds detailed rpc information.  This argument is
 * the name of the RPC, and is used as the element name directly under
 * the <rpc> element, as designated by the substitution group
 * "rpcOperation" in [RFC4741].
 *
 * The "rpc" statement defines an rpc node in the schema tree.  Under
 * the rpc node, a schema node with the name "input", and a schema node
 * with the name "output" are also defined.  The nodes "input" and
 * "output" are defined in the module's namespace.
 *
 * The rpc substatements
 *
 *    +--------------+---------+-------------+------------------+
 *    | substatement | section | cardinality |data model mapping|
 *    +--------------+---------+-------------+------------------+
 *    | description  | 7.19.3  | 0..1        | -string          |
 *    | grouping     | 7.11    | 0..n        | -child nodes     |
 *    | if-feature   | 7.18.2  | 0..n        | -TODO            |
 *    | input        | 7.13.2  | 0..1        | -child nodes     |
 *    | output       | 7.13.3  | 0..1        | -child nodes     |
 *    | reference    | 7.19.4  | 0..1        | -string          |
 *    | status       | 7.19.2  | 0..1        | -YangStatus      |
 *    | typedef      | 7.3     | 0..n        | -child nodes     |
 *    +--------------+---------+-------------+------------------+
 */

/**
 * Represents data model node to maintain information defined in YANG rpc.
 */
public class YangRpc extends YangNode implements YangCommonInfo, Parsable,
        CollisionDetector {

    private static final long serialVersionUID = 806201613L;

    /**
     * Name of the rpc.
     */
    private String name;

    /**
     * Description of rpc.
     */
    private String description;

    /**
     * Reference of the module.
     */
    private String reference;

    /**
     * Status of the node.
     */
    private YangStatusType status = YangStatusType.CURRENT;

    /**
     * Create a rpc node.
     */
    public YangRpc() {
        super(YangNodeType.RPC_NODE);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public void detectCollidingChild(String identifierName, YangConstructType dataType) throws DataModelException {
        // Detect colliding child.
        detectCollidingChildUtil(identifierName, dataType, this);
    }

    @Override
    public void detectSelfCollision(String identifierName, YangConstructType dataType) throws DataModelException {
        if (getName().equals(identifierName)) {
            throw new DataModelException("YANG file error: Duplicate input identifier detected, same as rpc \""
                    + getName() + "\"");
        }
    }

    @Override
    public YangConstructType getYangConstructType() {
        return YangConstructType.RPC_DATA;
    }

    @Override
    public void validateDataOnEntry() throws DataModelException {
        //TODO: implement the method.
    }

    @Override
    public void validateDataOnExit() throws DataModelException {
        //TODO: implement the method.
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String getReference() {
        return reference;
    }

    @Override
    public void setReference(String reference) {
        this.reference = reference;
    }

    @Override
    public YangStatusType getStatus() {
        return status;
    }

    @Override
    public void setStatus(YangStatusType status) {
        this.status = status;
    }
}
