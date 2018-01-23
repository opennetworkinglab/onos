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

import org.jboss.netty.buffer.ChannelBuffer;
import java.nio.ByteBuffer;

/**
 * Output interface route attribute.
 */
public final class RouteAttributeOif extends RouteAttribute {

    private static final int VALUE_LENGTH = 4;

    private final long outputInterface;

    /**
     * Class constructor.
     *
     * @param length length
     * @param type type
     * @param outputInterface output interface
     */
    private RouteAttributeOif(int length, int type, long outputInterface) {
        super(length, type);

        this.outputInterface = outputInterface;
    }

    /**
     * Returns the output interface.
     *
     * @return output interface
     */
    public long outputInterface() {
        return outputInterface;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("type", type())
                .add("length", length())
                .add("outputInterface", outputInterface)
                .toString();
    }

    /**
     * Returns a decoder for a output interface route attribute.
     *
     * @return output interface route attribute decoder
     */
    public static RouteAttributeDecoder<RouteAttributeOif> decoder() {
        return (int length, int type, byte[] value) -> {
            if (value.length != VALUE_LENGTH) {
                throw new DeserializationException("Wrong value length");
            }

            long outputInterface = Integer.reverseBytes(ByteBuffer.wrap(value).getInt());

            return new RouteAttributeOif(length, type, outputInterface);
        };
    }

    /**
     * Encode the RouteAttributeOif contents into the ChannelBuffer.
     *
     * @param cb channelbuffer to be filled in
     */
    @Override
    public void encode(ChannelBuffer cb) {

        super.encode(cb);
        cb.writeInt(Integer.reverseBytes((int) outputInterface));
    }

    /**
     * Returns a new RouteAttributeOif builder.
     *
     * @return RouteAttributeOif builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * RouteAttributeOif Builder.
     */
    public static final class Builder extends RouteAttribute.Builder<Builder> {

        private long outputInterface = 0;

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
         * Sets outputInterface for RouteAttributeOif that will be built.
         *
         * @param outputInterface to use for built RouteAttributeOif
         * @return this builder
         */
        public Builder outputInterface(long outputInterface) {
            this.outputInterface = outputInterface;
            return this;
        }

        /**
         * Builds the RouteAttributeOif.
         *
         * @return RouteAttributeOif reference
         */
        public RouteAttributeOif build() {
            return new RouteAttributeOif(length, type, outputInterface);
        }
    }
}
