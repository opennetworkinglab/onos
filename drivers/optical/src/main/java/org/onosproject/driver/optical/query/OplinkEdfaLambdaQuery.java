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

import org.onlab.util.Spectrum;
import org.onlab.util.GuavaCollectors;
import org.onosproject.net.ChannelSpacing;
import org.onosproject.net.GridType;
import org.onosproject.net.OchSignal;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.LambdaQuery;
import org.onosproject.net.driver.AbstractHandlerBehaviour;

/**
 * Lambda query implementation for Oplink EDFA.
 *
 * An Oplink EDFA port supports min bandwidth 6.25Hz (flex grid).
 * Usable optical spectrum range is C band, see {@link Spectrum} for spectrum definitions.
 */

public class OplinkEdfaLambdaQuery extends AbstractHandlerBehaviour implements LambdaQuery {

    // Wavelength range: 1528 - 1567 nm
    private long startSpacingMultiplier = Spectrum.C_BAND_MIN.subtract(Spectrum.CENTER_FREQUENCY).asHz() /
            ChannelSpacing.CHL_6P25GHZ.frequency().asHz();
    private long stopSpacingMultiplier = Spectrum.C_BAND_MAX.subtract(Spectrum.CENTER_FREQUENCY).asHz() /
            ChannelSpacing.CHL_6P25GHZ.frequency().asHz();

    @Override
    public Set<OchSignal> queryLambdas(PortNumber port) {
        return IntStream.rangeClosed((int) startSpacingMultiplier, (int) stopSpacingMultiplier)
                .mapToObj(x -> new OchSignal(GridType.FLEX, ChannelSpacing.CHL_6P25GHZ, x, 1))
                .collect(GuavaCollectors.toImmutableSet());
    }
}
