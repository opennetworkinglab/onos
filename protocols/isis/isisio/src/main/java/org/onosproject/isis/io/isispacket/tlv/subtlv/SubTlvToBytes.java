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
package org.onosproject.isis.io.isispacket.tlv.subtlv;

import com.google.common.primitives.Bytes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Representation of conversion of TLV's to bytes.
 */
public final class SubTlvToBytes {

    protected static final Logger log = LoggerFactory.getLogger(SubTlvToBytes.class);

    /**
     * Creates an instance.
     */
    private SubTlvToBytes() {
        //private constructor
    }

    /**
     * Sets the ISIS sub TLV and returns in the form of bytes.
     *
     * @param subTlv isisTlv.
     * @return subTlvBytes
     */
    public static List<Byte> tlvToBytes(TrafficEngineeringSubTlv subTlv) {

        List<Byte> subTlvBytes = new ArrayList<>();
        if (subTlv instanceof AdministrativeGroup) {
            AdministrativeGroup administrativeGroup = (AdministrativeGroup) subTlv;
            subTlvBytes.addAll(Bytes.asList(administrativeGroup.asBytes()));
        } else if (subTlv instanceof MaximumBandwidth) {
            MaximumBandwidth maximumBandwidth = (MaximumBandwidth) subTlv;
            subTlvBytes.addAll(Bytes.asList(maximumBandwidth.asBytes()));
        } else if (subTlv instanceof MaximumReservableBandwidth) {
            MaximumReservableBandwidth maximumReservableBandwidth = (MaximumReservableBandwidth) subTlv;
            subTlvBytes.addAll(Bytes.asList(maximumReservableBandwidth.asBytes()));
        } else if (subTlv instanceof TrafficEngineeringMetric) {
            TrafficEngineeringMetric trafficEngineeringMetric = (TrafficEngineeringMetric) subTlv;
            subTlvBytes.addAll(Bytes.asList(trafficEngineeringMetric.asBytes()));
        } else if (subTlv instanceof UnreservedBandwidth) {
            UnreservedBandwidth unreservedBandwidth = (UnreservedBandwidth) subTlv;
            subTlvBytes.addAll(Bytes.asList(unreservedBandwidth.asBytes()));
        } else if (subTlv instanceof NeighborIpAddress) {
            NeighborIpAddress interfaceIpAddress = (NeighborIpAddress) subTlv;
            subTlvBytes.addAll(Bytes.asList(interfaceIpAddress.asBytes()));
        } else {
            log.debug("TlvsToBytes::UNKNOWN TLV TYPE ::TlvsToBytes ");
        }

        return subTlvBytes;
    }
}
