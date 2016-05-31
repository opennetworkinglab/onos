/*
 * Copyright 2016-present Open Networking Laboratory
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

package org.onosproject.drivers.bmv2;

import org.onosproject.driver.pipeline.DefaultSingleTablePipeline;
import org.onosproject.net.DeviceId;
import org.onosproject.net.behaviour.NextGroup;
import org.onosproject.net.behaviour.Pipeliner;
import org.onosproject.net.behaviour.PipelinerContext;
import org.onosproject.net.driver.AbstractHandlerBehaviour;
import org.onosproject.net.flowobjective.FilteringObjective;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.net.flowobjective.NextObjective;

import java.util.List;

/**
 * Pipeliner device behaviour implementation for BMv2.
 */
public class Bmv2Pipeliner extends AbstractHandlerBehaviour implements Pipeliner {

    private Pipeliner pipeliner;

    @Override
    public void init(DeviceId deviceId, PipelinerContext context) {
        // TODO: get multi-table pipeliner dynamically based on BMv2 device running model (hard).
        // Right now we are able to map flow objectives only in the first table of the pipeline.
        pipeliner = new DefaultSingleTablePipeline();
        pipeliner.init(deviceId, context);
    }

    @Override
    public void filter(FilteringObjective filterObjective) {
        pipeliner.filter(filterObjective);
    }

    @Override
    public void forward(ForwardingObjective forwardObjective) {
        pipeliner.forward(forwardObjective);
    }

    @Override
    public void next(NextObjective nextObjective) {
        pipeliner.next(nextObjective);
    }

    @Override
    public List<String> getNextMappings(NextGroup nextGroup) {
        return pipeliner.getNextMappings(nextGroup);
    }
}
