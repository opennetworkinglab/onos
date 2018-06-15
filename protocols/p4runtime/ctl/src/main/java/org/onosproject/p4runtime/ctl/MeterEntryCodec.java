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

import org.onosproject.net.pi.model.PiMeterId;
import org.onosproject.net.pi.model.PiMeterType;
import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.net.pi.model.PiTableId;
import org.onosproject.net.pi.runtime.PiMeterBand;
import org.onosproject.net.pi.runtime.PiMeterCellConfig;
import org.onosproject.net.pi.runtime.PiMeterCellId;
import org.onosproject.net.pi.runtime.PiTableEntry;
import org.slf4j.Logger;
import p4.v1.P4RuntimeOuterClass;
import p4.v1.P4RuntimeOuterClass.DirectMeterEntry;
import p4.v1.P4RuntimeOuterClass.Entity;
import p4.v1.P4RuntimeOuterClass.MeterConfig;
import p4.v1.P4RuntimeOuterClass.MeterEntry;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.onosproject.p4runtime.ctl.P4RuntimeUtils.indexMsg;
import static org.slf4j.LoggerFactory.getLogger;
import static p4.v1.P4RuntimeOuterClass.Entity.EntityCase.DIRECT_METER_ENTRY;
import static p4.v1.P4RuntimeOuterClass.Entity.EntityCase.METER_ENTRY;

/**
 * Encoder/decoder of PI meter cell configurations to meter entry protobuf
 * messages, and vice versa.
 */
final class MeterEntryCodec {

    private static final Logger log = getLogger(MeterEntryCodec.class);

    private MeterEntryCodec() {
        // Hides constructor.
    }

