/*
 * Copyright 2017-present Open Networking Laboratory
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
package org.onosproject.drivers.lisp.extensions.codec;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.drivers.lisp.extensions.LispNatAddress;
import org.onosproject.mapping.addresses.MappingAddress;

import java.util.List;
import java.util.stream.IntStream;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * LISP NAT address codec.
 */
public final class LispNatAddressCodec extends JsonCodec<LispNatAddress> {

    protected static final String MS_UDP_PORT_NUMBER = "msUdpPortNumber";
    protected static final String ETR_UDP_PORT_NUMBER = "etrUdpPortNumber";
    protected static final String GLOBAL_ETR_RLOC_ADDRESS = "globalEtrRlocAddress";
    protected static final String MS_RLOC_ADDRESS = "msRlocAddress";
    protected static final String PRIVATE_ETR_RLOC_ADDRESS = "privateEtrRlocAddress";
    protected static final String RTR_RLOC_ADDRESSES = "rtrRlocAddresses";

    private static final String MISSING_MEMBER_MESSAGE =
                                " member is required in LispListAddress";

    @Override
    public ObjectNode encode(LispNatAddress address, CodecContext context) {
        checkNotNull(address, "LispListAddress cannot be null");

        final ObjectNode result = context.mapper().createObjectNode()
                .put(MS_UDP_PORT_NUMBER, address.getMsUdpPortNumber())
                .put(ETR_UDP_PORT_NUMBER, address.getEtrUdpPortNumber());

        final JsonCodec<MappingAddress> addressCodec =
                context.codec(MappingAddress.class);

        if (address.getGlobalEtrRlocAddress() != null) {
            ObjectNode globalEtrRlocNode =
                    addressCodec.encode(address.getGlobalEtrRlocAddress(), context);
            result.set(GLOBAL_ETR_RLOC_ADDRESS, globalEtrRlocNode);
        }

        if (address.getMsRlocAddress() != null) {
            ObjectNode msRlocNode =
                    addressCodec.encode(address.getMsRlocAddress(), context);
            result.set(MS_RLOC_ADDRESS, msRlocNode);
        }

        if (address.getPrivateEtrRlocAddress() != null) {
            ObjectNode privateEtrRlocNode =
                    addressCodec.encode(address.getPrivateEtrRlocAddress(), context);
            result.set(PRIVATE_ETR_RLOC_ADDRESS, privateEtrRlocNode);
        }

        final ArrayNode jsonRtrRlocNodes = result.putArray(RTR_RLOC_ADDRESSES);

        if (address.getRtrRlocAddresses() != null) {
            for (final MappingAddress mappingAddress : address.getRtrRlocAddresses()) {
                jsonRtrRlocNodes.add(addressCodec.encode(mappingAddress, context));
            }
        }

        return result;
    }

    @Override
    public LispNatAddress decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        final JsonCodec<MappingAddress> addressCodec =
                context.codec(MappingAddress.class);

        short msUdpPortNumber = (short) json.get(MS_UDP_PORT_NUMBER).asInt();
        short etrUdpPortNumber = (short) json.get(ETR_UDP_PORT_NUMBER).asInt();

        ObjectNode globalEtrRlocJson = get(json, GLOBAL_ETR_RLOC_ADDRESS);
        ObjectNode msRlocJson = get(json, MS_RLOC_ADDRESS);
        ObjectNode privateEtrRlocJson = get(json, PRIVATE_ETR_RLOC_ADDRESS);
        JsonNode rtrRlocJson = json.get(RTR_RLOC_ADDRESSES);
        MappingAddress globalEtrRlocAddress = null;
        MappingAddress msRlocAddress = null;
        MappingAddress privateEtrRlocAddress = null;
        List<MappingAddress> rtrRlocAddresses = Lists.newArrayList();

        if (globalEtrRlocJson != null) {
            globalEtrRlocAddress = addressCodec.decode(globalEtrRlocJson, context);
        }

        if (msRlocJson != null) {
            msRlocAddress = addressCodec.decode(msRlocJson, context);
        }

        if (privateEtrRlocJson != null) {
            privateEtrRlocAddress = addressCodec.decode(privateEtrRlocJson, context);
        }

        if (rtrRlocJson != null) {
            IntStream.range(0, rtrRlocJson.size())
                    .forEach(i -> rtrRlocAddresses.add(
                            addressCodec.decode(get(rtrRlocJson, i), context)));
        }

        return new LispNatAddress.Builder()
                            .withMsUdpPortNumber(msUdpPortNumber)
                            .withEtrUdpPortNumber(etrUdpPortNumber)
                            .withGlobalEtrRlocAddress(globalEtrRlocAddress)
                            .withMsRlocAddress(msRlocAddress)
                            .withPrivateEtrRlocAddress(privateEtrRlocAddress)
                            .withRtrRlocAddresses(rtrRlocAddresses)
                            .build();
    }
}
