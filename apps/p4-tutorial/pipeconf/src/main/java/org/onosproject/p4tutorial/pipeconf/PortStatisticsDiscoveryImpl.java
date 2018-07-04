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

package org.onosproject.p4tutorial.pipeconf;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DefaultPortStatistics;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.device.PortStatistics;
import org.onosproject.net.device.PortStatisticsDiscovery;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.pi.model.PiCounterId;
import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.net.pi.runtime.PiCounterCellData;
import org.onosproject.net.pi.runtime.PiCounterCellId;
import org.onosproject.net.pi.service.PiPipeconfService;
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

import static org.onosproject.net.pi.model.PiCounterType.INDIRECT;

/**
 * Implementation of the PortStatisticsDiscovery behaviour for the mytunnel.p4 program. This behaviour works by using a
 * P4Runtime client to read the values of the ingress/egress port counters defined in the P4 program.
 */
public final class PortStatisticsDiscoveryImpl extends AbstractHandlerBehaviour implements PortStatisticsDiscovery {

    private static final Logger log = LoggerFactory.getLogger(PortStatisticsDiscoveryImpl.class);

    private static final PiCounterId INGRESS_COUNTER_ID = PiCounterId.of("c_ingress.rx_port_counter");
    private static final PiCounterId EGRESS_COUNTER_ID = PiCounterId.of("c_ingress.tx_port_counter");

    @Override
    public Collection<PortStatistics> discoverPortStatistics() {

        DeviceService deviceService = this.handler().get(DeviceService.class);
        DeviceId deviceId = this.data().deviceId();

        // Get a client for this device.
        P4RuntimeController controller = handler().get(P4RuntimeController.class);
        P4RuntimeClient client = controller.getClient(deviceId);
        if (client == null) {
            log.warn("Unable to find client for {}, aborting operation", deviceId);
            return Collections.emptyList();
        }

        // Get the pipeconf of this device.
        PiPipeconfService piPipeconfService = handler().get(PiPipeconfService.class);
        if (!piPipeconfService.ofDevice(deviceId).isPresent() ||
                !piPipeconfService.getPipeconf(piPipeconfService.ofDevice(deviceId).get()).isPresent()) {
            log.warn("Unable to get the pipeconf of {}, aborting operation", deviceId);
            return Collections.emptyList();
        }
        PiPipeconf pipeconf = piPipeconfService.getPipeconf(piPipeconfService.ofDevice(deviceId).get()).get();

        // Prepare PortStatistics objects to return, one per port of this device.
        Map<Long, DefaultPortStatistics.Builder> portStatBuilders = Maps.newHashMap();
        deviceService
                .getPorts(deviceId)
                .forEach(p -> portStatBuilders.put(p.number().toLong(),
                                                   DefaultPortStatistics.builder()
                                                           .setPort(p.number())
                                                           .setDeviceId(deviceId)));

        // Generate the counter cell IDs.
        Set<PiCounterCellId> counterCellIds = Sets.newHashSet();
        portStatBuilders.keySet().forEach(p -> {
            // Counter cell/index = port number.
            counterCellIds.add(PiCounterCellId.ofIndirect(INGRESS_COUNTER_ID, p));
            counterCellIds.add(PiCounterCellId.ofIndirect(EGRESS_COUNTER_ID, p));
        });

        // Query the device.
        Collection<PiCounterCellData> counterEntryResponse;
        try {
            counterEntryResponse = client.readCounterCells(counterCellIds, pipeconf).get();
        } catch (InterruptedException | ExecutionException e) {
            log.warn("Exception while reading port counters from {}: {}", deviceId, e.toString());
            log.debug("", e);
            return Collections.emptyList();
        }

        // Process response.
        counterEntryResponse.forEach(counterData -> {
            if (counterData.cellId().counterType() != INDIRECT) {
                log.warn("Invalid counter data type {}, skipping", counterData.cellId().counterType());
                return;
            }
            if (!portStatBuilders.containsKey(counterData.cellId().index())) {
                log.warn("Unrecognized counter index {}, skipping", counterData);
                return;
            }
            DefaultPortStatistics.Builder statsBuilder = portStatBuilders.get(counterData.cellId().index());
            if (counterData.cellId().counterId().equals(INGRESS_COUNTER_ID)) {
                statsBuilder.setPacketsReceived(counterData.packets());
                statsBuilder.setBytesReceived(counterData.bytes());
            } else if (counterData.cellId().counterId().equals(EGRESS_COUNTER_ID)) {
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
