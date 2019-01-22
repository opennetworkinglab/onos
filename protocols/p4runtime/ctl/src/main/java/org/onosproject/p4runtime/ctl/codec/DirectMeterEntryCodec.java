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

import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.net.pi.runtime.PiMeterBand;
import org.onosproject.net.pi.runtime.PiMeterCellConfig;
import org.onosproject.net.pi.runtime.PiMeterCellHandle;
import org.onosproject.net.pi.runtime.PiMeterCellId;
import org.onosproject.p4runtime.ctl.utils.P4InfoBrowser;
import p4.v1.P4RuntimeOuterClass;

import static org.onosproject.p4runtime.ctl.codec.Codecs.CODECS;

/**
 * Codec for P4Runtime DirectMeterEntryCodec.
 */
public final class DirectMeterEntryCodec
        extends AbstractEntityCodec<PiMeterCellConfig, PiMeterCellHandle,
        P4RuntimeOuterClass.DirectMeterEntry, Object> {

    @Override
    protected P4RuntimeOuterClass.DirectMeterEntry encode(
            PiMeterCellConfig piEntity, Object ignored, PiPipeconf pipeconf,
            P4InfoBrowser browser)
            throws CodecException {
        return P4RuntimeOuterClass.DirectMeterEntry.newBuilder()
                .setTableEntry(CODECS.tableEntry().encode(
                        piEntity.cellId().tableEntry(), null, pipeconf))
                .setConfig(MeterEntryCodec.getP4Config(piEntity))
                .build();
    }

    @Override
    protected P4RuntimeOuterClass.DirectMeterEntry encodeKey(
            PiMeterCellHandle handle, Object metadata, PiPipeconf pipeconf,
            P4InfoBrowser browser)
            throws CodecException {
        return keyMsgBuilder(handle.cellId(), pipeconf).build();
    }

    @Override
    protected P4RuntimeOuterClass.DirectMeterEntry encodeKey(
            PiMeterCellConfig piEntity, Object metadata,
            PiPipeconf pipeconf, P4InfoBrowser browser)
            throws CodecException {
        return keyMsgBuilder(piEntity.cellId(), pipeconf).build();
    }

    private P4RuntimeOuterClass.DirectMeterEntry.Builder keyMsgBuilder(
            PiMeterCellId cellId, PiPipeconf pipeconf)
            throws CodecException {
        return P4RuntimeOuterClass.DirectMeterEntry.newBuilder()
                .setTableEntry(CODECS.tableEntry().encodeKey(
                        cellId.tableEntry(), null, pipeconf));
    }

    @Override
    protected PiMeterCellConfig decode(
            P4RuntimeOuterClass.DirectMeterEntry message, Object ignored,
            PiPipeconf pipeconf, P4InfoBrowser browser)
            throws CodecException {
        return PiMeterCellConfig.builder()
                .withMeterCellId(PiMeterCellId.ofDirect(
                        CODECS.tableEntry().decode(
                                message.getTableEntry(), null, pipeconf)))
                .withMeterBand(new PiMeterBand(message.getConfig().getCir(),
                                               message.getConfig().getCburst()))
                .withMeterBand(new PiMeterBand(message.getConfig().getPir(),
                                               message.getConfig().getPburst()))
                .build();
    }
}
