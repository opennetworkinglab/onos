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
package org.onosproject.bgpio.types.attr;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.bgpio.exceptions.BgpParseException;
import org.onosproject.bgpio.types.BgpErrorType;
import org.onosproject.bgpio.types.BgpValueType;
import org.onosproject.bgpio.util.Validation;

import com.google.common.base.MoreObjects;

/**
 * Implements BGP link Shared Risk Link Group attribute.
 */
public class BgpLinkAttrSrlg implements BgpValueType {

    public static final short ATTRNODE_SRLG = 1097;
    public static final short SIZE = 4;

    /* Shared Risk Link Group */
    private List<Integer> sRlg = new ArrayList<Integer>();

    /**
     * Constructor to initialize the date.
     *
     * @param sRlg Shared Risk link group data
     */
    public BgpLinkAttrSrlg(List<Integer> sRlg) {
        this.sRlg = sRlg;
    }

    /**
     * Returns object of this class with specified values.
     *
     * @param sRlg Shared Risk link group data
     * @return object of BgpLinkAttrSrlg
     */
    public static BgpLinkAttrSrlg of(ArrayList<Integer> sRlg) {
        return new BgpLinkAttrSrlg(sRlg);
    }

    /**
     * Reads the BGP link attributes Shared Risk link group data.
     *
     * @param cb Channel buffer
     * @return object of type BgpLinkAttrSrlg
     * @throws BgpParseException while parsing BgpLinkAttrSrlg
     */
    public static BgpLinkAttrSrlg read(ChannelBuffer cb)
            throws BgpParseException {
        int tempSrlg;
        ArrayList<Integer> sRlg = new ArrayList<Integer>();

        short lsAttrLength = cb.readShort();
        int len = lsAttrLength / SIZE; // each element is of 4 octets

        if (cb.readableBytes() < lsAttrLength) {
            Validation.validateLen(BgpErrorType.UPDATE_MESSAGE_ERROR,
                                   BgpErrorType.ATTRIBUTE_LENGTH_ERROR,
                                   lsAttrLength);
        }

        for (int i = 0; i < len; i++) {
            tempSrlg = cb.readInt();
            sRlg.add(new Integer(tempSrlg));
        }

        return BgpLinkAttrSrlg.of(sRlg);
    }

    /**
     * Returns the Shared Risk link group data.
     *
     * @return array of Shared Risk link group data
     */
    public List<Integer> attrSrlg() {
        return sRlg;
    }

    @Override
    public short getType() {
        return ATTRNODE_SRLG;
    }

    @Override
    public int hashCode() {
        return Objects.hash(sRlg);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof BgpLinkAttrSrlg) {
            BgpLinkAttrSrlg other = (BgpLinkAttrSrlg) obj;
            return Objects.equals(sRlg, other.sRlg);
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
        return MoreObjects.toStringHelper(getClass()).omitNullValues().add("sRlg", sRlg).toString();
    }

    @Override
    public int compareTo(Object o) {
        // TODO Auto-generated method stub
        return 0;
    }
}
