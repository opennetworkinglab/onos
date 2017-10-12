/*
 * Copyright 2014-present Open Networking Foundation
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
package org.onosproject.provider.of.flow.util;

import org.projectfloodlight.openflow.protocol.stat.Stat;
import org.projectfloodlight.openflow.protocol.stat.StatField;
import org.projectfloodlight.openflow.types.U32;
import org.projectfloodlight.openflow.types.U64;

/**
 * FlowStatParser helps to parse OXS which is added in OPF 1.5.
 */
public final class FlowStatParser {
    private final Stat stat;


    private long duration;
    private long idleTime;
    private long flowCount;
    private long packetCount;
    private long byteCount;
    private boolean isDurationReceived;

    public FlowStatParser(Stat stat) {
        this.stat = stat;
        parseStats();
    }

    public Stat getStat() {
        return stat;
    }

    private void parseStats() {
        U64 durationOfValue = this.stat.get(StatField.DURATION);
        U64 byteCountOfValue = this.stat.get(StatField.BYTE_COUNT);
        U32 flowCountOfValue = this.stat.get(StatField.FLOW_COUNT);
        U64 idleTimeOfValue = this.stat.get(StatField.IDLE_TIME);
        U64 packetCountOfValue = this.stat.get(StatField.PACKET_COUNT);

        isDurationReceived = durationOfValue != null;
        duration = durationOfValue != null ? durationOfValue.getValue() : 0;
        byteCount = byteCountOfValue != null ? byteCountOfValue.getValue() : 0;
        idleTime = idleTimeOfValue != null ? idleTimeOfValue.getValue() : 0;
        flowCount = flowCountOfValue != null ? flowCountOfValue.getValue() : 0;
        packetCount = packetCountOfValue != null ? packetCountOfValue.getValue() : 0;
    }


    public long getByteCount() {
        return byteCount;
    }

    public long getDuration() {
        return duration;
    }

    public long getFlowCount() {
        return flowCount;
    }

    public long getPacketCount() {
        return packetCount;
    }

    public long getIdleTime() {
        return idleTime;
    }

    public boolean isDurationReceived() {
        return isDurationReceived;
    }
}
