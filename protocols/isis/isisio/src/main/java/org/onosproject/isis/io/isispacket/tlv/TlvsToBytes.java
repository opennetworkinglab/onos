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

import com.google.common.primitives.Bytes;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents conversion of TLV's to bytes.
 */
public final class TlvsToBytes {
    /**
     * Sets the ISIS TLV and returns in the form of bytes.
     *
     * @param isisTlv isisTlv
     * @return tlvBytes TLV bytes
     */
    public static List<Byte> tlvToBytes(IsisTlv isisTlv) {

        List<Byte> tlvBytes = new ArrayList();
        if (isisTlv instanceof AreaAddressTlv) {
            AreaAddressTlv areaAddressTlv = (AreaAddressTlv) isisTlv;
            tlvBytes.addAll(Bytes.asList(areaAddressTlv.asBytes()));
        } else if (isisTlv instanceof IpInterfaceAddressTlv) {
            IpInterfaceAddressTlv ipInterfaceAddressTlv = (IpInterfaceAddressTlv) isisTlv;
            tlvBytes.addAll(Bytes.asList(ipInterfaceAddressTlv.asBytes()));
        } else if (isisTlv instanceof ProtocolSupportedTlv) {
            ProtocolSupportedTlv protocolSupportedTlv = (ProtocolSupportedTlv) isisTlv;
            tlvBytes.addAll(Bytes.asList(protocolSupportedTlv.asBytes()));
        } else if (isisTlv instanceof PaddingTlv) {
            PaddingTlv paddingTlv = (PaddingTlv) isisTlv;
            tlvBytes.addAll(Bytes.asList(paddingTlv.asBytes()));
        } else if (isisTlv instanceof IsisNeighborTlv) {
            IsisNeighborTlv isisNeighborTlv = (IsisNeighborTlv) isisTlv;
            tlvBytes.addAll(Bytes.asList(isisNeighborTlv.asBytes()));
        } else {
            System.out.println("UNKNOWN TLV TYPE ::TlvsToBytes ");
        }

        return tlvBytes;
    }
    /**
     * Creates an instance.
     */
    private TlvsToBytes() {
        //private constructor
    }
}