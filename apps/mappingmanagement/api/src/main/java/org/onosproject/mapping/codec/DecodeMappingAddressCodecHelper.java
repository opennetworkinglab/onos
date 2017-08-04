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
import com.google.common.collect.Maps;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onosproject.mapping.addresses.MappingAddress;
import org.onosproject.mapping.addresses.MappingAddresses;

import java.util.Map;

import static org.onlab.util.Tools.nullIsIllegal;

/**
 * Decode portion of the mapping address codec.
 */
public final class DecodeMappingAddressCodecHelper {

    private final ObjectNode json;

    protected static final String MISSING_MEMBER_MESSAGE =
                                  " member is required in Mapping Address";

    private final Map<String, MappingAddressDecoder> decoderMap;

    /**
     * Creates a decode mapping address codec object.
     * Initializes the lookup map for mapping address subclass decoders.
     *
     * @param json JSON object to decode
     */
    public DecodeMappingAddressCodecHelper(ObjectNode json) {
       this.json = json;
       decoderMap = Maps.newHashMap();

       decoderMap.put(MappingAddress.Type.IPV4.name(), new Ipv4TypeDecoder());
       decoderMap.put(MappingAddress.Type.IPV6.name(), new Ipv6TypeDecoder());
       decoderMap.put(MappingAddress.Type.AS.name(), new AsTypeDecoder());
       decoderMap.put(MappingAddress.Type.DN.name(), new DnTypeDecoder());
       decoderMap.put(MappingAddress.Type.ETH.name(), new EthTypeDecoder());
    }

    /**
     * An interface of mapping address type decoder.
     */
    private interface MappingAddressDecoder {
        MappingAddress decodeMappingAddress(ObjectNode json);
    }

    /**
     * Implementation of IPv4 mapping address decoder.
     */
    private class Ipv4TypeDecoder implements MappingAddressDecoder {

        @Override
        public MappingAddress decodeMappingAddress(ObjectNode json) {
            String ip = nullIsIllegal(json.get(MappingAddressCodec.IPV4),
                    MappingAddressCodec.IPV4 + MISSING_MEMBER_MESSAGE).asText();
            return MappingAddresses.ipv4MappingAddress(IpPrefix.valueOf(ip));
        }
    }

    /**
     * Implementation of IPv6 mapping address decoder.
     */
    private class Ipv6TypeDecoder implements MappingAddressDecoder {

        @Override
        public MappingAddress decodeMappingAddress(ObjectNode json) {
            String ip = nullIsIllegal(json.get(MappingAddressCodec.IPV6),
                    MappingAddressCodec.IPV6 + MISSING_MEMBER_MESSAGE).asText();
            return MappingAddresses.ipv6MappingAddress(IpPrefix.valueOf(ip));
        }
    }

    /**
     * Implementation of AS mapping address decoder.
     */
    private class AsTypeDecoder implements MappingAddressDecoder {

        @Override
        public MappingAddress decodeMappingAddress(ObjectNode json) {
            String as = nullIsIllegal(json.get(MappingAddressCodec.AS),
                    MappingAddressCodec.AS + MISSING_MEMBER_MESSAGE).asText();
            return MappingAddresses.asMappingAddress(as);
        }
    }

    /**
     * Implementation of DN mapping address decoder.
     */
    private class DnTypeDecoder implements MappingAddressDecoder {

        @Override
        public MappingAddress decodeMappingAddress(ObjectNode json) {
            String dn = nullIsIllegal(json.get(MappingAddressCodec.DN),
                    MappingAddressCodec.DN + MISSING_MEMBER_MESSAGE).asText();
            return MappingAddresses.dnMappingAddress(dn);
        }
    }

    /**
     * Implementation of Ethernet mapping address decoder.
     */
    private class EthTypeDecoder implements MappingAddressDecoder {

        @Override
        public MappingAddress decodeMappingAddress(ObjectNode json) {
            MacAddress mac = MacAddress.valueOf(nullIsIllegal(json.get(MappingAddressCodec.MAC),
                    MappingAddressCodec.MAC + MISSING_MEMBER_MESSAGE).asText());
            return MappingAddresses.ethMappingAddress(mac);
        }
    }

    /**
     * Decodes the JSON into a mapping address object.
     *
     * @return MappingAddress object
     * @throws IllegalArgumentException if the JSON is invalid
     */
    public MappingAddress decode() {
        String type =
                nullIsIllegal(json.get(MappingAddressCodec.TYPE),
                                    "Type not specified").asText();

        MappingAddressDecoder decoder = decoderMap.get(type);
        if (decoder != null) {
            return decoder.decodeMappingAddress(json);
        }

        throw new IllegalArgumentException("Type " + type + " is unknown");
    }
}
