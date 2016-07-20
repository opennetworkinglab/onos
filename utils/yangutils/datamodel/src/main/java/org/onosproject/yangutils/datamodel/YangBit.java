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
 *  The "bit" statement, which is a sub-statement to the "type" statement,
 *  MUST be present if the type is "bits".  It is repeatedly used to
 *  specify each assigned named bit of a bits type.  It takes as an
 *  argument a string that is the assigned name of the bit.  It is
 *  followed by a block of sub-statements that holds detailed bit
 *  information.
 *  All assigned names in a bits type MUST be unique.
 *
 *  The bit's sub-statements
 *
 *                +--------------+---------+-------------+------------------+
 *                | substatement | section | cardinality |data model mapping|
 *                +--------------+---------+-------------+------------------+
 *                | description  | 7.19.3  | 0..1        | - string         |
 *                | reference    | 7.19.4  | 0..1        | - string         |
 *                | status       | 7.19.2  | 0..1        | - YangStatus     |
 *                | position     | 9.7.4.2 | 0..1        | - int            |
 *                +--------------+---------+-------------+------------------+
 */

/**
 * Represents the bit data type information.
 */
public class YangBit implements YangCommonInfo, Parsable, Serializable {

    private static final long serialVersionUID = 806201640L;

    /**
     * Name of the bit.
     */
    private String bitName;

    /**
     * Description of the bit field.
     */
    private String description;

    /**
     * Reference info of the bit field.
     */
    private String reference;

    /**
     * Status of the bit field.
     */
    private YangStatusType status;

    /**
     * Position of the bit whose name bit is described.
     */
    private int position;

    /**
     * Create a YANG bit type object.
     */
    public YangBit() {

    }

    /**
     * Returns bit name.
     *
     * @return the bit name
     */
    public String getBitName() {
        return bitName;
    }

    /**
     * Sets the bit name.
     *
     * @param bitName the bit name to set
     */
    public void setBitName(String bitName) {
        this.bitName = bitName;
    }

    /**
     * Returns description.
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
     * Returns textual reference.
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
     * Returns status.
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
     * Returns bit position.
     *
     * @return the position
     */
    public int getPosition() {
        return position;
    }

    /**
     * Sets the bit position.
     *
     * @param position the position to set
     */
    public void setPosition(int position) {
        this.position = position;
    }

    /**
     * Returns the type of the data.
     *
     * @return ParsedDataType returns BIT_DATA
     */
    @Override
    public YangConstructType getYangConstructType() {
        return YangConstructType.BIT_DATA;
    }

    @Override
    public boolean equals(Object obj) {

        if (this == obj) {
            return true;
        }
        if (obj instanceof YangBit) {
            final YangBit other = (YangBit) obj;
            return Objects.equals(bitName, other.bitName);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(bitName);
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
}
