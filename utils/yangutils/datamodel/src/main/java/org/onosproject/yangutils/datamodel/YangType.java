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

import org.onosproject.yangutils.datamodel.exceptions.DataModelException;
import org.onosproject.yangutils.datamodel.utils.Parsable;
import org.onosproject.yangutils.datamodel.utils.ResolvableStatus;
import org.onosproject.yangutils.datamodel.utils.YangConstructType;
import org.onosproject.yangutils.datamodel.utils.builtindatatype.YangDataTypes;

import static org.onosproject.yangutils.datamodel.BuiltInTypeObjectFactory.getDataObjectFromString;
import static org.onosproject.yangutils.datamodel.utils.builtindatatype.YangDataTypeUtils.isOfRangeRestrictedType;
import static org.onosproject.yangutils.datamodel.utils.builtindatatype.YangDataTypes.DERIVED;

/*
 * Reference:RFC 6020.
 * The "type" statement takes as an argument a string that is the name
 *  of a YANG built-in type or a derived type, followed by an optional
 *  block of sub-statements that are used to put further restrictions
 *  on the type.
 *
 *  The restrictions that can be applied depend on the type being restricted.
 *  The type's sub-statements
 *
 * +------------------+---------+-------------+------------------------------------+
 * | substatement     | section | cardinality | mapped data type                   |
 * +------------------+---------+-------------+------------------------------------+
 * | bit              | 9.7.4   | 0..n        | - YangBit used in YangBits         |
 * | enum             | 9.6.4   | 0..n        | - YangEnum used in YangEnumeration |
 * | length           | 9.4.4   | 0..1        | - used for string                  |
 * | path             | 9.9.2   | 0..1        | - path for referred leaf/leaf-list |
 * | pattern          | 9.4.6   | 0..n        | - used for string                  |
 * | range            | 9.2.4   | 0..1        | - used for integer data type       |
 * | require-instance | 9.13.2  | 0..1        | - TODO instance-identifier         |
 * | type             | 7.4     | 0..n        | - TODO union                       |
 * +------------------+---------+-------------+------------------------------------+
 */

/**
 * Represents the data type information.
 *
 * @param <T> YANG data type info
 */
