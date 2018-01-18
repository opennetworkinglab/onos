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
package org.onosproject.bgpio.protocol.ver4;

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.bgpio.exceptions.BgpParseException;
import org.onosproject.bgpio.types.As4Path;
import org.onosproject.bgpio.types.AsPath;
import org.onosproject.bgpio.types.BgpErrorType;
import org.onosproject.bgpio.types.BgpExtendedCommunity;
import org.onosproject.bgpio.types.BgpValueType;
import org.onosproject.bgpio.types.LinkStateAttributes;
import org.onosproject.bgpio.types.LocalPref;
import org.onosproject.bgpio.types.Med;
import org.onosproject.bgpio.types.NextHop;
import org.onosproject.bgpio.types.Origin;
import org.onosproject.bgpio.types.MpReachNlri;
import org.onosproject.bgpio.types.MpUnReachNlri;
import org.onosproject.bgpio.util.UnSupportedAttribute;
import org.onosproject.bgpio.util.Validation;
import org.onosproject.bgpio.util.Constants;
import org.onosproject.bgpio.types.attr.WideCommunity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.MoreObjects;

/**
 * Provides Implementation of BGP Path Attribute.
 */
public class BgpPathAttributes {

    /* Path attribute:
           <attribute type, attribute length, attribute value>

           0                   1
           0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5
           +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
           |  Attr. Flags  |Attr. Type Code|
           +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
           REFERENCE : RFC 4271
    */
    private static final Logger log = LoggerFactory.getLogger(BgpPathAttributes.class);

    public static final int LINK_STATE_ATTRIBUTE_TYPE = 29;
    public static final int MPREACHNLRI_TYPE = 14;
    public static final int MPUNREACHNLRI_TYPE = 15;
    public static final int EXTENDED_COMMUNITY_TYPE = 16;

    private final List<BgpValueType> pathAttribute;

    /**
     * Initialize parameter.
     */
    public BgpPathAttributes() {
        this.pathAttribute = null;
    }

    /**
     * Constructor to initialize parameters for BGP path attributes.
     *
     * @param pathAttribute list of path attributes
     */
    public BgpPathAttributes(List<BgpValueType> pathAttribute) {
        this.pathAttribute = pathAttribute;
    }

    /**
     * Returns list of path attributes.
     *
     * @return list of path attributes
     */
    public List<BgpValueType> pathAttributes() {
        return this.pathAttribute;
    }

    /**
     * Reads from channelBuffer and parses BGP path attributes.
     *
     * @param cb channelBuffer
     * @return object of BgpPathAttributes
     * @throws BgpParseException while parsing BGP path attributes
     */
    public static BgpPathAttributes read(ChannelBuffer cb)
            throws BgpParseException {

        BgpValueType pathAttribute = null;
        List<BgpValueType> pathAttributeList = new LinkedList<>();
        boolean isOrigin = false;
        boolean isAsPath = false;
        boolean isNextHop = false;
        boolean isMpReach = false;
        boolean isMpUnReach = false;
        while (cb.readableBytes() > 0) {
            cb.markReaderIndex();
            byte flags = cb.readByte();
            byte typeCode = cb.readByte();
            cb.resetReaderIndex();
            switch (typeCode) {
            case Origin.ORIGIN_TYPE:
                pathAttribute = Origin.read(cb);
                isOrigin = ((Origin) pathAttribute).isOriginSet();
                break;
            case AsPath.ASPATH_TYPE:
                pathAttribute = AsPath.read(cb);
                isAsPath = ((AsPath) pathAttribute).isaspathSet();
                break;
            case As4Path.AS4PATH_TYPE:
                pathAttribute = As4Path.read(cb);
                break;
            case NextHop.NEXTHOP_TYPE:
                pathAttribute = NextHop.read(cb);
                isNextHop = ((NextHop) pathAttribute).isNextHopSet();
                break;
            case Med.MED_TYPE:
                pathAttribute = Med.read(cb);
                break;
            case LocalPref.LOCAL_PREF_TYPE:
                pathAttribute = LocalPref.read(cb);
                break;
            case MpReachNlri.MPREACHNLRI_TYPE:
                pathAttribute = MpReachNlri.read(cb);
                isMpReach = ((MpReachNlri) pathAttribute).isMpReachNlriSet();
                break;
            case MpUnReachNlri.MPUNREACHNLRI_TYPE:
                pathAttribute = MpUnReachNlri.read(cb);
                isMpUnReach = ((MpUnReachNlri) pathAttribute)
                        .isMpUnReachNlriSet();
                break;
            case LINK_STATE_ATTRIBUTE_TYPE:
                pathAttribute = LinkStateAttributes.read(cb);
                break;
            case EXTENDED_COMMUNITY_TYPE:
                pathAttribute = BgpExtendedCommunity.read(cb);
                break;
            case WideCommunity.TYPE:
                pathAttribute = WideCommunity.read(cb);
                break;
            default:
                log.debug("Skip bytes for unsupported attribute types");
                UnSupportedAttribute.read(cb);
            }
            pathAttributeList.add(pathAttribute);
        }

        checkMandatoryAttr(isOrigin, isAsPath, isNextHop, isMpReach, isMpUnReach);
        //TODO:if mp_reach or mp_unreach not present ignore the packet
        return new BgpPathAttributes(pathAttributeList);
    }

