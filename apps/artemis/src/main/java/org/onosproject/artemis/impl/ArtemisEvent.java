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
package org.onosproject.artemis.impl;

import com.google.common.base.MoreObjects;
import org.onosproject.event.AbstractEvent;

import java.util.Objects;

/**
 * Artemis event.
 */
public class ArtemisEvent extends AbstractEvent<ArtemisEvent.Type, Object> {

    /**
     * Creates an event of a given type and for the specified state and the
     * current time.
     *
     * @param type    upgrade event type
     * @param subject upgrade state
     */
    protected ArtemisEvent(Type type, Object subject) {
        super(type, subject);
    }

    /**
     * Creates an event of a given type and for the specified state and time.
     *
     * @param type    upgrade event type
     * @param subject upgrade state
     * @param time    occurrence time
     */
    protected ArtemisEvent(Type type, Object subject, long time) {
        super(type, subject, time);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type(), subject(), time());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof ArtemisEvent) {
            final ArtemisEvent other = (ArtemisEvent) obj;
            return Objects.equals(this.type(), other.type()) &&
                    Objects.equals(this.subject(), other.subject()) &&
                    Objects.equals(this.time(), other.time());
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this.getClass())
                .add("type", type())
                .add("subject", subject())
                .add("time", time())
                .toString();
    }

    /**
     * Type of artemis-related events.
     */
    public enum Type {

        /**
         * Indicates that a hijack was detected.
         */
        HIJACK_ADDED,

        /**
         * Indicates that a bgp update message was received.
         */
        BGPUPDATE_ADDED,
    }
}
