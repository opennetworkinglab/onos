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

package org.onosproject.drivers.p4runtime;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.Striped;
import org.onosproject.net.DeviceId;
import org.onosproject.net.meter.Band;
import org.onosproject.net.meter.DefaultMeter;
import org.onosproject.net.meter.DefaultMeterFeatures;
import org.onosproject.net.meter.Meter;
import org.onosproject.net.meter.MeterFeatures;
import org.onosproject.net.meter.MeterOperation;
import org.onosproject.net.meter.MeterProgrammable;
import org.onosproject.net.meter.MeterScope;
import org.onosproject.net.meter.MeterState;
import org.onosproject.net.pi.model.PiMeterId;
import org.onosproject.net.pi.model.PiMeterModel;
import org.onosproject.net.pi.model.PiPipelineModel;
import org.onosproject.net.pi.runtime.PiMeterCellConfig;
import org.onosproject.net.pi.runtime.PiMeterCellHandle;
import org.onosproject.net.pi.runtime.PiMeterCellId;
import org.onosproject.net.pi.service.PiMeterTranslator;
import org.onosproject.net.pi.service.PiTranslatedEntity;
import org.onosproject.net.pi.service.PiTranslationException;
import org.onosproject.p4runtime.api.P4RuntimeWriteClient.WriteRequest;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.p4runtime.api.P4RuntimeWriteClient.UpdateType;

/**
 * Implementation of MeterProgrammable behaviour for P4Runtime.
 */
public class P4RuntimeMeterProgrammable extends AbstractP4RuntimeHandlerBehaviour implements MeterProgrammable {

    private static final Striped<Lock> WRITE_LOCKS = Striped.lock(30);

    private PiMeterTranslator translator;
    private PiPipelineModel pipelineModel;

    @Override
    protected boolean setupBehaviour(String opName) {
        if (!super.setupBehaviour(opName)) {
            return false;
        }

        translator = translationService.meterTranslator();
        pipelineModel = pipeconf.pipelineModel();
        return true;
    }

    @Override
    public CompletableFuture<Boolean> performMeterOperation(MeterOperation meterOp) {

        if (!setupBehaviour("performMeterOperation()")) {
            return CompletableFuture.completedFuture(false);
        }

        return CompletableFuture.completedFuture(processMeterOp(meterOp));
    }

    private boolean processMeterOp(MeterOperation meterOp) {
        PiMeterCellConfig piMeterCellConfig;
        final PiMeterCellHandle handle = PiMeterCellHandle.of(deviceId,
                                            (PiMeterCellId) meterOp.meter().meterCellId());
        boolean result = true;
        WRITE_LOCKS.get(deviceId).lock();
        try {
            switch (meterOp.type()) {
                case ADD:
                case MODIFY:
                    // Create a config for modify operation
                    try {
                        piMeterCellConfig = translator.translate(meterOp.meter(), pipeconf);
                    } catch (PiTranslationException e) {
                        log.warn("Unable translate meter, aborting meter operation {}: {}",
                            meterOp.type(), e.getMessage());
                        log.debug("exception", e);
                        return false;
                    }
                    translator.learn(handle, new PiTranslatedEntity<>(meterOp.meter(), piMeterCellConfig, handle));
                    break;
                case REMOVE:
                    // Create a empty config for reset operation
                    PiMeterCellId piMeterCellId = (PiMeterCellId) meterOp.meter().meterCellId();
                    piMeterCellConfig = PiMeterCellConfig.reset(piMeterCellId);
                    translator.forget(handle);
                    break;
                default:
                    log.warn("Meter Operation type {} not supported", meterOp.type());
                    return false;
            }

            WriteRequest request = client.write(p4DeviceId, pipeconf)
                    .entity(piMeterCellConfig, UpdateType.MODIFY);
            if (!request.pendingUpdates().isEmpty()) {
                result = request.submitSync().isSuccess();
            }
        } finally {
            WRITE_LOCKS.get(deviceId).unlock();
        }
        return result;
    }

    @Override
    public CompletableFuture<Collection<Meter>> getMeters() {

        if (!setupBehaviour("getMeters()")) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }

        Collection<PiMeterCellConfig> piMeterCellConfigs;

        Set<PiMeterId> meterIds = new HashSet<>();
        for (PiMeterModel mode : pipelineModel.meters()) {
            meterIds.add(mode.id());
        }

        piMeterCellConfigs = client.read(p4DeviceId, pipeconf)
                .meterCells(meterIds).submitSync().all(PiMeterCellConfig.class)
                .stream()
                .filter(piMeterCellConfig -> !piMeterCellConfig.isDefaultConfig())
                .collect(Collectors.toList());

