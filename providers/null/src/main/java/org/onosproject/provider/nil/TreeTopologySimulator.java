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
 * Tree topology with hosts at the leaf devices.
 */
public class TreeTopologySimulator extends TopologySimulator {

    private int[] tierMultiplier;
    private int[] tierDeviceCount;
    private int[] tierOffset;

    @Override
    protected void processTopoShape(String shape) {
        super.processTopoShape(shape);
        tierOffset = new int[topoShape.length];
        tierMultiplier = new int[topoShape.length];
        tierDeviceCount = new int[topoShape.length];

        deviceCount = 1;

        tierOffset[0] = 0;
        tierMultiplier[0] = 1;
        tierDeviceCount[0] = deviceCount;

        for (int i = 1; i < topoShape.length; i++) {
            tierOffset[i] = deviceCount;
            tierMultiplier[i] = Integer.parseInt(topoShape[i]);
            tierDeviceCount[i] = tierDeviceCount[i - 1] * tierMultiplier[i];
            deviceCount = deviceCount + tierDeviceCount[i];
        }
    }

    @Override
    public void setUpTopology() {
        checkArgument(tierDeviceCount.length > 0, "There must be at least one tree tier");
        super.setUpTopology();
    }

    @Override
    protected void createLinks() {
        int portOffset = 1;
        for (int t = 1; t < tierOffset.length; t++) {
            int child = tierOffset[t];
            for (int parent = tierOffset[t - 1]; parent < tierOffset[t]; parent++) {
                for (int i = 0; i < tierMultiplier[t]; i++) {
                    createLink(parent, child, i + portOffset, 1);
                    child++;
                }
            }
            portOffset = 2; // beyond first tier, allow for up-links
        }
    }

    @Override
    protected void createHosts() {
        for (int i = tierOffset[tierOffset.length - 1]; i < deviceCount; i++) {
            createHosts(deviceIds.get(i), 2);
        }
    }
}
