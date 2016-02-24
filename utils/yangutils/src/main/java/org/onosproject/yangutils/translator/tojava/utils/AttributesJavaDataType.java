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

package org.onosproject.yangutils.translator.tojava.utils;

import org.onosproject.yangutils.datamodel.YangDataTypes;
import org.onosproject.yangutils.datamodel.YangType;
import org.onosproject.yangutils.translator.tojava.AttributeInfo;
import org.onosproject.yangutils.utils.UtilConstants;

/**
 * Provides java data types corresponding to YANG type.
 */
public final class AttributesJavaDataType {

    /**
     * Default constructor.
     */
    private AttributesJavaDataType() {
    }

    /**
     * Returns YANG type.
     *
     * @param yangType YANG type
     * @return YANG type
     */
    public static YangType<?> getJavaDataType(YangType<?> yangType) {
        yangType.setDataTypeName(yangType.getDataTypeName().replace("\"", ""));
        if (yangType.getDataType() != null) {
            yangType.setDataTypeName(parseYangDataType(yangType.getDataType()));
        }
        return yangType;
    }

    /**
     * Returns list string as attribute name when attribute is a list.
     *
     * @param attr attribute info.
     * @return list attribute
     */
    @SuppressWarnings("rawtypes")
    public static YangType<?> getListString(AttributeInfo attr) {
        String listString = JavaCodeSnippetGen.getListAttribute(attr.getAttributeType().getDataTypeName());
        YangType<?> type = new YangType();
        type.setDataTypeName(listString);
        attr.setAttributeType(type);
        return type;
    }

    /**
     * Parses YANG data type and returns corresponding java data type.
     *
     * @param type YANG data type
     * @return java data type
     */
    private static String parseYangDataType(YangDataTypes type) {
        if (type.equals(YangDataTypes.INT8)) {
            return UtilConstants.BYTE;
        } else if (type.equals(YangDataTypes.INT16)) {
            return UtilConstants.SHORT;
        } else if (type.equals(YangDataTypes.INT32)) {
            return UtilConstants.INT;
        } else if (type.equals(YangDataTypes.INT64)) {
            return UtilConstants.LONG;
        } else if (type.equals(YangDataTypes.UINT8)) {
            return UtilConstants.SHORT;
        } else if (type.equals(YangDataTypes.UINT16)) {
            return UtilConstants.INT;
        } else if (type.equals(YangDataTypes.UINT32)) {
            return UtilConstants.LONG;
        } else if (type.equals(YangDataTypes.UINT64)) {
            //TODO: BIGINTEGER.
        } else if (type.equals(YangDataTypes.DECIMAL64)) {
            //TODO: DECIMAL64.
        } else if (type.equals(YangDataTypes.STRING)) {
            return UtilConstants.STRING;
        } else if (type.equals(YangDataTypes.BOOLEAN)) {
            return UtilConstants.BOOLEAN;
        } else if (type.equals(YangDataTypes.ENUMERATION)) {
            //TODO: ENUMERATION.
        } else if (type.equals(YangDataTypes.BITS)) {
            //TODO:BITS
        } else if (type.equals(YangDataTypes.BINARY)) {
            //TODO:BINARY
        } else if (type.equals(YangDataTypes.LEAFREF)) {
            //TODO:LEAFREF
        } else if (type.equals(YangDataTypes.IDENTITYREF)) {
            //TODO:IDENTITYREF
        } else if (type.equals(YangDataTypes.EMPTY)) {
            //TODO:EMPTY
        } else if (type.equals(YangDataTypes.UNION)) {
            //TODO:UNION
        } else if (type.equals(YangDataTypes.INSTANCE_IDENTIFIER)) {
            //TODO:INSTANCE_IDENTIFIER
        } else if (type.equals(YangDataTypes.DERIVED)) {
            //TODO:DERIVED
        }
        return null;
    }
}
