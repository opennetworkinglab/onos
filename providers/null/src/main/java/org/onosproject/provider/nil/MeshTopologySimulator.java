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
package org.onosproject.provider.nil;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Full mesh topology with hosts at each device.
 */
public class MeshTopologySimulator extends TopologySimulator {

    @Override
    protected void processTopoShape(String shape) {
        super.processTopoShape(shape);
        deviceCount = (topoShape.length > 1) ? Integer.parseInt(topoShape[1]) : deviceCount;
        hostCount = (topoShape.length > 2) ? Integer.parseInt(topoShape[2]) : hostCount;
    }

    @Override
    public void setUpTopology() {
        checkArgument(deviceCount > 1, "There must be at least 2 devices");
        checkArgument(hostCount > 0, "There must be at least 1 host");
        super.setUpTopology();
    }

    @Override
    protected void createLinks() {
        for (int i = 0, n = deviceCount - 1; i < n; i++) {
            for (int j = 0; j < n - i; j++) {
                createLink(i, i + j + 1, i + j + 1, i + 1);
            }
        }
    }

    @Override
    protected void createHosts() {
        for (int i = 0, n = deviceCount; i < n; i++) {
            createHosts(deviceIds.get(i), deviceCount - 1);
        }
    }

}