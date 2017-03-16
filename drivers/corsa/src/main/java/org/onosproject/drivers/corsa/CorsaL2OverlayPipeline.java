/*
 * Copyright 2017-present Open Networking Laboratory
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

package org.onosproject.drivers.corsa;

import org.onosproject.driver.pipeline.DefaultSingleTablePipeline;
import org.onosproject.net.behaviour.Pipeliner;
import org.onosproject.net.flow.DefaultTrafficTreatment;
import org.onosproject.net.flow.TrafficTreatment;
import org.onosproject.net.flowobjective.DefaultForwardingObjective;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.slf4j.Logger;

import static org.onosproject.net.flowobjective.ForwardingObjective.Flag.VERSATILE;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Abstraction of the Corsa L2 Overlay pipeline.
 * This pipeline is single table with groups support.
 */
public class CorsaL2OverlayPipeline extends DefaultSingleTablePipeline implements Pipeliner {

    private final Logger log = getLogger(getClass());

    @Override
    public void forward(ForwardingObjective fwd) {
        // The Corsa devices don't support the clear deferred actions
        // so for now we have to filter this instruction for the fwd
        // objectives sent by the Packet Manager before to create the
        // flow rule
        if (fwd.flag() == VERSATILE && fwd.treatment().clearedDeferred()) {
            // First we create a new treatment without the unsupported action
            TrafficTreatment.Builder noClearTreatment = DefaultTrafficTreatment.builder();
            fwd.treatment().allInstructions().forEach(noClearTreatment::add);
            // Then we create a new forwarding objective without the unsupported action
            ForwardingObjective.Builder noClearFwd = DefaultForwardingObjective.builder(fwd);
            noClearFwd.withTreatment(noClearTreatment.build());
            // According to the operation we substitute fwd with the correct objective
            switch (fwd.op()) {
                case ADD:
                    fwd = noClearFwd.add(fwd.context().orElse(null));
                    break;
                case REMOVE:
                    fwd = noClearFwd.remove(fwd.context().orElse(null));
                    break;
                default:
                    log.warn("Unknown operation {}", fwd.op());
                    return;
            }
        }
        // Finally we send to the DefaultSingleTablePipeline for the real processing
        super.forward(fwd);
    }

}
