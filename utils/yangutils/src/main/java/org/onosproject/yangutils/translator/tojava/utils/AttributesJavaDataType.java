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

import java.util.Set;
import java.util.TreeSet;

import org.onosproject.yangutils.datamodel.YangDataTypes;
import org.onosproject.yangutils.datamodel.YangType;
import org.onosproject.yangutils.translator.tojava.JavaQualifiedTypeInfo;

import static org.onosproject.yangutils.datamodel.YangDataTypes.BINARY;
import static org.onosproject.yangutils.datamodel.YangDataTypes.BITS;
import static org.onosproject.yangutils.datamodel.YangDataTypes.BOOLEAN;
import static org.onosproject.yangutils.datamodel.YangDataTypes.DECIMAL64;
import static org.onosproject.yangutils.datamodel.YangDataTypes.DERIVED;
import static org.onosproject.yangutils.datamodel.YangDataTypes.EMPTY;
import static org.onosproject.yangutils.datamodel.YangDataTypes.ENUMERATION;
import static org.onosproject.yangutils.datamodel.YangDataTypes.IDENTITYREF;
import static org.onosproject.yangutils.datamodel.YangDataTypes.INSTANCE_IDENTIFIER;
import static org.onosproject.yangutils.datamodel.YangDataTypes.INT16;
import static org.onosproject.yangutils.datamodel.YangDataTypes.INT32;
import static org.onosproject.yangutils.datamodel.YangDataTypes.INT64;
import static org.onosproject.yangutils.datamodel.YangDataTypes.INT8;
import static org.onosproject.yangutils.datamodel.YangDataTypes.LEAFREF;
import static org.onosproject.yangutils.datamodel.YangDataTypes.STRING;
import static org.onosproject.yangutils.datamodel.YangDataTypes.UINT16;
import static org.onosproject.yangutils.datamodel.YangDataTypes.UINT32;
import static org.onosproject.yangutils.datamodel.YangDataTypes.UINT64;
import static org.onosproject.yangutils.datamodel.YangDataTypes.UINT8;
import static org.onosproject.yangutils.datamodel.YangDataTypes.UNION;
import static org.onosproject.yangutils.translator.tojava.utils.JavaIdentifierSyntax.getCamelCase;
import static org.onosproject.yangutils.translator.tojava.utils.JavaIdentifierSyntax.getCaptialCase;
import static org.onosproject.yangutils.utils.UtilConstants.BOOLEAN_DATA_TYPE;
import static org.onosproject.yangutils.utils.UtilConstants.BOOLEAN_WRAPPER;
import static org.onosproject.yangutils.utils.UtilConstants.BYTE;
import static org.onosproject.yangutils.utils.UtilConstants.BYTE_WRAPPER;
import static org.onosproject.yangutils.utils.UtilConstants.INT;
import static org.onosproject.yangutils.utils.UtilConstants.INTEGER_WRAPPER;
import static org.onosproject.yangutils.utils.UtilConstants.JAVA_LANG;
import static org.onosproject.yangutils.utils.UtilConstants.LONG;
import static org.onosproject.yangutils.utils.UtilConstants.LONG_WRAPPER;
import static org.onosproject.yangutils.utils.UtilConstants.SHORT;
import static org.onosproject.yangutils.utils.UtilConstants.SHORT_WRAPPER;
import static org.onosproject.yangutils.utils.UtilConstants.STRING_DATA_TYPE;

/**
 * Provides java data types corresponding to YANG type.
 */
public final class AttributesJavaDataType {

    private static Set<JavaQualifiedTypeInfo> importInfo = new TreeSet<>();

    /**
     * Default constructor.
     */
    private AttributesJavaDataType() {
    }

    /**
     * Returns import info.
     *
     * @return import info
     */
    public static Set<JavaQualifiedTypeInfo> getImportInfo() {

        return importInfo;
    }

    /**
     * Adds import info to the import info set.
     *
     * @param importData import info
     */
    public static void addImportInfo(JavaQualifiedTypeInfo importData) {

        getImportInfo().add(importData);
    }

