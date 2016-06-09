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

import java.util.Objects;

import org.jboss.netty.buffer.ChannelBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

/**
 * Provides PCEP Object Header which is common for all the objects.
 * Reference : RFC 5440.
 */

public class PcepObjectHeader {

    /*
        0                   1                   2                   3
        0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       | Object-Class  |   OT  |Res|P|I|   Object Length (bytes)       |
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |                                                               |
       //                        (Object body)                        //
       |                                                               |
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

                      PCEP Common Object Header
     */

    protected static final Logger log = LoggerFactory.getLogger(PcepObjectHeader.class);

    public static final boolean REQ_OBJ_MUST_PROCESS = true;
    public static final boolean REQ_OBJ_OPTIONAL_PROCESS = false;
    public static final boolean RSP_OBJ_IGNORED = true;
    public static final boolean RSP_OBJ_PROCESSED = false;
    public static final int OBJECT_TYPE_SHIFT_VALUE = 4;
    public static final byte PFLAG_SET = 0x02;
    public static final byte IFLAG_SET = 0x01;
    public static final int SET = 1;
    private byte objClass;
    private byte objType;
    private boolean bPFlag;
    private boolean bIFlag;
    private short objLen;

    /**
     * Constructor to initialize all the variables in object header.
     *
     * @param objClass PCEP Object class
     * @param objType PCEP Object type
     * @param bPFlag P flag
     * @param bIFlag I flag
     * @param objLen PCEP object length
     */

    public PcepObjectHeader(byte objClass, byte objType, boolean bPFlag, boolean bIFlag, short objLen) {
        this.objClass = objClass;
        this.objType = objType;
        this.bPFlag = bPFlag;
        this.bIFlag = bIFlag;
        this.objLen = objLen;
    }

    /**
     * Sets the Object class.
     *
     * @param value object class
     */
    public void setObjClass(byte value) {
        this.objClass = value;
    }

    /**
     * Sets the Object TYPE.
     *
     * @param value object type
     */
    public void setObjType(byte value) {
        this.objType = value;
    }

    /**
     * Sets the Object P flag.
     *
     * @param value p flag
     */
    public void setPFlag(boolean value) {
        this.bPFlag = value;
    }

    /**
     * Sets the Object I flag.
     *
     * @param value I flag
     */
    public void setIFlag(boolean value) {
        this.bIFlag = value;
    }

    /**
     * Sets the Object Length.
     *
     * @param value object length
     */
    public void setObjLen(short value) {
        this.objLen = value;
    }

    /**
     * Returns Object's P flag.
     *
     * @return bPFlag P flag
     */
    public boolean getPFlag() {
        return this.bPFlag;
    }

    /**
     * Returns Object's i flag.
     *
     * @return bIFlag I flag
     */
    public boolean getIFlag() {
        return this.bIFlag;
    }

    /**
     * Returns Object Length.
     *
     * @return objLen object length
     */
    public short getObjLen() {
        return this.objLen;
    }

    /**
     * Returns Object class.
     *
     * @return objClass object class
     */
    public byte getObjClass() {
        return this.objClass;
    }

    /**
     * Returns Object Type.
     *
     * @return objType object type
     */
    public byte getObjType() {
        return this.objType;
    }

    /**
     *  Writes Byte stream of PCEP object header to channel buffer.
     *
     * @param cb output channel buffer
     * @return objLenIndex object length index in channel buffer
     */
    public int write(ChannelBuffer cb) {

        cb.writeByte(this.objClass);
        byte temp = (byte) (this.objType << OBJECT_TYPE_SHIFT_VALUE);
        if (this.bPFlag) {
            temp = (byte) (temp | PFLAG_SET);
        }
        if (this.bIFlag) {
            temp = (byte) (temp | IFLAG_SET);
        }
        cb.writeByte(temp);
        int objLenIndex = cb.writerIndex();
        cb.writeShort((short) 0);
        return objLenIndex;
    }

    /**
     * Read from channel buffer and Returns PCEP Objects header.
     *
     * @param cb of type channel buffer
     * @return PCEP Object header
     */
    public static PcepObjectHeader read(ChannelBuffer cb) {

        byte objClass;
        byte objType;
        boolean bPFlag;
        boolean bIFlag;
        short objLen;
        objClass = cb.readByte();
        byte temp = cb.readByte();
        bIFlag = (temp & IFLAG_SET) == IFLAG_SET;
        bPFlag = (temp & PFLAG_SET) == PFLAG_SET;
        objType = (byte) (temp >> OBJECT_TYPE_SHIFT_VALUE);
        objLen = cb.readShort();
        return new PcepObjectHeader(objClass, objType, bPFlag, bIFlag, objLen);
    }

    @Override
    public int hashCode() {
        return Objects.hash(objClass, objType, bPFlag, bIFlag, objLen);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof PcepObjectHeader) {
            PcepObjectHeader other = (PcepObjectHeader) obj;
            return Objects.equals(objClass, other.objClass)
                    && Objects.equals(objType, other.objType)
                    && Objects.equals(bPFlag, other.bPFlag)
                    && Objects.equals(bIFlag, other.bIFlag)
                    && Objects.equals(objLen, other.objLen);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("ObjectClass", objClass)
                .add("ObjectType", objType)
                .add("ObjectLength", objLen)
                .add("PFlag", (bPFlag) ? 1 : 0)
                .add("IFlag", (bIFlag) ? 1 : 0)
                .toString();
    }
}
