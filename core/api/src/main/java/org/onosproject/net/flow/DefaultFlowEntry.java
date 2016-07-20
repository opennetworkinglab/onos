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
package org.onosproject.net.flow;

import static com.google.common.base.MoreObjects.toStringHelper;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.slf4j.LoggerFactory.getLogger;

import org.slf4j.Logger;

import java.util.concurrent.TimeUnit;

public class DefaultFlowEntry extends DefaultFlowRule
    implements StoredFlowEntry {

    private static final Logger log = getLogger(DefaultFlowEntry.class);

    /* Stored in nanoseconds (allows for 292 years) */
    private long life;

    private long packets;
    private long bytes;
    private FlowEntryState state;

    private long lastSeen = -1;

    private final int errType;

    private final int errCode;

    public DefaultFlowEntry(FlowRule rule, FlowEntryState state,
                            long life, TimeUnit lifeTimeUnit, long packets, long bytes) {
        super(rule);
        this.state = state;
        this.life = lifeTimeUnit.toNanos(life);
        this.packets = packets;
        this.bytes = bytes;
        this.errCode = -1;
        this.errType = -1;
        this.lastSeen = System.currentTimeMillis();
    }

    public DefaultFlowEntry(FlowRule rule, FlowEntryState state,
                            long lifeSecs, long packets, long bytes) {
        this(rule, state, lifeSecs, SECONDS, packets, bytes);
    }

    public DefaultFlowEntry(FlowRule rule) {
        this(rule, FlowEntryState.PENDING_ADD, 0, 0, 0);
    }

    public DefaultFlowEntry(FlowRule rule, int errType, int errCode) {
        super(rule);
        this.state = FlowEntryState.FAILED;
        this.errType = errType;
        this.errCode = errCode;
        this.lastSeen = System.currentTimeMillis();
    }

    @Override
    public long life() {
        return life(SECONDS);
    }

    @Override
    public long life(TimeUnit timeUnit) {
        return timeUnit.convert(life, NANOSECONDS);
    }

    @Override
    public long packets() {
        return packets;
    }

    @Override
    public long bytes() {
        return bytes;
    }

    @Override
    public FlowEntryState state() {
        return this.state;
    }

    @Override
    public long lastSeen() {
        return lastSeen;
    }

    @Override
    public void setLastSeen() {
        this.lastSeen = System.currentTimeMillis();
    }

    @Override
    public void setState(FlowEntryState newState) {
        this.state = newState;
    }

    @Override
    public void setLife(long life) {
        setLife(life, SECONDS);
    }

    @Override
    public void setLife(long life, TimeUnit timeUnit) {
        this.life = timeUnit.toNanos(life);
    }

    @Override
    public void setPackets(long packets) {
        this.packets = packets;
    }

    @Override
    public void setBytes(long bytes) {
        this.bytes = bytes;
    }

    @Override
    public int errType() {
        return this.errType;
    }

    @Override
    public int errCode() {
        return this.errCode;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("rule", super.toString())
                .add("state", state)
                .toString();
    }
}
