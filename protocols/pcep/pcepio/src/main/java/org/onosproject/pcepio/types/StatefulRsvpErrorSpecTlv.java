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
import org.onosproject.pcepio.exceptions.PcepParseException;
import org.onosproject.pcepio.protocol.PcepVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

/**
 * Provides StatefulRsvpErrorSpecTlv.
 */
public class StatefulRsvpErrorSpecTlv implements PcepValueType {

    protected static final Logger log = LoggerFactory.getLogger(StatefulRsvpErrorSpecTlv.class);

    /*                  RSVP-ERROR-SPEC TLV format
     * Reference :PCEP Extensions for Stateful PCE draft-ietf-pce-stateful-pce-10
     *
     *

    0                   1                   2                   3
    0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    |           Type=21             |            Length (variable)  |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
    |                                                               |
    +                RSVP ERROR_SPEC or USER_ERROR_SPEC Object      +
    |                                                               |
    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+

        0             1              2             3
    +-------------+-------------+-------------+-------------+
    |       Length (bytes)      |  Class-Num  |   C-Type    |
    +-------------+-------------+-------------+-------------+
    |                                                       |
    //                  (Object contents)                   //
    |                                                       |
    +-------------+-------------+-------------+-------------+

    Ref :  ERROR_SPEC @ RFC2205

    IPv4 ERROR_SPEC object: Class = 6, C-Type = 1
    +-------------+-------------+-------------+-------------+
    |            IPv4 Error Node Address (4 bytes)          |
    +-------------+-------------+-------------+-------------+
    |    Flags    |  Error Code |        Error Value        |
    +-------------+-------------+-------------+-------------+


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
    +-------------+-------------+-------------+-------------+


    Ref : USER_ERROR_SPEC @ RFC5284
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

    public static final short TYPE = 21;
    public static final int OBJECT_HEADER_LENGTH = 4;
    private short hLength;

    private final PcepRsvpErrorSpec rsvpErrSpecObj;
    private final boolean isErrSpceObjSet;

    /**
     * Constructor to initialize errSpecObj.
     *
     * @param rsvpErrSpecObj Rsvp error spec object
     */
    public StatefulRsvpErrorSpecTlv(PcepRsvpErrorSpec rsvpErrSpecObj) {
        this.rsvpErrSpecObj = rsvpErrSpecObj;
        this.isErrSpceObjSet = true;
    }

    /**
     * Returns PcepRsvpErrorSpecObject.
     *
     * @return rsvpErrSpecObj
     */
    public PcepRsvpErrorSpec getPcepRsvpErrorSpec() {
        return this.rsvpErrSpecObj;
    }

    @Override
    public PcepVersion getVersion() {
        return PcepVersion.PCEP_1;
    }

    @Override
    public short getType() {
        return TYPE;
    }

    @Override
    public short getLength() {
        return hLength;
    }

    /**
     * Reads channel buffer and returns object of StatefulRsvpErrorSpecTlv.
     *
     * @param cb of type channel buffer
     * @return object of StatefulRsvpErrorSpecTlv
     * @throws PcepParseException while parsing this tlv from channel buffer
     */
    public static PcepValueType read(ChannelBuffer cb) throws PcepParseException {

        PcepRsvpErrorSpec rsvpErrSpecObj = null;
        PcepRsvpSpecObjHeader rsvpErrSpecObjHeader;

        cb.markReaderIndex();
        rsvpErrSpecObjHeader = PcepRsvpSpecObjHeader.read(cb);
        cb.resetReaderIndex();

        if (PcepRsvpIpv4ErrorSpec.CLASS_NUM == rsvpErrSpecObjHeader.getObjClassNum()
                && PcepRsvpIpv4ErrorSpec.CLASS_TYPE == rsvpErrSpecObjHeader.getObjClassType()) {
            rsvpErrSpecObj = PcepRsvpIpv4ErrorSpec.read(cb);
        } else if (PcepRsvpIpv6ErrorSpec.CLASS_NUM == rsvpErrSpecObjHeader.getObjClassNum()
                && PcepRsvpIpv6ErrorSpec.CLASS_TYPE == rsvpErrSpecObjHeader.getObjClassType()) {
            rsvpErrSpecObj = PcepRsvpIpv6ErrorSpec.read(cb);
        } else if (PcepRsvpUserErrorSpec.CLASS_NUM == rsvpErrSpecObjHeader.getObjClassNum()
                && PcepRsvpUserErrorSpec.CLASS_TYPE == rsvpErrSpecObjHeader.getObjClassType()) {
            rsvpErrSpecObj = PcepRsvpUserErrorSpec.read(cb);
        }
        return new StatefulRsvpErrorSpecTlv(rsvpErrSpecObj);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rsvpErrSpecObj.hashCode());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof StatefulRsvpErrorSpecTlv) {
            StatefulRsvpErrorSpecTlv other = (StatefulRsvpErrorSpecTlv) obj;
            return Objects.equals(this.rsvpErrSpecObj, other.rsvpErrSpecObj);
        }
        return false;
    }

    @Override
    public int write(ChannelBuffer c) {
        int iStartIndex = c.writerIndex();
        c.writeShort(TYPE);
        int tlvLenIndex = c.writerIndex();
        hLength = 0;
        c.writeShort(hLength);
        if (isErrSpceObjSet) {
            rsvpErrSpecObj.write(c);
        }
        hLength = (short) (c.writerIndex() - iStartIndex);
        c.setShort(tlvLenIndex, (hLength - OBJECT_HEADER_LENGTH));

        return c.writerIndex() - iStartIndex;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .omitNullValues()
                .add("Type", TYPE)
                .add("Length", hLength)
                .add("RSVPErrorSpecObject", rsvpErrSpecObj)
                .toString();
    }
}
