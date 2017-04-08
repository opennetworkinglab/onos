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

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.onosproject.codec.CodecContext;
import org.onosproject.codec.JsonCodec;
import org.onosproject.drivers.lisp.extensions.LispListAddress;
import org.onosproject.mapping.addresses.MappingAddress;
import org.slf4j.Logger;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * LISP list address codec.
 */
public final class LispListAddressCodec extends JsonCodec<LispListAddress> {

    private final Logger log = getLogger(getClass());

    protected static final String IPV4 = "ipv4";
    protected static final String IPV6 = "ipv6";

    private static final String MISSING_MEMBER_MESSAGE =
                                " member is required in LispListAddress";

    @Override
    public ObjectNode encode(LispListAddress address, CodecContext context) {
        checkNotNull(address, "LispListAddress cannot be null");

        final ObjectNode result = context.mapper().createObjectNode();

        final JsonCodec<MappingAddress> addressCodec =
                context.codec(MappingAddress.class);

        if (address.getIpv4() != null) {
            ObjectNode ipv4Node = addressCodec.encode(address.getIpv4(), context);
            result.set(IPV4, ipv4Node);
        }

        if (address.getIpv6() != null) {
            ObjectNode ipv6Node = addressCodec.encode(address.getIpv6(), context);
            result.set(IPV6, ipv6Node);
        }

        if (address.getIpv4() == null && address.getIpv6() == null) {
            log.error("Either IPv4 or IPv6 address should be specified.");
        }

        return result;
    }

    @Override
    public LispListAddress decode(ObjectNode json, CodecContext context) {
        if (json == null || !json.isObject()) {
            return null;
        }

        final JsonCodec<MappingAddress> addressCodec =
                context.codec(MappingAddress.class);
        ObjectNode ipv4Json = get(json, IPV4);
        ObjectNode ipv6Json = get(json, IPV6);
        MappingAddress ipv4Address = null;
        MappingAddress ipv6Address = null;

        if (ipv4Json != null) {
            ipv4Address = addressCodec.decode(ipv4Json, context);
        }

        if (ipv6Json != null) {
            ipv6Address = addressCodec.decode(ipv6Json, context);
        }

        if (ipv4Json == null && ipv6Json == null) {
            log.error("Either IPv4 or IPv6 address should be specified.");
        }

        return new LispListAddress.Builder()
                                .withIpv4(ipv4Address)
                                .withIpv6(ipv6Address)
                                .build();
    }
}
