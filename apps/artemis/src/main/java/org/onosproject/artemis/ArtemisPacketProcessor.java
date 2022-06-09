/*
 * Copyright 2017-present Open Networking Foundation
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
package org.onosproject.artemis;

import io.netty.channel.ChannelHandlerContext;
import com.eclipsesource.json.JsonObject;
import org.onosproject.artemis.impl.objects.ArtemisMessage;

/**
 * Packet processor for artemis messages.
 */
public interface ArtemisPacketProcessor {

    /**
     * Process a packet received from a MOAS client/server.
     *
     * @param msg artemis message
     * @param ctx channel context
     */
    void processMoasPacket(ArtemisMessage msg, ChannelHandlerContext ctx);

    /**
     * Process a BGP Update packet received from a monitor.
     *
     * @param msg BGP Update message
     */
    void processMonitorPacket(JsonObject msg);
}
