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
import org.onosproject.net.pi.runtime.PiCounterCell;
import org.onosproject.net.pi.runtime.PiCounterCellHandle;
import org.onosproject.net.pi.runtime.PiCounterCellId;
import org.onosproject.p4runtime.ctl.utils.P4InfoBrowser;
import p4.v1.P4RuntimeOuterClass;

import static org.onosproject.p4runtime.ctl.codec.Codecs.CODECS;

/**
 * Codec for P4Runtime DirectCounterEntryCodec.
 */
public final class DirectCounterEntryCodec
        extends AbstractEntityCodec<PiCounterCell, PiCounterCellHandle,
        P4RuntimeOuterClass.DirectCounterEntry, Object> {

    @Override
    protected P4RuntimeOuterClass.DirectCounterEntry encode(
            PiCounterCell piEntity, Object ignored, PiPipeconf pipeconf,
            P4InfoBrowser browser)
            throws CodecException {
        return keyMsgBuilder(piEntity.cellId(), pipeconf)
                .setData(P4RuntimeOuterClass.CounterData.newBuilder()
                                 .setByteCount(piEntity.data().bytes())
                                 .setPacketCount(piEntity.data().packets())
                                 .build())
                .build();
    }

    @Override
    protected P4RuntimeOuterClass.DirectCounterEntry encodeKey(
            PiCounterCellHandle handle, Object metadata, PiPipeconf pipeconf,
            P4InfoBrowser browser)
            throws CodecException {
        return keyMsgBuilder(handle.cellId(), pipeconf).build();
    }

    @Override
    protected P4RuntimeOuterClass.DirectCounterEntry encodeKey(
            PiCounterCell piEntity, Object metadata,
            PiPipeconf pipeconf, P4InfoBrowser browser)
            throws CodecException {
        return keyMsgBuilder(piEntity.cellId(), pipeconf).build();
    }

    private P4RuntimeOuterClass.DirectCounterEntry.Builder keyMsgBuilder(
            PiCounterCellId cellId, PiPipeconf pipeconf)
            throws CodecException {
        return P4RuntimeOuterClass.DirectCounterEntry.newBuilder()
                .setTableEntry(CODECS.tableEntry().encodeKey(
                        cellId.tableEntry(), null, pipeconf));
    }

    @Override
    protected PiCounterCell decode(
            P4RuntimeOuterClass.DirectCounterEntry message, Object ignored,
            PiPipeconf pipeconf, P4InfoBrowser browser)
            throws CodecException {
        return new PiCounterCell(
                PiCounterCellId.ofDirect(
                        CODECS.tableEntry().decode(
                                message.getTableEntry(), null, pipeconf)),
                message.getData().getPacketCount(),
                message.getData().getByteCount());
    }
}
