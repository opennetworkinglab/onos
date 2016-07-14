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
 * Handles the YANG's Uint16 data type processing.
 *
 * Uint16 represents integer values between 0 and 65535, inclusively.
 */
public class YangUint16 implements YangBuiltInDataTypeInfo<YangUint16>, Serializable {

    private static final long serialVersionUID = 8006201663L;

    /**
     * YANG's min keyword.
     */
    private static final String MIN_KEYWORD = "min";

    /**
     * YANG's max keyword.
     */
    private static final String MAX_KEYWORD = "max";

    /**
     * Valid minimum value of YANG's Uint16.
     */
    public static final int MIN_VALUE = 0;

    /**
     * Valid maximum value of YANG's Uint16.
     */
    public static final int MAX_VALUE = 65535;

    /**
     * Value of the object.
     */
    private int value;

    /**
     * Creates an object with the value initialized with value represented in
     * string.
     *
     * @param valueInString value of the object in string
     */
    YangUint16(String valueInString) {

        if (valueInString.matches(MIN_KEYWORD)) {
            value = MIN_VALUE;
        } else if (valueInString.matches(MAX_KEYWORD)) {
            value = MAX_VALUE;
        } else {
            try {
                value = Integer.parseInt(valueInString);
            } catch (Exception e) {
                throw new DataTypeException("YANG file error : Input value \"" + valueInString + "\" is not a valid " +
                        "uint16.");
            }
        }

        if (value < MIN_VALUE) {
            throw new DataTypeException("YANG file error : " + valueInString + " is lesser than minimum value "
                    + MIN_VALUE + ".");
        } else if (value > MAX_VALUE) {
            throw new DataTypeException("YANG file error : " + valueInString + " is greater than maximum value "
                    + MAX_VALUE + ".");
        }
    }

    /**
     * Returns YANG's uint16 value.
     *
     * @return value of YANG's uint16
     */
    public int getValue() {
        return value;
    }

    @Override
    public int compareTo(YangUint16 another) {
        return Integer.compare(value, another.value);
    }

    @Override
    public YangDataTypes getYangType() {
        return YangDataTypes.UINT16;
    }

}
