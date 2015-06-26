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

package org.onosproject.aaa.packet;

import org.onlab.packet.BasePacket;
import org.onlab.packet.IPacket;
import org.slf4j.Logger;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;

import static org.slf4j.LoggerFactory.getLogger;

/**
 *
 */
public class RADIUS extends BasePacket {
    protected byte code;
    protected byte identifier;
    protected short length = RADIUS_MIN_LENGTH;
    protected byte[] authenticator = new byte[16];
    protected ArrayList<RADIUSAttribute> attributes = new ArrayList<>();

    /* RADIUS parameters */
    public static final short RADIUS_MIN_LENGTH = 20;
    public static final short MAX_ATTR_VALUE_LENGTH = 253;
    public static final short RADIUS_MAX_LENGTH = 4096;

    /* RADIUS packet types */
    public static final byte RADIUS_CODE_ACCESS_REQUEST = 0x01;
    public static final byte RADIUS_CODE_ACCESS_ACCEPT = 0x02;
    public static final byte RADIUS_CODE_ACCESS_REJECT = 0x03;
    public static final byte RADIUS_CODE_ACCOUNTING_REQUEST = 0x04;
    public static final byte RADIUS_CODE_ACCOUNTING_RESPONSE = 0x05;
    public static final byte RADIUS_CODE_ACCESS_CHALLENGE = 0x0b;

    private final Logger log = getLogger(getClass());

    public RADIUS() {
    }

    public RADIUS(byte code, byte identifier) {
        this.code = code;
        this.identifier = identifier;
    }

    public byte getCode() {
        return this.code;
    }

    public void setCode(byte code) {
        this.code = code;
    }

    public byte getIdentifier() {
        return this.identifier;
    }

    public void setIdentifier(byte identifier) {
        this.identifier = identifier;
    }

    public byte[] getAuthenticator() {
        return this.authenticator;
    }

    public void setAuthenticator(byte[] a) {
        this.authenticator = a;
    }

    public byte[] generateAuthCode() {
        new SecureRandom().nextBytes(this.authenticator);
        return this.authenticator;
    }

    public boolean isValidCode() {
        return this.code == RADIUS_CODE_ACCESS_REQUEST ||
                this.code == RADIUS_CODE_ACCESS_ACCEPT ||
                this.code == RADIUS_CODE_ACCESS_REJECT ||
                this.code == RADIUS_CODE_ACCOUNTING_REQUEST ||
                this.code == RADIUS_CODE_ACCOUNTING_RESPONSE ||
                this.code == RADIUS_CODE_ACCESS_CHALLENGE;
    }

    public RADIUSAttribute addMessageAuthenticator(String key) {
        /* Message-Authenticator = HMAC-MD5 (Type, Identifier, Length, Request Authenticator, Attributes)
           When the message integrity check is calculated the signature string should be considered to be
           sixteen octets of zero.
         */
        byte[] hashOutput = new byte[16];
        Arrays.fill(hashOutput, (byte) 0);

        RADIUSAttribute authAttribute = this.getAttribute(RADIUSAttribute.RADIUS_ATTR_MESSAGE_AUTH);
        if (authAttribute != null) {
            // If Message-Authenticator was already present, override it
            this.log.warn("Attempted to add duplicate Message-Authenticator");
            authAttribute = this.updateAttribute(RADIUSAttribute.RADIUS_ATTR_MESSAGE_AUTH, hashOutput);
        } else {
            // Else generate a new attribute padded with zeroes
            authAttribute = this.setAttribute(RADIUSAttribute.RADIUS_ATTR_MESSAGE_AUTH, hashOutput);
        }
        // Calculate the MD5 HMAC based on the message
        try {
            SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(), "HmacMD5");
            Mac mac = Mac.getInstance("HmacMD5");
            mac.init(keySpec);
            hashOutput = mac.doFinal(this.serialize());
            // Update HMAC in Message-Authenticator
            authAttribute = this.updateAttribute(RADIUSAttribute.RADIUS_ATTR_MESSAGE_AUTH, hashOutput);
        } catch (Exception e) {
            this.log.error("Failed to generate message authenticator: {}", e.getMessage());
        }

