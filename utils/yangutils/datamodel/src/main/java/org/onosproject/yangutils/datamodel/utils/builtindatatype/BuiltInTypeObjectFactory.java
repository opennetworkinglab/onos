/*-
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
package org.onosproject.yangutils.datamodel.utils.builtindatatype;

import java.io.Serializable;

import org.onosproject.yangutils.datamodel.YangDataTypes;

/**
 * Factory to create an object of required type.
 */
public final class BuiltInTypeObjectFactory implements Serializable {

    private static final long serialVersionUID = 8006201671L;

    /**
     * Utility factory class, hence the object creation is forbidden.
     */
    private BuiltInTypeObjectFactory() {
    }

    /**
     * Given the value represented in string return the corresponding types
     * object with the value initialized.
     *
     * @param valueInStr  value represented in string
     * @param builtInType built in data type
     * @param <T>         the data type of the target object
     * @return the target data type object with the value initialized
     */
    public static <T extends YangBuiltInDataTypeInfo<?>> T getDataObjectFromString(String valueInStr,
            YangDataTypes builtInType) {

        switch (builtInType) {
        case INT8: {
            return (T) new YangInt8(valueInStr);
        }
        case INT16: {
            return (T) new YangInt16(valueInStr);
        }
        case INT32: {
            return (T) new YangInt32(valueInStr);
        }
        case INT64: {
            return (T) new YangInt64(valueInStr);
        }
        case UINT8: {
            return (T) new YangUint8(valueInStr);
        }
        case UINT16: {
            return (T) new YangUint16(valueInStr);
        }
        case UINT32: {
            return (T) new YangUint32(valueInStr);
        }
        case UINT64: {
            return (T) new YangUint64(valueInStr);
        }
        default: {
            throw new DataTypeException("YANG file error : Unsupported data type");
        }
        }

    }

}
