/*
 * Copyright 2016 Open Networking Foundation
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

package org.onosproject.drivers.oplink;

import org.onlab.util.GuavaCollectors;
import org.onosproject.net.OchSignal;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.LambdaQuery;
import org.onosproject.net.driver.AbstractHandlerBehaviour;

import java.util.Set;
import java.util.stream.IntStream;

import static org.onlab.util.Spectrum.CENTER_FREQUENCY;
import static org.onosproject.drivers.oplink.OplinkOpticalUtility.CHANNEL_SPACING;
import static org.onosproject.drivers.oplink.OplinkOpticalUtility.STOP_CENTER_FREQ;
import static org.onosproject.drivers.oplink.OplinkOpticalUtility.START_CENTER_FREQ;

/**
 * Lambda query implementation for Oplink netconf devices in C band.
 *
 * 96 lambdas with 50GHz width (fixed grid).
 *
 * Channel id: Nominal central frequency = 193100 GHz + spacingMultiplier * channelSpacing).
 * Channel (-35 to 60): starting from 191350 GHz to 196100 GHz, Increment by 50GHz.
 */
public class OplinkOpticalLambdaQuery extends AbstractHandlerBehaviour implements LambdaQuery {

    @SuppressWarnings("MathRoundIntLong")
    private static final int MIN_CHANNEL = (int) Math.round(
            (START_CENTER_FREQ.subtract(CENTER_FREQUENCY).asHz() / CHANNEL_SPACING.frequency().asHz()));
    @SuppressWarnings("MathRoundIntLong")
    private static final int MAX_CHANNEL = (int) Math.round(
            (STOP_CENTER_FREQ.subtract(CENTER_FREQUENCY).asHz() / CHANNEL_SPACING.frequency().asHz()));

    @Override
    public Set<OchSignal> queryLambdas(PortNumber port) {
        return IntStream.rangeClosed(MIN_CHANNEL, MAX_CHANNEL)
                .mapToObj(x -> OchSignal.newDwdmSlot(CHANNEL_SPACING, x))
                .collect(GuavaCollectors.toImmutableSet());
    }
}
