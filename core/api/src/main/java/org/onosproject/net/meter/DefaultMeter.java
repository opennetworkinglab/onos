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

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableSet;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.DeviceId;

import java.util.Collection;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A default implementation of a meter.
 */
public final class DefaultMeter implements Meter, MeterEntry  {


    private final MeterId id;
    private final ApplicationId appId;
    private final Unit unit;
    private final boolean burst;
    private final Collection<Band> bands;
    private final DeviceId deviceId;

    private MeterState state;
    private long life;
    private long refCount;
    private long packets;
    private long bytes;

    private DefaultMeter(DeviceId deviceId, MeterId id, ApplicationId appId,
                        Unit unit, boolean burst,
                        Collection<Band> bands) {
        this.deviceId = deviceId;
        this.id = id;
        this.appId = appId;
        this.unit = unit;
        this.burst = burst;
        this.bands = bands;
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
    public MeterState state() {
        return state;
    }

    @Override
    public long life() {
        return life;
    }

    @Override
    public long referenceCount() {
        return refCount;
    }

    @Override
    public long packetsSeen() {
        return packets;
    }

    @Override
    public long bytesSeen() {
        return bytes;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public void setState(MeterState state) {
        this.state = state;
    }

    @Override
    public void setLife(long life) {
        this.life = life;
    }

    @Override
    public void setReferenceCount(long count) {
        this.refCount = count;
    }

    @Override
    public void setProcessedPackets(long packets) {
        this.packets = packets;
    }

    @Override
    public void setProcessedBytes(long bytes) {
        this.bytes = bytes;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("device", deviceId)
                .add("id", id)
                .add("appId", appId.name())
                .add("unit", unit)
                .add("isBurst", burst)
                .add("state", state)
                .add("bands", bands).toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DefaultMeter that = (DefaultMeter) o;
        return Objects.equal(id, that.id) &&
                Objects.equal(appId, that.appId) &&
                Objects.equal(unit, that.unit) &&
                Objects.equal(deviceId, that.deviceId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id, appId, unit, deviceId);
    }

    public static final class Builder implements Meter.Builder {

        private MeterId id;
        private ApplicationId appId;
        private Unit unit = Unit.KB_PER_SEC;
        private boolean burst = false;
        private Collection<Band> bands;
        private DeviceId deviceId;


        @Override
        public Meter.Builder forDevice(DeviceId deviceId) {
            this.deviceId = deviceId;
            return this;
        }

        @Override
        public Meter.Builder withId(MeterId id) {
            this.id = id;
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
            this.bands = ImmutableSet.copyOf(bands);
            return this;
        }

        @Override
        public DefaultMeter build() {
            checkNotNull(deviceId, "Must specify a device");
            checkNotNull(bands, "Must have bands.");
            checkArgument(!bands.isEmpty(), "Must have at least one band.");
            checkNotNull(appId, "Must have an application id");
            checkNotNull(id, "Must specify a meter id");
            return new DefaultMeter(deviceId, id, appId, unit, burst, bands);
        }


    }
}
