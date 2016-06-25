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
import org.onosproject.ospf.protocol.util.OspfUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Defines the Opaque LSA header, fields and the methods to access them.
 */
public class OpaqueLsaHeader extends LsaHeader {
    /*
       0                   1                   2                   3
       0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
      |            LS age             |     Options   |  9, 10, or 11 |
      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
      |  Opaque Type  |               Opaque ID                       |
      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
      |                      Advertising Router                       |
      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
      |                      LS Sequence Number                       |
      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
      |         LS checksum           |           Length              |
      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
      Opaque LSA header format
      REFERENCE : RFC 5250
     */
    private int opaqueId;
    private int opaqueType;

    /**
     * Populates the header from the lsaHeader instance.
     *
     * @param lsaHeader lsa header instance.
     */
    public void populateHeader(OpaqueLsaHeader lsaHeader) {
        //assign all the header values
        this.setAge(lsaHeader.age());
        this.setOptions(lsaHeader.options());
        this.setLsType(lsaHeader.lsType());
        this.setLinkStateId(lsaHeader.linkStateId());
        this.setAdvertisingRouter(lsaHeader.advertisingRouter());
        this.setLsSequenceNo(lsaHeader.lsSequenceNo());
        this.setLsCheckSum(lsaHeader.lsCheckSum());
        this.setLsPacketLen(lsaHeader.lsPacketLen());
        this.setOpaqueId(lsaHeader.opaqueId());
        this.setOpaqueType(lsaHeader.opaqueType());
    }

    /**
     * Gets the opaque id.
     *
     * @return opaque id
     */
    public int opaqueId() {
        return opaqueId;
    }

    /**
     * Sets the opaque id.
     *
     * @param opaqueId opaque id
     */
    public void setOpaqueId(int opaqueId) {
        this.opaqueId = opaqueId;
    }

    /**
     * Gets opaque type.
     *
     * @return opaque type
     */
    public int opaqueType() {
        return opaqueType;
    }

    /**
     * Sets opaque type.
     *
     * @param opaqueType opaque type
     */
    public void setOpaqueType(int opaqueType) {
        this.opaqueType = opaqueType;
    }

    /**
     * Gets header as byte array.
     *
     * @return header as byte array
     */
    public byte[] getOpaqueLsaHeaderAsByteArray() {
        List<Byte> headerLst = new ArrayList<>();
        try {
            headerLst.addAll(Bytes.asList(OspfUtil.convertToTwoBytes(this.age())));
            headerLst.add((byte) this.options());
            headerLst.add((byte) this.lsType());
            headerLst.add((byte) this.opaqueType());
            headerLst.addAll(Bytes.asList(OspfUtil.convertToThreeBytes(this.opaqueId())));
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        OpaqueLsaHeader that = (OpaqueLsaHeader) o;
        return Objects.equal(opaqueId, that.opaqueId) &&
                Objects.equal(opaqueType, that.opaqueType) &&
                Objects.equal(age(), that.age()) &&
                Objects.equal(options(), that.options()) &&
                Objects.equal(lsType(), that.lsType()) &&
                Objects.equal(lsSequenceNo(), that.lsSequenceNo()) &&
                Objects.equal(lsCheckSum(), that.lsCheckSum()) &&
                Objects.equal(lsPacketLen(), that.lsPacketLen()) &&
                Objects.equal(linkStateId(), that.linkStateId()) &&
                Objects.equal(advertisingRouter(), that.advertisingRouter());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(opaqueId, opaqueType);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .omitNullValues()
                .add("opaqueId", this.opaqueId)
                .add("opaqueType", opaqueType)
                .toString();
    }
}