    /**
     * Returns a collection of P4Runtime entity protobuf messages describing
     * both meter or direct meter entries, encoded from the given collection of
     * PI meter cell configurations, for the given pipeconf. If a PI meter cell
     * configurations cannot be encoded, it is skipped, hence the returned
     * collection might have different size than the input one.
     *
     * @param cellConfigs meter cell configurations
     * @param pipeconf    pipeconf
     * @return collection of entity messages describing both meter or direct
     * meter entries
     */
    static Collection<Entity> encodePiMeterCellConfigs(Collection<PiMeterCellConfig> cellConfigs,
                                                       PiPipeconf pipeconf) {
        final P4InfoBrowser browser = PipeconfHelper.getP4InfoBrowser(pipeconf);

        if (browser == null) {
            log.error("Unable to get a P4Info browser for pipeconf {}", pipeconf.id());
            return Collections.emptyList();
        }

        return cellConfigs
                .stream()
                .map(cellConfig -> {
                    try {
                        return encodePiMeterCellConfig(cellConfig, pipeconf, browser);
                    } catch (P4InfoBrowser.NotFoundException | EncodeException e) {
                        log.warn("Unable to encode PI meter cell id: {}", e.getMessage());
                        log.debug("exception", e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Returns a collection of P4Runtime entity protobuf messages to be used in
     * requests to read all cells from the given meter identifiers. Works for
     * both indirect or direct meters. If a PI meter identifier cannot be
     * encoded, it is skipped, hence the returned collection might have
     * different size than the input one.
     *
     * @param meterIds meter identifiers
     * @param pipeconf pipeconf
     * @return collection of entity messages
     */
    static Collection<Entity> readAllCellsEntities(Collection<PiMeterId> meterIds,
                                                   PiPipeconf pipeconf) {
        final P4InfoBrowser browser = PipeconfHelper.getP4InfoBrowser(pipeconf);

        if (browser == null) {
            log.error("Unable to get a P4Info browser for pipeconf {}", pipeconf.id());
            return Collections.emptyList();
        }

        return meterIds
                .stream()
                .map(meterId -> {
                    try {
                        return readAllCellsEntity(meterId, pipeconf, browser);
                    } catch (P4InfoBrowser.NotFoundException | EncodeException e) {
                        log.warn("Unable to encode meter ID to read-all-cells entity: {}",
                                 e.getMessage());
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * Returns a collection of PI meter cell configurations, decoded from the
     * given P4Runtime entity protobuf messages describing both meter or direct
     * meter entries, and pipeconf. If an entity message cannot be encoded, it
     * is skipped, hence the returned collection might have different size than
     * the input one.
     *
     * @param entities P4Runtime entity messages
     * @param pipeconf pipeconf
     * @return collection of PI meter cell data
     */
    static Collection<PiMeterCellConfig> decodeMeterEntities(Collection<Entity> entities,
                                                             PiPipeconf pipeconf) {
        final P4InfoBrowser browser = PipeconfHelper.getP4InfoBrowser(pipeconf);

        if (browser == null) {
            log.error("Unable to get a P4Info browser for pipeconf {}", pipeconf.id());
            return Collections.emptyList();
        }

        return entities
                .stream()
                .filter(entity -> entity.getEntityCase() == METER_ENTRY ||
                        entity.getEntityCase() == DIRECT_METER_ENTRY)
                .map(entity -> {
                    try {
                        return decodeMeterEntity(entity, pipeconf, browser);
                    } catch (EncodeException | P4InfoBrowser.NotFoundException e) {
                        log.warn("Unable to decode meter entity message: {}", e.getMessage());
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private static Entity encodePiMeterCellConfig(PiMeterCellConfig config,
                                                  PiPipeconf pipeconf,
                                                  P4InfoBrowser browser)
            throws P4InfoBrowser.NotFoundException, EncodeException {

        int meterId;
        Entity entity;
        MeterConfig meterConfig;

        PiMeterBand[] bands = config.meterBands()
                .toArray(new PiMeterBand[config.meterBands().size()]);
        if (bands.length == 2) {
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
            meterConfig = MeterConfig.newBuilder()
                    .setCir(cir)
                    .setCburst(cburst)
                    .setPir(pir)
                    .setPburst(pburst)
                    .build();
        } else if (bands.length == 0) {
            // When reading meter cells.
            meterConfig = null;
        } else {
            throw new EncodeException("number of meter bands should be either 2 or 0");
        }

        switch (config.cellId().meterType()) {
            case INDIRECT:
                meterId = browser.meters()
                        .getByName(config.cellId().meterId().id())
                        .getPreamble().getId();
                MeterEntry.Builder indEntryBuilder = MeterEntry.newBuilder()
                        .setMeterId(meterId)
                        .setIndex(indexMsg(config.cellId().index()));
                if (meterConfig != null) {
                    indEntryBuilder.setConfig(meterConfig);
                }
                entity = Entity.newBuilder()
                        .setMeterEntry(indEntryBuilder.build()).build();
                break;
            case DIRECT:
                DirectMeterEntry.Builder dirEntryBuilder = DirectMeterEntry.newBuilder()
                        .setTableEntry(TableEntryEncoder.encode(
                                config.cellId().tableEntry(), pipeconf));
                if (meterConfig != null) {
                    dirEntryBuilder.setConfig(meterConfig);
                }
                entity = Entity.newBuilder()
                        .setDirectMeterEntry(dirEntryBuilder.build()).build();
                break;
            default:
                throw new EncodeException(format("unrecognized PI meter type '%s'",
                                                 config.cellId().meterType()));
        }

        return entity;
    }

    private static Entity readAllCellsEntity(PiMeterId meterId,
                                             PiPipeconf pipeconf,
                                             P4InfoBrowser browser)
            throws P4InfoBrowser.NotFoundException, EncodeException {

        if (!pipeconf.pipelineModel().meter(meterId).isPresent()) {
            throw new EncodeException(format(
                    "not such meter '%s' in pipeline model", meterId));
        }
        final PiMeterType meterType = pipeconf.pipelineModel()
                .meter(meterId).get().meterType();

        switch (meterType) {
            case INDIRECT:
                final int p4InfoMeterId = browser.meters()
                        .getByName(meterId.id())
                        .getPreamble().getId();
                return Entity.newBuilder().setMeterEntry(
                        P4RuntimeOuterClass.MeterEntry.newBuilder()
                                // Index unset to read all cells
                                .setMeterId(p4InfoMeterId)
                                .build())
                        .build();
            case DIRECT:
                final PiTableId tableId = pipeconf.pipelineModel()
                        .meter(meterId).get().table();
                if (tableId == null) {
                    throw new EncodeException(format(
                            "null table for direct meter '%s'", meterId));
                }
                final int p4TableId = browser.tables().getByName(tableId.id())
                        .getPreamble().getId();
                return Entity.newBuilder().setDirectMeterEntry(
                        P4RuntimeOuterClass.DirectMeterEntry.newBuilder()
                                .setTableEntry(
                                        // Match unset to read all cells
                                        P4RuntimeOuterClass.TableEntry.newBuilder()
                                                .setTableId(p4TableId)
                                                .build())
                                .build())
                        .build();
            default:
                throw new EncodeException(format(
                        "unrecognized PI meter type '%s'", meterType));
        }
    }

    private static PiMeterCellConfig decodeMeterEntity(Entity entity,
                                                       PiPipeconf pipeconf,
                                                       P4InfoBrowser browser)
            throws EncodeException, P4InfoBrowser.NotFoundException {

        MeterConfig meterConfig;
        PiMeterCellId piCellId;

        if (entity.getEntityCase() == METER_ENTRY) {
            String meterName = browser.meters()
                    .getById(entity.getMeterEntry().getMeterId())
                    .getPreamble()
                    .getName();
            piCellId = PiMeterCellId.ofIndirect(
                    PiMeterId.of(meterName),
                    entity.getMeterEntry().getIndex().getIndex());
            meterConfig = entity.getMeterEntry().getConfig();
        } else if (entity.getEntityCase() == DIRECT_METER_ENTRY) {
            PiTableEntry piTableEntry = TableEntryEncoder.decode(
                    entity.getDirectMeterEntry().getTableEntry(),
                    pipeconf);
            piCellId = PiMeterCellId.ofDirect(piTableEntry);
            meterConfig = entity.getDirectMeterEntry().getConfig();
        } else {
            throw new EncodeException(format(
                    "unrecognized entity type '%s' in P4Runtime message",
                    entity.getEntityCase().name()));
        }

        return PiMeterCellConfig.builder()
                .withMeterCellId(piCellId)
                .withMeterBand(new PiMeterBand(meterConfig.getCir(),
                                               meterConfig.getCburst()))
                .withMeterBand(new PiMeterBand(meterConfig.getPir(),
                                               meterConfig.getPburst()))
                .build();
    }
}
