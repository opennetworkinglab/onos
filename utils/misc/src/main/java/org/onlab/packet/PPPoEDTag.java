/*
 * Copyright 2021-present Open Networking Foundation
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

package org.onlab.packet;

import java.util.Arrays;

public class PPPoEDTag {
    protected short type;
    protected short length;
    protected byte[] value;

    // PPPoED tag types
    public static final short PPPOED_TAG_END_OF_LIST = 0x0000;
    public static final short PPPOED_TAG_SERVICE_NAME = 0x0101;
    public static final short PPPOED_TAG_AC_NAME = 0x0102;
    public static final short PPPOED_TAG_HOST_UNIQ = 0x0103;
    public static final short PPPOED_TAG_AC_COOKIE = 0x0104;
    public static final short PPPOED_TAG_VENDOR_SPECIFIC = 0x0105;
    public static final short PPPOED_TAG_RELAY_SESSION_ID = 0x0110;
    public static final short PPPOED_TAG_SERVICE_NAME_ERROR = 0x0201;
    public static final short PPPOED_TAG_AC_SYSTEM_ERROR = 0x0202;
    public static final short PPPOED_TAG_GENERIC_ERROR = 0x0203;

    /**
     * Default constructor.
     */
    public PPPoEDTag() {
    }

    /**
     * Constructs a PPPoED tag with type, length and value.
     *
     * @param type type
     * @param length length
     * @param value value
     */
    public PPPoEDTag(final short type, final short length, final byte[] value) {
        this.type = type;
        this.length = length;
        this.value = value;
    }

    public short getType() {
        return type;
    }

    public void setType(short type) {
        this.type = type;
    }

    public short getLength() {
        return length;
    }

    public void setLength(short length) {
        this.length = length;
    }

    public byte[] getValue() {
        return value;
    }

    public void setValue(byte[] value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "PPPoEDTag{" +
                "type=" + type +
                ", length=" + length +
                ", value=" + Arrays.toString(value) +
                '}';
    }
}
