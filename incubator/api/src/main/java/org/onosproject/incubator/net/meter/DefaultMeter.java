/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.incubator.net.meter;

import org.onosproject.core.ApplicationId;
import org.onosproject.net.DeviceId;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A default implementation of a meter.
 */
public final class DefaultMeter implements Meter {


    private final MeterId id;
    private final ApplicationId appId;
    private final Unit unit;
    private final boolean burst;
    private final Collection<Band> bands;
    private final DeviceId deviceId;
    private final Optional<MeterContext> context;

    private DefaultMeter(DeviceId deviceId, MeterId id, ApplicationId appId,
                        Unit unit, boolean burst,
                        Collection<Band> bands, Optional<MeterContext> context) {
        this.deviceId = deviceId;
        this.id = id;
        this.appId = appId;
        this.unit = unit;
        this.burst = burst;
        this.bands = bands;
        this.context = context;
    }

    @Override
    public DeviceId deviceId() {
        return deviceId;
    }

    @Override
    public MeterId id() {
        return id;
    }

    @Override
    public ApplicationId appId() {
        return appId;
    }

    @Override
    public Unit unit() {
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
        return null;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder implements Meter.Builder {

        private MeterId id;
        private ApplicationId appId;
        private Unit unit = Unit.KB_PER_SEC;
        private boolean burst = false;
        private Collection<Band> bands;
        private DeviceId deviceId;
        private Optional<MeterContext> context;


        @Override
        public Meter.Builder forDevice(DeviceId deviceId) {
            this.deviceId = deviceId;
            return this;
        }

        @Override
        public Meter.Builder withId(int id) {
            this.id = MeterId.meterId(id);
            return this;
        }

        @Override
        public Meter.Builder fromApp(ApplicationId appId) {
            this.appId = appId;
            return this;
        }

        @Override
        public Meter.Builder withUnit(Unit unit) {
            this.unit = unit;
            return this;
        }

        @Override
        public Meter.Builder burst() {
            this.burst = true;
            return this;
        }

        @Override
        public Meter.Builder withBands(Collection<Band> bands) {
            this.bands = Collections.unmodifiableCollection(bands);
            return this;
        }

        @Override
        public Meter.Builder withContext(MeterContext context) {
            this.context = Optional.<MeterContext>ofNullable(context);
            return this;
        }

        @Override
        public Meter build() {
            checkNotNull(deviceId, "Must specify a device");
            checkNotNull(bands, "Must have bands.");
            checkArgument(bands.size() > 0, "Must have at least one band.");
            checkNotNull(appId, "Must have an application id");
            checkNotNull(id, "Must specify a meter id");
            return new DefaultMeter(deviceId, id, appId, unit, burst, bands, context);
        }


    }
}
