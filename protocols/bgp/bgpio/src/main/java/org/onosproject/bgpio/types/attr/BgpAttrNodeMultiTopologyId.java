/*
 * Copyright 2015-present Open Networking Foundation
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

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;

import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.bgpio.exceptions.BgpParseException;
import org.onosproject.bgpio.types.BgpErrorType;
import org.onosproject.bgpio.types.BgpValueType;
import org.onosproject.bgpio.util.Validation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

/**
 * BGP Multi-Topology ID of the LS attribute.
 */
public class BgpAttrNodeMultiTopologyId implements BgpValueType {

    private static final Logger log = LoggerFactory
            .getLogger(BgpAttrNodeMultiTopologyId.class);

    public static final int ATTRNODE_MULTITOPOLOGY = 263;

    /* Opaque Node Attribute */
    private List<Short> multiTopologyId = new ArrayList<Short>();

    /**
     * Constructor to initialize the Node attribute multi-topology ID.
     *
     * @param multiTopologyId multi-topology ID
     */
    public BgpAttrNodeMultiTopologyId(List<Short> multiTopologyId) {
        this.multiTopologyId = multiTopologyId;
    }

    /**
     * Returns object of this class with specified values.
     *
     * @param multiTopologyId Prefix Metric
     * @return object of BgpAttrNodeMultiTopologyId
     */
    public static BgpAttrNodeMultiTopologyId of(ArrayList<Short> multiTopologyId) {
        return new BgpAttrNodeMultiTopologyId(multiTopologyId);
    }

    /**
     * Reads the Multi-topology ID of Node attribute.
     *
     * @param cb ChannelBuffer
     * @param len Length of the TLV
     * @return Constructor of BgpAttrNodeMultiTopologyId
     * @throws BgpParseException while parsing BgpAttrNodeMultiTopologyId
     */
    public static BgpAttrNodeMultiTopologyId read(ChannelBuffer cb, int len)
            throws BgpParseException {
        ArrayList<Short> multiTopologyId = new ArrayList<Short>();
        short tempMultiTopologyId;
        //short lsAttrLength = cb.readShort();
        int numOfMtid = len / 2; // Length is 2*n and n is the number of MT-IDs

        if (cb.readableBytes() < len) {
            Validation.validateLen(BgpErrorType.UPDATE_MESSAGE_ERROR,
                                   BgpErrorType.ATTRIBUTE_LENGTH_ERROR,
                                   len);
        }

        for (int i = 0; i < numOfMtid; i++) {
            tempMultiTopologyId = cb.readShort();
            multiTopologyId.add(new Short(tempMultiTopologyId));
        }

        return new BgpAttrNodeMultiTopologyId(multiTopologyId);
    }

    /**
     * to get the multi-topology ID.
     *
     * @return multitopology ID
     */
    public List<Short> attrMultiTopologyId() {
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
        // TODO This will be implemented in the next version
        return 0;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .omitNullValues()
                .add("multiTopologyId", multiTopologyId)
                .toString();
    }

    @Override
    public int compareTo(Object o) {
        if (this.equals(o)) {
            return 0;
        }
        int countOtherSubTlv = ((BgpAttrNodeMultiTopologyId) o).multiTopologyId.size();
        int countObjSubTlv = multiTopologyId.size();
        if (countOtherSubTlv != countObjSubTlv) {
            if (countOtherSubTlv > countObjSubTlv) {
                return 1;
            } else {
                return -1;
            }
       }
        ListIterator<Short> listIterator = multiTopologyId.listIterator();
        ListIterator<Short> listIteratorOther = ((BgpAttrNodeMultiTopologyId) o).multiTopologyId.listIterator();
        while (listIterator.hasNext()) {
            short id = listIterator.next();
            short id1 = listIteratorOther.next();
            if (((Short) id).compareTo((Short) id1) != 0) {
                return ((Short) id).compareTo((Short) id1);
            }
        }
        return 0;
    }
}
