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
 * Spine-leaf topology with hosts at the leaf devices.
 */
public class SpineLeafTopologySimulator extends TopologySimulator {

    @Override
    protected void processTopoShape(String shape) {
        super.processTopoShape(shape);
        // FIXME: implement this
    }

    @Override
    public void setUpTopology() {
        // checkArgument(FIXME, "There must be at least one spine tier");
        super.setUpTopology();
    }

    @Override
    protected void createLinks() {
    }

    @Override
    protected void createHosts() {
    }

}
