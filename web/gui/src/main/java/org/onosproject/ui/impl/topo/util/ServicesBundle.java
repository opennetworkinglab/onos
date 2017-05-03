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

import org.onlab.osgi.ServiceDirectory;
import org.onosproject.cluster.ClusterService;
import org.onosproject.incubator.net.PortStatisticsService;
import org.onosproject.incubator.net.tunnel.TunnelService;
import org.onosproject.mastership.MastershipAdminService;
import org.onosproject.mastership.MastershipService;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.flow.FlowRuleService;
import org.onosproject.net.host.HostService;
import org.onosproject.net.intent.IntentService;
import org.onosproject.net.link.LinkService;
import org.onosproject.net.statistic.StatisticService;
import org.onosproject.net.topology.TopologyService;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A bundle of services that the topology view(s) require to get the job done.
 */
public class ServicesBundle {

    private ClusterService clusterService;

    private TopologyService topologyService;
    private DeviceService deviceService;
    private HostService hostService;
    private LinkService linkService;
    private TunnelService tunnelService;

    private MastershipService mastershipService;
    private MastershipAdminService mastershipAdminService;
    private IntentService intentService;
    private FlowRuleService flowService;
    private StatisticService flowStatsService;
    private PortStatisticsService portStatsService;


    /**
     * Creates the services bundle, from the given directly.
     *
     * @param directory service directory
     */
    public ServicesBundle(ServiceDirectory directory) {
        checkNotNull(directory, "Directory cannot be null");

        clusterService = directory.get(ClusterService.class);

        topologyService = directory.get(TopologyService.class);
        deviceService = directory.get(DeviceService.class);
        hostService = directory.get(HostService.class);
        linkService = directory.get(LinkService.class);
        tunnelService = directory.get(TunnelService.class);

        mastershipService = directory.get(MastershipService.class);
        mastershipAdminService = directory.get(MastershipAdminService.class);
        intentService = directory.get(IntentService.class);
        flowService = directory.get(FlowRuleService.class);
        flowStatsService = directory.get(StatisticService.class);
        portStatsService = directory.get(PortStatisticsService.class);
    }

    /**
     * Returns a reference to the cluster service.
     *
     * @return cluster service reference
     */
    public ClusterService cluster() {
        return clusterService;
    }

    /**
     * Returns a reference to the topology service.
     *
     * @return topology service reference
     */
    public TopologyService topology() {
        return topologyService;
    }

    /**
     * Returns a reference to the device service.
     *
     * @return device service reference
     */
    public DeviceService device() {
        return deviceService;
    }

    /**
     * Returns a reference to the host service.
     *
     * @return host service reference
     */
    public HostService host() {
        return hostService;
    }

    /**
     * Returns a reference to the link service.
     *
     * @return link service reference
     */
    public LinkService link() {
        return linkService;
    }

    /**
     * Returns a reference to the tunnel service.
     *
     * @return tunnel service reference
     */
    public TunnelService tunnel() {
        return tunnelService;
    }

    /**
     * Returns a reference to the mastership service.
     *
     * @return mastership service reference
     */
    public MastershipService mastership() {
        return mastershipService;
    }

    /**
     * Returns a reference to the mastership admin service.
     *
     * @return mastership admin service reference
     */
    public MastershipAdminService mastershipAdmin() {
        return mastershipAdminService;
    }

    /**
     * Returns a reference to the intent service.
     *
     * @return intent service reference
     */
    public IntentService intent() {
        return intentService;
    }

    /**
     * Returns a reference to the flow rule service.
     *
     * @return flow service reference
     */
    public FlowRuleService flow() {
        return flowService;
    }

    /**
     * Returns a reference to the flow statistics service.
     *
     * @return flow statistics service reference
     */
    public StatisticService flowStats() {
        return flowStatsService;
    }

    /**
     * Returns a reference to the port statistics service.
     *
     * @return port statistics service reference
     */
    public PortStatisticsService portStats() {
        return portStatsService;
    }
}
