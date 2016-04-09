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

import java.util.LinkedList;
import java.util.Objects;
import java.util.List;
import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.bgpio.exceptions.BgpParseException;
import org.onosproject.bgpio.util.Constants;
import com.google.common.base.MoreObjects;

/**
 * Provides implementation of BGP flow specification component.
 */
public class BgpFsDestinationPortNum implements BgpValueType {

    public static final byte FLOW_SPEC_TYPE = Constants.BGP_FLOWSPEC_DST_PORT;
    private List<BgpFsOperatorValue> operatorValue;

    /**
     * Constructor to initialize the value.
     *
     * @param operatorValue list of operator and value
     */
    public BgpFsDestinationPortNum(List<BgpFsOperatorValue> operatorValue) {
        this.operatorValue = operatorValue;
    }

    @Override
    public short getType() {
        return this.FLOW_SPEC_TYPE;
    }

    @Override
    public int hashCode() {
        return Objects.hash(operatorValue);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof BgpFsDestinationPortNum) {
            BgpFsDestinationPortNum other = (BgpFsDestinationPortNum) obj;
            return this.operatorValue.equals(other.operatorValue);
        }
        return false;
    }

    @Override
    public int write(ChannelBuffer cb) {
        int iLenStartIndex = cb.writerIndex();
        cb.writeByte(FLOW_SPEC_TYPE);

        for (BgpFsOperatorValue fsOperVal : operatorValue) {
            cb.writeByte(fsOperVal.option());
            cb.writeBytes(fsOperVal.value());
        }

        return cb.writerIndex() - iLenStartIndex;
    }

    /**
     * Reads the channel buffer and returns object.
     *
     * @param cb channelBuffer
     * @return object of flow spec destination port number
     * @throws BgpParseException while parsing BgpFsDestinationPortNum
     */
    public static BgpFsDestinationPortNum read(ChannelBuffer cb) throws BgpParseException {
        List<BgpFsOperatorValue> operatorValue = new LinkedList<>();
        byte option;
        short port;

        /*
        0   1   2   3   4   5   6   7
        +---+---+---+---+---+---+---+----+
        | e | a |   len  | 0 |lt |gt |eq |
        +---+---+---+---+---+---+---+----+
        */

        do {
            option = (byte) cb.readByte();
            int len = (option & Constants.BGP_FLOW_SPEC_LEN_MASK) >> 4;
            if ((1 << len) == 1) {
                port = cb.readByte();
                operatorValue.add(new BgpFsOperatorValue(option, new byte[] {(byte) port}));
            } else {
                port = cb.readShort();
                operatorValue.add(new BgpFsOperatorValue(option, new byte[] {(byte) (port >> 8), (byte) port}));
            }
        } while ((option & Constants.BGP_FLOW_SPEC_END_OF_LIST_MASK) == 0);

        return new BgpFsDestinationPortNum(operatorValue);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("FLOW_SPEC_TYPE", FLOW_SPEC_TYPE)
                .add("operatorValue", operatorValue).toString();
    }

    @Override
    public int compareTo(Object o) {
        // TODO Auto-generated method stub
        return 0;
    }
}
