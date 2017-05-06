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

package org.onosproject.driver.pipeline.ofdpa;

import org.onosproject.net.behaviour.PipelinerContext;

/**
 * Driver for software switch emulation of the OFDPA pipeline.
 * The software switch is the OVS OF 1.3 switch (version 2.5 or later).
 */
public class OvsOfdpa2Pipeline extends CpqdOfdpa2Pipeline {

    @Override
    protected void initDriverId() {
        driverId = coreService.registerApplication(
                "org.onosproject.driver.OvsOfdpa2Pipeline");
    }

    @Override
    protected void initGroupHander(PipelinerContext context) {
        groupHandler = new OvsOfdpa2GroupHandler();
        groupHandler.init(deviceId, context);
    }

    @Override
    protected boolean supportCopyTtl() {
        return false;
    }

    @Override
    protected boolean supportTaggedMpls() {
        return true;
    }

    @Override
    protected boolean supportPuntGroup() {
        return true;
    }
}
