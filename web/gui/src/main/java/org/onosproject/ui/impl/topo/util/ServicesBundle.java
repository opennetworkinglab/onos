/*
 *  Copyright 2016-present Open Networking Laboratory
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.onosproject.ui.impl.topo.util;

import org.onosproject.incubator.net.PortStatisticsService;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.host.HostService;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.link.LinkService;
import org.onosproject.net.statistic.StatisticService;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A bundle of services that the topology view requires to get its job done.
 */
public class ServicesBundle {

    private final IntentService intentService;
    private final DeviceService deviceService;
    private final HostService hostService;
    private final LinkService linkService;
    private final FlowRuleService flowService;
    private final StatisticService flowStatsService;
    private final PortStatisticsService portStatsService;

    /**
     * Creates the services bundle.
     *
     * @param intentService     intent service reference
     * @param deviceService     device service reference
     * @param hostService       host service reference
     * @param linkService       link service reference
     * @param flowService       flow service reference
     * @param flowStatsService  flow statistics service reference
     * @param portStatsService  port statistics service reference
     */
    public ServicesBundle(IntentService intentService,
                          DeviceService deviceService,
                          HostService hostService,
                          LinkService linkService,
                          FlowRuleService flowService,
                          StatisticService flowStatsService,
                          PortStatisticsService portStatsService) {
        this.intentService = checkNotNull(intentService);
        this.deviceService = checkNotNull(deviceService);
        this.hostService = checkNotNull(hostService);
        this.linkService = checkNotNull(linkService);
        this.flowService = checkNotNull(flowService);
        this.flowStatsService = checkNotNull(flowStatsService);
        this.portStatsService = checkNotNull(portStatsService);
    }

    /**
     * Returns a reference to the intent service.
     *
     * @return intent service reference
     */
    public IntentService intentService() {
        return intentService;
    }

    /**
     * Returns a reference to the device service.
     *
     * @return device service reference
     */
    public DeviceService deviceService() {
        return deviceService;
    }

    /**
     * Returns a reference to the host service.
     *
     * @return host service reference
     */
    public HostService hostService() {
        return hostService;
    }

    /**
     * Returns a reference to the link service.
     *
     * @return link service reference
     */
    public LinkService linkService() {
        return linkService;
    }

    /**
     * Returns a reference to the flow rule service.
     *
     * @return flow service reference
     */
    public FlowRuleService flowService() {
        return flowService;
    }

    /**
     * Returns a reference to the flow statistics service.
     *
     * @return flow statistics service reference
     */
    public StatisticService flowStatsService() {
        return flowStatsService;
    }

    /**
     * Returns a reference to the port statistics service.
     *
     * @return port statistics service reference
     */
    public PortStatisticsService portStatsService() {
        return portStatsService;
    }
}