    /**
     * Returns java type.
     *
     * @param yangType YANG type
     * @return java type
     */
    public static String getJavaDataType(YangType<?> yangType) {

        YangDataTypes type = yangType.getDataType();

        if (type.equals(INT8)) {
            return BYTE;
        } else if (type.equals(INT16)) {
            return SHORT;
        } else if (type.equals(INT32)) {
            return INT;
        } else if (type.equals(INT64)) {
            return LONG;
        } else if (type.equals(UINT8)) {
            return SHORT;
        } else if (type.equals(UINT16)) {
            return INT;
        } else if (type.equals(UINT32)) {
            return LONG;
        } else if (type.equals(UINT64)) {
            //TODO: BIGINTEGER.
        } else if (type.equals(DECIMAL64)) {
            //TODO: DECIMAL64.
        } else if (type.equals(STRING)) {
            return STRING_DATA_TYPE;
        } else if (type.equals(BOOLEAN)) {
            return BOOLEAN_DATA_TYPE;
        } else if (type.equals(ENUMERATION)) {
            //TODO: ENUMERATION.
        } else if (type.equals(BITS)) {
            //TODO:BITS
        } else if (type.equals(BINARY)) {
            //TODO:BINARY
        } else if (type.equals(LEAFREF)) {
            //TODO:LEAFREF
        } else if (type.equals(IDENTITYREF)) {
            //TODO:IDENTITYREF
        } else if (type.equals(EMPTY)) {
            //TODO:EMPTY
        } else if (type.equals(UNION)) {
            //TODO:UNION
        } else if (type.equals(INSTANCE_IDENTIFIER)) {
            //TODO:INSTANCE_IDENTIFIER
        } else if (type.equals(DERIVED)) {
            return yangType.getDataTypeName();
        }
        return null;
    }

    /**
     * Returns java import class.
     *
     * @param yangType YANG type
     * @param isListAttr if the attribute need to be a list
     * @return java import class
     */
    public static String getJavaImportClass(YangType<?> yangType, boolean isListAttr) {

        YangDataTypes type = yangType.getDataType();

        if (isListAttr) {
            if (type.equals(INT8)) {
                return BYTE_WRAPPER;
            } else if (type.equals(INT16)) {
                return SHORT_WRAPPER;
            } else if (type.equals(INT32)) {
                return INTEGER_WRAPPER;
            } else if (type.equals(INT64)) {
                return LONG_WRAPPER;
            } else if (type.equals(UINT8)) {
                return SHORT_WRAPPER;
            } else if (type.equals(UINT16)) {
                return INTEGER_WRAPPER;
            } else if (type.equals(UINT32)) {
                return LONG_WRAPPER;
            } else if (type.equals(UINT64)) {
                //TODO: BIGINTEGER.
            } else if (type.equals(DECIMAL64)) {
                //TODO: DECIMAL64.
            } else if (type.equals(STRING)) {
                return STRING_DATA_TYPE;
            } else if (type.equals(BOOLEAN)) {
                return BOOLEAN_WRAPPER;
            } else if (type.equals(ENUMERATION)) {
                //TODO: ENUMERATION.
            } else if (type.equals(BITS)) {
                //TODO:BITS
            } else if (type.equals(BINARY)) {
                //TODO:BINARY
            } else if (type.equals(LEAFREF)) {
                //TODO:LEAFREF
            } else if (type.equals(IDENTITYREF)) {
                //TODO:IDENTITYREF
            } else if (type.equals(EMPTY)) {
                //TODO:EMPTY
            } else if (type.equals(UNION)) {
                //TODO:UNION
            } else if (type.equals(INSTANCE_IDENTIFIER)) {
                //TODO:INSTANCE_IDENTIFIER
            } else if (type.equals(DERIVED)) {
                return getCaptialCase(getCamelCase(yangType.getDataTypeName()));
            }
        } else {
            if (type.equals(UINT64)) {
                //TODO: BIGINTEGER.
            } else if (type.equals(DECIMAL64)) {
                //TODO: DECIMAL64.
            } else if (type.equals(STRING)) {
                return STRING_DATA_TYPE;
            } else if (type.equals(ENUMERATION)) {
                //TODO: ENUMERATION.
            } else if (type.equals(BITS)) {
                //TODO:BITS
            } else if (type.equals(BINARY)) {
                //TODO:BINARY
            } else if (type.equals(LEAFREF)) {
                //TODO:LEAFREF
            } else if (type.equals(IDENTITYREF)) {
                //TODO:IDENTITYREF
            } else if (type.equals(EMPTY)) {
                //TODO:EMPTY
            } else if (type.equals(UNION)) {
                //TODO:UNION
            } else if (type.equals(INSTANCE_IDENTIFIER)) {
                //TODO:INSTANCE_IDENTIFIER
            } else if (type.equals(DERIVED)) {
                return getCaptialCase(getCamelCase(yangType.getDataTypeName()));
            }
        }
        return null;
    }

