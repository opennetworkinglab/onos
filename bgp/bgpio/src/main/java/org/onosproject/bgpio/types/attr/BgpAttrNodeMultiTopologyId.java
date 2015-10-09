/*
 * Copyright 2015 Open Networking Laboratory
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

package org.onosproject.bgpio.types.attr;

import java.util.Objects;

import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.bgpio.exceptions.BGPParseException;
import org.onosproject.bgpio.types.BGPErrorType;
import org.onosproject.bgpio.types.BGPValueType;
import org.onosproject.bgpio.util.Validation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

/**
 * BGP Multi-Topology ID of the LS attribute.
 */
public class BgpAttrNodeMultiTopologyId implements BGPValueType {

    private static final Logger log = LoggerFactory
            .getLogger(BgpAttrNodeMultiTopologyId.class);

    public static final int ATTRNODE_MULTITOPOLOGY = 263;

    /* Opaque Node Attribute */
    private short[] multiTopologyId;

    /**
     * Constructor to initialize the Node attribute multi-topology ID.
     *
     * @param multiTopologyId multi-topology ID
     */
    BgpAttrNodeMultiTopologyId(short[] multiTopologyId) {
        this.multiTopologyId = multiTopologyId;
    }

    /**
     * Reads the Multi-topology ID of Node attribute.
     *
     * @param cb ChannelBuffer
     * @return Constructor of BgpAttrNodeMultiTopologyId
     * @throws BGPParseException while parsing BgpAttrNodeMultiTopologyId
     */
    public static BgpAttrNodeMultiTopologyId read(ChannelBuffer cb)
            throws BGPParseException {

        log.debug("BgpAttrNodeMultiTopologyId");
        short lsAttrLength = cb.readShort();
        int len = lsAttrLength / 2; // Length is 2*n and n is the number of MT-IDs

        if (cb.readableBytes() < lsAttrLength) {
            Validation.validateLen(BGPErrorType.UPDATE_MESSAGE_ERROR,
                                   BGPErrorType.ATTRIBUTE_LENGTH_ERROR,
                                   cb.readableBytes());
        }

        short[] multiTopologyId;
        multiTopologyId = new short[len];
        for (int i = 0; i < len; i++) {
            multiTopologyId[i] = cb.readShort();
        }

        return new BgpAttrNodeMultiTopologyId(multiTopologyId);
    }

    /**
     * to get the multi-topology ID.
     *
     * @return multitopology ID
     */
    short[] getAttrMultiTopologyId() {
        return multiTopologyId;
    }

    @Override
    public short getType() {
        return ATTRNODE_MULTITOPOLOGY;
    }

    @Override
    public int hashCode() {
        return Objects.hash(multiTopologyId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof BgpAttrNodeMultiTopologyId) {
            BgpAttrNodeMultiTopologyId other = (BgpAttrNodeMultiTopologyId) obj;
            return Objects.equals(multiTopologyId, other.multiTopologyId);
        }
        return false;
    }

    @Override
    public int write(ChannelBuffer cb) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .omitNullValues()
                .add("multiTopologyId", multiTopologyId)
                .toString();
    }
}