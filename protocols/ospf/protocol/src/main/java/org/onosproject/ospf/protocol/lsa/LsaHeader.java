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

package org.onosproject.ospf.protocol.lsa;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.primitives.Bytes;
import org.onlab.packet.Ip4Address;
import org.onosproject.ospf.controller.OspfLsa;
import org.onosproject.ospf.controller.OspfLsaType;
import org.onosproject.ospf.protocol.util.OspfUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * Defines the LSA header, fields and the methods to access them.
 */
public class LsaHeader implements OspfLsa {

    /*
        0                   1                   2                   3
        0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |            LS age             |    Options    |    LS type    |
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |                        Link State ID                          |
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |                     Advertising Router                        |
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |                     LS sequence number                        |
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       |         LS checksum           |             length            |
       +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
       LSA header format
       REFERENCE : RFC 2328
     */
    protected static final Logger log = LoggerFactory.getLogger(LsaHeader.class);
    private int age;
    private int options;
    private int lsType;
    private long lsSequenceNo;
    private int lsCheckSum;
    private int lsPacketLen;
    private String linkStateId;
    private Ip4Address advertisingRouter;

    /**
     * Gets LSA age.
     *
     * @return LSA age
     */
    public int age() {
        return age;
    }

    /**
     * Sets LSA age.
     *
     * @param age LSA age
     */
    public void setAge(int age) {
        this.age = age;
    }

    /**
     * Gets options value.
     *
     * @return options header value
     */
    public int options() {
        return options;
    }

    /**
     * Sets options header value.
     *
     * @param options header value
     */
    public void setOptions(int options) {
        this.options = options;
    }

    /**
     * Gets LSA type.
     *
     * @return LSA type
     */
    public int lsType() {
        return lsType;
    }

    /**
     * Sets LSA type.
     *
     * @param lsType LSA type
     */
    public void setLsType(int lsType) {
        this.lsType = lsType;
    }

    /**
     * Gets link state id.
     *
     * @return linkStateId link state id
     */
    public String linkStateId() {
        return linkStateId;
    }

    /**
     * Sets link state id.
     *
     * @param linkStateId link state id
     */
    public void setLinkStateId(String linkStateId) {
        this.linkStateId = linkStateId;
    }

    /**
     * Gets advertising router IP.
     *
     * @return advertising router
     */
    public Ip4Address advertisingRouter() {
        return advertisingRouter;
    }

    /**
     * Sets advertising router.
     *
     * @param advertisingRouter advertising router
     */
    public void setAdvertisingRouter(Ip4Address advertisingRouter) {
        this.advertisingRouter = advertisingRouter;
    }

    /**
     * Gets LSA sequence number.
     *
     * @return LSA sequence number
     */
    public long lsSequenceNo() {
        return lsSequenceNo;
    }

    /**
     * Sets LSA sequence number.
     *
     * @param lsSequenceNo LSA sequence number
     */
    public void setLsSequenceNo(long lsSequenceNo) {
        this.lsSequenceNo = lsSequenceNo;
    }

    /**
     * Gets LSA check sum.
     *
     * @return lsCheckSum LSA checksum
     */
    public int lsCheckSum() {
        return lsCheckSum;
    }

    /**
     * Sets LSA checksum.
     *
     * @param lsCheckSum LSA checksum
     */
    public void setLsCheckSum(int lsCheckSum) {
        this.lsCheckSum = lsCheckSum;
    }

    /**
     * Gets lsa packet length.
     *
     * @return lsPacketLen LSA packet length
     */
    public int lsPacketLen() {
        return lsPacketLen;
    }

    /**
     * Sets LSA packet length.
     *
     * @param lsPacketLen LSA packet length
     */
    public void setLsPacketLen(int lsPacketLen) {
        this.lsPacketLen = lsPacketLen;
    }

