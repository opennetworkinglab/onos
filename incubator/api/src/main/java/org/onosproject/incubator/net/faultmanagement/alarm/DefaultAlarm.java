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
package org.onosproject.incubator.net.faultmanagement.alarm;

import org.onosproject.net.DeviceId;

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default implementation of an alarm.
 */
//TODO simpler creation and updating.
public final class DefaultAlarm implements Alarm {

    private final AlarmId id;

    private final DeviceId deviceId;
    private final String description;
    private final AlarmEntityId source;
    private final long timeRaised;
    private final long timeUpdated;
    private final Long timeCleared;
    private final SeverityLevel severity;
    private final boolean isServiceAffecting;
    private final boolean isAcknowledged;
    private final boolean isManuallyClearable;
    private final String assignedUser;

    //Only for Kryo
    DefaultAlarm() {
        id = null;
        deviceId = null;
        description = null;
        source = null;
        timeRaised = -1;
        timeUpdated = -1;
        timeCleared = null;
        severity = null;
        isServiceAffecting = false;
        isAcknowledged = false;
        isManuallyClearable = false;
        assignedUser = null;
    }

    /**
     * Instantiates a new Default alarm.
     *
     * @param id                  the id
     * @param deviceId            the device id
     * @param description         the description
     * @param source              the source, null indicates none.
     * @param timeRaised          the time raised.
     * @param timeUpdated         the time last updated.
     * @param timeCleared         the time cleared, null indicates uncleared.
     * @param severity            the severity
     * @param isServiceAffecting  the service affecting
     * @param isAcknowledged      the acknowledged
     * @param isManuallyClearable the manually clearable
     * @param assignedUser        the assigned user, `null` indicates none.
     */
    private DefaultAlarm(final AlarmId id,
                         final DeviceId deviceId,
                         final String description,
                         final AlarmEntityId source,
                         final long timeRaised,
                         final long timeUpdated,
                         final Long timeCleared,
                         final SeverityLevel severity,
                         final boolean isServiceAffecting,
                         final boolean isAcknowledged,
                         final boolean isManuallyClearable,
                         final String assignedUser) {
        this.id = id;
        this.deviceId = deviceId;
        this.description = description;
        this.source = source;
        this.timeRaised = timeRaised;
        this.timeUpdated = timeUpdated;
        this.timeCleared = timeCleared;
        this.severity = severity;
        this.isServiceAffecting = isServiceAffecting;
        this.isAcknowledged = isAcknowledged;
        this.isManuallyClearable = isManuallyClearable;
        this.assignedUser = assignedUser;
    }

    @Override
    public AlarmId id() {
        return id;
    }

    @Override
    public DeviceId deviceId() {
        return deviceId;
    }

    @Override
    public String description() {
        return description;
    }

    @Override
    public AlarmEntityId source() {
        return source;
    }

    @Override
    public long timeRaised() {
        return timeRaised;
    }

    @Override
    public long timeUpdated() {
        return timeUpdated;
    }

    @Override
    public Long timeCleared() {
        return timeCleared;
    }

    @Override
    public SeverityLevel severity() {
        return severity;
    }

    @Override
    public boolean serviceAffecting() {
        return isServiceAffecting;
    }

    @Override
    public boolean acknowledged() {
        return isAcknowledged;
    }

    @Override
    public boolean manuallyClearable() {
        return isManuallyClearable;
    }

    @Override
    public String assignedUser() {
        return assignedUser;
    }

    @Override
    public int hashCode() {
        // id or timeRaised or timeUpdated may differ
        return Objects.hash(deviceId, description,
                            source, timeCleared, severity,
                            isServiceAffecting, isAcknowledged,
                            isManuallyClearable, assignedUser);
    }

