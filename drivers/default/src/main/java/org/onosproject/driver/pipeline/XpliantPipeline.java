/*
 * Copyright 2017 Open Networking Foundation
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

package org.onosproject.driver.pipeline;

import org.onosproject.net.behaviour.PipelinerContext;
import org.onosproject.net.flow.FlowRule;
import org.onosproject.net.flowobjective.ForwardingObjective;
import org.onosproject.driver.pipeline.ofdpa.Ofdpa3Pipeline;
import java.util.Collection;

import static org.onosproject.driver.pipeline.ofdpa.OfdpaPipelineUtility.ACL_TABLE;

public class XpliantPipeline extends Ofdpa3Pipeline {

    @Override
    protected void initDriverId() {
        driverId = coreService.registerApplication(
                "org.onosproject.driver.XpliantPipeline");
    }

    @Override
    protected void initGroupHander(PipelinerContext context) {
        // Terminate internal references
        // We are terminating the references here
        // because when the device is offline the apps
        // are still sending flowobjectives
        if (groupHandler != null) {
            groupHandler.terminate();
        }
        groupHandler = new XpliantGroupHandler();
        groupHandler.init(deviceId, context);
    }

    @Override
    protected Collection<FlowRule> processEthTypeSpecific(ForwardingObjective fwd) {
        return processEthTypeSpecificInternal(fwd, true, ACL_TABLE);
    }

    @Override
    public boolean requireMplsPop() {
        return false;
    }

    @Override
    public boolean requireMplsBosMatch() {
        return false;
    }

    @Override
    public boolean requireMplsTtlModification() {
        return false;
    }

    @Override
    protected boolean requireEthType() {
        return false;
    }
}
