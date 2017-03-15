/*
 * Copyright 2016-present Open Networking Laboratory
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

package org.onosproject.segmentrouting;

import org.onosproject.incubator.net.neighbour.NeighbourMessageContext;
import org.onosproject.incubator.net.neighbour.NeighbourMessageHandler;
import org.onosproject.net.host.HostService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This handler dispatches to the appropriate handlers the
 * neighbour discovery protocols.
 */
public class SegmentRoutingNeighbourDispatcher implements NeighbourMessageHandler {

    private static Logger log = LoggerFactory.getLogger(SegmentRoutingNeighbourDispatcher.class);
    private SegmentRoutingManager manager;

    /**
     * Create a segment routing neighbour dispatcher.
     *
     * @param segmentRoutingManager the segment routing manager
     */
    public SegmentRoutingNeighbourDispatcher(SegmentRoutingManager segmentRoutingManager) {
        this.manager = segmentRoutingManager;
    }

    @Override
    public void handleMessage(NeighbourMessageContext context, HostService hostService) {
        log.trace("Received a {} packet {}", context.protocol(), context.packet());
        switch (context.protocol()) {
            case ARP:
                if (this.manager.arpHandler != null) {
                    this.manager.arpHandler.processPacketIn(context, hostService);
                }
                break;
            case NDP:
                if (this.manager.icmpHandler != null) {
                    this.manager.icmpHandler.processPacketIn(context, hostService);
                }
                break;
            default:
                log.warn("Unknown protocol", context.protocol());
        }
    }


}
