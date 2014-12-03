/*
 * Copyright 2014 Open Networking Laboratory
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

import java.util.concurrent.atomic.AtomicBoolean;

import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flow.TrafficTreatment.Builder;


public abstract class DefaultPacketContext implements PacketContext {

    private final long time;
    private final InboundPacket inPkt;
    private final OutboundPacket outPkt;
    private final TrafficTreatment.Builder builder;

    private final AtomicBoolean block;


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
        return time;
    }

    @Override
    public InboundPacket inPacket() {
        return inPkt;
    }

    @Override
    public OutboundPacket outPacket() {
        return outPkt;
    }

    @Override
    public Builder treatmentBuilder() {
        return builder;
    }

    @Override
    public abstract void send();

    @Override
    public boolean block() {
        return this.block.getAndSet(true);
    }

    @Override
    public boolean isHandled() {
        return this.block.get();
    }
}
