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
import org.onosproject.yangutils.parser.ParsableDataType;

/*-
 * Reference RFC 6020.
 *
 * The "choice" statement defines a set of alternatives, only one of
 *  which may exist at any one time.  The argument is an identifier,
 *  followed by a block of sub-statements that holds detailed choice
 *  information.  The identifier is used to identify the choice node in
 *  the schema tree.  A choice node does not exist in the data tree.
 *
 *  A choice consists of a number of branches, defined with the "case"
 *  sub-statement.  Each branch contains a number of child nodes.  The
 *  nodes from at most one of the choice's branches exist at the same
 *  time.
 *
 *  The choice's sub-statements
 *
 *                +--------------+---------+-------------+------------------+
 *                | substatement | section | cardinality |data model mapping|
 *                +--------------+---------+-------------+------------------+
 *                | anyxml       | 7.10    | 0..n        |-not supported    |
 *                | case         | 7.9.2   | 0..n        |-YangChoice       |
 *                | config       | 7.19.1  | 0..1        |-boolean          |
 *                | container    | 7.5     | 0..n        |-child case nodes |
 *                | default      | 7.9.3   | 0..1        |-string           |
 *                | description  | 7.19.3  | 0..1        |-string           |
 *                | if-feature   | 7.18.2  | 0..n        |-TODO             |
 *                | leaf         | 7.6     | 0..n        |-child case nodes |
 *                | leaf-list    | 7.7     | 0..n        |-child case nodes |
 *                | list         | 7.8     | 0..n        |-child case nodes |
 *                | mandatory    | 7.9.4   | 0..1        |-string           |
 *                | reference    | 7.19.4  | 0..1        |-string           |
 *                | status       | 7.19.2  | 0..1        |-string           |
 *                | when         | 7.19.5  | 0..1        |-TODO             |
 *                +--------------+---------+-------------+------------------+
 */
/**
 * Data model node to maintain information defined in YANG choice.
 */
public class YangChoice extends YangNode implements YangCommonInfo, Parsable {

    /**
     * Name of choice.
     */
    private String name;

    /**
     * List of cases for the current choice.
     */
    private List<YangCase> caseList;

    /**
     * If the choice represents config data.
     */
    private boolean isConfig;

    /**
     * Reference RFC 6020.
     *
     * The "default" statement indicates if a case should be considered as the
     * default if no child nodes from any of the choice's cases exist. The
     * argument is the identifier of the "case" statement. If the "default"
     * statement is missing, there is no default case.
     *
     * The "default" statement MUST NOT be present on choices where "mandatory"
     * is true.
     *
     * The default case is only important when considering the default values of
     * nodes under the cases. The default values for nodes under the default
     * case are used if none of the nodes under any of the cases are present.
     *
     * There MUST NOT be any mandatory nodes directly under the default case.
     *
     * Default values for child nodes under a case are only used if one of the
     * nodes under that case is present, or if that case is the default case. If
     * none of the nodes under a case are present and the case is not the
     * default case, the default values of the cases' child nodes are ignored.
     *
     * the default case to be used if no case members is present.
     */
    private String defaultCase;

    /**
     * Description.
     */
    private String description;

    /**
     * Reference RFC 6020.
     *
     * The "mandatory" statement, which is optional, takes as an argument the
     * string "true" or "false", and puts a constraint on valid data. If
     * "mandatory" is "true", at least one node from exactly one of the choice's
     * case branches MUST exist.
     *
     * If not specified, the default is "false".
     *
     * The behavior of the constraint depends on the type of the choice's
     * closest ancestor node in the schema tree which is not a non-presence
     * container:
     *
     * o If this ancestor is a case node, the constraint is enforced if any
     * other node from the case exists.
     *
     * o Otherwise, it is enforced if the ancestor node exists.
     */
    private String mandatory;

    /**
     * reference of the choice.
     */
    private String reference;

    /**
     * Status of the node.
     */
    private YangStatusType status;

