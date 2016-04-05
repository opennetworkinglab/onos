/*
 * Copyright 2016 Open Networking Laboratory
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

import io.netty.buffer.ByteBuf;
import org.onosproject.isis.io.util.IsisUtil;

/**
 * Represents TLV finder.
 */
public class TlvFinder extends TlvHeader {

    /**
     * Sets the value for TLV header and the body of the TLV.
     *
     * @param tlvHeader tlvHeader
     * @param byteBuf   byteBuf
     * @return isisTlv ISIS TLV
     */
    public static IsisTlv findTlv(TlvHeader tlvHeader, ByteBuf byteBuf) {

        IsisTlv isisTlv = null;

        switch (tlvHeader.tlvType()) {
            case IsisUtil.AREAADDRESS:
                AreaAddressTlv areaAddressTlv = new AreaAddressTlv(tlvHeader);
                areaAddressTlv.readFrom(byteBuf);
                isisTlv = areaAddressTlv;
                break;
            case IsisUtil.IPINTERFACEADDRESS:
                IpInterfaceAddressTlv ipTlv = new IpInterfaceAddressTlv(tlvHeader);
                ipTlv.readFrom(byteBuf);
                isisTlv = ipTlv;
                break;
            case IsisUtil.PROTOCOLSUPPORTED:
                ProtocolSupportedTlv psTlv = new ProtocolSupportedTlv(tlvHeader);
                psTlv.readFrom(byteBuf);
                isisTlv = psTlv;
                break;
            case IsisUtil.ISNEIGHBORS:
                IsisNeighborTlv isisNeighborTlv = new IsisNeighborTlv(tlvHeader);
                isisNeighborTlv.readFrom(byteBuf);
                isisTlv = isisNeighborTlv;
                break;
            case IsisUtil.PADDING:
                PaddingTlv paddingTlv = new PaddingTlv(tlvHeader);
                paddingTlv.readFrom(byteBuf);
                isisTlv = paddingTlv;
                break;
            default:
                break;
        }

        return isisTlv;
    }
}