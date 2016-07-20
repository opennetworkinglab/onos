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
import org.onosproject.pcepio.protocol.PcepVersion;

import com.google.common.base.MoreObjects;

/**
 * Provides Pcep Rsvp Ipv6 Error Spec.
 */
public class PcepRsvpIpv6ErrorSpec implements PcepRsvpErrorSpec {

    /*
        0             1              2             3
    +-------------+-------------+-------------+-------------+
    |       Length (bytes)      |  Class-Num  |   C-Type    |
    +-------------+-------------+-------------+-------------+
    |                                                       |
    //                  (Object contents)                   //
    |                                                       |
    +-------------+-------------+-------------+-------------+

    Ref :  ERROR_SPEC @ RFC2205

    IPv6 ERROR_SPEC object: Class = 6, C-Type = 2
    +-------------+-------------+-------------+-------------+
    |                                                       |
    +                                                       +
    |                                                       |
    +           IPv6 Error Node Address (16 bytes)          +
    |                                                       |
    +                                                       +
    |                                                       |
    +-------------+-------------+-------------+-------------+
    |    Flags    |  Error Code |        Error Value        |
    +-------------+-------------+-------------+-------------+     */

    PcepRsvpSpecObjHeader objHeader;
    public static final byte CLASS_NUM = 0x06;
    public static final byte CLASS_TYPE = 0x02;
    public static final byte CLASS_LENGTH = 0x18;
    public static final byte IPV6_LEN = 0x10;

    private byte[] ipv6Addr;
    private byte flags;
    private byte errCode;
    private short errValue;

    /**
     * Constructor to initialize obj header, ipv6 addr, flags, err code and err value.
     *
     * @param objHeader rsvp ipv6 error spec object header
     * @param ipv6Addr ipv6 address
     * @param flags flags value
     * @param errCode error code
     * @param errValue error value
     */
    public PcepRsvpIpv6ErrorSpec(PcepRsvpSpecObjHeader objHeader, byte[] ipv6Addr, byte flags, byte errCode,
            short errValue) {
        this.objHeader = objHeader;
        this.ipv6Addr = ipv6Addr;
        this.flags = flags;
        this.errCode = errCode;
        this.errValue = errValue;
    }

    /**
     * Constructor to initialize ipv6 addr, flags, err code and err value.
     *
     * @param ipv6Addr ipv6 address
     * @param flags flags value
     * @param errCode error code
     * @param errValue error value
     */
    public PcepRsvpIpv6ErrorSpec(byte[] ipv6Addr, byte flags, byte errCode, short errValue) {
        this.objHeader = new PcepRsvpSpecObjHeader(CLASS_LENGTH, CLASS_NUM, CLASS_TYPE);
        this.ipv6Addr = ipv6Addr;
        this.flags = flags;
        this.errCode = errCode;
        this.errValue = errValue;
    }

    @Override
    public int write(ChannelBuffer cb) {
        int objLenIndex = objHeader.write(cb);
        cb.writeBytes(ipv6Addr);
        cb.writeByte(flags);
        cb.writeByte(errCode);
        cb.writeShort(errValue);
        short objLen = (short) (cb.writerIndex() - objLenIndex);
        cb.setShort(objLenIndex, objLen);
        return objLen;
    }

    /**
     * Returns PCEP rsvp IPv6 error spce object.
     *
     * @param cb channel buffer
     * @return PCEP rsvp IPv6 error spce object
     */
    public static PcepRsvpErrorSpec read(ChannelBuffer cb) {
        PcepRsvpSpecObjHeader objHeader;
        byte[] ipv6Addr = new byte[IPV6_LEN];
        byte flags;
        byte errCode;
        short errValue;

        objHeader = PcepRsvpSpecObjHeader.read(cb);
        cb.readBytes(ipv6Addr, 0, IPV6_LEN);
        flags = cb.readByte();
        errCode = cb.readByte();
        errValue = cb.readShort();
        return new PcepRsvpIpv6ErrorSpec(objHeader, ipv6Addr, flags, errCode, errValue);
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
        return CLASS_LENGTH;
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
                .add("IPv6Address", ipv6Addr)
                .add("flags", flags)
                .add("errorCode", errCode)
                .add("errorValue", errValue)
                .toString();
    }
}