    /**
     * Create a Choice node.
     */
    public YangChoice() {
        super(YangNodeType.CHOICE_NODE);
    }

    /* (non-Javadoc)
     * @see org.onosproject.yangutils.datamodel.YangNode#getName()
     */
    @Override
    public String getName() {
        return name;
    }

    /* (non-Javadoc)
     * @see org.onosproject.yangutils.datamodel.YangNode#setName(java.lang.String)
     */
    @Override
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Get the list of cases.
     *
     * @return the case list
     */
    public List<YangCase> getCaseList() {
        return caseList;
    }

    /**
     * Set the list of cases.
     *
     * @param caseList list of cases.
     */
    private void setCaseList(List<YangCase> caseList) {
        this.caseList = caseList;
    }

    /**
     * Add a case.
     *
     * @param newCase new case for the choice
     */
    public void addCase(YangCase newCase) {
        if (getCaseList() == null) {
            setCaseList(new LinkedList<YangCase>());
        }

        getCaseList().add(newCase);
    }

    /**
     * Get config flag.
     *
     * @return the config flag.
     */
    public boolean isConfig() {
        return isConfig;
    }

    /**
     * Set config flag.
     *
     * @param isCfg the config flag.
     */
    public void setConfig(boolean isCfg) {
        isConfig = isCfg;
    }

    /**
     * Get the default case.
     *
     * @return the default case.
     */
    public String getDefaultCase() {
        return defaultCase;
    }

    /**
     * Set the default case.
     *
     * @param defaultCase the default case to set
     */
    public void setDefaultCase(String defaultCase) {
        this.defaultCase = defaultCase;
    }

    /**
     * Get the mandatory status.
     *
     * @return the mandatory status.
     */
    public String getMandatory() {
        return mandatory;
    }

    /**
     * Set the mandatory status.
     *
     * @param mandatory the mandatory status.
     */
    public void setMandatory(String mandatory) {
        this.mandatory = mandatory;
    }

    /**
     * Get the description.
     *
     * @return the description.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Set the description.
     *
     * @param description set the description.
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Get the textual reference.
     *
     * @return the reference.
     */
    public String getReference() {
        return reference;
    }

    /**
     * Set the textual reference.
     *
     * @param reference the reference to set.
     */
    public void setReference(String reference) {
        this.reference = reference;
    }

    /**
     * Get the status.
     *
     * @return the status.
     */
    public YangStatusType getStatus() {
        return status;
    }

    /**
     * Set the status.
     *
     * @param status the status to set.
     */
    public void setStatus(YangStatusType status) {
        this.status = status;
    }

    /**
     * Returns the type of the data.
     *
     * @return returns CHOICE_DATA
     */
    public ParsableDataType getParsableDataType() {
        return ParsableDataType.CHOICE_DATA;
    }

    /**
     * Validate the data on entering the corresponding parse tree node.
     *
     * @throws DataModelException a violation of data model rules.
     */
    public void validateDataOnEntry() throws DataModelException {
        // TODO auto-generated method stub, to be implemented by parser
    }

    /**
     * Validate the data on exiting the corresponding parse tree node.
     *
     * @throws DataModelException a violation of data model rules.
     */
    public void validateDataOnExit() throws DataModelException {
        // TODO auto-generated method stub, to be implemented by parser
    }

    /* (non-Javadoc)
     * @see org.onosproject.yangutils.datamodel.YangNode#getPackage()
     */
    @Override
    public String getPackage() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.onosproject.yangutils.datamodel.YangNode#setPackage(java.lang.String)
     */
    @Override
    public void setPackage(String pkg) {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see org.onosproject.yangutils.translator.CodeGenerator#generateJavaCodeEntry()
     */
    public void generateJavaCodeEntry() {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see org.onosproject.yangutils.translator.CodeGenerator#generateJavaCodeExit()
     */
    public void generateJavaCodeExit() {
        // TODO Auto-generated method stub

    }
}
