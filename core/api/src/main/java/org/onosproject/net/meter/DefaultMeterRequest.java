/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.net.meter;

import com.google.common.collect.ImmutableSet;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.DeviceId;

import java.util.Collection;
import java.util.Optional;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A default implementation of a meter.
 */
public final class DefaultMeterRequest implements MeterRequest {



    private final ApplicationId appId;
    private final Meter.Unit unit;
    private final boolean burst;
    private final Collection<Band> bands;
    private final DeviceId deviceId;
    private final Optional<MeterContext> context;
    private final Type op;

    private DefaultMeterRequest(DeviceId deviceId, ApplicationId appId,
                                Meter.Unit unit, boolean burst,
                                Collection<Band> bands, MeterContext context,
                                Type op) {
        this.deviceId = deviceId;
        this.appId = appId;
        this.unit = unit;
        this.burst = burst;
        this.bands = bands;
        this.context = Optional.ofNullable(context);
        this.op = op;
    }

    @Override
    public DeviceId deviceId() {
        return deviceId;
    }


    @Override
    public ApplicationId appId() {
        return appId;
    }

    @Override
    public Meter.Unit unit() {
        return unit;
    }

    @Override
    public boolean isBurst() {
        return burst;
    }

    @Override
    public Collection<Band> bands() {
        return bands;
    }

    @Override
    public Optional<MeterContext> context() {
        return context;
    }



    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("device", deviceId)
                .add("appId", appId.name())
                .add("unit", unit)
                .add("isBurst", burst)
                .add("bands", bands).toString();
    }

    public static final class Builder implements MeterRequest.Builder {

        private ApplicationId appId;
        private Meter.Unit unit = Meter.Unit.KB_PER_SEC;
        private boolean burst = false;
        private Collection<Band> bands;
        private DeviceId deviceId;
        private MeterContext context;
        private Optional<MeterId> desiredId = Optional.empty();


        @Override
        public MeterRequest.Builder forDevice(DeviceId deviceId) {
            this.deviceId = deviceId;
            return this;
        }

        @Override
        public MeterRequest.Builder fromApp(ApplicationId appId) {
            this.appId = appId;
            return this;
        }

        @Override
        public MeterRequest.Builder  withUnit(Meter.Unit unit) {
            this.unit = unit;
            return this;
        }

        @Override
        public MeterRequest.Builder  burst() {
            this.burst = true;
            return this;
        }

        @Override
        public MeterRequest.Builder  withBands(Collection<Band> bands) {
            this.bands = ImmutableSet.copyOf(bands);
            return this;
        }

        @Override
        public MeterRequest.Builder withContext(MeterContext context) {
            this.context = context;
            return this;
        }

        @Override
        public MeterRequest add() {
            validate();
            return new DefaultMeterRequest(deviceId, appId, unit, burst, bands,
                                           context, Type.ADD);
        }

        @Override
        public MeterRequest remove() {
            validate();
            return new DefaultMeterRequest(deviceId, appId, unit, burst, bands,
                                           context, Type.REMOVE);
        }

        private void validate() {
            checkNotNull(deviceId, "Must specify a device");
            checkNotNull(bands, "Must have bands.");
            checkArgument(!bands.isEmpty(), "Must have at least one band.");
            checkNotNull(appId, "Must have an application id");
        }


    }
}
