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

import org.onosproject.net.pi.model.PiMeterType;
import org.onosproject.net.pi.model.PiPipeconf;
import org.onosproject.net.pi.runtime.PiMeterBand;
import org.onosproject.net.pi.runtime.PiMeterCellConfig;
import org.onosproject.net.pi.runtime.PiMeterCellId;
import org.onosproject.net.pi.model.PiMeterId;
import org.onosproject.net.pi.runtime.PiTableEntry;
import org.slf4j.Logger;
import p4.P4RuntimeOuterClass.MeterConfig;
import p4.P4RuntimeOuterClass.MeterEntry;
import p4.P4RuntimeOuterClass.DirectMeterEntry;
import p4.P4RuntimeOuterClass.Entity;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static org.slf4j.LoggerFactory.getLogger;
import static p4.P4RuntimeOuterClass.Entity.EntityCase.*;

/**
 * Encoder/decoder of PI meter cell configurations to meter entry protobuf messages, and vice versa.
 */
final class MeterEntryCodec {

    private static final Logger log = getLogger(MeterEntryCodec.class);

    private MeterEntryCodec() {
        // Hides constructor.
    }

    /**
     * Returns a collection of P4Runtime entity protobuf messages describing both meter or direct meter entries,
     * encoded from the given collection of PI meter cell configurations, for the given pipeconf. If a PI meter cell
     * configurations cannot be encoded, it is skipped, hence the returned collection might have different size than the
     * input one.
     * <p>
     * This method takes as parameter also a map between numeric P4Info IDs and PI meter IDs, that will be populated
     * during the process and that is then needed to aid in the decode process.
     *
     * @param cellConfigs  meter cell configurations
     * @param meterIdMap   meter ID map (empty, it will be populated during this method execution)
     * @param pipeconf     pipeconf
     * @return collection of entity messages describing both meter or direct meter entries
     */
    static Collection<Entity> encodePiMeterCellConfigs(Collection<PiMeterCellConfig> cellConfigs,
                                                       Map<Integer, PiMeterId> meterIdMap,
                                                       PiPipeconf pipeconf) {
        return cellConfigs
                .stream()
                .map(cellConfig -> {
                    try {
                        return encodePiMeterCellConfig(cellConfig, meterIdMap, pipeconf);
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
     * Returns a collection of PI meter cell configurations, decoded from the given P4Runtime entity protobuf messages
     * describing both meter or direct meter entries, for the given meter ID map (populated by {@link
     * #encodePiMeterCellConfigs(Collection, Map, PiPipeconf)}), and pipeconf. If an entity message cannot be encoded,
     * it is skipped, hence the returned collection might have different size than the input one.
     *
     * @param entities     P4Runtime entity messages
     * @param meterIdMap   meter ID map (previously populated)
     * @param pipeconf     pipeconf
     * @return collection of PI meter cell data
     */
    static Collection<PiMeterCellConfig> decodeMeterEntities(Collection<Entity> entities,
                                                               Map<Integer, PiMeterId> meterIdMap,
                                                               PiPipeconf pipeconf) {
        return entities
                .stream()
                .filter(entity -> entity.getEntityCase() == METER_ENTRY ||
                        entity.getEntityCase() == DIRECT_METER_ENTRY)
                .map(entity -> {
                    try {
                        return decodeMeterEntity(entity, meterIdMap, pipeconf);
                    } catch (EncodeException | P4InfoBrowser.NotFoundException e) {
                        log.warn("Unable to decode meter entity message: {}", e.getMessage());
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private static Entity encodePiMeterCellConfig(PiMeterCellConfig config, Map<Integer, PiMeterId> meterIdMap,
                                                  PiPipeconf pipeconf)
            throws P4InfoBrowser.NotFoundException, EncodeException {

        final P4InfoBrowser browser = PipeconfHelper.getP4InfoBrowser(pipeconf);

        int meterId;
        Entity entity;
        //The band with bigger burst is peak if rate of them is equal,
        //if bands are not specificed, using default value(0).
        long cir = 0;
        long cburst = 0;
        long pir = 0;
        long pburst = 0;
        PiMeterBand[] bands = config.meterBands().toArray(new PiMeterBand[config.meterBands().size()]);
        if (bands.length == 2) {
            if (bands[0].rate() > bands[1].rate()) {
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
        }

        // Encode PI cell ID into entity message and add to read request.
        switch (config.cellId().meterType()) {
            case INDIRECT:
                meterId = browser.meters().getByName(config.cellId().meterId().id()).getPreamble().getId();
                entity = Entity.newBuilder().setMeterEntry(MeterEntry
                                                                   .newBuilder().setMeterId(meterId)
                                                                   .setIndex(config.cellId().index())
                                                                   .setConfig(MeterConfig.newBuilder()
                                                                                      .setCir(cir)
                                                                                      .setCburst(cburst)
                                                                                      .setPir(pir)
                                                                                      .setPburst(pburst)
                                                                                      .build())
                                                                   .build())
                        .build();
                break;
            case DIRECT:
                meterId = browser.directMeters().getByName(config.cellId().meterId().id()).getPreamble().getId();
                DirectMeterEntry.Builder entryBuilder = DirectMeterEntry.newBuilder()
                        .setMeterId(meterId)
                        .setConfig(MeterConfig.newBuilder()
                                           .setCir(cir)
                                           .setCburst(cburst)
                                           .setPir(pir)
                                           .setPburst(pburst)
                                           .build());

                if (!config.cellId().tableEntry().equals(PiTableEntry.EMTPY)) {
                    entryBuilder.setTableEntry(TableEntryEncoder.encode(config.cellId().tableEntry(), pipeconf));
                }
                entity = Entity.newBuilder().setDirectMeterEntry(entryBuilder.build()).build();
                break;
            default:
                throw new EncodeException(format("Unrecognized PI meter cell ID type '%s'",
                                                 config.cellId().meterType()));
        }
        meterIdMap.put(meterId, config.cellId().meterId());

        return entity;
    }

    private static PiMeterCellConfig decodeMeterEntity(Entity entity, Map<Integer, PiMeterId> meterIdMap,
                                                         PiPipeconf pipeconf)
            throws EncodeException, P4InfoBrowser.NotFoundException {

        int meterId;
        MeterConfig meterConfig;

        if (entity.getEntityCase() == METER_ENTRY) {
            meterId = entity.getMeterEntry().getMeterId();
            meterConfig = entity.getMeterEntry().getConfig();
        } else {
            meterId = entity.getDirectMeterEntry().getMeterId();
            meterConfig = entity.getDirectMeterEntry().getConfig();
        }

        // Process only meter IDs that were requested in the first place.
        if (!meterIdMap.containsKey(meterId)) {
            throw new EncodeException(format("Unrecognized meter ID '%s'", meterId));
        }

        PiMeterId piMeterId = meterIdMap.get(meterId);
        if (!pipeconf.pipelineModel().meter(piMeterId).isPresent()) {
            throw new EncodeException(format("Unable to find meter '%s' in pipeline model",  meterId));
        }

        PiMeterType piMeterType = pipeconf.pipelineModel().meter(piMeterId).get().meterType();
        // Compute PI cell ID.
        PiMeterCellId piCellId;

        switch (piMeterType) {
            case INDIRECT:
                if (entity.getEntityCase() != METER_ENTRY) {
                    throw new EncodeException(format(
                            "Meter ID '%s' is indirect, but processed entity is %s",
                            piMeterId, entity.getEntityCase()));
                }
                piCellId = PiMeterCellId.ofIndirect(piMeterId, entity.getMeterEntry().getIndex());
                break;
            case DIRECT:
                if (entity.getEntityCase() != DIRECT_METER_ENTRY) {
                    throw new EncodeException(format(
                            "Meter ID '%s' is direct, but processed entity is %s",
                            piMeterId, entity.getEntityCase()));
                }
                PiTableEntry piTableEntry = TableEntryEncoder.decode(entity.getDirectMeterEntry().getTableEntry(),
                                                                     pipeconf);
                piCellId = PiMeterCellId.ofDirect(piMeterId, piTableEntry);
                break;
            default:
                throw new EncodeException(format("Unrecognized PI meter ID type '%s'", piMeterType));
        }

        PiMeterCellConfig.Builder builder = PiMeterCellConfig.builder();
        builder.withMeterBand(new PiMeterBand(meterConfig.getCir(), meterConfig.getCburst()));
        builder.withMeterBand(new PiMeterBand(meterConfig.getPir(), meterConfig.getPburst()));

        return builder.withMeterCellId(piCellId).build();
    }
}