    /**
     * Write path attributes to channelBuffer.
     *
     * @param cb channelBuffer
     * @return object of BgpPathAttributes
     * @throws BgpParseException while parsing BGP path attributes
     */
    public int write(ChannelBuffer cb)
            throws BgpParseException {

        if (pathAttribute == null) {
            return 0;
        }
        int iLenStartIndex = cb.writerIndex();

        ListIterator<BgpValueType> iterator = pathAttribute.listIterator();

        int pathAttributeIndx = cb.writerIndex();
        cb.writeShort(0);

        while (iterator.hasNext()) {

            BgpValueType attr = iterator.next();

            switch (attr.getType()) {
            case Origin.ORIGIN_TYPE:
                Origin origin = (Origin) attr;
                origin.write(cb);
                break;
            case AsPath.ASPATH_TYPE:
                AsPath asPath = (AsPath) attr;
                asPath.write(cb);
                break;
            case As4Path.AS4PATH_TYPE:
                As4Path as4Path = (As4Path) attr;
                as4Path.write(cb);
                break;
            case NextHop.NEXTHOP_TYPE:
                NextHop nextHop = (NextHop) attr;
                nextHop.write(cb);
                break;
            case Med.MED_TYPE:
                Med med = (Med) attr;
                med.write(cb);
                break;
            case LocalPref.LOCAL_PREF_TYPE:
                LocalPref localPref = (LocalPref) attr;
                localPref.write(cb);
                break;
            case Constants.BGP_EXTENDED_COMMUNITY:
                BgpExtendedCommunity extendedCommunity = (BgpExtendedCommunity) attr;
                extendedCommunity.write(cb);
                break;
            case WideCommunity.TYPE:
                WideCommunity wideCommunity = (WideCommunity) attr;
                wideCommunity.write(cb);
                break;
            case MpReachNlri.MPREACHNLRI_TYPE:
                MpReachNlri mpReach = (MpReachNlri) attr;
                mpReach.write(cb);
                break;
            case MpUnReachNlri.MPUNREACHNLRI_TYPE:
                MpUnReachNlri mpUnReach = (MpUnReachNlri) attr;
                mpUnReach.write(cb);
                break;
            case LINK_STATE_ATTRIBUTE_TYPE:
                LinkStateAttributes linkState = (LinkStateAttributes) attr;
                linkState.write(cb);
                break;
            default:
                return cb.writerIndex() - iLenStartIndex;
            }
        }

        int pathAttrLen = cb.writerIndex() - pathAttributeIndx;
        cb.setShort(pathAttributeIndx, (short) (pathAttrLen - 2));
        return cb.writerIndex() - iLenStartIndex;
    }

    /**
     * Checks mandatory attributes are presents, if not present throws exception.
     *
     * @param isOrigin say whether origin attribute is present
     * @param isAsPath say whether aspath attribute is present
     * @param isNextHop say whether nexthop attribute is present
     * @param isMpReach say whether mpreach attribute is present
     * @param isMpUnReach say whether mpunreach attribute is present
     * @throws BgpParseException if mandatory path attribute is not present
     */
    public static void checkMandatoryAttr(boolean isOrigin, boolean isAsPath,
            boolean isNextHop, boolean isMpReach, boolean isMpUnReach)
            throws BgpParseException {
        // Mandatory attributes validation not required for MP_UNREACH
        if (isMpUnReach) {
            return;
        }

        if (!isOrigin) {
            log.debug("Mandatory attributes not Present");
            Validation.validateType(BgpErrorType.UPDATE_MESSAGE_ERROR,
                    BgpErrorType.MISSING_WELLKNOWN_ATTRIBUTE,
                    Origin.ORIGIN_TYPE);
        }
        if (!isAsPath) {
            log.debug("Mandatory attributes not Present");
            Validation.validateType(BgpErrorType.UPDATE_MESSAGE_ERROR,
                    BgpErrorType.MISSING_WELLKNOWN_ATTRIBUTE,
                    AsPath.ASPATH_TYPE);
        }
        if (!isMpReach && !isNextHop) {
            log.debug("Mandatory attributes not Present");
            Validation.validateType(BgpErrorType.UPDATE_MESSAGE_ERROR,
                    BgpErrorType.MISSING_WELLKNOWN_ATTRIBUTE,
                    NextHop.NEXTHOP_TYPE);
        }
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("pathAttribute", pathAttribute)
                .toString();
    }
}
