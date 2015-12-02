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
package org.onosproject.cpman;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Modified;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.metrics.MetricsService;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.net.device.DeviceService;
import org.slf4j.Logger;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Control plane management application.
 */
@Component(immediate = true)
public class ControlPlaneManager {

    private final Logger log = getLogger(getClass());
    private Set<ControlMetricsObserver> controlMetricsObservers = new HashSet<>();
    private ControlMetricsObserver cpObserver;

    private ApplicationId appId;

    private ControlMetricsFactory cmf;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected DeviceService deviceService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected CoreService coreService;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected MetricsService metricsService;

    @Activate
    public void activate() {
        appId = coreService.registerApplication("org.onosproject.cpman");

        cmf = ControlMetricsFactory.getInstance(metricsService, deviceService);
        // currently disable monitoring by default
        // cmf.startMonitor();

        registerObserver();

        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        unregisterObserver();
        cmf.stopMonitor();
        log.info("Stopped");
    }

    @Modified
    public void modified() {
    }

    private void registerObserver() {
        cpObserver = new DefaultControlMetricsObserver();
        this.addControlMetricsObserver(cpObserver);
    }

    private void unregisterObserver() {
        this.removeControlMetricsObserver(cpObserver);
    }

    private void executeMonitorTask() {

        // TODO: execute monitoring task with 1 minute period
        if (cmf.isMonitor()) {
            controlMetricsObservers.forEach(observer -> {

                // try to feed the CPU and memory stats
                observer.feedMetrics(cmf.cpuInfoMetric(), Optional.ofNullable(null));
                observer.feedMetrics(cmf.memoryInfoMetric(), Optional.ofNullable(null));

                // try to feed the control message stats
                cmf.getDeviceIds().forEach(v -> {
                    observer.feedMetrics(cmf.inboundPacketMetrics(v), Optional.of(v));
                    observer.feedMetrics(cmf.outboundPacketMetrics(v), Optional.of(v));
                    observer.feedMetrics(cmf.flowmodPacketMetrics(v), Optional.of(v));
                    observer.feedMetrics(cmf.flowrmvPacketMetrics(v), Optional.of(v));
                    observer.feedMetrics(cmf.requestPacketMetrics(v), Optional.of(v));
                    observer.feedMetrics(cmf.replyPacketMetrics(v), Optional.of(v));
                });
            });
        }
    }

    /**
     * Adds a new control metrics observer.
     *
     * @param cmObserver control metric observer instance
     */
    public void addControlMetricsObserver(ControlMetricsObserver cmObserver) {
        controlMetricsObservers.add(cmObserver);
    }

    /**
     * Removes an existing control metrics observer.
     *
     * @param cmObserver control metric observer instance
     */
    public void removeControlMetricsObserver(ControlMetricsObserver cmObserver) {
        controlMetricsObservers.remove(cmObserver);
    }
}
