/*
 * Copyright 2014-present Open Networking Foundation
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
/**
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 **/

package org.onlab.packet;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * The class representing LLDP Organizationally Specific TLV.
 *
 */
public class LLDPOrganizationalTLV extends LLDPTLV {
    public static final int OUI_LENGTH = 3;
    public static final int SUBTYPE_LENGTH = 1;
    public static final byte ORGANIZATIONAL_TLV_TYPE = 127;
    public static final int MAX_INFOSTRING_LENGTH = 507;

    protected byte[] oui;
    protected byte subType;
    private byte[] infoString;

    public LLDPOrganizationalTLV() {
        this.type = LLDPOrganizationalTLV.ORGANIZATIONAL_TLV_TYPE;
    }

    /**
     * Set the value of OUI.
     *
     * @param oui
     *            The value of OUI to be set.
     * @return This LLDP Organizationally Specific TLV.
     */
    public LLDPOrganizationalTLV setOUI(final byte[] oui) {
        if (oui.length != LLDPOrganizationalTLV.OUI_LENGTH) {
            throw new IllegalArgumentException("The length of OUI must be "
                    + LLDPOrganizationalTLV.OUI_LENGTH + ", but it is "
                    + oui.length);
        }
        this.oui = Arrays.copyOf(oui, oui.length);
        return this;
    }

    /**
     * Returns the value of the OUI.
     *
     * @return The value of the OUI .
     */
    public byte[] getOUI() {
        return Arrays.copyOf(this.oui, this.oui.length);
    }

    /**
     * Set the value of sub type.
     *
     * @param subType
     *            The value of sub type to be set.
     * @return This LLDP Organizationally Specific TLV.
     */
    public LLDPOrganizationalTLV setSubType(final byte subType) {
        this.subType = subType;
        return this;
    }

    /**
     * Returns the value of the sub type.
     *
     * @return The value of the sub type.
     */
    public byte getSubType() {
        return this.subType;
    }

    /**
     * Set the value of information string.
     *
     * @param infoString
     *            the byte array of the value of information string.
     * @return This LLDP Organizationally Specific TLV.
     */
    public LLDPOrganizationalTLV setInfoString(final byte[] infoString) {
        if (infoString.length > LLDPOrganizationalTLV.MAX_INFOSTRING_LENGTH) {
            throw new IllegalArgumentException(
                    "The length of infoString cannot exceed "
                            + LLDPOrganizationalTLV.MAX_INFOSTRING_LENGTH);
        }
        this.infoString = Arrays.copyOf(infoString, infoString.length);
        return this;
    }

    /**
     * Set the value of information string. The String value is automatically
     * converted into byte array with UTF-8 encoding.
     *
     * @param infoString
     *            the String value of information string.
     * @return This LLDP Organizationally Specific TLV.
     */
    public LLDPOrganizationalTLV setInfoString(final String infoString) {
        final byte[] infoStringBytes = infoString.getBytes(StandardCharsets.UTF_8);
        return this.setInfoString(infoStringBytes);
    }

    /**
     * Returns the value of information string.
     *
     * @return the value of information string.
     */
    public byte[] getInfoString() {
        return Arrays.copyOf(this.infoString, this.infoString.length);
    }

    @Override
    public byte[] serialize() {
        if (this.type != LLDPOrganizationalTLV.ORGANIZATIONAL_TLV_TYPE) {
            return super.serialize();
        }
        final int valueLength = LLDPOrganizationalTLV.OUI_LENGTH
                + LLDPOrganizationalTLV.SUBTYPE_LENGTH + this.infoString.length;
        this.value = new byte[valueLength];
        final ByteBuffer bb = ByteBuffer.wrap(this.value);
        bb.put(this.oui);
        bb.put(this.subType);
        bb.put(this.infoString);
        return super.serialize();
    }

    @Override
    public LLDPTLV deserialize(final ByteBuffer bb) throws DeserializationException {
        super.deserialize(bb);
        if (this.getType() != LLDPOrganizationalTLV.ORGANIZATIONAL_TLV_TYPE) {
            return this;
        }

        if (this.getLength() <= OUI_LENGTH + SUBTYPE_LENGTH) {
            throw new DeserializationException(
                    "TLV length is less than required for organizational TLV");
        }

        final ByteBuffer optionalField = ByteBuffer.wrap(this.value);

        final byte[] oui = new byte[LLDPOrganizationalTLV.OUI_LENGTH];
        optionalField.get(oui);
        this.setOUI(oui);

        this.setSubType(optionalField.get());

        final byte[] infoString = new byte[this.getLength()
                                           - LLDPOrganizationalTLV.OUI_LENGTH
                                           - LLDPOrganizationalTLV.SUBTYPE_LENGTH];
        optionalField.get(infoString);
        this.setInfoString(infoString);
        return this;
    }

    @Override
    public int hashCode() {
        final int prime = 1423;
        int result = 1;
        result = prime * result + this.type;
        result = prime * result + this.length;
        result = prime * result + Arrays.hashCode(this.oui);
        result = prime * result + this.subType;
        result = prime * result + Arrays.hashCode(this.infoString);
        return result;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == this) {
            return true;
        }

        if (!(o instanceof LLDPOrganizationalTLV)) {
            return false;
        }

        final LLDPOrganizationalTLV other = (LLDPOrganizationalTLV) o;
        if (this.type != other.type) {
            return false;
        }
        if (this.length != other.length) {
            return false;
        }
        if (!Arrays.equals(this.oui, other.oui)) {
            return false;
        }
        if (this.subType != other.subType) {
            return false;
        }
        if (!Arrays.equals(this.infoString, other.infoString)) {
            return false;
        }

        return true;
    }
}
