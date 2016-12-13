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
/**
 * Chordal topology simulator.
 */
public class ChordalTopologySimulator extends TopologySimulator {

    @Override
    protected void processTopoShape(String shape) {
        super.processTopoShape(shape);
        deviceCount = (topoShape.length > 1) ? Integer.parseInt(topoShape[1]) : deviceCount;
        hostCount = (topoShape.length > 2) ? Integer.parseInt(topoShape[2]) : hostCount;
    }

    @Override
    protected void createLinks() {
        for (int i = 0, n = deviceCount; i < n; i++) {
            for (int j = 1; j <= n / 2; j = 2 * j) {
                createLink(i % deviceCount, (i + j) % deviceCount,
                        (i + j) % deviceCount, i % deviceCount);
            }
        }
    }

    @Override
    protected void createHosts() {
        for (int i = 0; i < deviceCount; i++) {
            createHosts(deviceIds.get(i), hostCount);
        }
    }

}