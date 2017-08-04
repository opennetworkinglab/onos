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
package org.onosproject.mapping.codec;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.codec.CodecContext;
import org.onosproject.mapping.addresses.ASMappingAddress;
import org.onosproject.mapping.addresses.DNMappingAddress;
import org.onosproject.mapping.addresses.EthMappingAddress;
import org.onosproject.mapping.addresses.IPMappingAddress;
import org.onosproject.mapping.addresses.MappingAddress;

import java.util.EnumMap;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Encode portion of the mapping address codec.
 */
public final class EncodeMappingAddressCodecHelper {

    private final MappingAddress address;
    private final CodecContext context;

    private final EnumMap<MappingAddress.Type, MappingAddressTypeFormatter> formatMap;

    /**
     * Creates an encoder object for a mapping address.
     * Initializes the formatter lookup map for the mapping address subclasses.
     *
     * @param address MappingAddress to encode
     * @param context context of the JSON encoding
     */
    public EncodeMappingAddressCodecHelper(MappingAddress address, CodecContext context) {
        this.address = address;
        this.context = context;

        formatMap = new EnumMap<>(MappingAddress.Type.class);

        formatMap.put(MappingAddress.Type.IPV4, new FormatIpv4());
        formatMap.put(MappingAddress.Type.IPV6, new FormatIpv6());
        formatMap.put(MappingAddress.Type.AS, new FormatAs());
        formatMap.put(MappingAddress.Type.DN, new FormatDn());
        formatMap.put(MappingAddress.Type.ETH, new FormatEth());

        // TODO: not process extension mapping address for now
        formatMap.put(MappingAddress.Type.EXTENSION, new FormatUnknown());
    }

    /**
     * An interface of mapping address type formatter.
     */
    private interface MappingAddressTypeFormatter {
        ObjectNode encodeMappingAddress(ObjectNode root, MappingAddress address);
    }

    /**
     * Implementation of IPv4 mapping address type formatter.
     */
    private static class FormatIpv4 implements MappingAddressTypeFormatter {

        @Override
        public ObjectNode encodeMappingAddress(ObjectNode root, MappingAddress address) {
            final IPMappingAddress ipv4 = (IPMappingAddress) address;
            return root.put(MappingAddressCodec.IPV4, ipv4.ip().toString());
        }
    }

    /**
     * Implementation of IPv6 mapping address type formatter.
     */
    private static class FormatIpv6 implements MappingAddressTypeFormatter {

        @Override
        public ObjectNode encodeMappingAddress(ObjectNode root, MappingAddress address) {
            final IPMappingAddress ipv6 = (IPMappingAddress) address;
            return root.put(MappingAddressCodec.IPV6, ipv6.ip().toString());
        }
    }

    /**
     * Implementation of AS mapping address type formatter.
     */
    private static class FormatAs implements MappingAddressTypeFormatter {

        @Override
        public ObjectNode encodeMappingAddress(ObjectNode root, MappingAddress address) {
            final ASMappingAddress as = (ASMappingAddress) address;
            return root.put(MappingAddressCodec.AS, as.asNumber());
        }
    }

    /**
     * Implementation of Distinguished Name mapping address type formatter.
     */
    private static class FormatDn implements MappingAddressTypeFormatter {

        @Override
        public ObjectNode encodeMappingAddress(ObjectNode root, MappingAddress address) {
            final DNMappingAddress dn = (DNMappingAddress) address;
            return root.put(MappingAddressCodec.DN, dn.name());
        }
    }

    /**
     * Implementation of Ethernet mapping address type formatter.
     */
    private static class FormatEth implements MappingAddressTypeFormatter {

        @Override
        public ObjectNode encodeMappingAddress(ObjectNode root, MappingAddress address) {
            final EthMappingAddress eth = (EthMappingAddress) address;
            return root.put(MappingAddressCodec.MAC, eth.mac().toString());
        }
    }

    /**
     * Implementation of Extension mapping address type formatter.
     */
    private static class FormatExtension implements MappingAddressTypeFormatter {

        @Override
        public ObjectNode encodeMappingAddress(ObjectNode root, MappingAddress address) {
            return null;
        }
    }

    /**
     * Implementation of Unknown mapping address type formatter.
     */
    private static class FormatUnknown implements MappingAddressTypeFormatter {

        @Override
        public ObjectNode encodeMappingAddress(ObjectNode root, MappingAddress address) {
            return root;
        }
    }

    /**
     * Encodes a mapping address into a JSON node.
     *
     * @return encoded JSON object for the given mapping address
     */
    public ObjectNode encode() {
        final ObjectNode result = context.mapper().createObjectNode()
                    .put(MappingAddressCodec.TYPE, address.type().toString());

        MappingAddressTypeFormatter formatter =
                checkNotNull(formatMap.get(address.type()),
                        "No formatter found for mapping address type "
                        + address.type().toString());

        return formatter.encodeMappingAddress(result, address);
    }
}