    @Override
    public boolean equals(final Object obj) {
        // Make sure equals() is tune with hashCode() so works ok in a hashSet !
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final DefaultAlarm other = (DefaultAlarm) obj;

        // id or timeRaised or timeUpdated may differ
        if (!Objects.equals(this.deviceId, other.deviceId)) {
            return false;
        }
        if (!Objects.equals(this.description, other.description)) {
            return false;
        }
        if (!Objects.equals(this.source, other.source)) {
            return false;
        }

        if (!Objects.equals(this.timeCleared, other.timeCleared)) {
            return false;
        }
        if (this.severity != other.severity) {
            return false;
        }
        if (this.isServiceAffecting != other.isServiceAffecting) {
            return false;
        }
        if (this.isAcknowledged != other.isAcknowledged) {
            return false;
        }
        if (this.isManuallyClearable != other.isManuallyClearable) {
            return false;
        }
        if (!Objects.equals(this.assignedUser, other.assignedUser)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("id", id)
                .add("deviceId", deviceId)
                .add("description", description)
                .add("source", source)
                .add("timeRaised", timeRaised)
                .add("timeUpdated", timeUpdated)
                .add("timeCleared", timeCleared)
                .add("severity", severity)
                .add("serviceAffecting", isServiceAffecting)
                .add("acknowledged", isAcknowledged)
                .add("manuallyClearable", isManuallyClearable)
                .add("assignedUser", assignedUser)
                .toString();
    }

    public static class Builder {

        // Manadatory fields when constructing alarm ...
        private AlarmId id;
        private final DeviceId deviceId;
        private final String description;
        private SeverityLevel severity;
        private final long timeRaised;

        // Optional fields ..
        private AlarmEntityId source = AlarmEntityId.NONE;
        private long timeUpdated;
        private Long timeCleared = null;
        private boolean isServiceAffecting = false;
        private boolean isAcknowledged = false;
        private boolean isManuallyClearable = false;
        private String assignedUser = null;

        public Builder(final Alarm alarm) {
            this(alarm.deviceId(), alarm.description(), alarm.severity(), alarm.timeRaised());
            this.source = alarm.source();
            this.timeUpdated = alarm.timeUpdated();
            this.timeCleared = alarm.timeCleared();
            this.isServiceAffecting = alarm.serviceAffecting();
            this.isAcknowledged = alarm.acknowledged();
            this.isManuallyClearable = alarm.manuallyClearable();
            this.assignedUser = alarm.assignedUser();

        }

        public Builder(final DeviceId deviceId,
                       final String description, final SeverityLevel severity, final long timeRaised) {
            super();
            this.id = AlarmId.NONE;
            this.deviceId = deviceId;
            this.description = description;
            this.severity = severity;
            this.timeRaised = timeRaised;
            // Unless specified time-updated is same as raised.
            this.timeUpdated = timeRaised;
        }

        public Builder forSource(final AlarmEntityId source) {
            this.source = source;
            return this;
        }

        public Builder withTimeUpdated(final long timeUpdated) {
            this.timeUpdated = timeUpdated;
            return this;
        }

        public Builder withTimeCleared(final Long timeCleared) {
            this.timeCleared = timeCleared;
            return this;
        }

        public Builder withId(final AlarmId id) {
            this.id = id;
            return this;
        }

        public Builder clear() {
            this.severity = SeverityLevel.CLEARED;
            final long now = System.currentTimeMillis();
            return withTimeCleared(now).withTimeUpdated(now);
        }

        public Builder withServiceAffecting(final boolean isServiceAffecting) {
            this.isServiceAffecting = isServiceAffecting;
            return this;
        }

        public Builder withAcknowledged(final boolean isAcknowledged) {
            this.isAcknowledged = isAcknowledged;
            return this;
        }

        public Builder withManuallyClearable(final boolean isManuallyClearable) {
            this.isManuallyClearable = isManuallyClearable;
            return this;
        }

        public Builder withAssignedUser(final String assignedUser) {
            this.assignedUser = assignedUser;
            return this;
        }

        public DefaultAlarm build() {
            checkNotNull(id, "Must specify an alarm id");
            checkNotNull(deviceId, "Must specify a device");
            checkNotNull(description, "Must specify a description");
            checkNotNull(timeRaised, "Must specify a time raised");
            checkNotNull(timeUpdated, "Must specify a time updated");
            checkNotNull(severity, "Must specify a severity");

            return new DefaultAlarm(id, deviceId, description, source, timeRaised, timeUpdated, timeCleared,
                                    severity, isServiceAffecting, isAcknowledged, isManuallyClearable, assignedUser);
        }
    }
}
