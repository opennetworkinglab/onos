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
 * Handles the YANG's Uint8 data type processing.
 *
 * Uint8 represents integer values between 0 and 255, inclusively.
 */
public class YangUint8 implements YangBuiltInDataTypeInfo<YangUint8>, Serializable {

    private static final long serialVersionUID = 8006201660L;

    /**
     * YANG's min keyword.
     */
    private static final String MIN_KEYWORD = "min";

    /**
     * YANG's max keyword.
     */
    private static final String MAX_KEYWORD = "max";

    /**
     * Valid minimum value of YANG's Uint8.
     */
    public static final short MIN_VALUE = 0;

    /**
     * Valid maximum value of YANG's Uint8.
     */
    public static final short MAX_VALUE = 255;

    /**
     * Value of the object.
     */
    private short value;

    /**
     * Creates an object with the value initialized with value represented in
     * string.
     *
     * @param valueInString value of the object in string
     */
    YangUint8(String valueInString) {

        if (valueInString.matches(MIN_KEYWORD)) {
            value = MIN_VALUE;
        } else if (valueInString.matches(MAX_KEYWORD)) {
            value = MAX_VALUE;
        } else {
            try {
                value = Short.parseShort(valueInString);
            } catch (Exception e) {
                throw new DataTypeException("YANG file error : Input value \"" + valueInString + "\" is not a valid " +
                        "uint8.");
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
     * Returns YANG's uint8 value.
     *
     * @return value of YANG's uint8
     */
    public short getValue() {
        return value;
    }

    @Override
    public int compareTo(YangUint8 another) {
        return Short.compare(value, another.value);
    }

    @Override
    public YangDataTypes getYangType() {
        return YangDataTypes.UINT8;
    }

}
