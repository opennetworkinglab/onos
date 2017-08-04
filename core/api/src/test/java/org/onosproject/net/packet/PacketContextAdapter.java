/*
 * Copyright 2016-present Open Networking Foundation
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

/**
 * Instantiable version of DefaultPacketContext for use in unit tests that need
 * to provide a PacketContext as input data.
 */
public class PacketContextAdapter extends DefaultPacketContext {

    public PacketContextAdapter(long time, InboundPacket inPkt,
                                OutboundPacket outPkt, boolean block) {
        super(time, inPkt, outPkt, block);
    }

    @Override
    public void send() {

    }

}
