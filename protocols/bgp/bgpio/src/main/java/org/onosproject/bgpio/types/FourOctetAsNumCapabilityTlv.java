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
package org.onosproject.bgpio.types;

import java.util.Objects;

import org.jboss.netty.buffer.ChannelBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

/**
 * Provides FourOctetAsNumCapabilityTlv Capability Tlv.
 */
public class FourOctetAsNumCapabilityTlv implements BgpValueType {

    /**
     * support to indicate its support for four-octet AS numbers -CAPABILITY TLV format.
     */
    protected static final Logger log = LoggerFactory
            .getLogger(FourOctetAsNumCapabilityTlv.class);

    public static final byte TYPE = 65;
    public static final byte LENGTH = 4;

    private final int rawValue;

    /**
     * constructor to initialize rawValue.
     * @param rawValue FourOctetAsNumCapabilityTlv
     */
    public FourOctetAsNumCapabilityTlv(int rawValue) {
        this.rawValue = rawValue;
    }

    /**
     * constructor to initialize raw.
     * @param raw AS number
     * @return object of FourOctetAsNumCapabilityTlv
     */
    public static FourOctetAsNumCapabilityTlv of(final int raw) {
        return new FourOctetAsNumCapabilityTlv(raw);
    }

    /**
     * Returns value of TLV.
     * @return int value of rawValue
     */
    public int getInt() {
        return rawValue;
    }

    @Override
    public short getType() {
        return TYPE;
    }

    @Override
    public int hashCode() {
        return Objects.hash(rawValue);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof FourOctetAsNumCapabilityTlv) {
            FourOctetAsNumCapabilityTlv other = (FourOctetAsNumCapabilityTlv) obj;
            return Objects.equals(rawValue, other.rawValue);
        }
        return false;
    }

    @Override
    public int write(ChannelBuffer cb) {
        int iLenStartIndex = cb.writerIndex();
        cb.writeByte(TYPE);
        cb.writeByte(LENGTH);
        cb.writeInt(rawValue);
        return cb.writerIndex() - iLenStartIndex;
    }

    /**
     * Reads the channel buffer and returns object of FourOctetAsNumCapabilityTlv.
     * @param cb type of channel buffer
     * @return object of FourOctetAsNumCapabilityTlv
     */
    public static FourOctetAsNumCapabilityTlv read(ChannelBuffer cb) {
        return FourOctetAsNumCapabilityTlv.of(cb.readInt());
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("Type", TYPE)
                .add("Length", LENGTH)
                .add("Value", rawValue).toString();
    }

    @Override
    public int compareTo(Object o) {
        // TODO Auto-generated method stub
        return 0;
    }
}
