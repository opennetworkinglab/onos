/*
 * Copyright 2021-present Open Networking Foundation
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

package org.onosproject.pipelines.fabric.impl.behaviour.upf;

import com.google.common.collect.Queues;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.packet.OutboundPacket;
import org.onosproject.net.packet.PacketPriority;
import org.onosproject.net.packet.PacketProcessor;
import org.onosproject.net.packet.PacketProcessorEntry;
import org.onosproject.net.packet.PacketRequest;
import org.onosproject.net.packet.PacketService;

import java.util.List;
import java.util.Optional;
import java.util.Queue;

public class MockPacketService implements PacketService {

    Queue<OutboundPacket> emittedPackets = Queues.newArrayDeque();

    @Override
    public void addProcessor(PacketProcessor processor, int priority) {

    }

    @Override
    public void removeProcessor(PacketProcessor processor) {

    }

    @Override
    public List<PacketProcessorEntry> getProcessors() {
        return null;
    }

    @Override
    public void requestPackets(TrafficSelector selector, PacketPriority priority, ApplicationId appId) {

    }

    @Override
    public void requestPackets(TrafficSelector selector, PacketPriority priority,
                               ApplicationId appId, Optional<DeviceId> deviceId) {

    }

    @Override
    public void cancelPackets(TrafficSelector selector, PacketPriority priority, ApplicationId appId) {

    }

    @Override
    public void cancelPackets(TrafficSelector selector, PacketPriority priority,
                              ApplicationId appId, Optional<DeviceId> deviceId) {

    }

    @Override
    public List<PacketRequest> getRequests() {
        return null;
    }

    @Override
    public void emit(OutboundPacket packet) {
        emittedPackets.add(packet);
    }
}


