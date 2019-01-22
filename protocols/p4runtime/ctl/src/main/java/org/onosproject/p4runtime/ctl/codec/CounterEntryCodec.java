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

import org.onosproject.net.pi.model.PiCounterId;
import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.net.pi.runtime.PiCounterCell;
import org.onosproject.net.pi.runtime.PiCounterCellHandle;
import org.onosproject.net.pi.runtime.PiCounterCellId;
import org.onosproject.p4runtime.ctl.utils.P4InfoBrowser;
import p4.v1.P4RuntimeOuterClass;

/**
 * Codec for P4Runtime CounterEntry.
 */
public final class CounterEntryCodec
        extends AbstractEntityCodec<PiCounterCell, PiCounterCellHandle,
        P4RuntimeOuterClass.CounterEntry, Object> {

    @Override
    protected P4RuntimeOuterClass.CounterEntry encode(
            PiCounterCell piEntity, Object ignored, PiPipeconf pipeconf,
            P4InfoBrowser browser)
            throws P4InfoBrowser.NotFoundException {
        return keyMsgBuilder(piEntity.cellId(), browser)
                .setData(P4RuntimeOuterClass.CounterData.newBuilder()
                                 .setByteCount(piEntity.data().bytes())
                                 .setPacketCount(piEntity.data().packets())
                                 .build())
                .build();
    }

    @Override
    protected P4RuntimeOuterClass.CounterEntry encodeKey(
            PiCounterCellHandle handle, Object metadata, PiPipeconf pipeconf,
            P4InfoBrowser browser)
            throws P4InfoBrowser.NotFoundException {
        return keyMsgBuilder(handle.cellId(), browser).build();
    }

    @Override
    protected P4RuntimeOuterClass.CounterEntry encodeKey(
            PiCounterCell piEntity, Object metadata, PiPipeconf pipeconf,
            P4InfoBrowser browser)
            throws P4InfoBrowser.NotFoundException {
        return keyMsgBuilder(piEntity.cellId(), browser).build();
    }

    private P4RuntimeOuterClass.CounterEntry.Builder keyMsgBuilder(
            PiCounterCellId cellId, P4InfoBrowser browser)
            throws P4InfoBrowser.NotFoundException {
        final int counterId = browser.counters().getByName(
                cellId.counterId().id()).getPreamble().getId();
        return P4RuntimeOuterClass.CounterEntry.newBuilder()
                .setCounterId(counterId)
                .setIndex(P4RuntimeOuterClass.Index.newBuilder()
                                  .setIndex(cellId.index()).build());
    }

    @Override
    protected PiCounterCell decode(
            P4RuntimeOuterClass.CounterEntry message, Object ignored,
            PiPipeconf pipeconf, P4InfoBrowser browser)
            throws P4InfoBrowser.NotFoundException {
        final String counterName = browser.counters()
                .getById(message.getCounterId())
                .getPreamble()
                .getName();
        return new PiCounterCell(
                PiCounterCellId.ofIndirect(
                        PiCounterId.of(counterName), message.getIndex().getIndex()),
                message.getData().getPacketCount(),
                message.getData().getByteCount());
    }
}
