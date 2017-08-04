/*
 * Copyright 2016-present Open Networking Foundation
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
 * Rectangular grid topology with hosts at each device.
 */
public class GridTopologySimulator extends TopologySimulator {

    private int cols;
    private int rows;

    @Override
    protected void processTopoShape(String shape) {
        super.processTopoShape(shape);
        rows = topoShape.length > 1 ? Integer.parseInt(topoShape[1]) : 10;
        cols = topoShape.length > 2 ? Integer.parseInt(topoShape[2]) : rows;
        hostCount = topoShape.length > 3 ? Integer.parseInt(topoShape[3]) : 1;
        infrastructurePorts = 4;
        deviceCount = rows * cols;
    }

    @Override
    public void setUpTopology() {
        checkArgument(rows > 1, "There must be at least 2 rows");
        checkArgument(cols > 1, "There must be at least 2 columns");
        super.setUpTopology();
    }

    @Override
    protected void createLinks() {
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int i = r * cols + c;
                if (c < cols - 1) {
                    createLink(i, i + 1, 3, 1);
                }
                if (r < rows - 1) {
                    createLink(i, (r + 1) * cols + c, 4, 2);
                }
            }
        }
    }

    @Override
    protected void createHosts() {
        deviceIds.forEach(id -> createHosts(id, infrastructurePorts));
    }

}
