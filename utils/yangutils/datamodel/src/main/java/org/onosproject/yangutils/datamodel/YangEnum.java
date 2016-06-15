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

import java.io.Serializable;
import java.util.Objects;

import org.onosproject.yangutils.datamodel.exceptions.DataModelException;
import org.onosproject.yangutils.datamodel.utils.Parsable;
import org.onosproject.yangutils.datamodel.utils.YangConstructType;

/*-
 * The "ENUM" statement, which is a sub-statement to the "type"
 *  statement, MUST be present if the type is "enumeration".  It is
 *  repeatedly used to specify each assigned name of an enumeration type.
 *  It takes as an argument a string which is the assigned name.  The
 *  string MUST NOT be empty and MUST NOT have any leading or trailing
 *  whitespace characters.  The use of Unicode control codes SHOULD be
 *  avoided.
 *
 *  The statement is optionally followed by a block of sub-statements that
 *  holds detailed ENUM information.
 *  All assigned names in an enumeration MUST be unique.
 *
 *  The ENUM's sub-statements
 *
 *                +--------------+---------+-------------+------------------+
 *                | substatement | section | cardinality |data model mapping|
 *                +--------------+---------+-------------+------------------+
 *                | description  | 7.19.3  | 0..1        | - string         |
 *                | reference    | 7.19.4  | 0..1        | - string         |
 *                | status       | 7.19.2  | 0..1        | - YangStatus     |
 *                | value        | 9.6.4.2 | 0..1        | - int            |
 *                +--------------+---------+-------------+------------------+
 */

/**
 * Represents the ENUM data type information.
 */
public class YangEnum implements YangCommonInfo, Parsable, Comparable<YangEnum>, Serializable {

    private static final long serialVersionUID = 806201643L;

    /**
     * Named value for the ENUM.
     */
    private String namedValue;

    /**
     * Description of the ENUM value.
     */
    private String description;

    /**
     * Reference info of the ENUM value.
     */
    private String reference;

    /**
     * Status of the ENUM value.
     */
    private YangStatusType status;

    /**
     * Value of ENUM.
     */
    private int value;

    /**
     * Create a YANG ENUM.
     */
    public YangEnum() {

    }

    /**
     * Returns the named value.
     *
     * @return the named value
     */
    public String getNamedValue() {
        return namedValue;
    }

    /**
     * Sets the named value.
     *
     * @param namedValue the named value to set
     */
    public void setNamedValue(String namedValue) {
        this.namedValue = namedValue;
    }

    /**
     * Returns the description.
     *
     * @return the description
     */
    @Override
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description.
     *
     * @param description set the description
     */
    @Override
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Returns the textual reference.
     *
     * @return the reference
     */
    @Override
    public String getReference() {
        return reference;
    }

    /**
     * Sets the textual reference.
     *
     * @param reference the reference to set
     */
    @Override
    public void setReference(String reference) {
        this.reference = reference;
    }

    /**
     * Returns the status.
     *
     * @return the status
     */
    @Override
    public YangStatusType getStatus() {
        return status;
    }

    /**
     * Sets the status.
     *
     * @param status the status to set
     */
    @Override
    public void setStatus(YangStatusType status) {
        this.status = status;
    }

    /**
     * Returns the value.
     *
     * @return the value
     */
    public int getValue() {
        return value;
    }

    /**
     * Sets the value.
     *
     * @param value the value to set
     */
    public void setValue(int value) {
        this.value = value;
    }

    /**
     * Returns the type of the data.
     *
     * @return ParsedDataType returns ENUM_DATA
     */
    @Override
    public YangConstructType getYangConstructType() {
        return YangConstructType.ENUM_DATA;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof YangEnum) {
            final YangEnum other = (YangEnum) obj;
            return Objects.equals(namedValue, other.namedValue);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(namedValue);
    }

    /**
     * Validates the data on entering the corresponding parse tree node.
     *
     * @throws DataModelException a violation of data model rules
     */
    @Override
    public void validateDataOnEntry() throws DataModelException {
        // TODO auto-generated method stub, to be implemented by parser
    }

    /**
     * Validates the data on exiting the corresponding parse tree node.
     *
     * @throws DataModelException a violation of data model rules
     */
    @Override
    public void validateDataOnExit() throws DataModelException {
        // TODO auto-generated method stub, to be implemented by parser
    }

    @Override
    public int compareTo(YangEnum otherEnum) {
        if (namedValue.equals(otherEnum.getNamedValue())) {
            return 0;
        }
        return new Integer(value).compareTo(otherEnum.getValue());
    }
}
