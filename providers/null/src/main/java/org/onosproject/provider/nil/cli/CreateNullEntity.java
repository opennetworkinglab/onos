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
package org.onosproject.provider.nil.cli;

import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Longs;
import org.onlab.util.Tools;
import org.onosproject.cli.AbstractShellCommand;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.config.basics.BasicElementConfig;
import org.onosproject.net.edge.EdgePortService;
import org.onosproject.net.host.HostService;
import org.onosproject.provider.nil.CustomTopologySimulator;
import org.onosproject.provider.nil.TopologySimulator;

import java.util.List;
import java.util.Objects;

/**
 * Base command for adding simulated entities to the custom topology simulation.
 */
public abstract class CreateNullEntity extends AbstractShellCommand {

    protected static final String GEO = "geo";
    protected static final String GRID = "grid";

    protected static final int MAX_EDGE_PORT_TRIES = 5;

    /**
     * Validates that the simulator is custom.
     *
     * @param simulator topology simulator
     * @return true if valid
     */
    protected boolean validateSimulator(TopologySimulator simulator) {
        if (!(simulator instanceof CustomTopologySimulator)) {
            error("Custom topology simulator is not active.");
            return false;
        }
        return true;
    }

    /**
     * Validates that the location type is valid.
     *
     * @param locType location type
     * @return true if valid
     */
    protected boolean validateLocType(String locType) {
        if (!(GEO.equals(locType) || GRID.equals(locType))) {
            error("locType must be 'geo' or 'grid'.");
            return false;
        }
        return true;
    }

    /**
     * Sets the UI location coordinates appropriately.
     *
     * @param cfg     element config
     * @param locType location type
     * @param latOrY  latitude or Y grid
     * @param longOrX longitude or X grid
     */
    protected void setUiCoordinates(BasicElementConfig cfg,
                                    String locType, Double latOrY, Double longOrX) {
        if (latOrY != null && longOrX != null) {
            cfg.locType(locType);
            if (GEO.equals(locType)) {
                cfg.latitude(latOrY).longitude(longOrX);
            } else {
                cfg.gridX(longOrX).gridY(latOrY);
            }
        }
        cfg.apply();
    }

    /**
     * Finds an available connect point among edge ports of the specified device.
     *
     * @param deviceId   device identifier
     * @param otherPoint optional other point to be excluded from search
     * @return connect point available for link or host attachment
     */
    protected ConnectPoint findAvailablePort(DeviceId deviceId, ConnectPoint otherPoint) {
        HostService hs = get(HostService.class);
        return findAvailablePorts(deviceId).stream()
                .filter(p -> !Objects.equals(p, otherPoint) && hs.getConnectedHosts(p).isEmpty())
                .findFirst().orElse(null);
    }

    /**
     * Finds an available connect points among edge ports of the specified device.
     *
     * @param deviceId device identifier
     * @return list of connect points available for link or host attachments
     */
    protected List<ConnectPoint> findAvailablePorts(DeviceId deviceId) {
        EdgePortService eps = get(EdgePortService.class);

        // As there may be a slight delay in edge service getting updated, retry a few times
        for (int i = 0; i < MAX_EDGE_PORT_TRIES; i++) {
            List<ConnectPoint> points = ImmutableList
                    .sortedCopyOf((l, r) -> Longs.compare(l.port().toLong(), r.port().toLong()),
                                  eps.getEdgePoints(deviceId));
            if (!points.isEmpty()) {
                return points;
            }
            Tools.delay(100);
        }
        return ImmutableList.of();
    }

}