    /**
     * Returns java import package.
     *
     * @param yangType YANG type
     * @param isListAttr if the attribute is of list type
     * @param classInfo java import class info
     * @return java import package
     */
    public static String getJavaImportPackage(YangType<?> yangType, boolean isListAttr, String classInfo) {

        YangDataTypes type = yangType.getDataType();

        if (isListAttr) {
            if (type.equals(INT8)
                    || type.equals(INT16)
                    || type.equals(INT32)
                    || type.equals(INT64)
                    || type.equals(UINT8)
                    || type.equals(UINT16)
                    || type.equals(UINT32)
                    || type.equals(STRING)
                    || type.equals(BOOLEAN)) {
                return JAVA_LANG;
            } else if (type.equals(UINT64)) {
                //TODO: BIGINTEGER.
            } else if (type.equals(DECIMAL64)) {
                //TODO: DECIMAL64.
            } else if (type.equals(ENUMERATION)) {
                //TODO: ENUMERATION.
            } else if (type.equals(BITS)) {
                //TODO:BITS
            } else if (type.equals(BINARY)) {
                //TODO:BINARY
            } else if (type.equals(LEAFREF)) {
                //TODO:LEAFREF
            } else if (type.equals(IDENTITYREF)) {
                //TODO:IDENTITYREF
            } else if (type.equals(EMPTY)) {
                //TODO:EMPTY
            } else if (type.equals(UNION)) {
                //TODO:UNION
            } else if (type.equals(INSTANCE_IDENTIFIER)) {
                //TODO:INSTANCE_IDENTIFIER
            } else if (type.equals(DERIVED)) {
                for (JavaQualifiedTypeInfo imports : getImportInfo()) {
                    if (imports.getClassInfo().equals(classInfo)) {
                        return imports.getPkgInfo();
                    }
                }
            }
        } else {

            if (type.equals(UINT64)) {
                //TODO: BIGINTEGER.
            } else if (type.equals(DECIMAL64)) {
                //TODO: DECIMAL64.
            } else if (type.equals(STRING)) {
                return JAVA_LANG;
            } else if (type.equals(ENUMERATION)) {
                //TODO: ENUMERATION.
            } else if (type.equals(BITS)) {
                //TODO:BITS
            } else if (type.equals(BINARY)) {
                //TODO:BINARY
            } else if (type.equals(LEAFREF)) {
                //TODO:LEAFREF
            } else if (type.equals(IDENTITYREF)) {
                //TODO:IDENTITYREF
            } else if (type.equals(EMPTY)) {
                //TODO:EMPTY
            } else if (type.equals(UNION)) {
                //TODO:UNION
            } else if (type.equals(INSTANCE_IDENTIFIER)) {
                //TODO:INSTANCE_IDENTIFIER
            } else if (type.equals(DERIVED)) {
                for (JavaQualifiedTypeInfo imports : getImportInfo()) {
                    if (imports.getClassInfo().equals(classInfo)) {
                        return imports.getPkgInfo();
                    }
                }
            }
        }
        return null;
    }
}
