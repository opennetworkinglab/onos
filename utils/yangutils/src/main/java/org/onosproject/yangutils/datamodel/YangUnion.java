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

import org.onosproject.yangutils.datamodel.exceptions.DataModelException;
import org.onosproject.yangutils.parser.Parsable;
import org.onosproject.yangutils.utils.YangConstructType;

import java.util.LinkedList;
import java.util.List;

/*
 * Reference RFC 6020.
 *
 * The union built-in type represents a value that corresponds to one of
 * its member types.
 *
 * When the type is "union", the "type" statement (Section 7.4) MUST be
 * present.  It is used to repeatedly specify each member type of the
 * union.  It takes as an argument a string that is the name of a member
 * type.
 *
 * A member type can be of any built-in or derived type, except it MUST
 * NOT be one of the built-in types "empty" or "leafref".
 *
 * When a string representing a union data type is validated, the string
 * is validated against each member type, in the order they are
 * specified in the "type" statement, until a match is found.
 *
 * Any default value or "units" property defined in the member types is
 * not inherited by the union type.
 */

/**
 * Data model node to maintain information defined in YANG union.
 */
public class YangUnion implements Parsable {

    // List of YANG type.
    private List<YangType<?>> typeList;

    // Name of union.
    private String unionName;

    /**
     * Create a YANG union node.
     */
    public YangUnion() {
        typeList = new LinkedList<>();
    }

    /**
     * Returns list of YANG type.
     *
     * @return the list of YANG type
     */
    public List<YangType<?>> getTypeList() {
        return typeList;
    }

    /**
     * Returns union name.
     *
     * @return the union name
     */
    public String getUnionName() {
        return unionName;
    }

    /**
     * Set the list of YANG type.
     *
     * @param typeList list of YANG type.
     */
    public void setTypeList(List<YangType<?>> typeList) {
        this.typeList = typeList;
    }

    /**
     * Add YANG type to type list.
     *
     * @param yangType YANG type to be added to list
     * @throws DataModelException union member type must not be one of the
     * built-in types "empty" or "leafref"
     */
    public void addToTypeList(YangType<?> yangType) throws DataModelException {
        if (yangType.getDataType() == YangDataTypes.EMPTY || yangType.getDataType() == YangDataTypes.LEAFREF) {
            throw new DataModelException("Union member type must not be one of the built-in types \"empty\" or " +
                    "\"leafref\"");
        }
        getTypeList().add(yangType);
    }

    /**
     * Set the union name.
     *
     * @param unionName name of the union.
     */
    public void setUnionName(String unionName) {
        this.unionName = unionName;
    }

    @Override
    public YangConstructType getYangConstructType() {
        return YangConstructType.UNION_DATA;
    }

    @Override
    public void validateDataOnEntry() throws DataModelException {
        //TODO: implement the method.
    }

    @Override
    public void validateDataOnExit() throws DataModelException {
        //TODO: implement the method.
    }
}
