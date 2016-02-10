/*
 * Copyright 2016 Open Networking Laboratory
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

import java.util.Objects;

import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.bgpio.exceptions.BgpParseException;
import org.onosproject.bgpio.util.Constants;
import java.util.List;
import com.google.common.base.MoreObjects;

/**
 * Provides implementation of BGP flow specification component.
 */
public class BgpFsIcmpCode implements BgpValueType {
    public static final byte FLOW_SPEC_TYPE = Constants.BGP_FLOWSPEC_ICMP_CD;
    private List<BgpFsOperatorValue> operatorValue;

    /**
     * Constructor to initialize the value.
     *
     * @param operatorValue list of operator and value
     */
    public BgpFsIcmpCode(List<BgpFsOperatorValue> operatorValue) {
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
        if (obj instanceof BgpFsIcmpCode) {
            BgpFsIcmpCode other = (BgpFsIcmpCode) obj;
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
     * @param type address type
     * @return object of flow spec ICMP code
     * @throws BgpParseException while parsing BgpFsIcmpCode
     */
    public static BgpFsIcmpCode read(ChannelBuffer cb, short type) throws BgpParseException {
        return null;
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
