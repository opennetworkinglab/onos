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

import com.google.common.collect.ImmutableSet;
import org.onlab.packet.EthType;
import org.onlab.packet.IpAddress;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.mcast.api.McastRouteData;
import org.onosproject.mcast.api.MulticastRouteService;
import org.onosproject.net.flow.DefaultTrafficSelector;
import org.onosproject.net.flow.TrafficSelector;
import org.onosproject.t3.api.StaticPacketTrace;
import org.slf4j.Logger;

import java.util.Set;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of the generator class that yields a set of Packet Traces.
 */
public class McastGenerator extends Generator<Set<StaticPacketTrace>> {

    private static final Logger log = getLogger(McastGenerator.class);
    protected static final MacAddress IPV4_ADDRESS = MacAddress.valueOf("01:00:5E:00:00:00");
    protected static final MacAddress IPV6_ADDRESS = MacAddress.valueOf("33:33:00:00:00:00");

    private final MulticastRouteService mcastService;
    private final TroubleshootManager manager;
    private final VlanId vlanId;

    /**
     * Creates a generator for obtaining traces of all configured multicast routes.
     *
     * @param service the host service
     * @param manager the troubleshoot manager issuing the request.
     * @param vlanId  the multicast configured VlanId.
     */
    McastGenerator(MulticastRouteService service, TroubleshootManager manager, VlanId vlanId) {
        this.mcastService = service;
        this.manager = manager;
        this.vlanId = vlanId;
    }

    @Override
    protected void run() {
        mcastService.getRoutes().forEach(route -> {
            McastRouteData routeData = mcastService.routeData(route);
            IpAddress group = route.group();
            routeData.sources().forEach((host, sources) -> {
                sources.forEach(source -> {
                    TrafficSelector.Builder selector = DefaultTrafficSelector.builder()
                            .matchVlanId(vlanId)
                            .matchInPort(source.port());
                    if (group.isIp4()) {
                        selector.matchEthDst(IPV4_ADDRESS)
                                .matchIPDst(group.toIpPrefix())
                                .matchEthType(EthType.EtherType.IPV4.ethType().toShort());
                    } else {
                        selector.matchEthDst(IPV6_ADDRESS)
                                .matchIPv6Dst(group.toIpPrefix())
                                .matchEthType(EthType.EtherType.IPV6.ethType().toShort());
                    }
                    try {
                        yield(ImmutableSet.of(manager.trace(selector.build(), source)));
                    } catch (InterruptedException e) {
                        log.warn("Interrupted generator", e.getMessage());
                        log.debug("exception", e);
                    }
                });
            });

        });

    }
}
