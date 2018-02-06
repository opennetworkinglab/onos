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

import org.onlab.util.GuavaCollectors;
import org.onlab.util.Spectrum;
import org.onosproject.net.ChannelSpacing;
import org.onosproject.net.GridType;
import org.onosproject.net.OchSignal;
import org.onosproject.net.PortNumber;
import org.onosproject.net.behaviour.LambdaQuery;
import org.onosproject.net.driver.AbstractHandlerBehaviour;

import java.util.Set;
import java.util.stream.IntStream;

/**
 * Lambda query implementation for Calient S160 and S320 Optical Circuit Switch.
 *
 * The device consists of OMS ports only, and each port exposes lambda resources covering the whole
 * usable optical spectrum (U to O band, see {@link Spectrum} for spectrum definitions).
 */
public class CalientLambdaQuery extends AbstractHandlerBehaviour implements LambdaQuery {

    @Override
    public Set<OchSignal> queryLambdas(PortNumber port) {
        // S160 data sheet
        // Wavelength range: 1260 - 1630 nm
        long startSpacingMultiplier = Spectrum.U_BAND_MIN.subtract(Spectrum.CENTER_FREQUENCY).asHz() /
                ChannelSpacing.CHL_12P5GHZ.frequency().asHz();
        long stopSpacingMultiplier = Spectrum.O_BAND_MIN.subtract(Spectrum.CENTER_FREQUENCY).asHz() /
                ChannelSpacing.CHL_12P5GHZ.frequency().asHz();

        // Only consider odd values for the multiplier (for easy mapping to fixed grid)
        return IntStream.rangeClosed((int) startSpacingMultiplier, (int) stopSpacingMultiplier)
                .filter(i -> i % 2 == 1)
                .mapToObj(i -> new OchSignal(GridType.FLEX, ChannelSpacing.CHL_6P25GHZ, i, 1))
                .collect(GuavaCollectors.toImmutableSet());
    }
}
