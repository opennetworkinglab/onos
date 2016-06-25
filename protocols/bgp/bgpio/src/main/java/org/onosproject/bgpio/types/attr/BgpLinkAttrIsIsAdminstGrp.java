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
 * Implements BGP attribute Is Is Administrative area.
 */
public final class BgpLinkAttrIsIsAdminstGrp implements BgpValueType {

    protected static final Logger log = LoggerFactory
            .getLogger(BgpLinkAttrIsIsAdminstGrp.class);

    public static final int ATTRLINK_PROTECTIONTYPE = 1088;
    public static final int ISIS_ADMIN_DATA_LEN = 4;

    /* ISIS administrative group */
    private final long isisAdminGrp;

    /**
     * Constructor to initialize the values.
     *
     * @param isisAdminGrp ISIS protocol admin group
     */
    public BgpLinkAttrIsIsAdminstGrp(long isisAdminGrp) {
        this.isisAdminGrp = isisAdminGrp;
    }

    /**
     * Returns object of this class with specified values.
     *
     * @param isisAdminGrp ISIS admin group
     * @return object of BgpLinkAttrIsIsAdminstGrp
     */
    public static BgpLinkAttrIsIsAdminstGrp of(final long isisAdminGrp) {
        return new BgpLinkAttrIsIsAdminstGrp(isisAdminGrp);
    }

    /**
     * Reads the BGP link attributes of ISIS administrative group area.
     *
     * @param cb Channel buffer
     * @return object of type BgpLinkAttrIsIsAdminstGrp
     * @throws BgpParseException while parsing BgpLinkAttrIsIsAdminstGrp
     */
    public static BgpLinkAttrIsIsAdminstGrp read(ChannelBuffer cb)
            throws BgpParseException {
        long isisAdminGrp;
        short lsAttrLength = cb.readShort();

        if ((lsAttrLength != ISIS_ADMIN_DATA_LEN)
                || (cb.readableBytes() < lsAttrLength)) {
            Validation.validateLen(BgpErrorType.UPDATE_MESSAGE_ERROR,
                                   BgpErrorType.ATTRIBUTE_LENGTH_ERROR,
                                   lsAttrLength);
        }

        isisAdminGrp = cb.readUnsignedInt();

        return BgpLinkAttrIsIsAdminstGrp.of(isisAdminGrp);
    }

    /**
     * Link attributes of ISIS administrative group area.
     *
     * @return long value of the administrative group area
     */
    public long linkAttrIsIsAdminGrp() {
        return isisAdminGrp;
    }

    @Override
    public short getType() {
        return ATTRLINK_PROTECTIONTYPE;
    }

    @Override
    public int hashCode() {
        return Objects.hash(isisAdminGrp);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof BgpLinkAttrIsIsAdminstGrp) {
            BgpLinkAttrIsIsAdminstGrp other = (BgpLinkAttrIsIsAdminstGrp) obj;
            return Objects.equals(isisAdminGrp, other.isisAdminGrp);
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
                .add("isisAdminGrp", isisAdminGrp).toString();
    }

    @Override
    public int compareTo(Object o) {
        // TODO Auto-generated method stub
        return 0;
    }
}
