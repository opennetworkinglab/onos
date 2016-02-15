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
import org.onosproject.yangutils.parser.ParsableDataType;

/*
 * The enumeration built-in type represents values from a set of
 *  assigned names.
 */

/**
 * Maintains the enumeration data type information.
 */
public class YangEnumeration extends YangNode implements Parsable {

    /**
     * Enumeration info set.
     */
    private Set<YangEnum> enumSet;

    /**
     * Create an enumeration object.
     */
    public YangEnumeration() {
        super(YangNodeType.ENUMERATION_NODE);
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
     * Add ENUM value.
     *
     * @param enumInfo the ENUM value of string
     */
    public void addEnumInfo(YangEnum enumInfo) {

    }

    /**
     * Returns the type of the data.
     *
     * @return returns ENUMERATION_DATA
     */
    public ParsableDataType getParsableDataType() {
        return ParsableDataType.ENUMERATION_DATA;
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
     * @see org.onosproject.yangutils.datamodel.YangNode#getName()
     */
    @Override
    public String getName() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.onosproject.yangutils.datamodel.YangNode#setName(java.lang.String)
     */
    @Override
    public void setName(String name) {
        // TODO Auto-generated method stub

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
