/*
 * Copyright 2014-2015 Open Networking Laboratory
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
package org.onosproject.store.trivial;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.net.packet.OutboundPacket;
import org.onosproject.net.packet.PacketEvent;
import org.onosproject.net.packet.PacketEvent.Type;
import org.onosproject.net.packet.PacketRequest;
import org.onosproject.net.packet.PacketStore;
import org.onosproject.net.packet.PacketStoreDelegate;
import org.onosproject.store.AbstractStore;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Simple single instance implementation of the packet store.
 */
@Component(immediate = true)
@Service
public class SimplePacketStore
        extends AbstractStore<PacketEvent, PacketStoreDelegate>
        implements PacketStore {

    private Map<TrafficSelector, Set<PacketRequest>> requests = Maps.newConcurrentMap();

    @Override
    public void emit(OutboundPacket packet) {
        notifyDelegate(new PacketEvent(Type.EMIT, packet));
    }

    @Override
    public void requestPackets(PacketRequest request) {
        requests.compute(request.selector(), (s, existingRequests) -> {
            if (existingRequests == null) {
                return ImmutableSet.of(request);
            } else if (!existingRequests.contains(request)) {
                if (delegate != null) {
                    delegate.requestPackets(request);
                }
                return ImmutableSet.<PacketRequest>builder()
                        .addAll(existingRequests)
                        .add(request)
                        .build();
            } else {
                return existingRequests;
            }
        });
    }

    @Override
    public void cancelPackets(PacketRequest request) {
        requests.computeIfPresent(request.selector(), (s, existingRequests) -> {
            if (existingRequests.contains(request)) {
                HashSet<PacketRequest> newRequests = Sets.newHashSet(existingRequests);
                newRequests.remove(request);
                if (newRequests.size() > 0) {
                    return ImmutableSet.copyOf(newRequests);
                } else {
                    if (delegate != null) {
                        delegate.cancelPackets(request);
                    }
                    return null;
                }
            } else {
                return existingRequests;
            }
        });
    }

    @Override
    public List<PacketRequest> existingRequests() {
        List<PacketRequest> list = Lists.newArrayList();
        requests.values().forEach(list::addAll);
        list.sort((o1, o2) -> o1.priority().priorityValue() - o2.priority().priorityValue());
        return list;
    }

}
