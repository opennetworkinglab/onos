/*
 *
 *  * Copyright 2015 AT&T Foundry
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.onlab.packet;

import java.util.Arrays;

/**
 * An attribute in a RADIUS packet.
 */
public class RADIUSAttribute {
    protected byte type;
    protected byte length;
    protected byte[] value;

    // RADIUS attribute types
    public static final byte RADIUS_ATTR_USERNAME = 1;
    public static final byte RADIUS_ATTR_NAS_IP = 4;
    public static final byte RADIUS_ATTR_NAS_PORT = 5;
    public static final byte RADIUS_ATTR_FRAMED_MTU = 12;
    public static final byte RADIUS_ATTR_STATE = 24;
    public static final byte RADIUS_ATTR_VENDOR_SPECIFIC = 26;
    public static final byte RADIUS_ATTR_CALLING_STATION_ID = 31;
    public static final byte RADIUS_ATTR_NAS_ID = 32;
    public static final byte RADIUS_ATTR_ACCT_SESSION_ID = 44;
    public static final byte RADIUS_ATTR_NAS_PORT_TYPE = 61;
    public static final byte RADIUS_ATTR_EAP_MESSAGE = 79;
    public static final byte RADIUS_ATTR_MESSAGE_AUTH = 80;
    public static final byte RADIUS_ATTR_NAS_PORT_ID = 87;

    /**
     * Default constructor.
     */
    public RADIUSAttribute() {
    }

    /**
     * Constructs a RADIUS attribute with the give type, length and value.
     *
     * @param type type
     * @param length length
     * @param value value
     */
    public RADIUSAttribute(final byte type, final byte length, final byte[] value) {
        this.type = type;
        this.length = length;
        this.value = value;
    }

    /**
     * Checks if the attribute type is valid.
     *
     * @return whether the type is valid or not
     */
    public boolean isValidType() {
        return this.type == RADIUS_ATTR_USERNAME ||
                this.type == RADIUS_ATTR_NAS_IP ||
                this.type == RADIUS_ATTR_NAS_PORT ||
                this.type == RADIUS_ATTR_VENDOR_SPECIFIC ||
                this.type == RADIUS_ATTR_CALLING_STATION_ID ||
                this.type == RADIUS_ATTR_NAS_ID ||
                this.type == RADIUS_ATTR_ACCT_SESSION_ID ||
                this.type == RADIUS_ATTR_NAS_PORT_TYPE ||
                this.type == RADIUS_ATTR_EAP_MESSAGE ||
                this.type == RADIUS_ATTR_MESSAGE_AUTH ||
                this.type == RADIUS_ATTR_NAS_PORT_ID;
    }

    /**
     * Gets the attribute type.
     *
     * @return the type
     */
    public byte getType() {
        return this.type;
    }

    /**
     * Sets the attribute type.
     *
     * @param type the code to set
     * @return this
     */
    public RADIUSAttribute setType(final byte type) {
        this.type = type;
        return this;
    }

    /**
     * Gets the attribute length.
     *
     * @return the length
     */
    public byte getLength() {
        return this.length;
    }

    /**
     * Sets the attribute length.
     *
     * @param length the length to set
     * @return this
     */
    public RADIUSAttribute setLength(final byte length) {
        this.length = length;
        return this;
    }

    /**
     * Gets the attribute value.
     *
     * @return the value
     */
    public byte[] getValue() {
        return this.value;
    }

    /**
     * Sets the attribute value.
     *
     * @param value the data to set
     * @return this
     */
    public RADIUSAttribute setValue(final byte[] value) {
        this.value = value;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        sb.append("type= ");
        sb.append(type);
        sb.append("length= ");
        sb.append(length);
        sb.append("value= ");
        sb.append(Arrays.toString(value));
        sb.append("]");
        return sb.toString();
    }
}
