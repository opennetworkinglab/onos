/*
 * Copyright 2014 Open Networking Laboratory
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

import com.google.common.base.MoreObjects;


/**
 * Database read result.
 */
public class ReadResult {

    private final String tableName;
    private final String key;
    private final VersionedValue value;
    private final ReadStatus status;

    public ReadResult(ReadStatus status, String tableName, String key, VersionedValue value) {
        this.status = status;
        this.tableName = tableName;
        this.key = key;
        this.value = value;
    }

    /**
     * Returns the status of the read operation.
     * @return read operation status
     */
    public ReadStatus status() {
        return status;
    }

    /**
     * Returns database table name.
     * @return table name
     */
    public String tableName() {
        return tableName;
    }

    /**
     * Returns database table key.
     * @return key
     */
    public String key() {
        return key;
    }

    /**
     * Returns true if database table contained value for the key.
     *
     * @return true if database table contained value for the key
     */
    public boolean valueExists() {
        return value != null;
    }

    /**
     * Returns value associated with the key.
     * @return non-null value if the table contains one, null otherwise.
     */
    public VersionedValue value() {
        return value;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("status", status)
                .add("tableName", tableName)
                .add("key", key)
                .add("value", value)
                .toString();
    }
}
