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
 * Simple triangle topology with multiple links between same devices.
 */
public class AggLinkTopologySimulator extends CentipedeTopologySimulator {

    @Override
    protected void processTopoShape(String shape) {
        super.processTopoShape(shape);
        infrastructurePorts = 2 * deviceCount - 1;
    }

    @Override
    public void setUpTopology() {
        checkArgument(deviceCount > 2, "There must be at least 3 devices");
        super.setUpTopology();
    }

    @Override
    protected void createLinks() {
        int srcPortOffset = deviceCount + 1;
        for (int i = 0, n = deviceCount; i < n; i++) {
            int dstPortOffset = 1;
            for (int j = 0; j <= i; j++) {
                createLink(i, (i + 1) % n, srcPortOffset + j, dstPortOffset + j);
            }
            srcPortOffset = dstPortOffset + i + 1;
        }
    }

}
