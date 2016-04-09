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

import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.TrafficTreatment.Builder;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.onosproject.security.AppGuard.checkPermission;
import static org.onosproject.security.AppPermission.Type.*;

/**
 * Default implementation of a packet context.
 */
public abstract class DefaultPacketContext implements PacketContext {

    private final long time;
    private final InboundPacket inPkt;
    private final OutboundPacket outPkt;
    private final TrafficTreatment.Builder builder;

    private final AtomicBoolean block;

    /**
     * Creates a new packet context.
     *
     * @param time creation time
     * @param inPkt inbound packet
     * @param outPkt outbound packet
     * @param block whether the context is blocked or not
     */
    protected DefaultPacketContext(long time, InboundPacket inPkt,
            OutboundPacket outPkt, boolean block) {
        super();
        this.time = time;
        this.inPkt = inPkt;
        this.outPkt = outPkt;
        this.block = new AtomicBoolean(block);
        this.builder = DefaultTrafficTreatment.builder();
    }

    @Override
    public long time() {
        checkPermission(PACKET_READ);
        return time;
    }

    @Override
    public InboundPacket inPacket() {
        checkPermission(PACKET_READ);
        return inPkt;
    }

    @Override
    public OutboundPacket outPacket() {
        checkPermission(PACKET_READ);
        return outPkt;
    }

    @Override
    public Builder treatmentBuilder() {
        checkPermission(PACKET_READ);
        return builder;
    }

    @Override
    public abstract void send();

    @Override
    public boolean block() {
        checkPermission(PACKET_WRITE);
        return this.block.getAndSet(true);
    }

    @Override
    public boolean isHandled() {
        checkPermission(PACKET_READ);
        return this.block.get();
    }
}