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

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;

import com.google.common.base.MoreObjects;

/**
 * Database read request.
 */
public class ReadRequest {

    private final String tableName;
    private final String key;

    /**
     * Creates a read request,
     * which will retrieve the specified key from the table.
     *
     * @param tableName name of the table
     * @param key       key in the table
     * @return ReadRequest
     */
    public static ReadRequest get(String tableName, String key) {
        return new ReadRequest(tableName, key);
    }

    public ReadRequest(String tableName, String key) {
        this.tableName = checkNotNull(tableName);
        this.key = checkNotNull(key);
    }

    /**
     * Return the name of the table.
     * @return table name.
     */
    public String tableName() {
        return tableName;
    }

    /**
     * Returns the key.
     * @return key.
     */
    public String key() {
        return key;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("tableName", tableName)
                .add("key", key)
                .toString();
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, tableName);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        ReadRequest other = (ReadRequest) obj;
        return Objects.equals(this.key, other.key) &&
                Objects.equals(this.tableName, other.tableName);
    }
}