public class YangType<T>
        implements Parsable, Resolvable, Serializable {

    private static final long serialVersionUID = 8062016054L;

    /**
     * YANG node identifier.
     */
    private YangNodeIdentifier nodeIdentifier;

    /**
     * YANG data type.
     */
    private YangDataTypes dataType;

    /**
     * Additional information about data type, example restriction info, named
     * values, etc. The extra information is based on the data type. Based on
     * the data type, the extended info can vary.
     */
    private T dataTypeExtendedInfo;

    /**
     * Status of resolution. If completely resolved enum value is "RESOLVED",
     * if not enum value is "UNRESOLVED", in case reference of grouping/typedef
     * is added to uses/type but it's not resolved value of enum should be
     * "INTRA_FILE_RESOLVED".
     */
    private ResolvableStatus resolvableStatus;

    /**
     * Creates a YANG type object.
     */
    public YangType() {

        nodeIdentifier = new YangNodeIdentifier();
        resolvableStatus = ResolvableStatus.UNRESOLVED;
    }

    /**
     * Returns prefix associated with data type name.
     *
     * @return prefix associated with data type name
     */
    public String getPrefix() {
        return nodeIdentifier.getPrefix();
    }

    /**
     * Sets prefix associated with data type name.
     *
     * @param prefix prefix associated with data type name
     */
    public void setPrefix(String prefix) {
        nodeIdentifier.setPrefix(prefix);
    }

    /**
     * Returns the name of data type.
     *
     * @return the name of data type
     */
    public String getDataTypeName() {
        return nodeIdentifier.getName();
    }

    /**
     * Sets the name of the data type.
     *
     * @param typeName the name to set
     */
    public void setDataTypeName(String typeName) {
        nodeIdentifier.setName(typeName);
    }

    /**
     * Returns the type of data.
     *
     * @return the data type
     */
    public YangDataTypes getDataType() {
        return dataType;
    }

    /**
     * Sets the type of data.
     *
     * @param dataType data type
     */
    public void setDataType(YangDataTypes dataType) {
        this.dataType = dataType;
    }

    /**
     * Returns the data type meta data.
     *
     * @return the data type meta data
     */
    public T getDataTypeExtendedInfo() {
        return dataTypeExtendedInfo;
    }

    /**
     * Sets the data type meta data.
     *
     * @param dataTypeInfo the meta data to set
     */
    public void setDataTypeExtendedInfo(T dataTypeInfo) {
        this.dataTypeExtendedInfo = dataTypeInfo;
    }

    /**
     * Returns node identifier.
     *
     * @return node identifier
     */
    public YangNodeIdentifier getNodeIdentifier() {
        return nodeIdentifier;
    }

    /**
     * Sets node identifier.
     *
     * @param nodeIdentifier the node identifier
     */
    public void setNodeIdentifier(YangNodeIdentifier nodeIdentifier) {
        this.nodeIdentifier = nodeIdentifier;
    }

    /**
     * Resets the class attributes to its default value.
     */
    public void resetYangType() {
        nodeIdentifier = new YangNodeIdentifier();
        resolvableStatus = ResolvableStatus.UNRESOLVED;
        dataType = null;
        dataTypeExtendedInfo = null;
    }

    /**
     * Returns the type of the parsed data.
     *
     * @return returns TYPE_DATA
     */
    @Override
    public YangConstructType getYangConstructType() {
        return YangConstructType.TYPE_DATA;
    }

    /**
     * Validates the data on entering the corresponding parse tree node.
     *
     * @throws DataModelException a violation of data model rules
     */
    @Override
    public void validateDataOnEntry()
            throws DataModelException {
        // TODO auto-generated method stub, to be implemented by parser
    }

    /**
     * Validates the data on exiting the corresponding parse tree node.
     *
     * @throws DataModelException a violation of data model rules
     */
    @Override
    public void validateDataOnExit()
            throws DataModelException {
        // TODO auto-generated method stub, to be implemented by parser
    }

    @Override
    public ResolvableStatus getResolvableStatus() {
        return resolvableStatus;
    }

    @Override
    public void setResolvableStatus(ResolvableStatus resolvableStatus) {
        this.resolvableStatus = resolvableStatus;
    }

    @Override
    public Object resolve()
            throws DataModelException {
        /*
         * Check whether the data type is derived.
         */
        if (getDataType() != DERIVED) {
            throw new DataModelException("Linker Error: Resolve should only be called for derived data types.");
        }

        // Check if the derived info is present.
        YangDerivedInfo<?> derivedInfo = (YangDerivedInfo<?>) getDataTypeExtendedInfo();
        if (derivedInfo == null) {
            throw new DataModelException("Linker Error: Derived information is missing.");
        }

        // Initiate the resolution
        try {
            setResolvableStatus(derivedInfo.resolve());
        } catch (DataModelException e) {
            throw new DataModelException(e.getMessage());
        }
        return null;
    }

    /**
     * Validates the input data value against the permissible value for the
     * type as per the YANG file.
     *
     * @param value input data value
     * @return status of validation
     */
    public boolean isValidValue(String value) {
        switch (getDataType()) {
            case INT8:
            case INT16:
            case INT32:
            case INT64:
            case UINT8:
            case UINT16:
            case UINT32:
            case UINT64: {
                isValidValueForRangeRestrictedType(value);
            }
            case DECIMAL64: {
                // TODO
            }
            case STRING: {
                // TODO implement in string restriction similar to range restriction
            }
            case ENUMERATION: {
                // TODO validate using list of YANG enum of enumeration class in extended info.
            }
            case BINARY: {
                // TODO validate based on extended info
            }
            case BITS: {
                // TODO validate based on extended info
            }
            case BOOLEAN: {
                // TODO true or false
            }
            case LEAFREF: {
                // TODO validate based on extended info
            }
            case IDENTITYREF: {
                // TODO TBD
            }
            case EMPTY: {
                // TODO true or false
            }
            case UNION: {
                // TODO validate based on extended info
            }
            case INSTANCE_IDENTIFIER: {
                // TODO TBD
            }
            case DERIVED: {
                if (isOfRangeRestrictedType(((YangDerivedInfo) getDataTypeExtendedInfo()).getEffectiveBuiltInType())) {
                    try {
                        if (((YangDerivedInfo) getDataTypeExtendedInfo()).getResolvedExtendedInfo() == null) {
                            getDataObjectFromString(value,
                                    ((YangDerivedInfo) getDataTypeExtendedInfo()).getEffectiveBuiltInType());
                            return true;
                        } else {
                            return ((YangRangeRestriction) ((YangDerivedInfo) getDataTypeExtendedInfo())
                                    .getResolvedExtendedInfo()).isValidValueString(value);
                        }
                    } catch (Exception e) {
                        return false;
                    }
                } else {
                    // TODO
                }
            }
            default: {
                // TODO
            }
        }
        return true;
    }

    /**
     * Validates the input data value for range restricted types against the
     * permissible value for the type as per the YANG file.
     *
     * @param value input data value
     * @return status of validation
     */
    private boolean isValidValueForRangeRestrictedType(String value) {
        try {
            if (getDataTypeExtendedInfo() == null) {
                getDataObjectFromString(value, getDataType());
                return true;
            } else {
                return ((YangRangeRestriction) getDataTypeExtendedInfo()).isValidValueString(value);
            }
        } catch (Exception e) {
            return false;
        }
    }
}
