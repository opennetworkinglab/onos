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
package org.onosproject.driver.optical.query;

import java.util.Set;
import java.util.stream.IntStream;

import org.onlab.util.GuavaCollectors;
import org.onosproject.net.ChannelSpacing;
import org.onosproject.net.OchSignal;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.LambdaQuery;
import org.onosproject.net.driver.AbstractHandlerBehaviour;

/**
 * Lambda query implementation for Oplink ROADM.
 *
 * An Oplink ROADM port exposes OMSn resources: 88 lambdas with 50GHz width (fixed grid).
 *
 * Channel id: Nominal central frequency = 193.1 THz + spacingMultiplier * channelSpacing).
 * Channel (-28 to 59): starting from 191.7 THz to 196.05 THz, Increment by 50GHz.
 */

public class OplinkRoadmLambdaQuery extends AbstractHandlerBehaviour implements LambdaQuery {

    private static final int MIN_CHANNEL = -28;
    private static final int MAX_CHANNEL = 59;

    @Override
    public Set<OchSignal> queryLambdas(PortNumber port) {
        return IntStream.rangeClosed(MIN_CHANNEL, MAX_CHANNEL)
                .mapToObj(x -> OchSignal.newDwdmSlot(ChannelSpacing.CHL_50GHZ, x))
                .collect(GuavaCollectors.toImmutableSet());
    }
}
