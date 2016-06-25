/*
 * Copyright 2014-present Open Networking Laboratory
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
package org.onosproject.net.packet;

import org.onosproject.net.ConnectPoint;
import org.onlab.packet.Ethernet;

import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.Optional;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Default implementation of an immutable inbound packet.
 */
public final class DefaultInboundPacket implements InboundPacket {

    private final ConnectPoint receivedFrom;
    private final Ethernet parsed;
    private final ByteBuffer unparsed;
    private final Optional<Long> cookie;

    /**
     * Creates an immutable inbound packet.
     *
     * @param receivedFrom connection point where received
     * @param parsed       parsed ethernet frame
     * @param unparsed     unparsed raw bytes
     */
    public DefaultInboundPacket(ConnectPoint receivedFrom, Ethernet parsed,
                                ByteBuffer unparsed) {
        this(receivedFrom, parsed, unparsed, Optional.empty());
    }

    /**
     * Creates an immutable inbound packet with cookie.
     *
     * @param receivedFrom connection point where received
     * @param parsed       parsed ethernet frame
     * @param unparsed     unparsed raw bytes
     * @param cookie       cookie
     */
    public DefaultInboundPacket(ConnectPoint receivedFrom, Ethernet parsed,
            ByteBuffer unparsed, Optional<Long> cookie) {
        this.receivedFrom = receivedFrom;
        this.parsed = parsed;
        this.unparsed = unparsed;
        this.cookie = cookie;
    }

    @Override
    public ConnectPoint receivedFrom() {
        return receivedFrom;
    }

    @Override
    public Ethernet parsed() {
        return parsed;
    }

    @Override
    public ByteBuffer unparsed() {
        // FIXME: figure out immutability here
        return unparsed;
    }

    @Override
    public Optional<Long> cookie() {
        return cookie;
    }

    @Override
    public int hashCode() {
        return Objects.hash(receivedFrom, parsed, unparsed);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof InboundPacket) {
            final DefaultInboundPacket other = (DefaultInboundPacket) obj;
            return Objects.equals(this.receivedFrom, other.receivedFrom) &&
                    Objects.equals(this.parsed, other.parsed) &&
                    Objects.equals(this.unparsed, other.unparsed);
        }
        return false;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("receivedFrom", receivedFrom)
                .add("parsed", parsed)
                .toString();
    }
}
