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
package org.onosproject.bgpio.types.attr;

import com.google.common.base.MoreObjects;

import org.jboss.netty.buffer.ChannelBuffer;
import org.onlab.packet.IpAddress;
import org.onosproject.bgpio.exceptions.BgpParseException;
import org.onosproject.bgpio.types.BgpErrorType;
import org.onosproject.bgpio.types.BgpValueType;
import org.onosproject.bgpio.types.WideCommunityAttrHeader;
import org.onosproject.bgpio.types.WideCommunityExcludeTarget;
import org.onosproject.bgpio.types.WideCommunityInteger;
import org.onosproject.bgpio.types.WideCommunityIpV4Neighbour;
import org.onosproject.bgpio.types.WideCommunityParameter;
import org.onosproject.bgpio.types.WideCommunityTarget;
import org.onosproject.bgpio.util.Validation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * Provides implementation of wide community path attribute.
 */
public class WideCommunity implements BgpValueType {

    private static final Logger log = LoggerFactory.getLogger(WideCommunity.class);
    public static final byte TYPE = (byte) 129;
    public static final short LENGTH = 4;
    public static final byte TYPE_LENGTH_SIZE = 3;
    public static final byte FLAGS = (byte) 0x90;
    private WideCommunityAttrHeader wideCommunityHeader;
    private int community;
    private int localAsn;
    private int contextAsn;
    private WideCommunityTarget target;
    private WideCommunityExcludeTarget excludeTarget;
    private WideCommunityParameter parameter;

    /**
     * Creates an instance of wide community.
     *
     * @param wideCommunityHeader wide community header
     * @param community wide community
     * @param localAsn local ASN number
     * @param contextAsn context ASN number
     * @param target wide community include target
     * @param excludeTarget wide community exclude target
     * @param parameter wide community parameter
     */
    public WideCommunity(WideCommunityAttrHeader wideCommunityHeader, int community, int localAsn, int contextAsn,
                             WideCommunityTarget target, WideCommunityExcludeTarget excludeTarget,
                             WideCommunityParameter parameter) {
        this.wideCommunityHeader = wideCommunityHeader;
        this.community = community;
        this.localAsn = localAsn;
        this.contextAsn = contextAsn;
        this.target = target;
        this.excludeTarget = excludeTarget;
        this.parameter = parameter;
    }

    /**
     * Returns object of this class with specified values.
     *
     * @param wideCommunityHeader community header
     * @param community wide community
     * @param localAsn local ASN number
     * @param contextAsn context ASN number
     * @param target wide community include target
     * @param excludeTarget wide community exclude target
     * @param parameter wide community parameter
     * @return object of WideCommunityAttr
     */
    public static WideCommunity of(WideCommunityAttrHeader wideCommunityHeader, int community, int localAsn,
                                       int contextAsn, WideCommunityTarget target,
                                       WideCommunityExcludeTarget excludeTarget, WideCommunityParameter parameter) {
        return new WideCommunity(wideCommunityHeader, community, localAsn, contextAsn, target, excludeTarget,
                                     parameter);
    }

    /**
     * Returns wide community value.
     *
     * @return wide community value
     */
    public int community() {
        return community;
    }

    /**
     * Sets wide community value.
     *
     * @param community wide community value
     */
    public void setCommunity(int community) {
        this.community = community;
    }

    /**
     * Returns wide community local autonomous number.
     *
     * @return local autonomous number
     */
    public int localAsn() {
        return localAsn;
    }

    /**
     * Sets wide community local autonomous number.
     *
     * @param localAsn local autonomous number
     */
    public void setLocalAsn(int localAsn) {
        this.localAsn = localAsn;
    }

    /**
     * Returns wide community context autonomous number.
     *
     * @return contest autonomous number
     */
    public int contextAsn() {
        return contextAsn;
    }

    /**
     * Sets wide community context autonomous number.
     *
     * @param contextAsn context autonomous number
     */
    public void setContextAsn(int contextAsn) {
        this.contextAsn = contextAsn;
    }

    /**
     * Returns wide community target.
     *
     * @return wide community target
     */
    public WideCommunityTarget target() {
        return target;
    }

    /**
     * Sets wide community target.
     *
     * @param target wide community target
     */
    public void setTarget(WideCommunityTarget target) {
        this.target = target;
    }

    /**
     * Returns wide community exclude target.
     *
     * @return wide community exclude target
     */
    public WideCommunityExcludeTarget excludeTarget() {
        return excludeTarget;
    }

