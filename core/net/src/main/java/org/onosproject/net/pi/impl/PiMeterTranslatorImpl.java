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
import org.onosproject.net.pi.runtime.PiMeterBand;
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

    private static final int TRTCM_RATES = 2;

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

        // FIXME: we might want to move this check to P4Runtime driver or protocol layer.
        // In general, This check is more of P4Runtime limitation, we should do this check in the low level layer.
        if (meter.bands().size() > TRTCM_RATES) {
            throw new PiTranslationException("PI meter can not have more than 2 bands!");
        }


        PiMeterCellConfig.Builder builder = PiMeterCellConfig.builder();
        for (Band band : meter.bands()) {
            if (band.type() != Band.Type.NONE) {
                throw new PiTranslationException("PI meter can not have band with other types except NONE!");
            }

            PiMeterBand piMeterBand = new PiMeterBand(band.rate(), band.burst());
            builder.withMeterBand(piMeterBand);
        }

        return builder.withMeterCellId((PiMeterCellId) meter.meterCellId()).build();
    }
}
