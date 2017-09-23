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
package org.onosproject.upgrade;

import java.util.Objects;

import com.google.common.annotations.Beta;
import com.google.common.base.MoreObjects;
import org.onosproject.event.AbstractEvent;

/**
 * Upgrade event.
 */
@Beta
public class UpgradeEvent extends AbstractEvent<UpgradeEvent.Type, Upgrade> {

    /**
     * Type of upgrade-related events.
     */
    @Beta
    public enum Type {

        /**
         * Indicates that a new upgrade was initialized.
         */
        INITIALIZED,

        /**
         * Indicates that mastership was reassigned to the upgraded cluster.
         */
        UPGRADED,

        /**
         * Indicates that an upgrade was committed.
         */
        COMMITTED,

        /**
         * Indicates that an upgrade was rolled back.
         */
        ROLLED_BACK,

        /**
         * Indicates that an upgrade was reset.
         */
        RESET,
    }

    /**
     * Creates an event of a given type and for the specified state and the
     * current time.
     *
     * @param type  upgrade event type
     * @param state upgrade state
     */
    public UpgradeEvent(UpgradeEvent.Type type, Upgrade state) {
        super(type, state);
    }

    /**
     * Creates an event of a given type and for the specified state and time.
     *
     * @param type  upgrade event type
     * @param state upgrade state
     * @param time  occurrence time
     */
    public UpgradeEvent(UpgradeEvent.Type type, Upgrade state, long time) {
        super(type, state, time);
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
        if (obj instanceof UpgradeEvent) {
            final UpgradeEvent other = (UpgradeEvent) obj;
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

}