    @Override
    public OspfLsaType getOspfLsaType() {
        if (lsType == OspfLsaType.ROUTER.value()) {
            return OspfLsaType.ROUTER;
        } else if (lsType == OspfLsaType.NETWORK.value()) {
            return OspfLsaType.NETWORK;
        } else if (lsType == OspfLsaType.SUMMARY.value()) {
            return OspfLsaType.SUMMARY;
        } else if (lsType == OspfLsaType.ASBR_SUMMARY.value()) {
            return OspfLsaType.ASBR_SUMMARY;
        } else if (lsType == OspfLsaType.EXTERNAL_LSA.value()) {
            return OspfLsaType.EXTERNAL_LSA;
        } else if (lsType == OspfLsaType.LINK_LOCAL_OPAQUE_LSA.value()) {
            return OspfLsaType.LINK_LOCAL_OPAQUE_LSA;
        } else if (lsType == OspfLsaType.AREA_LOCAL_OPAQUE_LSA.value()) {
            return OspfLsaType.AREA_LOCAL_OPAQUE_LSA;
        } else if (lsType == OspfLsaType.AS_OPAQUE_LSA.value()) {
            return OspfLsaType.AS_OPAQUE_LSA;
        }

        return OspfLsaType.UNDEFINED;
    }

    @Override
    public OspfLsa lsaHeader() {
        return this;
    }

    /**
     * Gets the LSA header as bytes.
     *
     * @return LSA header as bytes
     */
    public byte[] getLsaHeaderAsByteArray() {
        List<Byte> headerLst = new ArrayList<>();
        try {
            headerLst.addAll(Bytes.asList(OspfUtil.convertToTwoBytes(this.age())));
            headerLst.add((byte) this.options());
            headerLst.add((byte) this.lsType());
            headerLst.addAll(Bytes.asList(InetAddress.getByName(this.linkStateId()).getAddress()));
            headerLst.addAll(Bytes.asList(this.advertisingRouter().toOctets()));
            headerLst.addAll(Bytes.asList(OspfUtil.convertToFourBytes(this.lsSequenceNo())));
            headerLst.addAll(Bytes.asList(OspfUtil.convertToTwoBytes(this.lsCheckSum())));
            headerLst.addAll(Bytes.asList(OspfUtil.convertToTwoBytes(this.lsPacketLen())));
        } catch (Exception e) {
            log.debug("Error::getLsaHeaderAsByteArray {}", e.getMessage());
            return Bytes.toArray(headerLst);
        }
        return Bytes.toArray(headerLst);
    }

    /**
     * Populates the header from the LSA header instance.
     *
     * @param lsaHeader LSA header instance
     */
    public void populateHeader(LsaHeader lsaHeader) {
        //assign all the header values
        this.setAge(lsaHeader.age());
        this.setOptions(lsaHeader.options());
        this.setLsType(lsaHeader.lsType());
        this.setLinkStateId(lsaHeader.linkStateId());
        this.setAdvertisingRouter(lsaHeader.advertisingRouter());
        this.setLsSequenceNo(lsaHeader.lsSequenceNo());
        this.setLsCheckSum(lsaHeader.lsCheckSum());
        this.setLsPacketLen(lsaHeader.lsPacketLen());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        LsaHeader that = (LsaHeader) o;
        return Objects.equal(age, that.age) &&
                Objects.equal(options, that.options) &&
                Objects.equal(lsType, that.lsType) &&
                Objects.equal(lsSequenceNo, that.lsSequenceNo) &&
                Objects.equal(lsCheckSum, that.lsCheckSum) &&
                Objects.equal(lsPacketLen, that.lsPacketLen) &&
                Objects.equal(linkStateId, that.linkStateId) &&
                Objects.equal(advertisingRouter, that.advertisingRouter);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(age, options, lsType, lsSequenceNo, lsCheckSum,
                                lsPacketLen, linkStateId, advertisingRouter);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .omitNullValues()
                .add("age", age)
                .add("options", options)
                .add("lsType", lsType)
                .add("lsSequenceNo", lsSequenceNo)
                .add("lsCheckSum", lsCheckSum)
                .add("lsPacketLen", lsPacketLen)
                .add("linkStateId;", linkStateId)
                .add("advertisingRouter", advertisingRouter)
                .toString();
    }
}