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

import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.net.pi.runtime.PiCounterCellData;
import org.onosproject.net.pi.runtime.PiCounterCellId;
import org.onosproject.net.pi.runtime.PiCounterId;
import org.onosproject.net.pi.runtime.PiDirectCounterCellId;
import org.onosproject.net.pi.runtime.PiIndirectCounterCellId;
import org.onosproject.net.pi.runtime.PiTableEntry;
import org.slf4j.Logger;
import p4.P4RuntimeOuterClass.CounterData;
import p4.P4RuntimeOuterClass.CounterEntry;
import p4.P4RuntimeOuterClass.DirectCounterEntry;
import p4.P4RuntimeOuterClass.Entity;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.slf4j.LoggerFactory.getLogger;
import static p4.P4RuntimeOuterClass.Entity.EntityCase.COUNTER_ENTRY;
import static p4.P4RuntimeOuterClass.Entity.EntityCase.DIRECT_COUNTER_ENTRY;

/**
 * Encoder/decoder of PI counter IDs to counter entry protobuf messages, and vice versa.
 */
final class CounterEntryCodec {

    private static final Logger log = getLogger(CounterEntryCodec.class);

    private CounterEntryCodec() {
        // Hides constructor.
    }

    /**
     * Returns a collection of P4Runtime entity protobuf messages describing both counter or direct counter entries,
     * encoded from the given collection of PI counter cell identifiers, for the given pipeconf. If a PI counter cell
     * identifier cannot be encoded, it is skipped, hence the returned collection might have different size than the
     * input one.
     * <p>
     * This method takes as parameter also a map between numeric P4Info IDs and PI counter IDs, that will be populated
     * during the process and that is then needed to aid in the decode process.
     *
     * @param cellIds      counter cell identifiers
     * @param counterIdMap counter ID map (empty, it will be populated during this method execution)
     * @param pipeconf     pipeconf
     * @return collection of entity messages describing both counter or direct counter entries
     */
    static Collection<Entity> encodePiCounterCellIds(Collection<PiCounterCellId> cellIds,
                                                     Map<Integer, PiCounterId> counterIdMap,
                                                     PiPipeconf pipeconf) {
        return cellIds
                .stream()
                .map(cellId -> {
                    try {
                        return encodePiCounterCellId(cellId, counterIdMap, pipeconf);
                    } catch (P4InfoBrowser.NotFoundException | EncodeException e) {
                        log.warn("Unable to encode PI counter cell id: {}", e.getMessage());
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Returns a collection of PI counter cell data, decoded from the given P4Runtime entity protobuf messages
     * describing both counter or direct counter entries, for the given counter ID map (populated by {@link
     * #encodePiCounterCellIds(Collection, Map, PiPipeconf)}), and pipeconf. If an entity message cannot be encoded, it
     * is skipped, hence the returned collection might have different size than the input one.
     *
     * @param entities     P4Runtime entity messages
     * @param counterIdMap counter ID map (previously populated)
     * @param pipeconf     pipeconf
     * @return collection of PI counter cell data
     */
    static Collection<PiCounterCellData> decodeCounterEntities(Collection<Entity> entities,
                                                               Map<Integer, PiCounterId> counterIdMap,
                                                               PiPipeconf pipeconf) {
        return entities
                .stream()
                .filter(entity -> entity.getEntityCase() == COUNTER_ENTRY ||
                        entity.getEntityCase() == DIRECT_COUNTER_ENTRY)
                .map(entity -> {
                    try {
                        return decodeCounterEntity(entity, counterIdMap, pipeconf);
                    } catch (EncodeException | P4InfoBrowser.NotFoundException e) {
                        log.warn("Unable to decode counter entity message: {}", e.getMessage());
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private static Entity encodePiCounterCellId(PiCounterCellId cellId, Map<Integer, PiCounterId> counterIdMap,
                                                PiPipeconf pipeconf)
            throws P4InfoBrowser.NotFoundException, EncodeException {

        final P4InfoBrowser browser = PipeconfHelper.getP4InfoBrowser(pipeconf);

        int counterId;
        Entity entity;
        // Encode PI cell ID into entity message and add to read request.
        switch (cellId.type()) {
            case INDIRECT:
                counterId = browser.counters().getByNameOrAlias(cellId.counterId().name()).getPreamble().getId();
                PiIndirectCounterCellId indCellId = (PiIndirectCounterCellId) cellId;
                entity = Entity.newBuilder().setCounterEntry(CounterEntry.newBuilder()
                                                                     .setCounterId(counterId)
                                                                     .setIndex(indCellId.index())
                                                                     .build())
                        .build();
                break;
            case DIRECT:
                counterId = browser.directCounters().getByNameOrAlias(cellId.counterId().name()).getPreamble().getId();
                PiDirectCounterCellId dirCellId = (PiDirectCounterCellId) cellId;
                DirectCounterEntry.Builder entryBuilder = DirectCounterEntry.newBuilder().setCounterId(counterId);
                if (!dirCellId.tableEntry().equals(PiTableEntry.EMTPY)) {
                    entryBuilder.setTableEntry(TableEntryEncoder.encode(dirCellId.tableEntry(), pipeconf));
                }
                entity = Entity.newBuilder().setDirectCounterEntry(entryBuilder.build()).build();
                break;
            default:
                throw new EncodeException(format("Unrecognized PI counter cell ID type '%s'", cellId.type()));
        }
        counterIdMap.put(counterId, cellId.counterId());

        return entity;
    }

    private static PiCounterCellData decodeCounterEntity(Entity entity, Map<Integer, PiCounterId> counterIdMap,
                                                         PiPipeconf pipeconf)
            throws EncodeException, P4InfoBrowser.NotFoundException {

        int counterId;
        CounterData counterData;

        if (entity.getEntityCase() == COUNTER_ENTRY) {
            counterId = entity.getCounterEntry().getCounterId();
            counterData = entity.getCounterEntry().getData();
        } else {
            counterId = entity.getDirectCounterEntry().getCounterId();
            counterData = entity.getDirectCounterEntry().getData();
        }

        // Process only counter IDs that were requested in the first place.
        if (!counterIdMap.containsKey(counterId)) {
            throw new EncodeException(format("Unrecognized counter ID '%s'", counterId));
        }

        PiCounterId piCounterId = counterIdMap.get(counterId);

        // Compute PI cell ID.
        PiCounterCellId piCellId;

        switch (piCounterId.type()) {
            case INDIRECT:
                if (entity.getEntityCase() != COUNTER_ENTRY) {
                    throw new EncodeException(format(
                            "Counter ID '%s' is indirect, but processed entity is %s",
                            piCounterId, entity.getEntityCase()));
                }
                piCellId = PiIndirectCounterCellId.of(piCounterId,
                                                      entity.getCounterEntry().getIndex());
                break;
            case DIRECT:
                if (entity.getEntityCase() != DIRECT_COUNTER_ENTRY) {
                    throw new EncodeException(format(
                            "Counter ID '%s' is direct, but processed entity is %s",
                            piCounterId, entity.getEntityCase()));
                }
                PiTableEntry piTableEntry = TableEntryEncoder.decode(entity.getDirectCounterEntry().getTableEntry(),
                                                                     pipeconf);
                piCellId = PiDirectCounterCellId.of(piCounterId, piTableEntry);
                break;
            default:
                throw new EncodeException(format("Unrecognized PI counter ID type '%s'", piCounterId.type()));
        }

        return new PiCounterCellData(piCellId, counterData.getPacketCount(), counterData.getByteCount());
    }
}
