/*
 * Copyright 2017-present Open Networking Foundation
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
package org.onosproject.cfm.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip6Address;
import org.onlab.packet.IpAddress;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.incubator.net.l2monitoring.cfm.Mep;
import org.onosproject.incubator.net.l2monitoring.cfm.Mep.FngAddress;

import java.util.Locale;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onlab.util.Tools.nullIsIllegal;

/**
 * Encode and decode to/from JSON to FngAddress object.
 */
public class FngAddressCodec extends JsonCodec<FngAddress> {

    /**
     * Encodes the FngAddress entity into JSON.
     *
     * @param fngAddress  FngAddress to encode
     * @param context encoding context
     * @return JSON node
     * @throws java.lang.UnsupportedOperationException if the codec does not
     *                                                 support encode operations
     */
    @Override
    public ObjectNode encode(FngAddress fngAddress, CodecContext context) {
        checkNotNull(fngAddress, "FngAddress cannot be null");
        ObjectNode result = context.mapper().createObjectNode()
                .put("address-type", fngAddress.addressType().name());

        if (fngAddress.addressType().equals(Mep.FngAddressType.IPV4) ||
                fngAddress.addressType().equals(Mep.FngAddressType.IPV6)) {
            result.put("ip-address", fngAddress.ipAddress().toString());
        }

        return result;
    }

    /**
     * Decodes the FngAddress entity from JSON.
     *
     * @param json    JSON to decode
     * @param context decoding context
     * @return decoded FngAddress
     * @throws java.lang.UnsupportedOperationException if the codec does not
     *                                                 support decode operations
     */
    @Override
    public FngAddress decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        JsonNode node = json.get("fng-address");

        String addressType = nullIsIllegal(node.get("address-type"),
                            "address type is required").asText();
        Mep.FngAddressType type = Mep.FngAddressType.valueOf(addressType.toUpperCase(Locale.ENGLISH));
        JsonNode ipAddressNode = node.get("ipAddress");

        switch (type) {
            case IPV4:
                return FngAddress.ipV4Address(Ip4Address.valueOf(ipAddressNode.asText()));
            case IPV6:
                return FngAddress.ipV6Address(Ip6Address.valueOf(ipAddressNode.asText()));
            case NOT_TRANSMITTED:
                return FngAddress.notTransmitted(IpAddress.valueOf(ipAddressNode.asText()));
            case NOT_SPECIFIED:
            default:
                return FngAddress.notSpecified();
        }
    }
}
