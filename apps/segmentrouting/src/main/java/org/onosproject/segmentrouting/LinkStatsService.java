/*
 * Copyright 2015 Open Networking Laboratory
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

import org.onosproject.net.Device;
import org.onosproject.net.Link;
import org.onosproject.net.device.DefaultPortStatistics;

import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LinkStatsService {

    public static class LinkStats {

        private long bytesTransferred;
        private long packetsTransferred;
        private long packetsDropped;
        private long durationInNanosec; // total duration the port has been live
        private long durationInSec; // total duration the port has been live

        public LinkStats(long bytesTransferred,
                         long packetsTransferred,
                         long packetsDropped,
                         long durationInNanosec,
                         long durationInSec) {
            this.bytesTransferred = bytesTransferred;
            this.packetsTransferred = packetsTransferred;
            this.packetsDropped = packetsDropped;
            this.durationInNanosec = durationInNanosec;
            this.durationInSec = durationInSec;
        }

        public long bytesTransferred() {
            return bytesTransferred;
        }

        public long packetsTransferred() {
            return packetsTransferred;
        }

        public long packetsDropped() {
            return packetsDropped;
        }

        public long durationInNanosec() {
            return durationInNanosec;
        }

        public long durationInSec() {
            return durationInSec;
        }
    }

    private static Logger log = LoggerFactory.getLogger(LinkStatsService.class);
    private SegmentRoutingManager srManager;

    public LinkStatsService(SegmentRoutingManager srManager) {
        this.srManager = srManager;
    }

    public HashMap<Link, LinkStats> stats() {
        HashMap<Link, LinkStats> linkStatsMapper = new HashMap<Link, LinkStats>();
        /* Lists all the switches in the network */
        for (Device sw : srManager.deviceService.getDevices()) {
            /* Stats for all the outgoing links */
            for (Link link : srManager.linkService.getDeviceEgressLinks(sw.id())) {
                int portNum = (int) link.src().port().toLong();
                DefaultPortStatistics portStats = DefaultPortStatistics
                                                  .builder()
                                                  .setPort(portNum)
                                                  .build();
                LinkStats linkStats = new LinkStats(portStats.bytesSent(),
                                                    portStats.packetsSent(),
                                                    portStats.packetsTxDropped(),
                                                    portStats.durationNano(),
                                                    portStats.durationSec());
                linkStatsMapper.put(link, linkStats);
            }
            /* Stats for all the incoming links */
            for (Link link : srManager.linkService.getDeviceIngressLinks(sw.id())) {
                int portNum = (int) link.dst().port().toLong();
                DefaultPortStatistics portStats = DefaultPortStatistics
                                                  .builder()
                                                  .setPort(portNum)
                                                  .build();
                LinkStats linkStats = new LinkStats(portStats.bytesReceived(),
                                                    portStats.packetsReceived(),
                                                    portStats.packetsRxDropped(),
                                                    portStats.durationNano(),
                                                    portStats.durationSec());
                linkStatsMapper.put(link, linkStats);
            }
        }
        return linkStatsMapper;
    }
}
