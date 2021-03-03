/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.provider.nil;

import org.onosproject.net.Device;
import org.onosproject.net.Port;
import org.onosproject.net.device.DefaultPortStatistics;
import org.onosproject.net.device.DeviceProviderService;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.device.PortStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import static java.util.concurrent.Executors.newSingleThreadScheduledExecutor;
import static org.onlab.util.Tools.delay;
import static org.onlab.util.Tools.groupedThreads;

/**
 * Drives port statistics simulation using random generator.
 */
class PortStatsDriver implements Runnable {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final int WAIT_DELAY = 2_000;

    private final Random random = new SecureRandom();

    private volatile boolean stopped = true;

    private DeviceService deviceService;
    private DeviceProviderService deviceProviderService;

    private final ExecutorService executor =
            newSingleThreadScheduledExecutor(groupedThreads("onos/null", "port-stats-mutator", log));

    /**
     * Starts the mutation process.
     *
     * @param deviceService         device service
     * @param deviceProviderService device provider service
     */
    void start(DeviceService deviceService,
               DeviceProviderService deviceProviderService) {
        if (stopped) {
            stopped = false;
            this.deviceService = deviceService;
            this.deviceProviderService = deviceProviderService;
            executor.execute(this);
        }
    }

    /**
     * Stops the mutation process.
     */
    void stop() {
        stopped = true;
    }

    @Override
    public void run() {
        while (!stopped) {
            delay(WAIT_DELAY);
            deviceService.getAvailableDevices().forEach(this::updatePorts);
        }
    }

    public void updatePorts(Device device) {
        Set<PortStatistics> portStats = new HashSet<>();
        for (Port port : deviceService.getPorts(device.id())) {
            portStats.add(DefaultPortStatistics.builder()
                                  .setBytesReceived(Math.abs(random.nextInt()))
                                  .setBytesSent(Math.abs(random.nextInt()))
                                  .setPacketsReceived(Math.abs(random.nextInt()))
                                  .setPacketsSent(Math.abs(random.nextInt()))
                                  .setDurationSec(2)
                                  .setDeviceId(device.id())
                                  .setPort(port.number())
                                  .build());
        }
        deviceProviderService.updatePortStatistics(device.id(), portStats);
    }

}
