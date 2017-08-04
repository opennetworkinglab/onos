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
 * Provides implementation of BGP wide community target.
 */
public class WideCommunityTarget implements BgpValueType {

    public static final byte TYPE = 1;
    private List<BgpValueType> targetTlv;

    /**
     * Creates an instance of Wide community targets.
     *
     * @param targetTlv wide community targets to match
     */
    public WideCommunityTarget(List<BgpValueType> targetTlv) {
        this.targetTlv = targetTlv;
    }

    /**
     * Returns object of this class with specified values.
     *
     * @param targetTlv wide community target
     * @return object of WideCommunityTarget
     */
    public static WideCommunityTarget of(List<BgpValueType> targetTlv) {
        return new WideCommunityTarget(targetTlv);
    }

    /**
     * Returns wide community targets.
     *
     * @return wide community targets
     */
    public List<BgpValueType> targetTlv() {
        return targetTlv;
    }

    /**
     * Sets wide community target.
     *
     * @param targetTlv wide community targets to match
     */
    public void setTargetTlv(List<BgpValueType> targetTlv) {
        this.targetTlv = targetTlv;
    }

    @Override
    public int hashCode() {
        return Objects.hash(targetTlv);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (obj instanceof WideCommunityTarget) {
            WideCommunityTarget other = (WideCommunityTarget) obj;
            return Objects.equals(targetTlv, other.targetTlv);
        }
        return false;
    }

    @Override
    public int write(ChannelBuffer c) {
        int iLenStartIndex = c.writerIndex();
        WideCommunity.encodeWideCommunityTlv(c, targetTlv());
        return c.writerIndex() - iLenStartIndex;
    }

    /**
     * Reads the channel buffer and returns object of WideCommunityTarget.
     *
     * @param c ChannelBuffer
     * @return object of WideCommunityTarget
     * @throws BgpParseException on read error
     */
    public static WideCommunityTarget read(ChannelBuffer c) throws BgpParseException {
        return new WideCommunityTarget(WideCommunity.decodeWideCommunityTlv(c));
    }

    @Override
    public short getType() {
        return TYPE;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("Type", TYPE)
                .add("targetTlv", targetTlv)
                .toString();
    }

    @Override
    public int compareTo(Object o) {
        // TODO Auto-generated method stub
        return 0;
    }
}
