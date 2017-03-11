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
package org.onosproject.drivers.lumentum;

import org.onlab.util.Frequency;
import org.onlab.util.GuavaCollectors;
import org.onlab.util.Spectrum;
import org.onosproject.net.OchSignal;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.LambdaQuery;
import org.onosproject.net.driver.AbstractHandlerBehaviour;

import java.util.Set;
import java.util.stream.IntStream;

/**
 * Implementation of lambda query interface for Lumentum SDN ROADMs.
 *
 * Device supports 96 wavelengths of 50 GHz, between center frequencies 191.350 THz and 196.075 THz.
 */
public class LumentumSdnRoadmLambdaQuery extends AbstractHandlerBehaviour implements LambdaQuery {
    private static final int LAMBDA_COUNT = 96;

    @Override
    public Set<OchSignal> queryLambdas(PortNumber port) {
        int startMultiplier = (int) (LumentumSnmpDevice.START_CENTER_FREQ.subtract(Spectrum.CENTER_FREQUENCY).asHz()
                / Frequency.ofGHz(50).asHz());

        return IntStream.range(0, LAMBDA_COUNT)
                .mapToObj(x -> new OchSignal(LumentumSnmpDevice.GRID_TYPE,
                        LumentumSnmpDevice.CHANNEL_SPACING,
                        startMultiplier + x,
                        4))
                .collect(GuavaCollectors.toImmutableSet());
    }
}
