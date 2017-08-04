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

import com.google.common.base.MoreObjects;

import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.bgpio.exceptions.BgpParseException;
import org.onosproject.bgpio.types.attr.WideCommunity;

import java.util.List;
import java.util.Objects;

/**
 * Provides implementation of BGP wide community parameter.
 */
public class WideCommunityParameter implements BgpValueType {

    public static final byte TYPE = 3;
    private List<BgpValueType> parameterTlv;

    /**
     * Creates an instance of wide community parameter.
     *
     * @param parameterTlv wide community parameter
     */
    public WideCommunityParameter(List<BgpValueType> parameterTlv) {
        this.parameterTlv = parameterTlv;
    }

    /**
     * Returns object of this class with specified values.
     *
     * @param parameterTlv wide community parameter
     * @return object of WideCommunityParameter
     */
    public static WideCommunityParameter of(List<BgpValueType> parameterTlv) {
        return new WideCommunityParameter(parameterTlv);
    }

    /**
     * Returns wide community parameter.
     *
     * @return wide community parameter
     */
    public List<BgpValueType> parameterTlv() {
        return parameterTlv;
    }

    /**
     * Sets wide community parameter.
     *
     * @param parameterTlv wide community parameter
     */
    public void setParameterTlv(List<BgpValueType> parameterTlv) {
        this.parameterTlv = parameterTlv;
    }

    @Override
    public int hashCode() {
        return Objects.hash(parameterTlv);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (obj instanceof WideCommunityParameter) {
            WideCommunityParameter other = (WideCommunityParameter) obj;
            return Objects.equals(parameterTlv, other.parameterTlv);
        }
        return false;
    }

    @Override
    public int write(ChannelBuffer c) {
        int iLenStartIndex = c.writerIndex();
        WideCommunity.encodeWideCommunityTlv(c, parameterTlv());
        return c.writerIndex() - iLenStartIndex;
    }

    /**
     * Reads the channel buffer and returns object of WideCommunityParameter.
     *
     * @param c ChannelBuffer
     * @return object of WideCommunityParameter
     * @throws BgpParseException on read error
     */
    public static WideCommunityParameter read(ChannelBuffer c) throws BgpParseException {
        return new WideCommunityParameter(WideCommunity.decodeWideCommunityTlv(c));
    }

    @Override
    public short getType() {
        return TYPE;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .omitNullValues()
                .add("Type", TYPE)
                .add("parameterTlv", parameterTlv)
                .toString();
    }

    @Override
    public int compareTo(Object o) {
        // TODO Auto-generated method stub
        return 0;
    }
}