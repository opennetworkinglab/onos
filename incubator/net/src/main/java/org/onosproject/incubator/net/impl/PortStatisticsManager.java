/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.incubator.net.impl;

import com.google.common.collect.Maps;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.incubator.net.PortStatisticsService;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.device.DeviceEvent;
import org.onosproject.net.device.DeviceListener;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.device.PortStatistics;
import org.onosproject.net.statistic.DefaultLoad;
import org.onosproject.net.statistic.Load;
import org.slf4j.Logger;

import java.util.Map;
import java.util.stream.Collectors;

import static org.onosproject.net.PortNumber.portNumber;
import static org.onosproject.net.device.DeviceEvent.Type.*;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Implementation of the port statistics service.
 */
@Component(immediate = true)
@Service
public class PortStatisticsManager implements PortStatisticsService {

    private final Logger log = getLogger(getClass());

    private static final long POLL_FREQUENCY = 10_000; // milliseconds
    private static final long STALE_LIMIT = (long) (1.5 * POLL_FREQUENCY);
    private static final int SECOND = 1_000; // milliseconds

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    private final DeviceListener deviceListener = new InternalDeviceListener();

    private Map<ConnectPoint, DataPoint> current = Maps.newConcurrentMap();
    private Map<ConnectPoint, DataPoint> previous = Maps.newConcurrentMap();

    @Activate
    public void activate() {
        deviceService.addListener(deviceListener);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        deviceService.removeListener(deviceListener);
        log.info("Stopped");
    }

    @Override
    public Load load(ConnectPoint connectPoint) {
        return load(connectPoint, MetricType.BYTES);
    }

    @Override
    public Load load(ConnectPoint connectPoint, MetricType metricType) {
        DataPoint c = current.get(connectPoint);
        DataPoint p = previous.get(connectPoint);
        long now = System.currentTimeMillis();

        if (c != null && p != null && (now - c.time < STALE_LIMIT)) {
            if (c.time > p.time + SECOND) {
                long cve = getEgressValue(c.stats, metricType);
                long cvi = getIngressValue(c.stats, metricType);
                long pve = getEgressValue(p.stats, metricType);
                long pvi = getIngressValue(p.stats, metricType);
                //Use max of either Tx or Rx load as the total load of a port
                Load load = null;
                if (cve >= pve) {
                    load = new DefaultLoad(cve, pve, (int) (c.time - p.time) / SECOND);
                }
                if (cvi >= pvi) {
                    Load rcvLoad = new DefaultLoad(cvi, pvi, (int) (c.time - p.time) / SECOND);
                    load = ((load == null) || (rcvLoad.rate() > load.rate())) ? rcvLoad : load;
                }
                return load;
            }
        }
        return null;
    }

    private long getEgressValue(PortStatistics stats, MetricType metricType) {
        return metricType == MetricType.BYTES ? stats.bytesSent() : stats.packetsSent();
    }

    private long getIngressValue(PortStatistics stats, MetricType metricType) {
        return metricType == MetricType.BYTES ? stats.bytesReceived() : stats.packetsReceived();
    }

    // Monitors port stats update messages.
    private class InternalDeviceListener implements DeviceListener {
        @Override
        public void event(DeviceEvent event) {
            DeviceEvent.Type type = event.type();
            DeviceId deviceId = event.subject().id();
            if (type == PORT_STATS_UPDATED) {
                // Update port load
                updateDeviceData(deviceId);

            } else if (type == DEVICE_REMOVED ||
                    (type == DEVICE_AVAILABILITY_CHANGED &&
                            !deviceService.isAvailable(deviceId))) {
                // Clean-up all port loads
                pruneDeviceData(deviceId);
            }
        }
    }

    // Updates the port stats for the specified device
    private void updateDeviceData(DeviceId deviceId) {
        deviceService.getPortStatistics(deviceId)
                .forEach(stats -> updatePortData(deviceId, stats));
    }

    // Updates the port stats for the specified port
    private void updatePortData(DeviceId deviceId, PortStatistics stats) {
        ConnectPoint cp = new ConnectPoint(deviceId, portNumber(stats.port()));
        DataPoint c = current.get(cp);

        // Create a new data point and make it the current one
        current.put(cp, new DataPoint(stats));

        // If we have a current data point, demote it to previous
        if (c != null) {
            previous.put(cp, c);
        }
    }

    // Cleans all port loads for the specified device
    private void pruneDeviceData(DeviceId deviceId) {
        pruneMap(current, deviceId);
        pruneMap(previous, deviceId);
    }

    private void pruneMap(Map<ConnectPoint, DataPoint> map, DeviceId deviceId) {
        map.keySet().stream().filter(cp -> deviceId.equals(cp.deviceId()))
                .collect(Collectors.toSet()).forEach(map::remove);
    }

    // Auxiliary data point to track when we receive different samples.
    private class DataPoint {
        long time;
        PortStatistics stats;

        DataPoint(PortStatistics stats) {
            time = System.currentTimeMillis();
            this.stats = stats;
        }
    }

}
