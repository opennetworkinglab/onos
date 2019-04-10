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

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.onosproject.drivers.p4runtime.mirror.P4RuntimeMeterMirror;
import org.onosproject.net.meter.Band;
import org.onosproject.net.meter.DefaultBand;
import org.onosproject.net.meter.DefaultMeter;
import org.onosproject.net.meter.Meter;
import org.onosproject.net.meter.MeterOperation;
import org.onosproject.net.meter.MeterProgrammable;
import org.onosproject.net.meter.MeterState;
import org.onosproject.net.pi.model.PiMeterId;
import org.onosproject.net.pi.model.PiMeterModel;
import org.onosproject.net.pi.model.PiPipelineModel;
import org.onosproject.net.pi.runtime.PiMeterCellConfig;
import org.onosproject.net.pi.runtime.PiMeterCellHandle;
import org.onosproject.net.pi.service.PiMeterTranslator;
import org.onosproject.net.pi.service.PiTranslationException;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;

/**
 * Implementation of MeterProgrammable behaviour for P4Runtime.
 */
public class P4RuntimeMeterProgrammable extends AbstractP4RuntimeHandlerBehaviour implements MeterProgrammable {

    private static final int METER_LOCK_EXPIRE_TIME_IN_MIN = 10;
    private static final LoadingCache<PiMeterCellHandle, Lock>
            ENTRY_LOCKS = CacheBuilder.newBuilder()
            .expireAfterAccess(METER_LOCK_EXPIRE_TIME_IN_MIN, TimeUnit.MINUTES)
            .build(new CacheLoader<PiMeterCellHandle, Lock>() {
                @Override
                public Lock load(PiMeterCellHandle handle) {
                    return new ReentrantLock();
                }
            });

    private PiMeterTranslator translator;
    private P4RuntimeMeterMirror meterMirror;
    private PiPipelineModel pipelineModel;

    @Override
    protected boolean setupBehaviour(String opName) {
        if (!super.setupBehaviour(opName)) {
            return false;
        }

        translator = translationService.meterTranslator();
        meterMirror = handler().get(P4RuntimeMeterMirror.class);
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

        if (meterOp.type() != MeterOperation.Type.MODIFY) {
            log.warn("P4Runtime meter operations must be MODIFY!");
            return false;
        }

        PiMeterCellConfig piMeterCellConfig;
        try {
            piMeterCellConfig = translator.translate(meterOp.meter(), pipeconf);
        } catch (PiTranslationException e) {
            log.warn("Unable translate meter, aborting meter operation {}: {}", meterOp.type(), e.getMessage());
            log.debug("exception", e);
            return false;
        }

        final PiMeterCellHandle handle = PiMeterCellHandle.of(deviceId, piMeterCellConfig);
        ENTRY_LOCKS.getUnchecked(handle).lock();
        final boolean result = client.write(p4DeviceId, pipeconf)
                .modify(piMeterCellConfig).submitSync().isSuccess();
        if (result) {
            meterMirror.put(handle, piMeterCellConfig);
        }
        ENTRY_LOCKS.getUnchecked(handle).unlock();

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
                .meterCells(meterIds).submitSync().all(PiMeterCellConfig.class);

        Collection<Meter> meters = piMeterCellConfigs.stream()
                .map(p -> {
                    DefaultMeter meter = (DefaultMeter) DefaultMeter.builder()
                            .withBands(p.meterBands().stream().map(b -> DefaultBand.builder()
                                    .withRate(b.rate())
                                    .burstSize(b.burst())
                                    .ofType(Band.Type.NONE)
                                    .build()).collect(Collectors.toList()))
                            .withCellId(p.cellId()).build();
                    meter.setState(MeterState.ADDED);
                    return meter;
                })
                .collect(Collectors.toList());

        return CompletableFuture.completedFuture(meters);
    }
}
