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

package org.onosproject.p4runtime.ctl;

import org.onosproject.net.pi.model.PiCounterId;
import org.onosproject.net.pi.model.PiCounterType;
import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.net.pi.model.PiTableId;
import org.onosproject.net.pi.runtime.PiCounterCellData;
import org.onosproject.net.pi.runtime.PiCounterCellId;
import org.onosproject.net.pi.runtime.PiTableEntry;
import org.slf4j.Logger;
import p4.v1.P4RuntimeOuterClass;
import p4.v1.P4RuntimeOuterClass.CounterData;
import p4.v1.P4RuntimeOuterClass.CounterEntry;
import p4.v1.P4RuntimeOuterClass.DirectCounterEntry;
import p4.v1.P4RuntimeOuterClass.Entity;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.onosproject.p4runtime.ctl.P4RuntimeUtils.indexMsg;
import static org.slf4j.LoggerFactory.getLogger;
import static p4.v1.P4RuntimeOuterClass.Entity.EntityCase.COUNTER_ENTRY;
import static p4.v1.P4RuntimeOuterClass.Entity.EntityCase.DIRECT_COUNTER_ENTRY;

/**
 * Encoder/decoder of PI counter IDs to counter entry protobuf messages, and
 * vice versa.
 */
final class CounterEntryCodec {

    private static final Logger log = getLogger(CounterEntryCodec.class);

    private CounterEntryCodec() {
        // Hides constructor.
    }

