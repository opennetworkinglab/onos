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
package org.onosproject.bgpio.types;

import com.google.common.base.MoreObjects;
import org.jboss.netty.buffer.ChannelBuffer;
import org.onosproject.bgpio.exceptions.BgpParseException;
import org.onosproject.bgpio.util.Constants;
import org.onosproject.bgpio.util.Validation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Provides Implementation of As4Path BGP Path Attribute.
 */
public class As4Path implements BgpValueType {
    private static final Logger log = LoggerFactory.getLogger(AsPath.class);
    public static final byte AS4PATH_TYPE = 17;
    public static final byte ASNUM_SIZE = 4;
    public static final byte FLAGS = (byte) 0x40;

    private List<Integer> as4pathSet;
    private List<Integer> as4pathSeq;

    /**
     * Initialize fields.
     */
    public As4Path() {
        this.as4pathSeq = null;
        this.as4pathSet = null;
    }

    /**
     * Constructor to initialize parameters.
     *
     * @param as4pathSet AS4path Set
     * @param as4pathSeq AS4path Sequence
     */
    public As4Path(List<Integer> as4pathSet, List<Integer> as4pathSeq) {
        this.as4pathSeq = as4pathSeq;
        this.as4pathSet = as4pathSet;
    }

    /**
     * Reads from the channel buffer and parses As4Path.
     *
     * @param cb ChannelBuffer
     * @return object of As4Path
     * @throws BgpParseException while parsing As4Path
     */
    public static As4Path read(ChannelBuffer cb) throws BgpParseException {
        List<Integer> as4pathSet = new ArrayList<>();
        List<Integer> as4pathSeq = new ArrayList<>();
        ChannelBuffer tempCb = cb.copy();
        Validation validation = Validation.parseAttributeHeader(cb);

        if (cb.readableBytes() < validation.getLength()) {
            Validation.validateLen(BgpErrorType.UPDATE_MESSAGE_ERROR, BgpErrorType.ATTRIBUTE_LENGTH_ERROR,
                    validation.getLength());
        }
        //if fourth bit is set length is read as short otherwise as byte , len includes type, length and value
        int len = validation.isShort() ? validation.getLength() + Constants.TYPE_AND_LEN_AS_SHORT : validation
                .getLength() + Constants.TYPE_AND_LEN_AS_BYTE;
        ChannelBuffer data = tempCb.readBytes(len);
        if (validation.getFirstBit() && !validation.getSecondBit() && validation.getThirdBit()) {
            throw new BgpParseException(BgpErrorType.UPDATE_MESSAGE_ERROR, BgpErrorType.ATTRIBUTE_FLAGS_ERROR, data);
        }

        ChannelBuffer tempBuf = cb.readBytes(validation.getLength());
        while (tempBuf.readableBytes() > 0) {
            byte pathSegType = tempBuf.readByte();
            //no of ASes
            byte pathSegLen = tempBuf.readByte();
            //length = no of Ases * ASnum size (4 bytes)
            int length = pathSegLen * ASNUM_SIZE;
            if (tempBuf.readableBytes() < length) {
                Validation.validateLen(BgpErrorType.UPDATE_MESSAGE_ERROR,
                        BgpErrorType.ATTRIBUTE_LENGTH_ERROR, length);
            }
            ChannelBuffer aspathBuf = tempBuf.readBytes(length);
            while (aspathBuf.readableBytes() > 0) {
                int asNum;
                asNum = aspathBuf.readInt();
                switch (pathSegType) {
                case AsPath.ASPATH_SET_TYPE:
                    as4pathSet.add(asNum);
                    break;
                case AsPath.ASPATH_SEQ_TYPE:
                    as4pathSeq.add(asNum);
                    break;
                default: log.debug("Other type Not Supported:" + pathSegType);
                }
            }
        }
        return new As4Path(as4pathSet, as4pathSeq);
    }

    @Override
    public short getType() {
        return AS4PATH_TYPE;
    }

    /**
     * Returns list of ASNum in AS4path Sequence.
     *
     * @return list of ASNum in AS4path Sequence
     */
    public List<Integer> as4PathSeq() {
        return this.as4pathSeq;
    }

    /**
     * Returns list of ASNum in AS4path Set.
     *
     * @return list of ASNum in AS4path Set
     */
    public List<Integer> as4PathSet() {
        return this.as4pathSet;
    }

    @Override
    public int hashCode() {
        return Objects.hash(as4pathSet, as4pathSeq);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof As4Path) {
            As4Path other = (As4Path) obj;
            return Objects.equals(as4pathSet, other.as4pathSet) && Objects.equals(as4pathSeq, other.as4pathSeq);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .omitNullValues()
                .add("as4pathSet", as4pathSet)
                .add("as4pathSeq", as4pathSeq)
                .toString();
    }

    @Override
    public int write(ChannelBuffer cb) {

        int iLenStartIndex = cb.writerIndex();

        cb.writeByte(FLAGS);
        cb.writeByte(getType());
        if ((as4pathSet != null) && (as4pathSeq != null)) {
            int iAsLenIndex = cb.writerIndex();
            cb.writeByte(0);
            if (!as4pathSeq.isEmpty()) {
                cb.writeByte(AsPath.ASPATH_SEQ_TYPE);
                cb.writeByte(as4pathSeq.size());

                for (int j = 0; j < as4pathSeq.size(); j++) {
                    cb.writeInt(as4pathSeq.get(j));
                }

                int asLen = cb.writerIndex() - iAsLenIndex;
                cb.setByte(iAsLenIndex, (byte) (asLen - 1));
            }
        } else {
            cb.writeByte(0);
        }
        return cb.writerIndex() - iLenStartIndex;
    }

    @Override
    public int compareTo(Object o) {
        // TODO Auto-generated method stub
        return 0;
    }
}
