/*
 * Copyright 2015-present Open Networking Laboratory
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

package org.onosproject.pcepio.types;

import org.jboss.netty.buffer.ChannelBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

/**
 * Provides PcepRsvpObjectHeader.
 */
public class PcepRsvpSpecObjHeader {

    /*
    0             1              2             3
    +-------------+-------------+-------------+-------------+
    |       Length (bytes)      |  Class-Num  |   C-Type    |
    +-------------+-------------+-------------+-------------+
    |                                                       |
    //                  (Object contents)                   //
    |                                                       |
    +-------------+-------------+-------------+-------------+

              ERROR_SPEC object Header
     */

    protected static final Logger log = LoggerFactory.getLogger(PcepRsvpSpecObjHeader.class);

    private short objLen;
    private byte objClassNum;
    private byte objClassType;

    /**
     * Constructor to initialize length, class num and type.
     *
     * @param objLen object length
     * @param objClassNum pcep rsvp error spec object class num
     * @param objClassType pcep rsvp error spec object class type
     */
    public PcepRsvpSpecObjHeader(short objLen, byte objClassNum, byte objClassType) {
        this.objLen = objLen;
        this.objClassNum = objClassNum;
        this.objClassType = objClassType;
    }

    /**
     * Sets the Class num.
     *
     * @param value pcep rsvp error spec object class num
     */
    public void setObjClassNum(byte value) {
        this.objClassNum = value;
    }

    /**
     * Sets the Class type.
     *
     * @param value pcep rsvp error spec object class type
     */
    public void setObjClassType(byte value) {
        this.objClassType = value;
    }

    /**
     * Sets the Class Length.
     *
     * @param value pcep rsvp error spec object length
     */
    public void setObjLen(short value) {
        this.objLen = value;
    }

    /**
     * Returns Object Length.
     *
     * @return objLen pcep rsvp error spec object length
     */
    public short getObjLen() {
        return this.objLen;
    }

    /**
     * Returns Object num.
     *
     * @return objClassNum pcep rsvp error spec object class num
     */
    public byte getObjClassNum() {
        return this.objClassNum;
    }

    /**
     * Returns Object type.
     *
     * @return objClassType pcep rsvp error spec object class type
     */
    public byte getObjClassType() {
        return this.objClassType;
    }

    /**
     * Writes the byte stream of PcepRsvpObjectHeader to channel buffer.
     *
     * @param cb of type channel buffer
     * @return object length index
     */
    public int write(ChannelBuffer cb) {
        int objLenIndex = cb.writerIndex();
        objLen = 0;
        cb.writeShort(objLen);
        cb.writeByte(objClassNum);
        cb.writeByte(objClassType);
        return objLenIndex;
    }

    /**
     * Reads the PcepRsvpObjectHeader.
     *
     * @param cb of type channel buffer
     * @return PcepRsvpObjectHeader
     */
    public static PcepRsvpSpecObjHeader read(ChannelBuffer cb) {
        byte objClassNum;
        byte objClassType;
        short objLen;
        objLen = cb.readShort();
        objClassNum = cb.readByte();
        objClassType = cb.readByte();

        return new PcepRsvpSpecObjHeader(objLen, objClassNum, objClassType);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("ObjectClassNum: ", objClassNum)
                .add("ObjectCType: ", objClassType)
                .add("ObjectLength: ", objLen)
                .toString();
    }
}
