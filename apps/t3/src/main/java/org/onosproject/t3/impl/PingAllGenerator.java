/*
 * Copyright 2018-present Open Networking Foundation
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

package org.onosproject.t3.impl;

import com.google.common.collect.Sets;
import org.onlab.packet.EthType;
import org.onlab.packet.IpAddress;
import org.onosproject.net.host.HostService;
import org.onosproject.t3.api.StaticPacketTrace;
import org.slf4j.Logger;

import java.util.List;
import java.util.Set;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of the generator class that yields a set of Packet Traces.
 */
public class PingAllGenerator extends Generator<Set<StaticPacketTrace>> {

    private static final Logger log = getLogger(PingAllGenerator.class);

    private final EthType.EtherType etherType;
    private final HostService hostService;
    private final TroubleshootManager manager;

    /**
     * Creates a generator for obtaining traces of pings between all the hosts in the network.
     *
     * @param etherType the type of traffic we are tracing.
     * @param service   the host service
     * @param manager   the troubleshoot manager issuing the request.
     */
    PingAllGenerator(EthType.EtherType etherType, HostService service, TroubleshootManager manager) {
        this.etherType = etherType;
        this.hostService = service;
        this.manager = manager;
    }

    @Override
    protected void run() throws InterruptedException {
        hostService.getHosts().forEach(host -> {
            List<IpAddress> ipAddresses = manager.getIpAddresses(host, etherType, false);
            if (ipAddresses.size() > 0) {
                //check if the host has only local IPs of that ETH type
                boolean onlyLocalSrc = ipAddresses.size() == 1 && ipAddresses.get(0).isLinkLocal();
                hostService.getHosts().forEach(hostToPing -> {
                    List<IpAddress> ipAddressesToPing = manager.getIpAddresses(hostToPing, etherType, false);
                    //check if the other host has only local IPs of that ETH type
                    boolean onlyLocalDst = ipAddressesToPing.size() == 1 && ipAddressesToPing.get(0).isLinkLocal();
                    boolean sameLocation = Sets.intersection(host.locations(), hostToPing.locations()).size() > 0;
                    //Trace is done only if they are both local and under the same location
                    // or not local and if they are not the same host.
                    if (((sameLocation && onlyLocalDst && onlyLocalSrc) ||
                            (!onlyLocalSrc && !onlyLocalDst && ipAddressesToPing.size() > 0))
                            && !host.equals(hostToPing)) {
                        try {
                            yield(manager.trace(host.id(), hostToPing.id(), etherType));
                        } catch (InterruptedException e) {
                            log.warn("Interrupted generator", e.getMessage());
                            log.debug("exception", e);
                        }
                    }
                });
            }
        });

    }
}
