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

import com.google.common.annotations.Beta;
import org.onosproject.core.Version;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Represents the state of an upgrade.
 * <p>
 * An upgrade consists of a {@link #source() source} and {@link #target() target} version and an upgrade
 * {@link #status()}.
 */
@Beta
public class Upgrade {

    /**
     * Represents the phase of the upgrade protocol.
     */
    @Beta
    public enum Status {

        /**
         * Represents state in which no upgrade has been initialized.
         */
        INACTIVE(false, false),

        /**
         * Indicates that an upgrade is being initialized.
         */
        INITIALIZING(true, false),

        /**
         * Indicates that an upgrade has been initialized.
         */
        INITIALIZED(true, false),

        /**
         * Indicates that an upgrade is in progress.
         */
        UPGRADING(true, true),

        /**
         * Indicates that an upgrade is complete.
         */
        UPGRADED(true, true),

        /**d
         * Indicates that an upgrade is being committed.
         */
        COMMITTING(true, true),

        /**
         * Indicates that an upgrade has been committed.
         */
        COMMITTED(false, true),

        /**
         * Indicates that an upgrade is being rolled back.
         */
        ROLLING_BACK(true, false),

        /**
         * Indicates that an upgrade has been rolled back.
         */
        ROLLED_BACK(true, false),

        /**
         * Indicates that an upgrade is being reset.
         */
        RESETTING(true, false),

        /**
         * Indicates that an upgrade has been reset.
         */
        RESET(false, false);

        private final boolean active;
        private final boolean upgraded;

        Status(boolean active, boolean upgraded) {
            this.active = active;
            this.upgraded = upgraded;
        }

        /**
         * Returns whether the upgrade status is active.
         *
         * @return whether the upgrade status is active
         */
        public boolean active() {
            return active;
        }

        /**
         * Returns whether the upgraded version is active.
         *
         * @return whether the upgraded version is active
         */
        public boolean upgraded() {
            return upgraded;
        }
    }

    private final Version source;
    private final Version target;
    private final Status status;

    public Upgrade(Version source, Version target, Status status) {
        this.source = source;
        this.target = target;
        this.status = status;
    }

    /**
     * Returns the source version.
     *
     * @return the source version
     */
    public Version source() {
        return source;
    }

    /**
     * Returns the target version.
     *
     * @return the target version
     */
    public Version target() {
        return target;
    }

    /**
     * Returns the upgrade status.
     *
     * @return the upgrade status
     */
    public Status status() {
        return status;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("source", source)
                .add("target", target)
                .add("status", status)
                .toString();
    }
}
