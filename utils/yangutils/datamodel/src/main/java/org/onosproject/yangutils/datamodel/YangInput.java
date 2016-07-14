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

import java.util.LinkedList;
import java.util.List;

import org.onosproject.yangutils.datamodel.exceptions.DataModelException;
import org.onosproject.yangutils.datamodel.utils.Parsable;
import org.onosproject.yangutils.datamodel.utils.YangConstructType;

import static org.onosproject.yangutils.datamodel.utils.DataModelUtils.detectCollidingChildUtil;

/*
 * Reference RFC 6020.
 *
 * The "input" statement, which is optional, is used to define input
 * parameters to the RPC operation.  It does not take an argument.  The
 * substatements to "input" define nodes under the RPC's input node.
 *
 * If a leaf in the input tree has a "mandatory" statement with the
 * value "true", the leaf MUST be present in a NETCONF RPC invocation.
 * Otherwise, the server MUST return a "missing-element" error.
 *
 * If a leaf in the input tree has a default value, the NETCONF server
 * MUST use this value in the same cases as described in Section 7.6.1.
 * In these cases, the server MUST operationally behave as if the leaf
 * was present in the NETCONF RPC invocation with the default value as
 * its value.
 *
 * If a "config" statement is present for any node in the input tree,
 * the "config" statement is ignored.
 *
 * If any node has a "when" statement that would evaluate to false, then
 * this node MUST NOT be present in the input tree.
 *
 * The input substatements
 *
 *     +--------------+---------+-------------+------------------+
 *     | substatement | section | cardinality |data model mapping|
 *     +--------------+---------+-------------+------------------+
 *     | anyxml       | 7.10    | 0..n        | -not supported   |
 *     | choice       | 7.9     | 0..n        | -child nodes     |
 *     | container    | 7.5     | 0..n        | -child nodes     |
 *     | grouping     | 7.11    | 0..n        | -child nodes     |
 *     | leaf         | 7.6     | 0..n        | -YangLeaf        |
 *     | leaf-list    | 7.7     | 0..n        | -YangLeafList    |
 *     | list         | 7.8     | 0..n        | -child nodes     |
 *     | typedef      | 7.3     | 0..n        | -child nodes     |
 *     | uses         | 7.12    | 0..n        | -child nodes     |
 *     +--------------+---------+-------------+------------------+
 */

/**
 * Represents data model node to maintain information defined in YANG input.
 */
public class YangInput
        extends YangNode
        implements YangLeavesHolder, Parsable, CollisionDetector, YangAugmentationHolder {

    private static final long serialVersionUID = 806201608L;

    /**
     * Name of the input.
     */
    private String name;

    /**
     * List of leaves contained.
     */
    private List<YangLeaf> listOfLeaf;

    /**
     * List of leaf-lists contained.
     */
    private List<YangLeafList> listOfLeafList;

    /**
     * Create a rpc input node.
     */
    public YangInput() {
        super(YangNodeType.INPUT_NODE);
        listOfLeaf = new LinkedList<YangLeaf>();
        listOfLeafList = new LinkedList<YangLeafList>();
    }

    @Override
    public void detectCollidingChild(String identifierName, YangConstructType dataType)
            throws DataModelException {
        // Detect colliding child.
        detectCollidingChildUtil(identifierName, dataType, this);
    }

    @Override
    public void detectSelfCollision(String identifierName, YangConstructType dataType)
            throws DataModelException {
        if (getName().equals(identifierName)) {
            throw new DataModelException("YANG file error: Duplicate input identifier detected, same as input \""
                    + getName() + "\"");
        }
    }

    @Override
    public YangConstructType getYangConstructType() {
        return YangConstructType.INPUT_DATA;
    }

    @Override
    public void validateDataOnEntry()
            throws DataModelException {
        //TODO: implement the method.
    }

    @Override
    public void validateDataOnExit()
            throws DataModelException {
        //TODO: implement the method.
    }

    @Override
    public List<YangLeaf> getListOfLeaf() {
        return listOfLeaf;
    }

    @Override
    public void setListOfLeaf(List<YangLeaf> leafsList) {
        listOfLeaf = leafsList;
    }


    @Override
    public void addLeaf(YangLeaf leaf) {
        getListOfLeaf().add(leaf);
    }

    @Override
    public List<YangLeafList> getListOfLeafList() {
        return listOfLeafList;
    }

    @Override
    public void setListOfLeafList(List<YangLeafList> listOfLeafList) {
        this.listOfLeafList = listOfLeafList;
    }

    @Override
    public void addLeafList(YangLeafList leafList) {
        getListOfLeafList().add(leafList);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }
}
