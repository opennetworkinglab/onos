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

import java.util.Objects;
import org.onosproject.bgpio.util.Constants;
import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.bgpio.exceptions.BgpParseException;
import com.google.common.base.MoreObjects;

/**
 * Provides implementation of BGP flow specification action.
 */
public class BgpFsActionTrafficRate implements BgpValueType {

    public static final short TYPE = Constants.BGP_FLOWSPEC_ACTION_TRAFFIC_RATE;
    private short asn;
    private float rate;

    /**
     * Constructor to initialize the value.
     *
     * @param asn autonomous system number
     * @param rate traffic rate
     */
    public BgpFsActionTrafficRate(short asn, float rate) {
        this.asn = asn;
        this.rate = rate;
    }

    @Override
    public short getType() {
        return this.TYPE;
    }

    @Override
    public int hashCode() {
        return Objects.hash(asn, rate);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof BgpFsActionTrafficRate) {
            BgpFsActionTrafficRate other = (BgpFsActionTrafficRate) obj;
            return Objects.equals(this.asn, other.asn) && Objects.equals(this.rate, other.rate);
        }
        return false;
    }

    @Override
    public int write(ChannelBuffer cb) {

        int iLenStartIndex = cb.writerIndex();
        cb.writeShort(TYPE);

        cb.writeShort(asn);

        cb.writeFloat(rate);

        return cb.writerIndex() - iLenStartIndex;
    }

    /**
     * Reads the channel buffer and returns object.
     *
     * @param cb channelBuffer
     * @return object of flow spec action traffic rate
     * @throws BgpParseException while parsing BgpFsActionTrafficRate
     */
    public static BgpFsActionTrafficRate read(ChannelBuffer cb) throws BgpParseException {
        short asn;
        float rate;

        asn = cb.readShort();
        rate = cb.readFloat();
        return new BgpFsActionTrafficRate(asn, rate);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("TYPE", TYPE)
                .add("asn", asn)
                .add("rate", rate).toString();
    }

    @Override
    public int compareTo(Object o) {
        // TODO Auto-generated method stub
        return 0;
    }
}