    /**
     * Returns a collection of P4Runtime entity protobuf messages describing
     * both counter or direct counter entries, encoded from the given collection
     * of PI counter cell identifiers, for the given pipeconf. If a PI counter
     * cell identifier cannot be encoded, it is skipped, hence the returned
     * collection might have different size than the input one.
     *
     * @param cellIds  counter cell identifiers
     * @param pipeconf pipeconf
     * @return collection of entity messages describing both counter or direct
     * counter entries
     */
    static Collection<Entity> encodePiCounterCellIds(Collection<PiCounterCellId> cellIds,
                                                     PiPipeconf pipeconf) {

        final P4InfoBrowser browser = PipeconfHelper.getP4InfoBrowser(pipeconf);

        if (browser == null) {
            log.error("Unable to get a P4Info browser for pipeconf {}", pipeconf.id());
            return Collections.emptyList();
        }

        return cellIds
                .stream()
                .map(cellId -> {
                    try {
                        return encodePiCounterCellId(cellId, pipeconf, browser);
                    } catch (P4InfoBrowser.NotFoundException | EncodeException e) {
                        log.warn("Unable to encode PI counter cell id: {}", e.getMessage());
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Returns a collection of P4Runtime entity protobuf messages to be used in
     * requests to read all cells from the given counter identifiers. Works for
     * both indirect or direct counters. If a PI counter identifier cannot be
     * encoded, it is skipped, hence the returned collection might have
     * different size than the input one.
     *
     * @param counterIds counter identifiers
     * @param pipeconf   pipeconf
     * @return collection of entity messages
     */
    static Collection<Entity> readAllCellsEntities(Collection<PiCounterId> counterIds,
                                                   PiPipeconf pipeconf) {
        final P4InfoBrowser browser = PipeconfHelper.getP4InfoBrowser(pipeconf);

        if (browser == null) {
            log.error("Unable to get a P4Info browser for pipeconf {}", pipeconf.id());
            return Collections.emptyList();
        }

        return counterIds
                .stream()
                .map(counterId -> {
                    try {
                        return readAllCellsEntity(counterId, pipeconf, browser);
                    } catch (P4InfoBrowser.NotFoundException | EncodeException e) {
                        log.warn("Unable to encode counter ID to read-all-cells entity: {}",
                                 e.getMessage());
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Returns a collection of PI counter cell data, decoded from the given
     * P4Runtime entity protobuf messages describing both counter or direct
     * counter entries, and pipeconf. If an entity message cannot be encoded, it
     * is skipped, hence the returned collection might have different size than
     * the input one.
     *
     * @param entities P4Runtime entity messages
     * @param pipeconf pipeconf
     * @return collection of PI counter cell data
     */
    static Collection<PiCounterCellData> decodeCounterEntities(Collection<Entity> entities,
                                                               PiPipeconf pipeconf) {

        final P4InfoBrowser browser = PipeconfHelper.getP4InfoBrowser(pipeconf);

        if (browser == null) {
            log.error("Unable to get a P4Info browser for pipeconf {}", pipeconf.id());
            return Collections.emptyList();
        }

        return entities
                .stream()
                .filter(entity -> entity.getEntityCase() == COUNTER_ENTRY ||
                        entity.getEntityCase() == DIRECT_COUNTER_ENTRY)
                .map(entity -> {
                    try {
                        return decodeCounterEntity(entity, pipeconf, browser);
                    } catch (EncodeException | P4InfoBrowser.NotFoundException e) {
                        log.warn("Unable to decode counter entity message: {}",
                                 e.getMessage());
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private static Entity encodePiCounterCellId(PiCounterCellId cellId,
                                                PiPipeconf pipeconf,
                                                P4InfoBrowser browser)
            throws P4InfoBrowser.NotFoundException, EncodeException {

        int counterId;
        Entity entity;
        // Encode PI cell ID into entity message and add to read request.
        switch (cellId.counterType()) {
            case INDIRECT:
                counterId = browser.counters()
                        .getByName(cellId.counterId().id())
                        .getPreamble()
                        .getId();
                entity = Entity.newBuilder()
                        .setCounterEntry(
                                CounterEntry.newBuilder()
                                        .setCounterId(counterId)
                                        .setIndex(indexMsg(cellId.index()))
                                        .build())
                        .build();
                break;
            case DIRECT:
                DirectCounterEntry.Builder entryBuilder = DirectCounterEntry.newBuilder();
                entryBuilder.setTableEntry(
                        TableEntryEncoder.encode(cellId.tableEntry(), pipeconf));
                entity = Entity.newBuilder()
                        .setDirectCounterEntry(entryBuilder.build())
                        .build();
                break;
            default:
                throw new EncodeException(format(
                        "Unrecognized PI counter cell ID type '%s'",
                        cellId.counterType()));
        }

        return entity;
    }

    private static Entity readAllCellsEntity(PiCounterId counterId,
                                             PiPipeconf pipeconf,
                                             P4InfoBrowser browser)
            throws P4InfoBrowser.NotFoundException, EncodeException {

        if (!pipeconf.pipelineModel().counter(counterId).isPresent()) {
            throw new EncodeException(format(
                    "not such counter '%s' in pipeline model", counterId));
        }
        final PiCounterType counterType = pipeconf.pipelineModel()
                .counter(counterId).get().counterType();

        switch (counterType) {
            case INDIRECT:
                final int p4InfoCounterId = browser.counters()
                        .getByName(counterId.id())
                        .getPreamble().getId();
                return Entity.newBuilder().setCounterEntry(
                        P4RuntimeOuterClass.CounterEntry.newBuilder()
                                // Index unset to read all cells
                                .setCounterId(p4InfoCounterId)
                                .build())
                        .build();
            case DIRECT:
                final PiTableId tableId = pipeconf.pipelineModel()
                        .counter(counterId).get().table();
                if (tableId == null) {
                    throw new EncodeException(format(
                            "null table for direct counter '%s'", counterId));
                }
                final int p4TableId = browser.tables().getByName(tableId.id())
                        .getPreamble().getId();
                return Entity.newBuilder().setDirectCounterEntry(
                        P4RuntimeOuterClass.DirectCounterEntry.newBuilder()
                                .setTableEntry(
                                        // Match unset to read all cells
                                        P4RuntimeOuterClass.TableEntry.newBuilder()
                                                .setTableId(p4TableId)
                                                .build())
                                .build())
                        .build();
            default:
                throw new EncodeException(format(
                        "unrecognized PI counter type '%s'", counterType));
        }
    }

    private static PiCounterCellData decodeCounterEntity(Entity entity,
                                                         PiPipeconf pipeconf,
                                                         P4InfoBrowser browser)
            throws EncodeException, P4InfoBrowser.NotFoundException {

        CounterData counterData;
        PiCounterCellId piCellId;

        if (entity.getEntityCase() == COUNTER_ENTRY) {
            String counterName = browser.counters()
                    .getById(entity.getCounterEntry().getCounterId())
                    .getPreamble()
                    .getName();
            piCellId = PiCounterCellId.ofIndirect(
                    PiCounterId.of(counterName),
                    entity.getCounterEntry().getIndex().getIndex());
            counterData = entity.getCounterEntry().getData();
        } else if (entity.getEntityCase() == DIRECT_COUNTER_ENTRY) {
            PiTableEntry piTableEntry = TableEntryEncoder.decode(
                    entity.getDirectCounterEntry().getTableEntry(), pipeconf);
            piCellId = PiCounterCellId.ofDirect(piTableEntry);
            counterData = entity.getDirectCounterEntry().getData();
        } else {
            throw new EncodeException(format(
                    "Unrecognized entity type '%s' in P4Runtime message",
                    entity.getEntityCase().name()));
        }

        return new PiCounterCellData(piCellId,
                                     counterData.getPacketCount(),
                                     counterData.getByteCount());
    }
}
