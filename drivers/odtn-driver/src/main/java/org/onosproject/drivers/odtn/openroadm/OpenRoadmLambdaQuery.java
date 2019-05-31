/*
 * Copyright 2018-present Open Networking Foundation
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

 * This work was partially supported by EC H2020 project METRO-HAUL (761727).
 */
package org.onosproject.drivers.odtn.openroadm;

import java.util.Set;
import org.onosproject.net.ChannelSpacing;
import org.onosproject.net.OchSignal;
import org.onosproject.net.PortNumber;

/**
 * Lambda Query for OpenROADM ports (see Optical model).
 *
 *  This needs to be addapted to a given use case.
 */
public class OpenRoadmLambdaQuery extends OpenRoadmCBandLambdaQuery {

    @Override
    public Set<OchSignal> queryLambdas(PortNumber port) {
        channelSpacing = ChannelSpacing.CHL_50GHZ;
        lambdaCount = 32;
        slotGranularity = 4;
        Set<OchSignal> set = super.queryLambdas(port);
        return set;
    }
}
