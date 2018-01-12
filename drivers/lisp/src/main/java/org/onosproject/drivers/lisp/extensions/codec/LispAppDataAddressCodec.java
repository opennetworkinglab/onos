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
package org.onosproject.drivers.lisp.extensions.codec;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.drivers.lisp.extensions.LispAppDataAddress;
import org.onosproject.mapping.addresses.MappingAddress;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * LISP application data address codec.
 */
public final class LispAppDataAddressCodec extends JsonCodec<LispAppDataAddress> {

    static final String PROTOCOL = "protocol";
    static final String IP_TOS = "ipTos";
    static final String LOCAL_PORT_LOW = "localPortLow";
    static final String LOCAL_PORT_HIGH = "localPortHigh";
    static final String REMOTE_PORT_LOW = "remotePortLow";
    static final String REMOTE_PORT_HIGH = "remotePortHigh";
    static final String ADDRESS = "address";

    @Override
    public ObjectNode encode(LispAppDataAddress address, CodecContext context) {
        checkNotNull(address, "LispAppDataAddress cannot be null");

        final ObjectNode result = context.mapper().createObjectNode()
                .put(PROTOCOL, address.getProtocol())
                .put(IP_TOS, address.getIpTos())
                .put(LOCAL_PORT_LOW, address.getLocalPortLow())
                .put(LOCAL_PORT_HIGH, address.getLocalPortHigh())
                .put(REMOTE_PORT_LOW, address.getRemotePortLow())
                .put(REMOTE_PORT_HIGH, address.getRemotePortHigh());

        if (address.getAddress() != null) {
            final JsonCodec<MappingAddress> addressCodec =
                    context.codec(MappingAddress.class);
            ObjectNode addressNode = addressCodec.encode(address.getAddress(), context);
            result.set(ADDRESS, addressNode);
        }

        return result;
    }

    @Override
    public LispAppDataAddress decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        byte protocol = (byte) json.get(PROTOCOL).asInt();
        int ipTos = json.get(IP_TOS).asInt();
        short localPortLow = (short) json.get(LOCAL_PORT_LOW).asInt();
        short localPortHigh = (short) json.get(LOCAL_PORT_HIGH).asInt();
        short remotePortLow = (short) json.get(REMOTE_PORT_LOW).asInt();
        short remotePortHigh = (short) json.get(REMOTE_PORT_HIGH).asInt();

        ObjectNode addressJson = get(json, ADDRESS);
        MappingAddress mappingAddress = null;

        if (addressJson != null) {
            final JsonCodec<MappingAddress> addressCodec =
                    context.codec(MappingAddress.class);
            mappingAddress = addressCodec.decode(addressJson, context);
        }

        return new LispAppDataAddress.Builder()
                        .withProtocol(protocol)
                        .withIpTos(ipTos)
                        .withLocalPortLow(localPortLow)
                        .withLocalPortHigh(localPortHigh)
                        .withRemotePortLow(remotePortLow)
                        .withRemotePortHigh(remotePortHigh)
                        .withAddress(mappingAddress)
                        .build();
    }
}
