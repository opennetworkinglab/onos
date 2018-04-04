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
 * Spine-leaf topology with hosts at the leaf devices.
 */
public class SpineLeafTopologySimulator extends TopologySimulator {
    private static final int DEFAULT_SPINE_COUNT = 2;
    private static final int DEFAULT_LEAF_COUNT = 2;
    private static final int DEFAULT_HOST_PER_LEAF = 2;
    private int spineCount = DEFAULT_SPINE_COUNT;
    private int leafCount = DEFAULT_LEAF_COUNT;
    private int hostPerLeaf = DEFAULT_HOST_PER_LEAF;

    @Override
    protected void processTopoShape(String shape) {
        super.processTopoShape(shape);
        // topology shape: spineleaf,[spine count],[leaf count],[host per leaf]

        if (topoShape.length > 1) {
            spineCount = Integer.parseInt(topoShape[1]);
        }

        if (topoShape.length > 2) {
            leafCount = Integer.parseInt(topoShape[2]);
        }

        if (topoShape.length > 3) {
            hostPerLeaf = Integer.parseInt(topoShape[3]);
        }
    }

    @Override
    public void setUpTopology() {
        checkArgument(spineCount > 0, "There must be at least one spine");
        checkArgument(leafCount > 0, "There must be at least one leaf");

        deviceCount = spineCount + leafCount;
        hostCount = hostPerLeaf;
        log.info("Setup Spine-Leaf topology with {} spine(s), {} leaf(s), and {} host per leaf",
                 spineCount, leafCount, hostPerLeaf);

        super.setUpTopology();
    }

    @Override
    protected void createLinks() {
        for (int spineIndex = 0; spineIndex < spineCount; spineIndex++) {
            for (int leafIndex = spineCount; leafIndex < spineCount + leafCount; leafIndex++) {
                // Nth port of spine connect to Nth leaf, vice versa
                createLink(spineIndex, leafIndex, leafIndex - spineCount + 1, spineIndex + 1);
            }
        }
    }

    @Override
    protected void createHosts() {
        for (int i = spineCount; i < spineCount + leafCount; i++) {
            createHosts(deviceIds.get(i), spineCount);
        }
    }

}
