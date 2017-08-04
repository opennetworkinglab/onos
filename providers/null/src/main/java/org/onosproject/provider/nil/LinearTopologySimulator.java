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

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Linear topology simulator.
 */
public class LinearTopologySimulator extends TopologySimulator {

    @Override
    protected void processTopoShape(String shape) {
        super.processTopoShape(shape);
        deviceCount = (topoShape.length == 1) ? deviceCount : Integer.parseInt(topoShape[1]);
    }

    @Override
    public void setUpTopology() {
        checkArgument(deviceCount > 1, "There must be at least 2 devices");

        prepareForDeviceEvents(deviceCount);
        createDevices();
        waitForDeviceEvents();

        createLinks();
        createHosts();
    }

    @Override
    protected void createLinks() {
        int portOffset = 1;
        for (int i = 0, n = deviceCount - 1; i < n; i++) {
            createLink(i, i + 1, portOffset, 1);
            portOffset = 2;
        }
    }

    @Override
    protected void createHosts() {
        createHosts(deviceIds.get(0), infrastructurePorts);
        createHosts(deviceIds.get(deviceCount - 1), infrastructurePorts);
    }

}
