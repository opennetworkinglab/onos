/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onosproject.bgpio.types;

import java.util.Arrays;

import org.onosproject.bgpio.util.Constants;
import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.bgpio.exceptions.BgpParseException;

import com.google.common.base.MoreObjects;

/**
 * Provides implementation of BGP flow specification action.
 */
public class BgpFsActionTrafficAction implements BgpValueType {

    public static final short TYPE = Constants.BGP_FLOWSPEC_ACTION_TRAFFIC_ACTION;
    private byte[] bitMask;
    public static final byte BIT_MASK_LEN = 6;

    /**
     * Constructor to initialize the value.
     *
     * @param bitMask traffic action bit mask
     */
    public BgpFsActionTrafficAction(byte[] bitMask) {
        this.bitMask = Arrays.copyOf(bitMask, bitMask.length);
    }

    @Override
    public short getType() {
        return this.TYPE;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(bitMask);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof BgpFsActionTrafficAction) {
            BgpFsActionTrafficAction other = (BgpFsActionTrafficAction) obj;
            return Arrays.equals(this.bitMask, other.bitMask);
        }
        return false;
    }

    @Override
    public int write(ChannelBuffer cb) {
        int iLenStartIndex = cb.writerIndex();

        cb.writeShort(TYPE);

        cb.writeBytes(bitMask);

        return cb.writerIndex() - iLenStartIndex;
    }

    /**
     * Reads the channel buffer and returns object.
     *
     * @param cb channelBuffer
     * @return object of flow spec action traffic rate
     * @throws BgpParseException while parsing BgpFsActionTrafficAction
     */
    public static BgpFsActionTrafficAction read(ChannelBuffer cb) throws BgpParseException {
        byte[] bitMask;

        bitMask = cb.readBytes(BIT_MASK_LEN).array();
        return new BgpFsActionTrafficAction(bitMask);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("TYPE", TYPE)
                .add("bitMask", bitMask).toString();
    }

    @Override
    public int compareTo(Object o) {
        // TODO Auto-generated method stub
        return 0;
    }
}
