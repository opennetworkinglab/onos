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

import java.util.HashSet;
import java.util.Set;

import org.onosproject.yangutils.datamodel.exceptions.DataModelException;
import org.onosproject.yangutils.parser.Parsable;
import org.onosproject.yangutils.utils.YangConstructType;

/*
 * The enumeration built-in type represents values from a set of
 *  assigned names.
 */

/**
 * Maintains the enumeration data type information.
 */
public class YangEnumeration implements Parsable {

    // Enumeration info set.
    private Set<YangEnum> enumSet;

    // Enumeration name.
    private String enumerationName;

    /**
     * Creates an enumeration object.
     */
    public YangEnumeration() {
        setEnumSet(new HashSet<YangEnum>());
    }

    /**
     * Get the ENUM set.
     *
     * @return the ENUM set
     */
    public Set<YangEnum> getEnumSet() {
        return enumSet;
    }

    /**
     * Set the ENUM set.
     *
     * @param enumSet the ENUM set to set
     */
    private void setEnumSet(Set<YangEnum> enumSet) {
        this.enumSet = enumSet;
    }

    /**
     * Add ENUM information.
     *
     * @param enumInfo the ENUM information to be added
     * @throws DataModelException due to violation in data model rules
     */
    public void addEnumInfo(YangEnum enumInfo) throws DataModelException {
        if (!getEnumSet().add(enumInfo)) {
            throw new DataModelException("YANG ENUM already exists");
        }
    }

    /**
     * Return enumeration name.
     *
     * @return the enumeration name
     */
    public String getEnumerationName() {
        return enumerationName;
    }

    /**
     * Set the enumeration name.
     *
     * @param enumerationName enumeration name
     */
    public void setEnumerationName(String enumerationName) {
        this.enumerationName = enumerationName;
    }

    /**
     * Returns the type of the data.
     *
     * @return returns ENUMERATION_DATA
     */
    @Override
    public YangConstructType getYangConstructType() {
        return YangConstructType.ENUMERATION_DATA;
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
}
