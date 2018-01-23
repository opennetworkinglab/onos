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

package org.onosproject.routing.fpm.protocol;

import com.google.common.base.MoreObjects;
import org.onlab.packet.DeserializationException;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.Ip6Address;
import org.onlab.packet.IpAddress;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;

/**
 * Gateway route attribute.
 */
public final class RouteAttributeGateway extends RouteAttribute {

    public static final int VALUE_LENGTH = 4;

    private final IpAddress gateway;

    /**
     * Class constructor.
     *
     * @param length length
     * @param type type
     * @param gateway gateway address
     */
    private RouteAttributeGateway(int length, int type, IpAddress gateway) {
        super(length, type);

        this.gateway = gateway;
    }

    /**
     * Returns the gateway address.
     *
     * @return gateway address
     */
    public IpAddress gateway() {
        return gateway;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("type", type())
                .add("length", length())
                .add("gateway", gateway)
                .toString();
    }

    /**
     * Returns a decoder for a gateway route attribute.
     *
     * @return gateway route attribute decoder
     */
    public static RouteAttributeDecoder<RouteAttributeGateway> decoder() {
        return (int length, int type, byte[] value) -> {

            IpAddress gateway;
            if (value.length == Ip4Address.BYTE_LENGTH) {
                gateway = IpAddress.valueOf(IpAddress.Version.INET, value);
            } else if (value.length == Ip6Address.BYTE_LENGTH) {
                gateway = IpAddress.valueOf(IpAddress.Version.INET6, value);
            } else {
                throw new DeserializationException("Invalid address length");
            }

            return new RouteAttributeGateway(length, type, gateway);
        };
    }

    /**
     * Encode the RouteAttributeGateway contents into the ChannelBuffer.
     *
     * @param cb channelbuffer to be filled in
     */
    @Override
    public void encode(ChannelBuffer cb) {

        super.encode(cb);

        ChannelBuffer buffer =  ChannelBuffers.copiedBuffer(gateway.toOctets());
        if (length() == Ip6Address.BYTE_LENGTH +
                RouteAttribute.ROUTE_ATTRIBUTE_HEADER_LENGTH) {
            cb.writeBytes(buffer, Ip6Address.BYTE_LENGTH);
        } else if (length() == Ip4Address.BYTE_LENGTH +
                RouteAttribute.ROUTE_ATTRIBUTE_HEADER_LENGTH) {
            cb.writeBytes(buffer, Ip4Address.BYTE_LENGTH);
        } else {
            throw new IllegalArgumentException("Gateway address length incorrect!");
        }
    }

    /**
     * Returns a new RouteAttributeGateway builder.
     *
     * @return RouteAttributeGateway builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * RouteAttributeGateway Builder.
     */
    public static final class Builder extends RouteAttribute.Builder<Builder> {

        private IpAddress gateway = null;

        /**
        * Hide class constructor.
        */
        private Builder() {}

        /**
         * Override abstract method.
         */
        @Override
        public Builder getThis() {
            return this;
        }

        /**
         * Sets gateway for RouteAttributeGateway that will be built.
         *
         * @param gateway to use for built RouteAttributeGateway
         * @return this builder
         */
        public Builder gateway(IpAddress gateway) {
            this.gateway = gateway;
            return this;
        }

        /**
         * Builds the RouteAttributeGateway.
         *
         * @return RouteAttributeGateway reference
         */
        public RouteAttributeGateway build() {
            return new RouteAttributeGateway(length, type, gateway);
        }
    }
}