    /**
     * Sets wide community exclude target.
     *
     * @param excludeTarget wide community texclude arget
     */
    public void setExcludeTarget(WideCommunityExcludeTarget excludeTarget) {
        this.excludeTarget = excludeTarget;
    }

    /**
     * Returns wide community parameter.
     *
     * @return wide community parameter
     */
    public WideCommunityParameter parameter() {
        return parameter;
    }

    /**
     * Sets wide community parameter.
     *
     * @param parameter wide community parameter
     */
    public void setParameter(WideCommunityParameter parameter) {
        this.parameter = parameter;
    }

    @Override
    public int hashCode() {
        return Objects.hash(wideCommunityHeader, community, localAsn, contextAsn, target, excludeTarget, parameter);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof WideCommunity) {
            WideCommunity other = (WideCommunity) obj;
            return Objects.equals(wideCommunityHeader, other.wideCommunityHeader)
                    && Objects.equals(community, other.community) && Objects.equals(localAsn, other.localAsn)
                    && Objects.equals(contextAsn, other.contextAsn) && Objects.equals(target, other.target)
                    && Objects.equals(excludeTarget, other.excludeTarget) && Objects.equals(parameter, other.parameter);
        }
        return false;
    }

    @Override
    public int write(ChannelBuffer c) {
        int iTargetLenIndex;
        int length;
        int iLenStartIndex = c.writerIndex();
        c.writeByte(FLAGS); // TODO: update flag value
        c.writeByte(TYPE);

        int iLengthIndex = c.writerIndex();
        c.writeShort(0);

        wideCommunityHeader.write(c);

        int iComLengthIndex = c.writerIndex();
        c.writeShort(0);

        c.writeInt(community);
        c.writeInt(localAsn);
        c.writeInt(contextAsn);

        if (target() != null) {
            c.writeByte(WideCommunityTarget.TYPE);
            iTargetLenIndex = c.writerIndex();
            c.writeShort(0); // length

            target.write(c);

            length = c.writerIndex() - iTargetLenIndex;
            c.setShort(iTargetLenIndex, (short) (length - 2));
        }

        if (excludeTarget() != null) {
            c.writeByte(WideCommunityExcludeTarget.TYPE);
            iTargetLenIndex = c.writerIndex();
            c.writeShort(0); // length

            excludeTarget.write(c);

            length = c.writerIndex() - iTargetLenIndex;
            c.setShort(iTargetLenIndex, (short) (length - 2));
        }

        if (parameter() != null) {
            c.writeByte(WideCommunityParameter.TYPE);
            iTargetLenIndex = c.writerIndex();
            c.writeShort(0); // length

            parameter.write(c);

            length = c.writerIndex() - iTargetLenIndex;
            c.setShort(iTargetLenIndex, (short) (length - 2));
        }

        length = c.writerIndex() - iComLengthIndex;
        c.setShort(iComLengthIndex, (short) (length - 2));

        length = c.writerIndex() - iLengthIndex;
        c.setShort(iLengthIndex, (short) (length - 2));

        return c.writerIndex() - iLenStartIndex;
    }

    /**
     * Reads the wide community attribute.
     *
     * @param c ChannelBuffer
     * @return object of WideCommunityAttr
     * @throws BgpParseException while parsing BgpPrefixAttrRouteTag
     */
    public static WideCommunity read(ChannelBuffer c) throws BgpParseException {

        WideCommunityAttrHeader wideCommunityHeader;
        int community;
        int localAsn;
        int contextAsn;
        WideCommunityTarget target = null;
        WideCommunityExcludeTarget excludeTarget = null;
        WideCommunityParameter parameter = null;

        short length = c.readShort();

        if (c.readableBytes() < length) {
            Validation.validateLen(BgpErrorType.UPDATE_MESSAGE_ERROR, BgpErrorType.ATTRIBUTE_LENGTH_ERROR, length);
        }

        wideCommunityHeader = WideCommunityAttrHeader.read(c);
        if ((c.readableBytes() < 12) || (c.readableBytes() < wideCommunityHeader.length())) {
            Validation.validateLen(BgpErrorType.UPDATE_MESSAGE_ERROR, BgpErrorType.ATTRIBUTE_LENGTH_ERROR, length);
        }

        community = c.readInt();
        localAsn = c.readInt();
        contextAsn = c.readInt();

        while (c.readableBytes() > 0) {

            if (c.readableBytes() < TYPE_LENGTH_SIZE) {
                Validation.validateLen(BgpErrorType.UPDATE_MESSAGE_ERROR, BgpErrorType.ATTRIBUTE_LENGTH_ERROR,
                                       c.readableBytes());
            }

            byte type = c.readByte();
            length = c.readShort();

            if (c.readableBytes() < length) {
                Validation.validateLen(BgpErrorType.UPDATE_MESSAGE_ERROR, BgpErrorType.ATTRIBUTE_LENGTH_ERROR,
                                       c.readableBytes());
            }

            if (type == WideCommunityTarget.TYPE) {
                target = WideCommunityTarget.read(c);
            } else if (type == WideCommunityExcludeTarget.TYPE) {
                excludeTarget = WideCommunityExcludeTarget.read(c);
            } else if (type == WideCommunityParameter.TYPE) {
                parameter = WideCommunityParameter.read(c);
            }
        }
        return new WideCommunity(wideCommunityHeader, community, localAsn, contextAsn,
                                     target, excludeTarget, parameter);
    }

    /**
     * Encode wide community target(s).
     *
     * @param c channel buffer
     * @param targetTlv wide community include/exclude target
     */
    public static void encodeWideCommunityTlv(ChannelBuffer c,
                                    List<BgpValueType> targetTlv) {

        /*
         +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
         |  IPV4Neig(8)  |   Length:                   8 |
         +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
         | local                                               10101010  |
         +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
         | remote                                              10101010  |
         +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
         * */
        List<BgpValueType> target = targetTlv;
        if (target == null) {
           log.debug("target is null");
           return;
        }
        Iterator<BgpValueType> listIterator = targetTlv.iterator();

        while (listIterator.hasNext()) {
            BgpValueType attr = listIterator.next();
            if (attr instanceof WideCommunityIpV4Neighbour) {
                WideCommunityIpV4Neighbour ipv4Neig = (WideCommunityIpV4Neighbour) attr;
                ipv4Neig.write(c);
            } else if (attr instanceof WideCommunityInteger) {
                WideCommunityInteger integer = (WideCommunityInteger) attr;
                integer.write(c);
            }
        }
        return;
    }

    /**
     * Decode wide community target(s).
     *
     * @param c channel buffer
     * @return target list
     * @throws BgpParseException on decode error
     */
    public static List<BgpValueType> decodeWideCommunityTlv(ChannelBuffer c) throws BgpParseException {
        List<BgpValueType> targetTlv = new ArrayList<>();

        while (c.readableBytes() > 0) {
            if (c.readableBytes() < TYPE_LENGTH_SIZE) {
                Validation.validateLen(BgpErrorType.UPDATE_MESSAGE_ERROR, BgpErrorType.ATTRIBUTE_LENGTH_ERROR,
                                       c.readableBytes());
            }

            byte atomType = c.readByte();
            short atomLength = c.readShort();

            if (c.readableBytes() < atomLength) {
                Validation.validateLen(BgpErrorType.UPDATE_MESSAGE_ERROR, BgpErrorType.ATTRIBUTE_LENGTH_ERROR,
                                       atomLength);
            }

            if (atomType == WideCommunityIpV4Neighbour.TYPE) {
                ChannelBuffer tempBuf = c.readBytes(atomLength);

                WideCommunityIpV4Neighbour wideCommAtom = new WideCommunityIpV4Neighbour();

                while (tempBuf.readableBytes() > 0) {
                    wideCommAtom.add(IpAddress.valueOf(tempBuf.readInt()),
                                     IpAddress.valueOf(tempBuf.readInt()));
                }
                targetTlv.add(wideCommAtom);
            } else if (atomType == WideCommunityInteger.TYPE) {
                ChannelBuffer tempBuf = c.readBytes(atomLength);
                List<Integer> integer = new ArrayList<>();
                while (tempBuf.readableBytes() > 0) {
                    integer.add(tempBuf.readInt());
                }
                targetTlv.add(new WideCommunityInteger(integer));
            } else {
                Validation.validateLen(BgpErrorType.UPDATE_MESSAGE_ERROR, BgpErrorType.MALFORMED_ATTRIBUTE_LIST,
                                       atomLength);
            }
        }
        return targetTlv;
    }

    @Override
    public short getType() {
        return TYPE;
    }

    @Override
    public int compareTo(Object o) {
        // TODO:
        return 0;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .omitNullValues()
                .add("FLAGS", FLAGS)
                .add("wideCommunityHeader", wideCommunityHeader)
                .add("community", community)
                .add("localAsn", localAsn)
                .add("contextAsn", contextAsn)
                .add("target", target)
                .add("excludeTarget", excludeTarget)
                .add("parameter", parameter).toString();
    }
}