        return authAttribute;
    }

    public boolean checkMessageAuthenticator(String key) {
        byte[] newHash = new byte[16];
        Arrays.fill(newHash, (byte) 0);
        byte[] messageAuthenticator = this.getAttribute(RADIUSAttribute.RADIUS_ATTR_MESSAGE_AUTH).getValue();
        this.updateAttribute(RADIUSAttribute.RADIUS_ATTR_MESSAGE_AUTH, newHash);
        // Calculate the MD5 HMAC based on the message
        try {
            SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(), "HmacMD5");
            Mac mac = Mac.getInstance("HmacMD5");
            mac.init(keySpec);
            newHash = mac.doFinal(this.serialize());
        } catch (Exception e) {
            log.error("Failed to generate message authenticator: {}", e.getMessage());
        }
        this.updateAttribute(RADIUSAttribute.RADIUS_ATTR_MESSAGE_AUTH, messageAuthenticator);
        // Compare the calculated Message-Authenticator with the one in the message
        return Arrays.equals(newHash, messageAuthenticator);
    }

    /**
     * @param message
     *            EAP message object to be embedded in the RADIUS EAP-Message attributed
     */
    public void encapsulateMessage(EAP message) {
        if (message.length <= MAX_ATTR_VALUE_LENGTH) {
            // Use the regular serialization method as it fits into one EAP-Message attribute
            this.setAttribute(RADIUSAttribute.RADIUS_ATTR_EAP_MESSAGE,
                    message.serialize());
        } else {
            // Segment the message into chucks and embed them in several EAP-Message attributes
            short remainingLength = message.length;
            byte[] messageBuffer = message.serialize();
            final ByteBuffer bb = ByteBuffer.wrap(messageBuffer);
            while (bb.hasRemaining()) {
                byte[] messageAttributeData;
                if (remainingLength > MAX_ATTR_VALUE_LENGTH) {
                    // The remaining data is still too long to fit into one attribute, keep going
                    messageAttributeData = new byte[MAX_ATTR_VALUE_LENGTH];
                    bb.get(messageAttributeData, 0, MAX_ATTR_VALUE_LENGTH);
                    remainingLength -= MAX_ATTR_VALUE_LENGTH;
                } else {
                    // The remaining data fits, this will be the last chunk
                    messageAttributeData = new byte[remainingLength];
                    bb.get(messageAttributeData, 0, remainingLength);
                }
                this.attributes.add(new RADIUSAttribute(RADIUSAttribute.RADIUS_ATTR_EAP_MESSAGE,
                        (byte) (messageAttributeData.length + 2), messageAttributeData));

                // Adding the size of the data to the total RADIUS length
                this.length += (short) (messageAttributeData.length & 0xFF);
                // Adding the size of the overhead attribute type and length
                this.length += 2;
            }
        }
    }

    /**
     * @return An EAP object containing the reassembled EAP message
     */
    public EAP decapsulateMessage() {
        EAP message = new EAP();
        ByteArrayOutputStream messageStream = new ByteArrayOutputStream();
        // Iterating through EAP-Message attributes to concatenate their value
        for (RADIUSAttribute ra : this.getAttributeList(RADIUSAttribute.RADIUS_ATTR_EAP_MESSAGE)) {
            try {
                messageStream.write(ra.getValue());
            } catch (IOException e) {
                log.error("Error while reassembling EAP message: {}", e.getMessage());
            }
        }
        // Assembling EAP object from the concatenated stream
        message.deserialize(messageStream.toByteArray(), 0, messageStream.size());
        return message;
    }

    /**
     * @param attrType
     *            the type field of the required attributes
     * @return List of the attributes that matches the type or an empty list if there is none
     */
    public ArrayList<RADIUSAttribute> getAttributeList(byte attrType) {
        ArrayList<RADIUSAttribute> attrList = new ArrayList<>();
        for (int i = 0; i < this.attributes.size(); i++) {
            if (this.attributes.get(i).getType() == attrType) {
                attrList.add(this.attributes.get(i));
            }
        }
        return attrList;
    }

    /**
     * @param attrType
     *            the type field of the required attribute
     * @return the first attribute that matches the type or null if does not exist
     */
    public RADIUSAttribute getAttribute(byte attrType) {
        for (int i = 0; i < this.attributes.size(); i++) {
            if (this.attributes.get(i).getType() == attrType) {
                return this.attributes.get(i);
            }
        }
        return null;
    }

    /**
     * @param attrType
     *            the type field of the attribute to set
     * @param value
     *            value to be set
     * @return reference to the attribute object
     */
    public RADIUSAttribute setAttribute(byte attrType, byte[] value) {
        byte attrLength = (byte) (value.length + 2);
        RADIUSAttribute newAttribute = new RADIUSAttribute(attrType, attrLength, value);
        this.attributes.add(newAttribute);
        this.length += (short) (attrLength & 0xFF);
        return newAttribute;
    }

    public RADIUSAttribute updateAttribute(byte attrType, byte[] value) {
        for (int i = 0; i < this.attributes.size(); i++) {
            if (this.attributes.get(i).getType() == attrType) {
                this.length -= (short) (this.attributes.get(i).getLength() & 0xFF);
                RADIUSAttribute newAttr = new RADIUSAttribute(attrType, (byte) (value.length + 2), value);
                this.attributes.set(i, newAttr);
                this.length += (short) (newAttr.getLength() & 0xFF);
                return newAttr;
            }
        }
        return null;
    }

    @Override
    public byte[] serialize() {
        final byte[] data = new byte[this.length];
        final ByteBuffer bb = ByteBuffer.wrap(data);

        bb.put(this.code);
        bb.put(this.identifier);
        bb.putShort(this.length);
        bb.put(this.authenticator);
        for (int i = 0; i < this.attributes.size(); i++) {
            RADIUSAttribute attr = this.attributes.get(i);
            bb.put(attr.getType());
            bb.put(attr.getLength());
            bb.put(attr.getValue());
        }

        return data;
    }

    @Override
    public IPacket deserialize(final byte[] data, final int offset,
                               final int length) {
        final ByteBuffer bb = ByteBuffer.wrap(data, offset, length);
        this.code = bb.get();
        this.identifier = bb.get();
        this.length = bb.getShort();
        bb.get(this.authenticator, 0, 16);

        int remainingLength = this.length - RADIUS_MIN_LENGTH;
        while (remainingLength > 0 && bb.hasRemaining()) {
            RADIUSAttribute attr = new RADIUSAttribute();
            attr.setType(bb.get());
            attr.setLength(bb.get());
            short attrLength = (short) (attr.length & 0xff);
            attr.value = new byte[attrLength - 2];
            bb.get(attr.value, 0, attrLength - 2);
            this.attributes.add(attr);
            remainingLength -= attr.length;
        }
        return this;
    }

}
