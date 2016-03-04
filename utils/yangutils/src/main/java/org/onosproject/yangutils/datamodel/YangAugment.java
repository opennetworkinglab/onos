/*
 * Copyright 2016 Open Networking Laboratory
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
import org.onosproject.yangutils.parser.Parsable;
import org.onosproject.yangutils.utils.YangConstructType;
import org.onosproject.yangutils.translator.CachedFileHandle;

/*-
 * Reference RFC 6020.
 *
 * The "augment" statement allows a module or submodule to add to the
 *  schema tree defined in an external module, or the current module and
 *  its submodules, and to add to the nodes from a grouping in a "uses"
 *  statement.  The argument is a string that identifies a node in the
 *  schema tree.  This node is called the augment's target node.  The
 *  target node MUST be either a container, list, choice, case, input,
 *  output, or notification node.  It is augmented with the nodes defined
 *  in the sub-statements that follow the "augment" statement.
 *
 *  The argument string is a schema node identifier.
 *  If the "augment" statement is on the top level in a module or
 *  submodule, the absolute form of a schema node identifier
 *  MUST be used.  If the "augment" statement is a sub-statement to the
 *  "uses" statement, the descendant form MUST be used.
 *
 *  If the target node is a container, list, case, input, output, or
 *  notification node, the "container", "leaf", "list", "leaf-list",
 *  "uses", and "choice" statements can be used within the "augment"
 *  statement.
 *
 *  If the target node is a choice node, the "case" statement, or a case
 *  shorthand statement can be used within the "augment" statement.
 *
 *   If the target node is in another module, then nodes added by the
 *  augmentation MUST NOT be mandatory nodes.
 *
 *  The "augment" statement MUST NOT add multiple nodes with the same
 *  name from the same module to the target node.
 *  The augment's sub-statements
 *
 *                +--------------+---------+-------------+------------------+
 *                | substatement | section | cardinality |data model mapping|
 *                +--------------+---------+-------------+------------------+
 *                | anyxml       | 7.10    | 0..n        |-not supported    |
 *                | case         | 7.9.2   | 0..n        |-child nodes      |
 *                | choice       | 7.9     | 0..n        |-child nodes      |
 *                | container    | 7.5     | 0..n        |-child nodes      |
 *                | description  | 7.19.3  | 0..1        |-string           |
 *                | if-feature   | 7.18.2  | 0..n        |-TODO             |
 *                | leaf         | 7.6     | 0..n        |-YangLeaf         |
 *                | leaf-list    | 7.7     | 0..n        |-YangLeafList     |
 *                | list         | 7.8     | 0..n        |-child nodes      |
 *                | reference    | 7.19.4  | 0..1        |-String           |
 *                | status       | 7.19.2  | 0..1        |-YangStatus       |
 *                | uses         | 7.12    | 0..n        |-child nodes      |
 *                | when         | 7.19.5  | 0..1        |-TODO             |
 *                +--------------+---------+-------------+------------------+
 */
/**
 * Data model node to maintain information defined in YANG augment.
 */
public class YangAugment extends YangNode implements YangLeavesHolder, YangCommonInfo, Parsable {

    /**
     * Augment target node.
     */
    private String targetNode;

    /**
     * Description of augment.
     */
    private String description;

    /**
     * List of leaves.
     */
    private List<YangLeaf> listOfLeaf;

    /**
     * List of leaf-lists.
     */
    private List<YangLeafList> listOfLeafList;

    /**
     * Reference of the YANG augment.
     */
    private String reference;

    /**
     * Status of the node.
     */
    private YangStatusType status;

    /**
     * Package of the generated java code.
     */
    private String pkg;

    /**
     * Create a YANG augment node.
     */
    public YangAugment() {
        super(YangNodeType.AUGMENT_NODE);
    }

    /**
     * Get the augmented node.
     *
     * @return the augmented node
     */
    public String getTargetNode() {
        return targetNode;
    }

    /**
     * Set the augmented node.
     *
     * @param targetNode the augmented node
     */
    public void setTargetNode(String targetNode) {
        this.targetNode = targetNode;
    }

    /**
     * Get the description.
     *
     * @return the description
     */
    @Override
    public String getDescription() {
        return description;
    }

