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

import java.util.Arrays;

import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.pcepio.protocol.PcepVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;

/**
 * Provides SharedRiskLinkGroupTlv.
 */
public class SharedRiskLinkGroupSubTlv implements PcepValueType {

    /*
     * Reference :[I-D.ietf-idr- Group ls-distribution] /3.3.2.5
     *
     *  0                   1                   2                   3
      0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |              Type =TDB41      |             Length            |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |                  Shared Risk Link Group Value                 |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     //                         ............                        //
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     |                  Shared Risk Link Group Value                 |
     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     */

    protected static final Logger log = LoggerFactory.getLogger(SharedRiskLinkGroupSubTlv.class);

    public static final short TYPE = 30;

    private final short hLength;

    private final int[] srlgValue;

    /**
     * Constructor to initialize SRLG value.
     *
     * @param srlgValue Shared Risk Link Group Value
     * @param hLength length
     */
    public SharedRiskLinkGroupSubTlv(int[] srlgValue, short hLength) {
        this.srlgValue = srlgValue;
        if (0 == hLength) {
            this.hLength = (short) ((srlgValue.length) * 4);
        } else {
            this.hLength = hLength;
        }
    }

    /**
     * Returns object of SharedRiskLinkGroupTlv.
     *
     * @param raw value
     * @param hLength length
     * @return object of SharedRiskLinkGroupTlv
     */
    public static SharedRiskLinkGroupSubTlv of(final int[] raw, short hLength) {
        return new SharedRiskLinkGroupSubTlv(raw, hLength);
    }

    /**
     * Returns SRLG Value.
     *
     * @return srlgValue
     */
    public int[] getValue() {
        return srlgValue;
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

    @Override
    public int hashCode() {
        return  Arrays.hashCode(srlgValue);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof SharedRiskLinkGroupSubTlv) {
            SharedRiskLinkGroupSubTlv other = (SharedRiskLinkGroupSubTlv) obj;
            return Arrays.equals(this.srlgValue, other.srlgValue);
        }
        return false;
    }

    @Override
    public int write(ChannelBuffer c) {
        int iLenStartIndex = c.writerIndex();
        c.writeShort(TYPE);
        c.writeShort(hLength);
        for (int b : srlgValue) {
            c.writeInt(b);
        }
        return c.writerIndex() - iLenStartIndex;
    }

    /**
     * Reads from channel buffer and returns object of SharedRiskLinkGroupTlv.
     *
     * @param c input channel buffer
     * @param hLength length
     * @return object of SharedRiskLinkGroupTlv
     */
    public static PcepValueType read(ChannelBuffer c, short hLength) {
        int iLength = hLength / 4;
        int[] iSharedRiskLinkGroup = new int[iLength];
        for (int i = 0; i < iLength; i++) {
            iSharedRiskLinkGroup[i] = c.readInt();
        }
        return new SharedRiskLinkGroupSubTlv(iSharedRiskLinkGroup, hLength);
    }


    @Override
    public String toString() {
        ToStringHelper toStrHelper = MoreObjects.toStringHelper(getClass());

        toStrHelper.add("Type", TYPE);
        toStrHelper.add("Length", hLength);

        StringBuffer result = new StringBuffer();
        for (int b : srlgValue) {
            result.append(String.format("%02X ", b));
        }
        toStrHelper.add("Value", result);

        return toStrHelper.toString();
    }
}
