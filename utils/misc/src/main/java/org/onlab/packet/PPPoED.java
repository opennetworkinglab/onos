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

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.onlab.packet.PacketUtils.checkInput;

public class PPPoED extends BasePacket {
    protected byte version;
    protected byte type;
    protected byte code;
    protected short sessionId;
    protected short payloadLength;
    protected List<PPPoEDTag> tags = new ArrayList<>();

    // PPPoED packet types
    public static final byte PPPOED_CODE_PADI = (byte) 0x09;
    public static final byte PPPOED_CODE_PADO = (byte) 0x07;
    public static final byte PPPOED_CODE_PADR = (byte) 0x19;
    public static final byte PPPOED_CODE_PADS = (byte) 0x65;
    public static final byte PPPOED_CODE_PADT = (byte) 0xa7;

    private static final int HEADER_LENGTH = 6;
    private static final int TAG_HEADER_LENGTH = 4;

    public PPPoED() {
    }

    public byte getVersion() {
        return version;
    }

    public void setVersion(byte version) {
        this.version = version;
    }

    public byte getType() {
        return type;
    }

    public void setType(byte type) {
        this.type = type;
    }

    public byte getCode() {
        return code;
    }

    public void setCode(byte code) {
        this.code = code;
    }

    public short getSessionId() {
        return sessionId;
    }

    public void setSessionId(short sessionId) {
        this.sessionId = sessionId;
    }

    public short getPayloadLength() {
        return payloadLength;
    }

    public void setPayloadLength(short payloadLength) {
        this.payloadLength = payloadLength;
    }

    public List<PPPoEDTag> getTags() {
        return tags;
    }

    public void setTags(List<PPPoEDTag> tags) {
        this.tags = tags;
    }

    /**
     * Gets a list of tags from the packet.
     *
     * @param tagType the type field of the required tags
     * @return List of the tags that match the type or an empty list if there is none
     */
    public ArrayList<PPPoEDTag> getTagList(short tagType) {
        ArrayList<PPPoEDTag> tagList = new ArrayList<>();
        for (int i = 0; i < this.tags.size(); i++) {
            if (this.tags.get(i).getType() == tagType) {
                tagList.add(this.tags.get(i));
            }
        }
        return tagList;
    }

    /**
     * Gets a tag from the packet.
     *
     * @param tagType the type field of the required tag
     * @return the first tag that matches the type or null if does not exist
     */
    public PPPoEDTag getTag(short tagType) {
        for (int i = 0; i < this.tags.size(); i++) {
            if (this.tags.get(i).getType() == tagType) {
                return this.tags.get(i);
            }
        }
        return null;
    }

    /**
     * Sets a tag in the packet.
     *
     * @param tagType the type field of the tag to set
     * @param value    value to be set
     * @return reference to the tag object
     */
    public PPPoEDTag setTag(short tagType, byte[] value) {
        short tagLength = (short) (value.length);
        PPPoEDTag newTag = new PPPoEDTag(tagType, tagLength, value);
        this.tags.add(newTag);
        this.payloadLength += TAG_HEADER_LENGTH + tagLength;
        return newTag;
    }

    /**
     * Deserializer for PPPoED packets.
     *
     * @return deserializer
     */
    public static Deserializer<PPPoED> deserializer() {
        return (data, offset, length) -> {
            checkInput(data, offset, length, HEADER_LENGTH);

            final ByteBuffer bb = ByteBuffer.wrap(data, offset, length);
            PPPoED pppoed = new PPPoED();
            byte versionByte = bb.get();
            pppoed.setVersion((byte) (versionByte >> 4 & 0xf));
            pppoed.setType((byte) (versionByte & 0xf));
            pppoed.setCode(bb.get());
            pppoed.setSessionId(bb.getShort());
            pppoed.setPayloadLength(bb.getShort());
            int remainingLength = pppoed.payloadLength;
            while (remainingLength > 0 && bb.hasRemaining()) {
                PPPoEDTag tag = new PPPoEDTag();
                tag.setType(bb.getShort());
                tag.setLength(bb.getShort());
                tag.value = new byte[tag.length];
                bb.get(tag.value, 0, tag.length);
                pppoed.tags.add(tag);
                remainingLength -= tag.length + TAG_HEADER_LENGTH;
            }
            return pppoed;
        };
    }

    @Override
    public byte[] serialize() {
        final byte[] data = new byte[this.payloadLength + HEADER_LENGTH];
        final ByteBuffer bb = ByteBuffer.wrap(data);
        bb.put((byte) ((this.version & 0xf) << 4 | this.type & 0xf));
        bb.put(this.code);
        bb.putShort(this.sessionId);
        bb.putShort(this.payloadLength);
        for (int i = 0; i < this.tags.size(); i++) {
            PPPoEDTag tag = this.tags.get(i);
            bb.putShort(tag.getType());
            bb.putShort(tag.getLength());
            bb.put(tag.getValue());
        }
        return data;
    }

    @Override
    public String toString() {
        return "PPPoED{" +
                "version=" + version +
                ", type=" + type +
                ", code=" + code +
                ", session_id=" + sessionId +
                ", payload_length=" + payloadLength +
                ", tags=" + tags +
                '}';
    }

    public enum Type {
        PADI(PPPOED_CODE_PADI),
        PADO(PPPOED_CODE_PADO),
        PADR(PPPOED_CODE_PADR),
        PADS(PPPOED_CODE_PADS),
        PADT(PPPOED_CODE_PADT);

        public int value;

        Type(int value) {
            this.value = value;
        }

        public static Type getTypeByValue(int value) {
            return Stream.of(values())
                    .filter(el -> el.value == value)
                    .findFirst()
                    .orElse(null);
        }
    }
}
