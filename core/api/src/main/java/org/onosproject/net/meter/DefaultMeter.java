/*
 * Copyright 2015-present Open Networking Foundation
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
import org.onosproject.net.AbstractAnnotated;
import org.onosproject.net.Annotations;
import org.onosproject.net.DeviceId;

import java.util.Collection;
import java.util.Optional;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.net.meter.MeterCellId.MeterCellType.INDEX;

/**
 * A default implementation of a meter.
 */
public final class DefaultMeter extends AbstractAnnotated implements Meter, MeterEntry {

    private final MeterCellId cellId;
    private final Optional<ApplicationId> appId;
    private final Unit unit;
    private final boolean burst;
    private final Collection<Band> bands;
    private final DeviceId deviceId;

    private MeterState state;
    private long life;
    private long refCount;
    private long packets;
    private long bytes;

    private DefaultMeter(DeviceId deviceId, MeterCellId cellId,
                         Optional<ApplicationId> appId, Unit unit,
                         boolean burst, Collection<Band> bands,
                         Annotations... annotations) {
        super(annotations);
        this.deviceId = deviceId;
        this.cellId = cellId;
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
        // Workaround until we remove this method. Deprecated in 1.13.
        // Should use meterCellId() instead.
        return cellId.type() == INDEX
                ? (MeterId) cellId
                : MeterId.meterId((cellId.hashCode()));
    }

    @Override
    public MeterCellId meterCellId() {
        return cellId;
    }

    @Override
    public ApplicationId appId() {
        return appId.orElse(null);
        // TODO: Deprecate this API because AppId becomes optional in Meter
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
                .add("cellId", cellId)
                .add("appId", appId.orElse(null))
                .add("unit", unit)
                .add("isBurst", burst)
                .add("state", state)
                .add("bands", bands)
                .add("annotations", annotations())
                .toString();
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
        return Objects.equal(cellId, that.cellId) &&
                Objects.equal(appId, that.appId) &&
                Objects.equal(unit, that.unit) &&
                Objects.equal(deviceId, that.deviceId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(cellId, appId, unit, deviceId);
    }

    public static final class Builder implements Meter.Builder {

        private MeterCellId cellId;
        private Optional<ApplicationId> appId = Optional.empty();
        private Unit unit = Unit.KB_PER_SEC;
        private boolean burst = false;
        private Collection<Band> bands;
        private DeviceId deviceId;
        private Annotations annotations;

        @Override
        public Meter.Builder forDevice(DeviceId deviceId) {
            this.deviceId = deviceId;
            return this;
        }

        @Override
        public Meter.Builder withId(MeterId id) {
            this.withCellId(id);
            return this;
        }

        @Override
        public Meter.Builder withCellId(MeterCellId cellId) {
            this.cellId = cellId;
            return this;
        }

        @Override
        public Meter.Builder fromApp(ApplicationId appId) {
            this.appId = Optional.ofNullable(appId);
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
        public Builder withAnnotations(Annotations anns) {
            this.annotations = anns;
            return this;
        }

        @Override
        public DefaultMeter build() {
            checkNotNull(deviceId, "Must specify a device");
            checkNotNull(bands, "Must have bands.");
            checkArgument(!bands.isEmpty(), "Must have at least one band.");
            checkArgument(cellId != null, "Must specify a cell id.");
            return new DefaultMeter(deviceId, cellId, appId, unit, burst, bands,
                                    annotations);
        }


    }
}
