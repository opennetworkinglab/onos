/*
 * Copyright 2021-present Open Networking Foundation
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

package org.onosproject.drivers.barefoot;

import org.onosproject.drivers.p4runtime.P4RuntimeMeterProgrammable;
import org.onosproject.net.meter.MeterProgrammable;
import org.onosproject.net.pi.runtime.PiMeterBand;
import org.onosproject.net.pi.runtime.PiMeterCellConfig;

/**
 * Implementation of the MeterProgrammable behaviour for a Tofino-based switch.
 */
public class TofinoMeterProgrammable extends P4RuntimeMeterProgrammable implements MeterProgrammable {
    // More insights for these magic numbers can be found in this doc
    // https://docs.google.com/document/d/1Vuf_2RaO0BJPTj_weE9h2spUBCMsiBX-iIMb4OoEIB0/edit?usp=sharing
    private static final double RATE_ERROR = 0.02;
    private static final long BURST_LOWER_DIVIDER = 126;
    private static final long BURST_UPPER_DIVIDER = 125;
    private static final long BURST_MULTIPLIER = 125;

    @Override
    public boolean isSimilar(PiMeterCellConfig onosMeter, PiMeterCellConfig deviceMeter) {
        final PiMeterBand onosCommittedBand = onosMeter.committedBand();
        final PiMeterBand onosPeakBand = onosMeter.peakBand();
        final PiMeterBand deviceCommittedBand = deviceMeter.committedBand();
        final PiMeterBand devicePeakBand = deviceMeter.peakBand();

        // Fail fast, this can easily happen if we send a write very
        // close to a read, read can still return the default config
        if (deviceCommittedBand == null || devicePeakBand == null) {
            return false;
        }

        final long onosCir = onosCommittedBand.rate();
        final long onosCburst = onosCommittedBand.burst();
        final long onosPir = onosPeakBand.rate();
        final long onosPburst = onosPeakBand.burst();
        final long deviceCir = deviceCommittedBand.rate();
        final long deviceCburst = deviceCommittedBand.burst();
        final long devicePir = devicePeakBand.rate();
        final long devicePburst = devicePeakBand.burst();

        return isRateSimilar(onosCir, deviceCir) && isRateSimilar(onosPir, devicePir) &&
                isBurstSimilar(onosCburst, deviceCburst) && isBurstSimilar(onosPburst, devicePburst);
    }

    // Verify if device rate is included in the confidence interval
    // derived from the rate stored in ONOS
    private boolean isRateSimilar(long onosRate, long deviceRate) {
        double lowerEnd = (double) onosRate * (1.0 - RATE_ERROR);
        double upperEnd = (double) onosRate * (1.0 + RATE_ERROR);

        if (log.isDebugEnabled()) {
            log.debug("isRateSimilar {} in [{}, {}]", deviceRate, lowerEnd, upperEnd);
        }

        return deviceRate >= lowerEnd && deviceRate <= upperEnd;
    }

    // Verify if device burst is included in the confidence interval
    // derived from the burst stored in ONOS
    private boolean isBurstSimilar(long onosBurst, long deviceBurst) {
        // Rundown removing the decimal part
        long lowerEnd = (onosBurst / BURST_LOWER_DIVIDER) * BURST_MULTIPLIER;
        long upperEnd = (onosBurst / BURST_UPPER_DIVIDER) * BURST_MULTIPLIER;

        if (log.isDebugEnabled()) {
            log.debug("isBurstSimilar {} in [{}, {}]", deviceBurst, lowerEnd, upperEnd);
        }

        return deviceBurst >= lowerEnd && deviceBurst <= upperEnd;
    }

}
