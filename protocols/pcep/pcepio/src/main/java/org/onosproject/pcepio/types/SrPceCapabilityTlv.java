/*
 * Copyright 2016-present Open Networking Laboratory
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
import org.onosproject.pcepio.protocol.PcepVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

/**
 * Provides SR PCE Capability Tlv.
 */
public class SrPceCapabilityTlv implements PcepValueType {

    /*
     *
       reference : draft-ietf-pce-segment-routing-06, section 5.1.1

       0                   1                   2                   3
       0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
      |            Type=TBD           |            Length=4           |
      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
      |         Reserved              |     Flags     |      MSD      |
      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
               fig: SR-PCE-CAPABILITY TLV format
     */
    protected static final Logger log = LoggerFactory.getLogger(SrPceCapabilityTlv.class);

    public static final short TYPE = 26;
    public static final short LENGTH = 4;

    private final byte msd;

    /**
     * Constructor to initialize its parameter.
     *
     * @param msd maximum SID depth
     */
    public SrPceCapabilityTlv(byte msd) {
        this.msd = msd;
    }

    /**
     * Obtains newly created SrPceCapabilityTlv object.
     *
     * @param msd maximum SID depth
     * @return object of SrPceCapabilityTlv
     */
    public static SrPceCapabilityTlv of(final byte msd) {
        return new SrPceCapabilityTlv(msd);
    }

    /**
     * Obtains msd.
     *
     * @return msd
     */
    public byte msd() {
        return msd;
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
        return LENGTH;
    }

    @Override
    public int hashCode() {
        return Objects.hash(msd);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof SrPceCapabilityTlv) {
            SrPceCapabilityTlv other = (SrPceCapabilityTlv) obj;
            return Objects.equals(msd, other.msd);
        }
        return false;
    }

    @Override
    public int write(ChannelBuffer c) {
        int iLenStartIndex = c.writerIndex();
        c.writeShort(TYPE);
        c.writeShort(LENGTH);
        c.writeInt(msd);
        return c.writerIndex() - iLenStartIndex;
    }

    /**
     * Reads the channel buffer and returns object of SrPceCapabilityTlv.
     *
     * @param cb channel buffer
     * @return object of Gmpls-Capability-Tlv
     */
    public static SrPceCapabilityTlv read(ChannelBuffer cb) {
        //read reserved bits
        cb.readShort();
        //read flags
        cb.readByte();
        return SrPceCapabilityTlv.of(cb.readByte());
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("Type", TYPE)
                .add("Length", LENGTH)
                .add("msd", msd)
                .toString();
    }
}