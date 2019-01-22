/*
 * Copyright 2019-present Open Networking Foundation
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

package org.onosproject.p4runtime.ctl.codec;

import org.onosproject.net.pi.model.PiMeterId;
import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.net.pi.runtime.PiMeterBand;
import org.onosproject.net.pi.runtime.PiMeterCellConfig;
import org.onosproject.net.pi.runtime.PiMeterCellHandle;
import org.onosproject.net.pi.runtime.PiMeterCellId;
import org.onosproject.p4runtime.ctl.utils.P4InfoBrowser;
import p4.v1.P4RuntimeOuterClass;

/**
 * Codec for P4Runtime MeterEntry.
 */
public final class MeterEntryCodec
        extends AbstractEntityCodec<PiMeterCellConfig, PiMeterCellHandle,
        P4RuntimeOuterClass.MeterEntry, Object> {

    static P4RuntimeOuterClass.MeterConfig getP4Config(PiMeterCellConfig piConfig)
            throws CodecException {
        if (piConfig.meterBands().size() != 2) {
            throw new CodecException("Number of meter bands should be 2");
        }
        final PiMeterBand[] bands = piConfig.meterBands().toArray(new PiMeterBand[0]);
        long cir, cburst, pir, pburst;
        // The band with bigger burst is peak if rate of them is equal.
        if (bands[0].rate() > bands[1].rate() ||
                (bands[0].rate() == bands[1].rate() &&
                        bands[0].burst() >= bands[1].burst())) {
            cir = bands[1].rate();
            cburst = bands[1].burst();
            pir = bands[0].rate();
            pburst = bands[0].burst();
        } else {
            cir = bands[0].rate();
            cburst = bands[0].burst();
            pir = bands[1].rate();
            pburst = bands[1].burst();
        }
        return P4RuntimeOuterClass.MeterConfig.newBuilder()
                .setCir(cir)
                .setCburst(cburst)
                .setPir(pir)
                .setPburst(pburst)
                .build();
    }

    @Override
    protected P4RuntimeOuterClass.MeterEntry encode(
            PiMeterCellConfig piEntity, Object ignored, PiPipeconf pipeconf,
            P4InfoBrowser browser)
            throws P4InfoBrowser.NotFoundException, CodecException {
        final int meterId = browser.meters().getByName(
                piEntity.cellId().meterId().id()).getPreamble().getId();
        return P4RuntimeOuterClass.MeterEntry.newBuilder()
                .setMeterId(meterId)
                .setIndex(P4RuntimeOuterClass.Index.newBuilder()
                                  .setIndex(piEntity.cellId().index()).build())
                .setConfig(getP4Config(piEntity))
                .build();
    }

    @Override
    protected P4RuntimeOuterClass.MeterEntry encodeKey(
            PiMeterCellHandle handle, Object metadata, PiPipeconf pipeconf,
            P4InfoBrowser browser)
            throws P4InfoBrowser.NotFoundException {
        return keyMsgBuilder(handle.cellId(), browser).build();
    }

    @Override
    protected P4RuntimeOuterClass.MeterEntry encodeKey(
            PiMeterCellConfig piEntity, Object metadata, PiPipeconf pipeconf,
            P4InfoBrowser browser)
            throws P4InfoBrowser.NotFoundException {
        return keyMsgBuilder(piEntity.cellId(), browser).build();
    }

    private P4RuntimeOuterClass.MeterEntry.Builder keyMsgBuilder(
            PiMeterCellId cellId, P4InfoBrowser browser)
            throws P4InfoBrowser.NotFoundException {
        final int meterId = browser.meters().getByName(
                cellId.meterId().id()).getPreamble().getId();
        return P4RuntimeOuterClass.MeterEntry.newBuilder()
                .setMeterId(meterId)
                .setIndex(P4RuntimeOuterClass.Index.newBuilder()
                                  .setIndex(cellId.index()).build());
    }

    @Override
    protected PiMeterCellConfig decode(
            P4RuntimeOuterClass.MeterEntry message, Object ignored,
            PiPipeconf pipeconf, P4InfoBrowser browser)
            throws P4InfoBrowser.NotFoundException {
        final String meterName = browser.meters()
                .getById(message.getMeterId())
                .getPreamble()
                .getName();
        return PiMeterCellConfig.builder()
                .withMeterCellId(PiMeterCellId.ofIndirect(
                        PiMeterId.of(meterName), message.getIndex().getIndex()))
                .withMeterBand(new PiMeterBand(message.getConfig().getCir(),
                                               message.getConfig().getCburst()))
                .withMeterBand(new PiMeterBand(message.getConfig().getPir(),
                                               message.getConfig().getPburst()))
                .build();
    }
}
