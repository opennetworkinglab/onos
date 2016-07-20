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
package org.onosproject.isis.io.isispacket.tlv;

import com.google.common.base.MoreObjects;
import org.jboss.netty.buffer.ChannelBuffer;

/**
 * Representation of IDRP information TLV.
 */
public class IdrpInformationTlv extends TlvHeader implements IsisTlv {

    private byte irdpInformationType;
    private int externalInformation;

    /**
     * Creates an instance of IDRP information TLV.
     *
     * @param tlvHeader TLV header
     */
    public IdrpInformationTlv(TlvHeader tlvHeader) {
        this.setTlvType(tlvHeader.tlvType());
        this.setTlvLength(tlvHeader.tlvLength());
    }

    /**
     * Returns the external information of IDRP information TLV.
     *
     * @return external information
     */
    public int externalInformation() {
        return externalInformation;
    }

    /**
     * Sets the external information for IDRP information TLV.
     *
     * @param externalInformation external information
     */
    public void setExternalInformation(int externalInformation) {
        this.externalInformation = externalInformation;
    }

    /**
     * Returns the IDRP information of IDRP information TLV.
     *
     * @return IDRP information type
     */
    public byte irdpInformationType() {
        return irdpInformationType;
    }

    /**
     * Sets the IDRP information for IDRP information TLV.
     *
     * @param irdpInformationType IDRP information type
     */
    public void setIrdpInformationType(byte irdpInformationType) {
        this.irdpInformationType = irdpInformationType;
    }

    @Override
    public void readFrom(ChannelBuffer channelBuffer) {
        //TODO
    }


    @Override
    public byte[] asBytes() {
        //TODO
        return null;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .omitNullValues()
                .add("externalInformation", externalInformation)
                .add("irdpInformationType", irdpInformationType)
                .toString();
    }
}