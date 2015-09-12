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

package org.onosproject.store.service;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.common.base.MoreObjects;

/**
 * Database update operation.
 *
 */
public final class DatabaseUpdate {

    /**
     * Type of database update operation.
     */
    public enum Type {
        /**
         * Insert/Update entry without any checks.
         */
        PUT,
        /**
         * Insert an entry iff there is no existing entry for that key.
         */
        PUT_IF_ABSENT,

        /**
         * Update entry if the current version matches specified version.
         */
        PUT_IF_VERSION_MATCH,

        /**
         * Update entry if the current value matches specified value.
         */
        PUT_IF_VALUE_MATCH,

        /**
         * Remove entry without any checks.
         */
        REMOVE,

        /**
         * Remove entry if the current version matches specified version.
         */
        REMOVE_IF_VERSION_MATCH,

        /**
         * Remove entry if the current value matches specified value.
         */
        REMOVE_IF_VALUE_MATCH,
    }

    private Type type;
    private String mapName;
    private String key;
    private byte[] value;
    private byte[] currentValue;
    private long currentVersion = -1;

    /**
     * Returns the type of update operation.
     * @return type of update.
     */
    public Type type() {
        return type;
    }

    /**
     * Returns the name of map being updated.
     * @return map name.
     */
    public String mapName() {
        return mapName;
    }

    /**
     * Returns the item key being updated.
     * @return item key
     */
    public String key() {
        return key;
    }

    /**
     * Returns the new value.
     * @return item's target value.
     */
    public byte[] value() {
        return value;
    }

    /**
     * Returns the expected current value in the database value for the key.
     * @return current value in database.
     */
    public byte[] currentValue() {
        return currentValue;
    }

    /**
     * Returns the expected current version in the database for the key.
     * @return expected version.
     */
    public long currentVersion() {
        return currentVersion;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("type", type)
            .add("mapName", mapName)
            .add("key", key)
            .add("value", value)
            .add("currentValue", currentValue)
            .add("currentVersion", currentVersion)
            .toString();
    }

    /**
     * Creates a new builder instance.
     *
     * @return builder.
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * DatabaseUpdate builder.
     *
     */
    public static final class Builder {

        private DatabaseUpdate update = new DatabaseUpdate();

        public DatabaseUpdate build() {
            validateInputs();
            return update;
        }

        public Builder withType(Type type) {
            update.type = checkNotNull(type, "type cannot be null");
            return this;
        }

        public Builder withMapName(String mapName) {
            update.mapName = checkNotNull(mapName, "mapName cannot be null");
            return this;
        }

        public Builder withKey(String key) {
            update.key = checkNotNull(key, "key cannot be null");
            return this;
        }

        public Builder withCurrentValue(byte[] value) {
            update.currentValue = checkNotNull(value, "currentValue cannot be null");
            return this;
        }

        public Builder withValue(byte[] value) {
            update.value = checkNotNull(value, "value cannot be null");
            return this;
        }

        public Builder withCurrentVersion(long version) {
            checkArgument(version >= 0, "version cannot be negative");
            update.currentVersion = version;
            return this;
        }

        private void validateInputs() {
            checkNotNull(update.type, "type must be specified");
            checkNotNull(update.mapName, "map name must be specified");
            checkNotNull(update.key, "key must be specified");
            switch (update.type) {
            case PUT:
            case PUT_IF_ABSENT:
                checkNotNull(update.value, "value must be specified.");
                break;
            case PUT_IF_VERSION_MATCH:
                checkNotNull(update.value, "value must be specified.");
                checkState(update.currentVersion >= 0, "current version must be specified");
                break;
            case PUT_IF_VALUE_MATCH:
                checkNotNull(update.value, "value must be specified.");
                checkNotNull(update.currentValue, "currentValue must be specified.");
                break;
            case REMOVE:
                break;
            case REMOVE_IF_VERSION_MATCH:
                checkState(update.currentVersion >= 0, "current version must be specified");
                break;
            case REMOVE_IF_VALUE_MATCH:
                checkNotNull(update.currentValue, "currentValue must be specified.");
                break;
            default:
                throw new IllegalStateException("Unknown operation type");
            }
        }
    }
}
