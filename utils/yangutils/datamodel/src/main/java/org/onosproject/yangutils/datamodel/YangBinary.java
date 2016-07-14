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
import java.util.Base64;

import org.onosproject.yangutils.datamodel.utils.builtindatatype.YangBuiltInDataTypeInfo;
import org.onosproject.yangutils.datamodel.utils.builtindatatype.YangDataTypes;

/*
 * Reference RFC 6020.
 *
 * The binary built-in type represents any binary data,
 * i.e., a sequence of octets.
 */
public class YangBinary implements YangBuiltInDataTypeInfo<YangBinary>, Serializable, Comparable<YangBinary> {

    private static final long serialVersionUID = 2106201608L;

    // Binary data is a decoded value by base64 decoding scheme from data input (jason)
    private byte[] binaryData;

    /**
     * Creates a binary object corresponding to the base 64 encoding value.
     *
     * @param strValue base64 encoded value
     */
    public YangBinary(String strValue) {
        setBinaryData(Base64.getDecoder().decode(strValue));
    }

    /**
     * Retrieves decoded binary data.
     *
     * @return binary data
     */
    public byte[] getBinaryData() {
        return binaryData;
    }

    /**
     * Sets binary data.
     *
     * @param binaryData binary data
     */
    public void setBinaryData(byte[] binaryData) {
        this.binaryData = binaryData;
    }

    /**
     * Encodes binary data by base64 encoding scheme.
     *
     * @return encoded binary data
     */
    public String toString() {
        return Base64.getEncoder()
                     .encodeToString(binaryData);
    }

    @Override
    public YangDataTypes getYangType() {
        return YangDataTypes.BINARY;
    }

    @Override
    public int compareTo(YangBinary o) {
        for (int i = 0, j = 0; i < this.binaryData.length && j < o.binaryData.length; i++, j++) {
            int a = (this.binaryData[i] & 0xff);
            int b = (o.binaryData[j] & 0xff);
            if (a != b) {
                return a - b;
            }
        }
        return this.binaryData.length - o.binaryData.length;
    }
}
