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
import org.onosproject.bgpio.util.Constants;
import org.onosproject.bgpio.util.Validation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;

/**
 * Provides implementation of extended community BGP Path Attribute.
 */
public class BgpExtendedCommunity implements BgpValueType {

    private static final Logger log = LoggerFactory.getLogger(BgpExtendedCommunity.class);
    public static final short TYPE = Constants.BGP_EXTENDED_COMMUNITY;
    public static final byte FLAGS = (byte) 0xC0;
    private List<BgpValueType> fsActionTlv;

    /**
     * Constructor to initialize the value.
     *
     * @param fsActionTlv flow specification action type
     */
    public BgpExtendedCommunity(List<BgpValueType> fsActionTlv) {
        this.fsActionTlv = fsActionTlv;
    }

    /**
     * Returns extended community type.
     *
     * @return extended community
     */
    public List<BgpValueType> fsActionTlv() {
        return this.fsActionTlv;
    }

    /**
     * Reads from the channel buffer and parses extended community.
     *
     * @param cb ChannelBuffer
     * @return object of BgpExtendedCommunity
     * @throws BgpParseException while parsing extended community
     */
    public static BgpExtendedCommunity read(ChannelBuffer cb) throws BgpParseException {

        ChannelBuffer tempCb = cb.copy();
        Validation validation = Validation.parseAttributeHeader(cb);
        List<BgpValueType> fsActionTlvs = new LinkedList<>();

        if (cb.readableBytes() < validation.getLength()) {
            Validation.validateLen(BgpErrorType.UPDATE_MESSAGE_ERROR, BgpErrorType.ATTRIBUTE_LENGTH_ERROR,
                    validation.getLength());
        }
        //if fourth bit is set, length is read as short otherwise as byte , len includes type, length and value
        int len = validation.isShort() ? validation.getLength() + Constants.TYPE_AND_LEN_AS_SHORT : validation
                .getLength() + Constants.TYPE_AND_LEN_AS_BYTE;
        ChannelBuffer data = tempCb.readBytes(len);
        if (validation.getFirstBit() && !validation.getSecondBit() && validation.getThirdBit()) {
            throw new BgpParseException(BgpErrorType.UPDATE_MESSAGE_ERROR, BgpErrorType.ATTRIBUTE_FLAGS_ERROR, data);
        }

        ChannelBuffer tempBuf = cb.readBytes(validation.getLength());
        if (tempBuf.readableBytes() > 0) {
            BgpValueType fsActionTlv = null;
            ChannelBuffer actionBuf = tempBuf.readBytes(validation.getLength());

            while (actionBuf.readableBytes() > 0) {
                short actionType = actionBuf.readShort();
                switch (actionType) {
                    case Constants.BGP_FLOWSPEC_ACTION_TRAFFIC_ACTION:
                        fsActionTlv = BgpFsActionTrafficAction.read(actionBuf);
                        break;
                    case Constants.BGP_FLOWSPEC_ACTION_TRAFFIC_MARKING:
                        fsActionTlv = BgpFsActionTrafficMarking.read(actionBuf);
                        break;
                    case Constants.BGP_FLOWSPEC_ACTION_TRAFFIC_RATE:
                        fsActionTlv = BgpFsActionTrafficRate.read(actionBuf);
                        break;
                    case Constants.BGP_FLOWSPEC_ACTION_TRAFFIC_REDIRECT:
                        fsActionTlv = BgpFsActionReDirect.read(actionBuf);
                        break;
                    default: log.debug("Other type Not Supported:" + actionType);
                        break;
                }
                if (fsActionTlv != null) {
                    fsActionTlvs.add(fsActionTlv);
                }
            }
        }
        return new BgpExtendedCommunity(fsActionTlvs);
    }

    @Override
    public short getType() {
        return TYPE;
    }

    @Override
    public int hashCode() {
        return Objects.hash(fsActionTlv);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof BgpExtendedCommunity) {
            BgpExtendedCommunity other = (BgpExtendedCommunity) obj;
            return Objects.equals(fsActionTlv, other.fsActionTlv);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .omitNullValues()
                .add("fsActionTlv", fsActionTlv)
                .toString();
    }

    @Override
    public int write(ChannelBuffer cb) {
        int iLenStartIndex = cb.writerIndex();
        ListIterator<BgpValueType> listIterator = fsActionTlv().listIterator();

        cb.writeByte(FLAGS);
        cb.writeByte(getType());

        int iActionLenIndex = cb.writerIndex();
        cb.writeByte(0);

        while (listIterator.hasNext()) {
            BgpValueType fsTlv = listIterator.next();
            if (fsTlv.getType() == Constants.BGP_FLOWSPEC_ACTION_TRAFFIC_ACTION) {
                BgpFsActionTrafficAction trafficAction = (BgpFsActionTrafficAction) fsTlv;
                trafficAction.write(cb);
            } else if (fsTlv.getType() == Constants.BGP_FLOWSPEC_ACTION_TRAFFIC_MARKING) {
                BgpFsActionTrafficMarking trafficMarking = (BgpFsActionTrafficMarking) fsTlv;
                trafficMarking.write(cb);
            } else if (fsTlv.getType() == Constants.BGP_FLOWSPEC_ACTION_TRAFFIC_RATE) {
                BgpFsActionTrafficRate trafficRate = (BgpFsActionTrafficRate) fsTlv;
                trafficRate.write(cb);
            } else if (fsTlv.getType() == Constants.BGP_FLOWSPEC_ACTION_TRAFFIC_REDIRECT) {
                BgpFsActionReDirect trafficRedirect = (BgpFsActionReDirect) fsTlv;
                trafficRedirect.write(cb);
            }
        }

        int fsActionLen = cb.writerIndex() - iActionLenIndex;
        cb.setByte(iActionLenIndex, (byte) (fsActionLen - 1));

        return cb.writerIndex() - iLenStartIndex;
    }

    @Override
    public int compareTo(Object o) {
        // TODO Auto-generated method stub
        return 0;
    }
}
