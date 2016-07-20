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

package org.onosproject.yangutils.datamodel.utils.builtindatatype;

import java.io.Serializable;

import org.onosproject.yangutils.datamodel.YangDataTypes;

/**
 * Handles the YANG's int16 data type processing.
 *
 * int16 represents integer values between -32768 and 32767, inclusively.
 */
public class YangInt16 implements YangBuiltInDataTypeInfo<YangInt16>, Serializable {

    private static final long serialVersionUID = 8006201667L;

    /**
     * YANG's min keyword.
     */
    private static final String MIN_KEYWORD = "min";

    /**
     * YANG's max keyword.
     */
    private static final String MAX_KEYWORD = "max";

    /**
     * Valid minimum value of YANG's int16.
     */
    public static final short MIN_VALUE = -32768;

    /**
     * Valid maximum value of YANG's int16.
     */
    public static final short MAX_VALUE = 32767;

    /**
     * The value of YANG's int16.
     */
    private final short value;

    /**
     * Creates an object with the value initialized with value represented in
     * string.
     *
     * @param valueInString value of the object in string
     */
    public YangInt16(String valueInString) {

        if (valueInString.matches(MIN_KEYWORD)) {
            value = MIN_VALUE;
        } else if (valueInString.matches(MAX_KEYWORD)) {
            value = MAX_VALUE;
        } else {
            try {
                value = Short.parseShort(valueInString);
            } catch (Exception e) {
                throw new DataTypeException("YANG file error : Input value \"" + valueInString + "\" is not a valid " +
                        "int16.");
            }
        }
    }

    /**
     * Returns YANG's int16 value.
     *
     * @return value of YANG's int16
     */
    public short getValue() {
        return value;
    }

    @Override
    public int compareTo(YangInt16 anotherYangInt16) {
        return Short.compare(value, anotherYangInt16.value);
    }

    @Override
    public YangDataTypes getYangType() {
        return YangDataTypes.INT16;
    }

}
