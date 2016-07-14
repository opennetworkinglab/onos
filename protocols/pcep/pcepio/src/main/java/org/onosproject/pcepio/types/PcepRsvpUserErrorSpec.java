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

import java.util.LinkedList;
import java.util.ListIterator;

import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.pcepio.exceptions.PcepParseException;
import org.onosproject.pcepio.protocol.PcepVersion;

import com.google.common.base.MoreObjects;

/**
 * Provides Pcep Rsvp User Error Spec.
 */
public class PcepRsvpUserErrorSpec implements PcepRsvpErrorSpec {

    /*
        RSVP error spec object header.
        0             1              2             3
    +-------------+-------------+-------------+-------------+
    |       Length (bytes)      |  Class-Num  |   C-Type    |
    +-------------+-------------+-------------+-------------+
    |                                                       |
    //                  (Object contents)                   //
    |                                                       |
    +-------------+-------------+-------------+-------------+

    Ref : USER_ERROR_SPEC @ RFC5284.
    USER_ERROR_SPEC object: Class = 194, C-Type = 1

    0                   1                   2                   3
    0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
    +---------------+---------------+---------------+---------------+
    |                       Enterprise Number                       |
    +---------------+---------------+---------------+---------------+
    |    Sub Org    |  Err Desc Len |        User Error Value       |
    +---------------+---------------+---------------+---------------+
    |                                                               |
    ~                       Error Description                       ~
    |                                                               |
    +---------------+---------------+---------------+---------------+
    |                                                               |
    ~                     User-Defined Subobjects                   ~
    |                                                               |
    +---------------+---------------+---------------+---------------+
     */

    public static final byte CLASS_NUM = (byte) 0xc2;
    public static final byte CLASS_TYPE = 0x01;

    private PcepRsvpSpecObjHeader objHeader;
    private int enterpriseNum;
    private byte subOrg;
    private byte errDescLen;
    private short userErrorValue;
    private byte[] errDesc;
    private LinkedList<PcepValueType> llRsvpUserSpecSubObj;

    /**
     * Default constructor.
     *
     * @param objHeader pcep rsvp spec object header
     * @param enterpriseNum enterprise number
     * @param subOrg organization identifier value
     * @param errDescLen error description length
     * @param userErrorValue user error value
     * @param errDesc error description
     * @param llRsvpUserSpecSubObj list of subobjects
     */
    public PcepRsvpUserErrorSpec(PcepRsvpSpecObjHeader objHeader, int enterpriseNum, byte subOrg, byte errDescLen,
            short userErrorValue, byte[] errDesc, LinkedList<PcepValueType> llRsvpUserSpecSubObj) {
        this.objHeader = objHeader;
        this.enterpriseNum = enterpriseNum;
        this.subOrg = subOrg;
        this.errDescLen = errDescLen;
        this.userErrorValue = userErrorValue;
        this.errDesc = errDesc;
        this.llRsvpUserSpecSubObj = llRsvpUserSpecSubObj;
    }

    @Override
    public int write(ChannelBuffer cb) {
        int objLenIndex = objHeader.write(cb);
        cb.writeInt(enterpriseNum);
        cb.writeByte(subOrg);
        cb.writeByte(errDescLen);
        cb.writeShort(userErrorValue);
        cb.writeBytes(errDesc);

        if (llRsvpUserSpecSubObj != null) {

            ListIterator<PcepValueType> listIterator = llRsvpUserSpecSubObj.listIterator();

            while (listIterator.hasNext()) {
                PcepValueType tlv = listIterator.next();
                if (tlv == null) {
                    continue;
                }
                tlv.write(cb);
                // need to take care of padding
                int pad = tlv.getLength() % 4;
                if (0 != pad) {
                    pad = 4 - pad;
                    for (int i = 0; i < pad; ++i) {
                        cb.writeByte((byte) 0);
                    }
                }
            }
        }
        short objLen = (short) (cb.writerIndex() - objLenIndex);
        cb.setShort(objLenIndex, objLen);
        return objLen;
    }

    /**
     * Reads the channel buffer and returns object of PcepRsvpErrorSpec.
     *
     * @param cb of type channel buffer
     * @return object of PcepRsvpErrorSpec
     * @throws PcepParseException when expected object is not received
     */
    public static PcepRsvpErrorSpec read(ChannelBuffer cb) throws PcepParseException {
        PcepRsvpSpecObjHeader objHeader;
        int enterpriseNum;
        byte subOrg;
        byte errDescLen;
        short userErrorValue;
        byte[] errDesc;
        LinkedList<PcepValueType> llRsvpUserSpecSubObj = null;

        objHeader = PcepRsvpSpecObjHeader.read(cb);

        if (objHeader.getObjClassNum() != CLASS_NUM || objHeader.getObjClassType() != CLASS_TYPE) {
            throw new PcepParseException("Expected PcepRsvpUserErrorSpec object.");
        }
        enterpriseNum = cb.readInt();
        subOrg = cb.readByte();
        errDescLen = cb.readByte();
        userErrorValue = cb.readShort();
        errDesc = new byte[errDescLen];
        cb.readBytes(errDesc, 0, errDescLen);

        llRsvpUserSpecSubObj = parseErrSpecSubObj(cb);

        return new PcepRsvpUserErrorSpec(objHeader, enterpriseNum, subOrg, errDescLen, userErrorValue, errDesc,
                llRsvpUserSpecSubObj);
    }

    private static LinkedList<PcepValueType> parseErrSpecSubObj(ChannelBuffer cb) throws PcepParseException {
        LinkedList<PcepValueType> llRsvpUserSpecSubObj = new LinkedList<>();
        while (0 < cb.readableBytes()) {
            PcepValueType tlv = null;
            short hType = cb.readShort();
            int iValue = 0;
            //short hLength = cb.readShort();
            switch (hType) {
            case AutonomousSystemSubTlv.TYPE:
                iValue = cb.readInt();
                tlv = new AutonomousSystemSubTlv(iValue);
                break;
            default:
                throw new PcepParseException("Unsupported Sub TLV type :" + hType);
            }
            llRsvpUserSpecSubObj.add(tlv);
        }
        return llRsvpUserSpecSubObj;
    }

    @Override
    public PcepVersion getVersion() {
        return PcepVersion.PCEP_1;
    }

    @Override
    public short getType() {
        return StatefulRsvpErrorSpecTlv.TYPE;
    }

    @Override
    public short getLength() {
        return objHeader.getObjLen();
    }

    @Override
    public byte getClassNum() {
        return CLASS_NUM;
    }

    @Override
    public byte getClassType() {
        return CLASS_TYPE;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("enterpriseNumber", enterpriseNum)
                .add("subOrganization", subOrg)
                .add("errDescLength", errDescLen)
                .add("userErrorValue", userErrorValue)
                .add("errDesc", errDesc)
                .add("RsvpUserSpecSubObject", llRsvpUserSpecSubObj)
                .toString();
    }
}
