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

package org.onosproject.pipelines.basic;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DefaultPortStatistics;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.device.PortStatistics;
import org.onosproject.net.device.PortStatisticsDiscovery;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.net.pi.runtime.PiCounterCellData;
import org.onosproject.net.pi.runtime.PiCounterCellId;
import org.onosproject.net.pi.runtime.PiCounterId;
import org.onosproject.net.pi.runtime.PiIndirectCounterCellId;
import org.onosproject.net.pi.runtime.PiPipeconfService;
import org.onosproject.p4runtime.api.P4RuntimeClient;
import org.onosproject.p4runtime.api.P4RuntimeController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static org.onosproject.net.pi.runtime.PiCounterType.INDIRECT;

/**
 * Implementation of the PortStatisticsBehaviour for basic.p4.
 */
public class PortStatisticsDiscoveryImpl extends AbstractHandlerBehaviour implements PortStatisticsDiscovery {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    private static final String SCOPE = "port_counters_control";
    private static final PiCounterId INGRESS_COUNTER_ID = PiCounterId.of("port_counters_ingress",
                                                                         "ingress_port_counter", INDIRECT);
    private static final PiCounterId EGRESS_COUNTER_ID = PiCounterId.of("port_counters_egress",
                                                                        "egress_port_counter", INDIRECT);

    /**
     * Returns the ID of the ingress port counter.
     *
     * @return counter ID
     */
    public PiCounterId ingressCounterId() {
        return INGRESS_COUNTER_ID;
    }

    /**
     * Returns the ID of the egress port counter.
     *
     * @return counter ID
     */
    public PiCounterId egressCounterId() {
        return EGRESS_COUNTER_ID;
    }

    @Override
    public Collection<PortStatistics> discoverPortStatistics() {

        DeviceService deviceService = this.handler().get(DeviceService.class);
        DeviceId deviceId = this.data().deviceId();

        PiPipeconfService piPipeconfService = handler().get(PiPipeconfService.class);
        if (!piPipeconfService.ofDevice(deviceId).isPresent() ||
                !piPipeconfService.getPipeconf(piPipeconfService.ofDevice(deviceId).get()).isPresent()) {
            log.warn("Unable to get the pipeconf of {}, aborting operation", deviceId);
            return Collections.emptyList();
        }
        PiPipeconf pipeconf = piPipeconfService.getPipeconf(piPipeconfService.ofDevice(deviceId).get()).get();

        P4RuntimeController controller = handler().get(P4RuntimeController.class);
        if (!controller.hasClient(deviceId)) {
            log.warn("Unable to find client for {}, aborting operation", deviceId);
            return Collections.emptyList();
        }
        P4RuntimeClient client = controller.getClient(deviceId);

        Map<Long, DefaultPortStatistics.Builder> portStatBuilders = Maps.newHashMap();
        deviceService.getPorts(deviceId)
                .forEach(p -> portStatBuilders.put(p.number().toLong(),
                                                   DefaultPortStatistics.builder()
                                                           .setPort(p.number())
                                                           .setDeviceId(deviceId)));

        Set<PiCounterCellId> counterCellIds = Sets.newHashSet();
        portStatBuilders.keySet().forEach(p -> {
            // Counter cell/index = port number.
            counterCellIds.add(PiIndirectCounterCellId.of(ingressCounterId(), p));
            counterCellIds.add(PiIndirectCounterCellId.of(egressCounterId(), p));
        });

        Collection<PiCounterCellData> counterEntryResponse;
        try {
            counterEntryResponse = client.readCounterCells(counterCellIds, pipeconf).get();
        } catch (InterruptedException | ExecutionException e) {
            log.warn("Exception while reading port counters from {}: {}", deviceId, e.toString());
            log.debug("", e);
            return Collections.emptyList();
        }

        counterEntryResponse.forEach(counterData -> {
            if (counterData.cellId().type() != INDIRECT) {
                log.warn("Invalid counter data type {}, skipping", counterData.cellId().type());
                return;
            }
            PiIndirectCounterCellId indCellId = (PiIndirectCounterCellId) counterData.cellId();
            if (!portStatBuilders.containsKey(indCellId.index())) {
                log.warn("Unrecognized counter index {}, skipping", counterData);
                return;
            }
            DefaultPortStatistics.Builder statsBuilder = portStatBuilders.get(indCellId.index());
            if (counterData.cellId().counterId().equals(ingressCounterId())) {
                statsBuilder.setPacketsReceived(counterData.packets());
                statsBuilder.setBytesReceived(counterData.bytes());
            } else if (counterData.cellId().counterId().equals(egressCounterId())) {
                statsBuilder.setPacketsSent(counterData.packets());
                statsBuilder.setBytesSent(counterData.bytes());
            } else {
                log.warn("Unrecognized counter ID {}, skipping", counterData);
            }
        });

        return portStatBuilders
                .values()
                .stream()
                .map(DefaultPortStatistics.Builder::build)
                .collect(Collectors.toList());
    }
}
