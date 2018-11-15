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
 *
 * This work was partially supported by EC H2020 project METRO-HAUL (761727).
 */

package org.onosproject.drivers.odtn.openconfig;

import org.onosproject.driver.optical.query.CBandLambdaQuery;
import org.onosproject.net.ChannelSpacing;
import org.onosproject.net.OchSignal;
import org.onosproject.net.PortNumber;
import org.slf4j.Logger;

import java.util.Set;

import static org.slf4j.LoggerFactory.getLogger;


/**
 * Lambda Query for OpenConfig based terminal devices.
 */
public class TerminalDeviceLambdaQuery extends CBandLambdaQuery {

    protected static final Logger log = getLogger(TerminalDeviceLambdaQuery.class);

    @Override
    public Set<OchSignal> queryLambdas(PortNumber port) {
        log.debug("OPENCONFIG: queried lambdas for port {}", port);

        // Profile 1
        channelSpacing = ChannelSpacing.CHL_50GHZ;
        lambdaCount = 96;
        slotGranularity = 4;
        return super.queryLambdas(port);

        // Another option, commented until best option for transceiver tunability deduction is chosen Profile 2
        // short lambdaCount = 96;
        // // fixed grid lambdas of 50GHz width
        // return IntStream.rangeClosed(1, lambdaCount)
        //         .mapToObj(x -> OchSignal.newDwdmSlot(ChannelSpacing.CHL_50GHZ, x))
        //         .collect(Collectors.toSet());
    }
}
