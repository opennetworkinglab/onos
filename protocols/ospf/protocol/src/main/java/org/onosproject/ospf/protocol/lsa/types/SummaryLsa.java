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
package org.onosproject.ospf.protocol.lsa.types;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.primitives.Bytes;
import org.jboss.netty.buffer.ChannelBuffer;
import org.onlab.packet.Ip4Address;
import org.onosproject.ospf.controller.OspfLsaType;
import org.onosproject.ospf.exceptions.OspfErrorType;
import org.onosproject.ospf.exceptions.OspfParseException;
import org.onosproject.ospf.protocol.lsa.LsaHeader;
import org.onosproject.ospf.protocol.util.OspfUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Representation of a Summary LSA, fields and methods to access them.
 */
public class SummaryLsa extends LsaHeader {
    /*
        0                   1                   2                   3
        0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |            LS age             |     Options   |    3 or 4     |
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |                        Link State ID                          |
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |                     Advertising Router                        |
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |                     LS sequence number                        |
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |         LS checksum           |             length            |
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |                         Network Mask                          |
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |      0        |                  metric                       |
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |     TOS       |                TOS  metric                    |
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |                              ...                              |

     */
    private static final Logger log = LoggerFactory.getLogger(SummaryLsa.class);
    private Ip4Address networkMask;
    private int metric;

    /**
     * Creates an instance of Summary LSA.
     *
     * @param lsaHeader LSA header instance
     */
    public SummaryLsa(LsaHeader lsaHeader) {
        populateHeader(lsaHeader);
    }

    /**
     * Gets network mask.
     *
     * @return network mask
     */
    public Ip4Address networkMask() {
        return networkMask;
    }

    /**
     * Sets network mask.
     *
     * @param networkMask network mask
     */
    public void setNetworkMask(Ip4Address networkMask) {
        this.networkMask = networkMask;
    }

    /**
     * Gets metric value.
     *
     * @return metric
     */
    public int metric() {
        return metric;
    }

    /**
     * Sets metric value.
     *
     * @param metric metric value
     */
    public void setMetric(int metric) {
        this.metric = metric;
    }

    /**
     * Reads from channel buffer and populate instance.
     *
     * @param channelBuffer channelBuffer instance
     * @throws OspfParseException might throws exception while parsing buffer
     */
    public void readFrom(ChannelBuffer channelBuffer) throws OspfParseException {

        try {
            byte[] tempByteArray = new byte[OspfUtil.FOUR_BYTES];
            channelBuffer.readBytes(tempByteArray, 0, OspfUtil.FOUR_BYTES);
            this.setNetworkMask(Ip4Address.valueOf(tempByteArray));
            int unsedByte = channelBuffer.readByte();
            this.setMetric(channelBuffer.readUnsignedMedium());
        } catch (Exception e) {
            log.debug("Error::SummaryLsa:: {}", e.getMessage());
            throw new OspfParseException(OspfErrorType.OSPF_MESSAGE_ERROR, OspfErrorType.BAD_MESSAGE);
        }
    }

    /**
     * Returns instance as bytes.
     *
     * @return instance as bytes
     */
    public byte[] asBytes() {
        byte[] lsaMessage = null;
        byte[] lsaHeader = getLsaHeaderAsByteArray();
        byte[] lsaBody = getLsaBodyAsByteArray();
        lsaMessage = Bytes.concat(lsaHeader, lsaBody);

        return lsaMessage;
    }

    /**
     * Get the LSA body.
     *
     * @return LSA body
     */
    public byte[] getLsaBodyAsByteArray() {
        List<Byte> bodyLst = new ArrayList<>();

        try {
            bodyLst.addAll(Bytes.asList(this.networkMask().toOctets()));
            bodyLst.add((byte) 0);
            bodyLst.addAll(Bytes.asList(OspfUtil.convertToThreeBytes(this.metric())));
        } catch (Exception e) {
            log.debug("Error::getLsrBodyAsByteArray {}", e.getMessage());
            return Bytes.toArray(bodyLst);
        }

        return Bytes.toArray(bodyLst);
    }

    @Override
    public OspfLsaType getOspfLsaType() {
        return OspfLsaType.SUMMARY;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .omitNullValues()
                .add("networkMask", networkMask)
                .add("metric", metric)
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        SummaryLsa that = (SummaryLsa) o;
        return Objects.equal(networkMask, that.networkMask) &&
                Objects.equal(metric, that.metric);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(networkMask, metric);
    }
}