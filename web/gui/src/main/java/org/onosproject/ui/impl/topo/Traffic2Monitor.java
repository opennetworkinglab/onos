/*
 * Copyright 2017-present Open Networking Laboratory
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
 *
 */

package org.onosproject.ui.impl.topo;

import org.onosproject.ui.impl.TrafficMonitorBase;
import org.onosproject.ui.impl.topo.util.ServicesBundle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Encapsulates the behavior of monitoring specific traffic patterns in the
 * Topology-2 view.
 */
public class Traffic2Monitor extends TrafficMonitorBase {

    private static final Logger log =
            LoggerFactory.getLogger(Traffic2Monitor.class);

    /**
     * Constructs a traffic monitor.
     *
     * @param trafficPeriod traffic task period in ms
     * @param servicesBundle bundle of services
     */
    public Traffic2Monitor(long trafficPeriod, ServicesBundle servicesBundle) {
        super(trafficPeriod, servicesBundle);
    }

    @Override
    protected void sendAllFlowTraffic() {
        // TODO
    }

    @Override
    protected void sendAllPortTrafficBits() {
        // TODO
    }

    @Override
    protected void sendAllPortTrafficPackets() {
        // TODO
    }

    @Override
    protected void sendDeviceLinkFlows() {
        // NOTE: currently this monitor holds no state - nothing to do
    }

    @Override
    protected void sendSelectedIntentTraffic() {
        // NOTE: currently this monitor holds no state - nothing to do
    }

    @Override
    protected void sendClearHighlights() {
        // TODO
    }

    @Override
    protected void clearSelection() {
        // NOTE: currently this monitor holds no state - nothing to do
    }
}
