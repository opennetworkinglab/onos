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

import com.google.common.base.MoreObjects;

import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.bgpio.exceptions.BgpParseException;
import org.onosproject.bgpio.types.attr.WideCommunity;

import java.util.List;
import java.util.Objects;

/**
 * Provides implementation of BGP wide community exclude target.
 */
public class WideCommunityExcludeTarget implements BgpValueType {

    public static final byte TYPE = 2;
    private List<BgpValueType> excludeTargetTlv;

    /**
     * Wide community targets.
     *
     * @param excludeTargetTlv wide community exclude target
     */
    public WideCommunityExcludeTarget(List<BgpValueType> excludeTargetTlv) {
        this.excludeTargetTlv = excludeTargetTlv;
    }

    /**
     * Returns object of this class with specified values.
     *
     * @param excludeTargetTlv exclude target
     * @return object of WideCommunityExcludeTarget
     */
    public static BgpValueType of(List<BgpValueType> excludeTargetTlv) {
        return new WideCommunityExcludeTarget(excludeTargetTlv);
    }

    /**
     * Returns wide community targets.
     *
     * @return wide community targets
     */
    public List<BgpValueType> excludeTargetTlv() {
        return excludeTargetTlv;
    }

    /**
     * Sets wide community target.
     *
     * @param excludeTargetTlv wide community  exclude targets
     */
    public void setExcludeTargetTlv(List<BgpValueType> excludeTargetTlv) {
        this.excludeTargetTlv = excludeTargetTlv;
    }


    @Override
    public int hashCode() {
        return Objects.hash(excludeTargetTlv);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (obj instanceof WideCommunityExcludeTarget) {
            WideCommunityExcludeTarget other = (WideCommunityExcludeTarget) obj;
            return Objects.equals(excludeTargetTlv, other.excludeTargetTlv);
        }
        return false;
    }

    @Override
    public int write(ChannelBuffer c) {
        int iLenStartIndex = c.writerIndex();
        WideCommunity.encodeWideCommunityTlv(c, excludeTargetTlv());
        return c.writerIndex() - iLenStartIndex;
    }

    /**
     * Reads the channel buffer and returns object of WideCommunityExcludeTarget.
     *
     * @param c ChannelBuffer
     * @return object of WideCommunityExcludeTarget
     * @throws BgpParseException on read error
     */
    public static WideCommunityExcludeTarget read(ChannelBuffer c) throws BgpParseException {
        return new WideCommunityExcludeTarget(WideCommunity.decodeWideCommunityTlv(c));
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
                .add("targetTlv", excludeTargetTlv)
                .toString();
    }

    @Override
    public int compareTo(Object o) {
        // TODO Auto-generated method stub
        return 0;
    }
}