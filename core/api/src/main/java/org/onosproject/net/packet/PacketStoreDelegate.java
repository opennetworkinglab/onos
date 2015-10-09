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

import org.onosproject.store.StoreDelegate;

/**
 * Packet store delegate abstraction.
 */
public interface PacketStoreDelegate extends StoreDelegate<PacketEvent> {

    /**
     * Requests that packets matching to following request be collected
     * from all switches.
     *
     * @param request packet request
     */
    void requestPackets(PacketRequest request);

    /**
     * Requests that packets matching to following request no longer be
     * collected from any switches.
     *
     * @param request packet request
     */
    void cancelPackets(PacketRequest request);
}
