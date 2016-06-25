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
package org.onosproject.bgpio.types;

import java.util.Arrays;

import org.onosproject.bgpio.util.Constants;
import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.bgpio.exceptions.BgpParseException;

import com.google.common.base.MoreObjects;

/**
 * Provides implementation of BGP flow specification action.
 */
public class BgpFsActionTrafficMarking implements BgpValueType {

    public static final short TYPE = Constants.BGP_FLOWSPEC_ACTION_TRAFFIC_MARKING;
    private byte[] dscpValue;
    public static final byte DSCP_LEN = 6;

    /**
     * Constructor to initialize the value.
     *
     * @param dscpValue DSCP value
     */
    public BgpFsActionTrafficMarking(byte[] dscpValue) {
        this.dscpValue = Arrays.copyOf(dscpValue, dscpValue.length);
    }

    @Override
    public short getType() {
        return this.TYPE;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(dscpValue);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof BgpFsActionTrafficMarking) {
            BgpFsActionTrafficMarking other = (BgpFsActionTrafficMarking) obj;
            return Arrays.equals(this.dscpValue, other.dscpValue);
        }
        return false;
    }

    @Override
    public int write(ChannelBuffer cb) {
        int iLenStartIndex = cb.writerIndex();

        cb.writeShort(TYPE);

        cb.writeBytes(dscpValue);

        return cb.writerIndex() - iLenStartIndex;
    }

    /**
     * Reads the channel buffer and returns object.
     *
     * @param cb channelBuffer
     * @return object of flow spec action traffic marking
     * @throws BgpParseException while parsing BgpFsActionTrafficMarking
     */
    public static BgpFsActionTrafficMarking read(ChannelBuffer cb) throws BgpParseException {
        byte[] dscpValue;

        dscpValue = cb.readBytes(DSCP_LEN).array();
        return new BgpFsActionTrafficMarking(dscpValue);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("TYPE", TYPE)
                .add("dscpValue", dscpValue).toString();
    }

    @Override
    public int compareTo(Object o) {
        // TODO Auto-generated method stub
        return 0;
    }
}