        if (piMeterCellConfigs.isEmpty()) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }

        List<PiMeterCellId> inconsistentOrDefaultCells = Lists.newArrayList();
        List<Meter> meters = Lists.newArrayList();

        // Check the consistency of meter config
        for (PiMeterCellConfig config : piMeterCellConfigs) {
            PiMeterCellHandle handle = PiMeterCellHandle.of(deviceId, config);
            DefaultMeter meter = (DefaultMeter) forgeMeter(config, handle);
            if (meter == null) {
                // A default config cannot be used to forge meter
                // because meter has at least 1 band while default config has no band
                inconsistentOrDefaultCells.add(config.cellId());
            } else {
                meters.add(meter);
            }
        }

        // Reset all inconsistent meter cells to the default config
        if (!inconsistentOrDefaultCells.isEmpty()) {
            WriteRequest request = client.write(p4DeviceId, pipeconf);
            inconsistentOrDefaultCells.forEach(cellId ->
                    request.entity(PiMeterCellConfig.reset(cellId), UpdateType.MODIFY));
            request.submit().whenComplete((response, ex) -> {
                if (ex != null) {
                    log.error("Exception resetting inconsistent meter entries", ex);
                } else {
                    log.debug("Successfully removed {} out of {} inconsistent meter entries",
                            response.success().size(), response.all().size());
                }
            });
        }

        return CompletableFuture.completedFuture(meters);
    }

    @Override
    public CompletableFuture<Collection<MeterFeatures>> getMeterFeatures() {

        if (!setupBehaviour("getMeterFeatures()")) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }

        Collection<MeterFeatures> meterFeatures = new HashSet<>();
        pipeconf.pipelineModel().meters().forEach(
            m -> meterFeatures.add(new P4RuntimeMeterFeaturesBuilder(m, deviceId).build()));

        return CompletableFuture.completedFuture(meterFeatures);
    }

    private Meter forgeMeter(PiMeterCellConfig config, PiMeterCellHandle handle) {
        final Optional<PiTranslatedEntity<Meter, PiMeterCellConfig>>
            translatedEntity = translator.lookup(handle);

        // A meter cell config might not be present in the translation store if it
        // is default configuration.
        if (translatedEntity.isEmpty()) {
            if (!config.isDefaultConfig()) {
                log.warn("Meter Cell Config obtained from device {} is different from " +
                         "one in in translation store: device={}, store=Default", deviceId, config);
            } else {
                log.debug("Configs for {} obtained from device: {} and from the store are default, " +
                          "skipping the forge section", config.cellId(), deviceId);
            }
            return null;
        }

        // The config is not consistent. MeterProgrammable should remember
        // that config from devices can be default which means no band
        if (!isSimilar(translatedEntity.get().translated(), config)) {
            log.warn("Meter Cell Config obtained from device {} is different from " +
                             "one in in translation store: device={}, store={}",
                     deviceId, config, translatedEntity.get().translated());
            return null;
        }

        Meter original = translatedEntity.get().original();
        // Forge a meter with MeterCellId, Bands and DeviceId using the original value.
        DefaultMeter meter = (DefaultMeter) DefaultMeter.builder()
                .withBands(original.bands())
                .withCellId(original.meterCellId())
                .forDevice(deviceId)
                .build();
        meter.setState(MeterState.ADDED);
        return meter;
    }

    /**
     * Returns true if the given PiMeterCellConfigs are similar enough to be deemed equal
     * for reconciliation purposes. This is required to handle read/write asymmetry in devices
     * that allow variations in the meter rate/burst. E.g., devices that implement metering
     * with a rate or burst size that is slightly higher/lower than the configured ones,
     * so the values written by ONOS will be different than those read from the device.
     *
     * @param onosMeter the ONOS meter
     * @param deviceMeter the meter in the device
     * @return true if the meters are similar, false otherwise
     */
    protected boolean isSimilar(PiMeterCellConfig onosMeter, PiMeterCellConfig deviceMeter) {
        return onosMeter.equals(deviceMeter);
    }

    /**
     * P4 meter features builder.
     */
    public static class P4RuntimeMeterFeaturesBuilder {
        private final PiMeterModel piMeterModel;
        private DeviceId deviceId;

        private static final long PI_METER_START_INDEX = 0L;
        private static final short PI_METER_MAX_BAND = 2;
        private static final short PI_METER_MAX_COLOR = 3;

        public P4RuntimeMeterFeaturesBuilder(PiMeterModel piMeterModel, DeviceId deviceId) {
            this.piMeterModel = checkNotNull(piMeterModel);
            this.deviceId = deviceId;
        }

        /**
         * To build a MeterFeatures using the PiMeterModel object
         * retrieved from pipeconf.
         *
         * @return the meter features object
         */
        public MeterFeatures build() {
            // We set the basic values before to extract the other information.
            MeterFeatures.Builder builder = DefaultMeterFeatures.builder()
                    .forDevice(deviceId)
                    // The scope value will be PiMeterId
                    .withScope(MeterScope.of(piMeterModel.id().id()))
                    .withMaxBands(PI_METER_MAX_BAND)
                    .withMaxColors(PI_METER_MAX_COLOR);

            // We extract the number of supported meters.
            if (piMeterModel.size() > 0) {
                builder.withStartIndex(PI_METER_START_INDEX)
                       .withEndIndex(piMeterModel.size() - 1);
            }

            // p4rt meters support MARK_YELLOW (committed config) and
            // MARK_RED (peak config) band types.
            Set<Band.Type> bands = Sets.newHashSet();
            bands.add(Band.Type.MARK_YELLOW);
            bands.add(Band.Type.MARK_RED);
            builder.withBandTypes(bands);

            // We extract the supported units;
            Set<Meter.Unit> units = Sets.newHashSet();
            if (piMeterModel.unit() == PiMeterModel.Unit.BYTES) {
                units.add(Meter.Unit.BYTES_PER_SEC);
            } else if (piMeterModel.unit() == PiMeterModel.Unit.PACKETS) {
                units.add(Meter.Unit.PKTS_PER_SEC);
            }

            return builder.withUnits(units)
                    .hasBurst(true)
                    .hasStats(false)
                    .build();
        }

        /**
         * To build an empty meter features.
         *
         * @param deviceId the device id
         * @return the meter features
         */
        public MeterFeatures noMeterFeatures(DeviceId deviceId) {
            return DefaultMeterFeatures.noMeterFeatures(deviceId);
        }
    }
}
