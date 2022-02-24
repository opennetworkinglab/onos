/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.net.pi.impl;

import org.onosproject.net.Device;
import org.onosproject.net.meter.Band;
import org.onosproject.net.meter.Meter;
import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.net.pi.runtime.PiMeterCellConfig;
import org.onosproject.net.pi.runtime.PiMeterCellId;
import org.onosproject.net.pi.service.PiTranslationException;

import static org.onosproject.net.meter.MeterCellId.MeterCellType.PIPELINE_INDEPENDENT;

/**
 * Implementation of meter translation logic.
 */
final class PiMeterTranslatorImpl {

    private PiMeterTranslatorImpl() {
        // Hides constructor.
    }

    private static final int TCM_BANDS = 2;

    /**
     * Returns a PI meter config equivalent to the given meter, for the given pipeconf and device.
     *
     * @param meter    meter
     * @param pipeconf pipeconf
     * @param device   device
     * @return PI meter configs
     * @throws PiTranslationException if the meter cannot be translated
     */
    static PiMeterCellConfig translate(Meter meter, PiPipeconf pipeconf, Device device) throws PiTranslationException {

        if (meter.meterCellId().type() != PIPELINE_INDEPENDENT) {
            throw new PiTranslationException("PI meter cell type must be PIPELINE_INDEPENDENT!");
        }

        // In general, this check is more of P4Runtime limitation, we should do this check in the low level layer.
        // At the same time we could extend to support other configurations (for example srTCM).
        // TODO implement SRTCM and TRTCM helper classes to improve readability of the code.
        //  Or in future when we support other markers we can simply create two different methods.
        if (meter.bands().size() != TCM_BANDS) {
            throw new PiTranslationException("PI meter must have 2 bands in order to implement TCM metering!");
        }

        final Band[] bands = meter.bands().toArray(new Band[0]);
        // Validate proper config of the TCM settings.
        if ((bands[0].type() != Band.Type.MARK_YELLOW && bands[0].type() != Band.Type.MARK_RED) ||
                (bands[1].type() != Band.Type.MARK_YELLOW && bands[1].type() != Band.Type.MARK_RED) ||
                (bands[0].type() == bands[1].type())) {
            throw new PiTranslationException("PI TCM meter must have a MARK_YELLOW band and a MARK_RED band!");
        }

        // Validate proper config. NOTE that we have relaxed some checks
        // and the ONOS meters are not spec compliants with trTCM RFC
        if (bands[0].burst() < 0 || bands[1].burst() < 0) {
            throw new PiTranslationException("PI trTCM meter can not have band with burst < 0!");
        }
        if (bands[0].rate() < 0 || bands[1].rate() < 0) {
            throw new PiTranslationException("PI trTCM meter can not have band with rate < 0!");
        }

        long cir, cburst, pir, pburst;
        if (bands[0].type() == Band.Type.MARK_YELLOW) {
            cir = bands[0].rate();
            cburst = bands[0].burst();
            pir = bands[1].rate();
            pburst = bands[1].burst();
        } else {
            pir = bands[0].rate();
            pburst = bands[0].burst();
            cir = bands[1].rate();
            cburst = bands[1].burst();
        }

        if (cir > pir) {
            throw new PiTranslationException("PI trTCM meter must have a pir >= cir!");
        }

        return PiMeterCellConfig.builder()
                .withCommittedBand(cir, cburst)
                .withPeakBand(pir, pburst)
                .withMeterCellId((PiMeterCellId) meter.meterCellId())
                .build();
    }

}
