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
 * Handles the YANG's Uint32 data type processing.
 *
 * Uint32 represents integer values between 0 and 4294967295, inclusively.
 */
public class YangUint32 implements YangBuiltInDataTypeInfo<YangUint32>, Serializable {

    private static final long serialVersionUID = 8006201662L;

    private static final String MIN_KEYWORD = "min";
    private static final String MAX_KEYWORD = "max";

    /**
     * Valid minimum value of YANG's Uint32.
     */
    public static final long MIN_VALUE = 0;

    /**
     * Valid maximum value of YANG's Uint32.
     */
    public static final long MAX_VALUE = 4294967295L;

    /**
     * Value of the object.
     */
    private long value;

    /**
     * Creates an object with the value initialized with value represented in
     * string.
     *
     * @param valueInString value of the object in string
     */
    YangUint32(String valueInString) {

        if (valueInString.matches(MIN_KEYWORD)) {
            value = MIN_VALUE;
        } else if (valueInString.matches(MAX_KEYWORD)) {
            value = MAX_VALUE;
        } else {
            try {
                value = Long.parseLong(valueInString);
            } catch (Exception e) {
                throw new DataTypeException("YANG file error : Input value \"" + valueInString + "\" is not a valid " +
                        "uint32.");
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
     * Returns YANG's uint32 value.
     *
     * @return value of YANG's uint32
     */
    public long getValue() {
        return value;
    }

    @Override
    public int compareTo(YangUint32 another) {
        return Long.compare(value, another.value);
    }

    @Override
    public YangDataTypes getYangType() {
        return YangDataTypes.UINT32;
    }

}
