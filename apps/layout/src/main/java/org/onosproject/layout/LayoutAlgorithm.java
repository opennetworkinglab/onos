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

package org.onosproject.layout;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import org.onosproject.net.Device;
import org.onosproject.net.DeviceId;
import org.onosproject.net.ElementId;
import org.onosproject.net.Host;
import org.onosproject.net.HostId;
import org.onosproject.net.config.NetworkConfigService;
import org.onosproject.net.config.basics.BasicDeviceConfig;
import org.onosproject.net.config.basics.BasicHostConfig;
import org.onosproject.net.device.DeviceService;
import org.onosproject.net.host.HostService;
import org.onosproject.net.link.LinkService;

import java.util.Collection;

/**
 * Represents a topology layout algorithm.
 */
public abstract class LayoutAlgorithm {

    public static final String SPINE = "spine";
    public static final String AGGREGATION = "aggregation";
    public static final String LEAF = "leaf";
    public static final String ACCESS = "access";
    public static final String GATEWAY = "gateway";
    public static final String COMPUTE = "compute";

    protected DeviceService deviceService;
    protected HostService hostService;
    protected LinkService linkService;
    protected NetworkConfigService netConfigService;

    protected ListMultimap<String, DeviceId> deviceCategories = ArrayListMultimap.create();
    protected ListMultimap<String, HostId> hostCategories = ArrayListMultimap.create();


    /**
     * Initializes layout algorithm for operating on device and host inventory.
     *
     * @param deviceService        device service
     * @param hostService          host service
     * @param linkService          link service
     * @param networkConfigService net config service
     */
    protected void init(DeviceService deviceService,
                        HostService hostService,
                        LinkService linkService,
                        NetworkConfigService networkConfigService) {
        this.deviceService = deviceService;
        this.hostService = hostService;
        this.linkService = linkService;
        this.netConfigService = networkConfigService;
    }

    /**
     * Places the specified device on the layout grid.
     *
     * @param id device identifier
     * @param x grid X
     * @param y grid Y
     */
    protected void place(DeviceId id, double x, double y) {
        netConfigService.addConfig(id, BasicDeviceConfig.class)
                .gridX(x).gridY(y).locType("grid").apply();
    }

    /**
     * Places the specified device on the layout grid.
     *
     * @param id host identifier
     * @param x grid X
     * @param y grid Y
     */
    protected void place(HostId id, double x, double y) {
        netConfigService.addConfig(id, BasicHostConfig.class)
                .gridX(x).gridY(y).locType("grid").apply();
    }

    /**
     * Computes grid coordinate for the i-th element of n-elements in a tier
     * using a default gap of 400.
     *
     * @param i element index
     * @param n number of elements
     * @return grid Y
     */
    protected double c(int i, int n) {
        return c(i, n, 400);
    }

    /**
     * Computes grid coordinate for the i-th element of n-elements in a tier.
     *
     * @param i   element index
     * @param n   number of elements
     * @param gap gap width
     * @return grid Y
     */
    protected double c(int i, int n, double gap) {
        return c(i, n, gap, 0);
    }

    /**
     * Computes grid coordinate for the i-th element of n-elements in a tier.
     *
     * @param i      element index
     * @param n      number of elements
     * @param gap    gap width
     * @param offset additional Y offset
     * @return grid Y
     */
    protected double c(int i, int n, double gap, double offset) {
        return gap * (i - 1) - (gap * (n - 1)) / 2 + offset;
    }

    /**
     * Places the specified collection of hosts (all presumably connected to
     * the same network device) in a block.
     *
     * @param hosts       hosts to place
     * @param gridX       grid X of the top of the block
     * @param gridY       grid Y of the center of the block
     * @param hostsPerRow number of hosts in a 'row'
     * @param rowGap      gap width between rows
     * @param colGap      gap width between columns
     */
    protected void placeHostBlock(Collection<HostId> hosts,
                                  double gridX, double gridY, int hostsPerRow,
                                  double rowGap, double colGap) {
        double yStep = rowGap / hostsPerRow;
        double y = gridY;
        double x = gridX - (colGap * (hostsPerRow - 1)) / 2;
        int i = 1;

        for (HostId id : hosts) {
            place(id, x, y);
            if ((i % hostsPerRow) == 0) {
                x = gridX - (colGap * (hostsPerRow - 1)) / 2;
            } else {
                x += colGap;
                y += yStep;
            }
            i++;
        }
    }

    /**
     * Applies device and host classifications.
     */
    public void classify() {
        deviceService.getDevices().forEach(this::classify);
        hostService.getHosts().forEach(this::classify);
    }

    /**
     * Classifies the specified device.
     *
     * @param device device to be classified
     * @return true if classified
     */
    protected boolean classify(Device device) {
        BasicDeviceConfig cfg = netConfigService.getConfig(device.id(), BasicDeviceConfig.class);
        if (cfg != null && !cfg.roles().isEmpty()) {
            cfg.roles().forEach(r -> deviceCategories.put(r, device.id()));
            return true;
        }
        return false;
    }

    /**
     * Classifies the specified host.
     *
     * @param host host to be classified
     * @return true if classified
     */
    protected boolean classify(Host host) {
        BasicHostConfig cfg = netConfigService.getConfig(host.id(), BasicHostConfig.class);
        if (cfg != null && !cfg.roles().isEmpty()) {
            cfg.roles().forEach(r -> hostCategories.put(r, host.id()));
            return true;
        }
        return false;
    }


    /**
     * Applies the specified layout algorithm.
     */
    abstract void apply();

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("deviceCategories", count(deviceCategories))
                .add("hostCategories", count(hostCategories))
                .toString();
    }

    private String count(ListMultimap<String, ? extends ElementId> categories) {
        StringBuilder sb = new StringBuilder("[ ");
        categories.keySet().forEach(k -> sb.append(k).append("=").append(categories.get(k).size()).append(" "));
        return sb.append("]").toString();
    }

}
