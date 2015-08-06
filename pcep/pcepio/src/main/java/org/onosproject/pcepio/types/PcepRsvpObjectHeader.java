/*
 * Copyright 2015 Open Networking Laboratory
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

/**
 * Provides PcepRsvpObjectHeader.
 */
public class PcepRsvpObjectHeader {

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

    protected static final Logger log = LoggerFactory.getLogger(PcepRsvpObjectHeader.class);

    public static final boolean REQ_OBJ_MUST_PROCESS = true;
    public static final boolean REQ_OBJ_OPTIONAL_PROCESS = false;
    public static final boolean RSP_OBJ_IGNORED = true;
    public static final boolean RSP_OBJ_PROCESSED = false;
    public static final int OBJECT_TYPE_SHIFT_VALUE = 4;
    private byte objClassNum;
    private byte objClassType;
    private short objLen;

    /**
     * Constructor to initialize class num , length and type.
     *
     * @param objClassNum object class number
     * @param objClassType object class type
     * @param objLen object length
     */
    public PcepRsvpObjectHeader(byte objClassNum, byte objClassType, short objLen) {
        this.objClassNum = objClassNum;
        this.objClassType = objClassType;
        this.objLen = objLen;
    }

    /**
     * Sets the Class num.
     *
     * @param value object class number
     */
    public void setObjClassNum(byte value) {
        this.objClassNum = value;
    }

    /**
     * Sets the Class type.
     *
     * @param value object class type
     */
    public void setObjClassType(byte value) {
        this.objClassType = value;
    }

    /**
     * Sets the Class Length.
     *
     * @param value object length
     */
    public void setObjLen(short value) {
        this.objLen = value;
    }

    /**
     * Returns Object Length.
     *
     * @return objLen
     */
    public short getObjLen() {
        return this.objLen;
    }

    /**
     * Returns Object num.
     *
     * @return objClassNum
     */
    public byte getObjClassNum() {
        return this.objClassNum;
    }

    /**
     * Returns Object type.
     *
     * @return objClassType
     */
    public byte getObjClassType() {
        return this.objClassType;
    }

    /**
     * Writes the byte stream of PcepRsvpObjectHeader to channel buffer.
     *
     * @param bb of type channel buffer
     * @return object length index in channel buffer
     */
    public int write(ChannelBuffer bb) {
        int iLenStartIndex = bb.writerIndex();
        bb.writeShort((short) 0);
        bb.writeByte(this.objClassNum);
        bb.writeByte(this.objClassType);
        return bb.writerIndex() - iLenStartIndex;
    }

    /**
     * Reads the PcepRsvpObjectHeader.
     *
     * @param bb of type channel buffer
     * @return PcepRsvpObjectHeader
     */
    public static PcepRsvpObjectHeader read(ChannelBuffer bb) {
        log.debug("PcepRsvpObjectHeader ::read ");
        byte objClassNum;
        byte objClassType;
        short objLen;
        objLen = bb.readShort();
        objClassNum = bb.readByte();
        objClassType = bb.readByte();

        return new PcepRsvpObjectHeader(objClassNum, objClassType, objLen);
    }

    /**
     * Prints the attribute of PcepRsvpObjectHeader.
     */
    public void print() {

        log.debug("PcepObjectHeader");
        log.debug("Object Class-Num: " + objClassNum);
        log.debug("Object C-Type: " + objClassType);
        log.debug("Object Length: " + objLen);
    }
}
