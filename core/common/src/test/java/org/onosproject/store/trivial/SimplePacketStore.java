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

import com.google.common.collect.Sets;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.net.packet.OutboundPacket;
import org.onosproject.net.packet.PacketEvent;
import org.onosproject.net.packet.PacketEvent.Type;
import org.onosproject.net.packet.PacketRequest;
import org.onosproject.net.packet.PacketStore;
import org.onosproject.net.packet.PacketStoreDelegate;
import org.onosproject.store.AbstractStore;


import java.util.Collections;
import java.util.Set;

/**
 * Simple single instance implementation of the packet store.
 */
@Component(immediate = true)
@Service
public class SimplePacketStore
        extends AbstractStore<PacketEvent, PacketStoreDelegate>
        implements PacketStore {

    private Set<PacketRequest> requests = Sets.newConcurrentHashSet();

    @Override
    public void emit(OutboundPacket packet) {
        notifyDelegate(new PacketEvent(Type.EMIT, packet));
    }

    @Override
    public boolean requestPackets(PacketRequest request) {
        return requests.add(request);
    }

    @Override
    public boolean cancelPackets(PacketRequest request) {
        return requests.remove(request);
    }

    @Override
    public Set<PacketRequest> existingRequests() {
        return Collections.unmodifiableSet(requests);
    }

}
