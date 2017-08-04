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

import com.fasterxml.jackson.databind.JsonNode;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.onosproject.mapping.addresses.ASMappingAddress;
import org.onosproject.mapping.addresses.DNMappingAddress;
import org.onosproject.mapping.addresses.EthMappingAddress;
import org.onosproject.mapping.addresses.IPMappingAddress;
import org.onosproject.mapping.addresses.MappingAddress;

/**
 * Hamcrest matcher for mapping objects.
 */
public final class MappingAddressJsonMatcher extends
                                            TypeSafeDiagnosingMatcher<JsonNode> {

    private final MappingAddress address;
    private Description description;
    private JsonNode node;

    /**
     * Constructs a matcher object.
     *
     * @param address mapping address to match
     */
    private MappingAddressJsonMatcher(MappingAddress address) {
        this.address = address;
    }

    /**
     * Factory to allocate a mapping address matcher.
     *
     * @param address mapping address object we are looking for
     * @return matcher
     */
    public static MappingAddressJsonMatcher matchesMappingAddress(MappingAddress address) {
        return new MappingAddressJsonMatcher(address);
    }

    /**
     * Matches an AS mapping address object.
     *
     * @param address mapping address to match
     * @return true if the JSON matches the mapping address, false otherwise
     */
    private boolean matchMappingAddress(ASMappingAddress address) {
        final String as = address.asNumber();
        final String jsonAs = node.get(MappingAddressCodec.AS).textValue();
        if (!as.equals(jsonAs)) {
            description.appendText("AS was " + jsonAs);
            return false;
        }
        return true;
    }

    /**
     * Matches a Distinguished Name mapping address object.
     *
     * @param address mapping address to match
     * @return true if the JSON matches the mapping address, false otherwise
     */
    private boolean matchMappingAddress(DNMappingAddress address) {
        final String dn = address.name();
        final String jsonDn = node.get(MappingAddressCodec.DN).textValue();
        if (!dn.equals(jsonDn)) {
            description.appendText("Distinguished Name was " + jsonDn);
            return false;
        }
        return true;
    }

    /**
     * Matches an IP mapping address object.
     *
     * @param address mapping address to match
     * @return true if the JSON matches the mapping address, false otherwise
     */
    private boolean matchMappingAddress(IPMappingAddress address) {
        final String ip = address.ip().toString();
        String jsonIp = null;
        if (address.type() == MappingAddress.Type.IPV4) {
            jsonIp = node.get(MappingAddressCodec.IPV4).textValue();

        } else if (address.type() == MappingAddress.Type.IPV6) {
            jsonIp = node.get(MappingAddressCodec.IPV6).textValue();
        }
        if (!ip.equals(jsonIp)) {
            description.appendText("IP was " + jsonIp);
            return false;
        }
        return true;
    }

    /**
     * Matches a MAC mapping address object.
     *
     * @param address mapping address to match
     * @return true if the JSON matches the mapping address, false otherwise
     */
    private boolean matchMappingAddress(EthMappingAddress address) {
        final String mac = address.mac().toString();
        final String jsonMac = node.get(MappingAddressCodec.MAC).textValue();
        if (!mac.equals(jsonMac)) {
            description.appendText("MAC was " + jsonMac);
            return false;
        }
        return true;
    }

    @Override
    protected boolean matchesSafely(JsonNode jsonNode, Description description) {

        this.description = description;
        this.node = jsonNode;
        final String type = address.type().name();
        final String jsonType = jsonNode.get(MappingAddressCodec.TYPE).asText();
        if (!type.equals(jsonType)) {
            description.appendText("type was " + type);
            return false;
        }

        switch (address.type()) {

            case IPV4:
            case IPV6:
                return matchMappingAddress((IPMappingAddress) address);

            case AS:
                return matchMappingAddress((ASMappingAddress) address);

            case DN:
                return matchMappingAddress((DNMappingAddress) address);

            case ETH:
                return matchMappingAddress((EthMappingAddress) address);

            default:
                // Don't know how to format this type
                description.appendText("unknown mapping address type " + address.type());
                return false;
        }
    }

    @Override
    public void describeTo(Description description) {
        description.appendText(address.toString());
    }
}