    /**
     * Set the description.
     *
     * @param description set the description
     */
    @Override
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Get the list of leaves.
     *
     * @return the list of leaves
     */
    @Override
    public List<YangLeaf> getListOfLeaf() {
        return listOfLeaf;
    }

    /**
     * Set the list of leaves.
     *
     * @param leafsList the list of leaf to set
     */
    private void setListOfLeaf(List<YangLeaf> leafsList) {
        listOfLeaf = leafsList;
    }

    /**
     * Add a leaf.
     *
     * @param leaf the leaf to be added
     */
    @Override
    public void addLeaf(YangLeaf leaf) {
        if (getListOfLeaf() == null) {
            setListOfLeaf(new LinkedList<YangLeaf>());
        }

        getListOfLeaf().add(leaf);
    }

    /**
     * Get the list of leaf-list.
     *
     * @return the list of leaf-list
     */
    @Override
    public List<YangLeafList> getListOfLeafList() {
        return listOfLeafList;
    }

    /**
     * Set the list of leaf-list.
     *
     * @param listOfLeafList the list of leaf-list to set
     */
    private void setListOfLeafList(List<YangLeafList> listOfLeafList) {
        this.listOfLeafList = listOfLeafList;
    }

    /**
     * Add a leaf-list.
     *
     * @param leafList the leaf-list to be added
     */
    @Override
    public void addLeafList(YangLeafList leafList) {
        if (getListOfLeafList() == null) {
            setListOfLeafList(new LinkedList<YangLeafList>());
        }

        getListOfLeafList().add(leafList);
    }

    /**
     * Get the textual reference.
     *
     * @return the reference
     */
    @Override
    public String getReference() {
        return reference;
    }

    /**
     * Set the textual reference.
     *
     * @param reference the reference to set
     */
    @Override
    public void setReference(String reference) {
        this.reference = reference;
    }

    /**
     * Get the status.
     *
     * @return the status
     */
    @Override
    public YangStatusType getStatus() {
        return status;
    }

    /**
     * Set the status.
     *
     * @param status the status to set
     */
    @Override
    public void setStatus(YangStatusType status) {
        this.status = status;
    }

    /**
     * Returns the type of the data as belongs-to.
     *
     * @return returns AUGMENT_DATA
     */
    @Override
    public YangConstructType getYangConstructType() {
        return YangConstructType.AUGMENT_DATA;
    }

    /**
     * Validate the data on entering the corresponding parse tree node.
     *
     * @throws DataModelException a violation of data model rules
     */
    @Override
    public void validateDataOnEntry() throws DataModelException {
        // TODO auto-generated method stub, to be implemented by parser
    }

    /**
     * Validate the data on exiting the corresponding parse tree node.
     *
     * @throws DataModelException a violation of data model rules
     */
    @Override
    public void validateDataOnExit() throws DataModelException {
        // TODO auto-generated method stub, to be implemented by parser
    }

    /**
     * Get the target nodes name where the augmentation is being done.
     *
     * @return target nodes name where the augmentation is being done
     */
    @Override
    public String getName() {
        return targetNode;
    }

    /**
     * Set the target nodes name where the augmentation is being done.
     *
     * @param name target nodes name where the augmentation is being done
     */
    @Override
    public void setName(String name) {
        targetNode = name;

    }

    /**
     * Get the mapped java package.
     *
     * @return the java package
     */
    @Override
    public String getPackage() {
        return pkg;
    }

    /**
     * Set the mapped java package.
     *
     * @param pakg the package to set
     */
    @Override
    public void setPackage(String pakg) {
        pkg = pakg;

    }

    /**
     * Prepare the information for java code generation corresponding to YANG
     * grouping info.
     *
     * @param codeGenDir code generation directory
     */
    @Override
    public void generateJavaCodeEntry(String codeGenDir) {
        // TODO Auto-generated method stub

    }

    /**
     * Create a java file using the YANG grouping info.
     */
    @Override
    public void generateJavaCodeExit() {
        // TODO Auto-generated method stub

    }

    @Override
    public CachedFileHandle getFileHandle() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void setFileHandle(CachedFileHandle fileHandle) {
        // TODO Auto-generated method stub

    }
}
