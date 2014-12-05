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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.store.service.WriteRequest.Type.*;

import java.util.Objects;

import org.onlab.util.ByteArraySizeHashPrinter;

import com.google.common.base.MoreObjects;

/**
 * Database write request.
 */
public class WriteRequest {

    public static final int ANY_VERSION = -1;

    private final String tableName;
    private final String key;

    private final Type type;

    private final byte[] newValue;
    private final long previousVersion;
    private final byte[] oldValue;

    /**
     * Creates a write request, which will
     * put the specified value to the table regardless of the previous value.
     *
     * @param tableName name of the table
     * @param key       key in the table
     * @param newValue  value to write, must not be null
     * @return WriteRequest
     */
    public static WriteRequest put(String tableName, String key,
                                   byte[] newValue) {
        return new WriteRequest(PUT, tableName, key,
                                checkNotNull(newValue), ANY_VERSION, null);
    }

    /**
     * Creates a write request, which will
     * put the specified value to the table if the previous version matches.
     *
     * @param tableName name of the table
     * @param key       key in the table
     * @param newValue  value to write, must not be null
     * @param previousVersion previous version expected
     * @return WriteRequest
     */
    public static WriteRequest putIfVersionMatches(String tableName, String key,
                                                   byte[] newValue,
                                                   long previousVersion) {
        checkArgument(previousVersion >= 0);
        return new WriteRequest(PUT_IF_VERSION, tableName, key,
                                checkNotNull(newValue), previousVersion, null);
    }

    /**
     * Creates a write request, which will
     * put the specified value to the table if the previous value matches.
     *
     * @param tableName name of the table
     * @param key       key in the table
     * @param oldValue  previous value expected, must not be null
     * @param newValue  value to write, must not be null
     * @return WriteRequest
     */
    public static WriteRequest putIfValueMatches(String tableName, String key,
                                                 byte[] oldValue,
                                                 byte[] newValue) {
        return new WriteRequest(PUT_IF_VALUE, tableName, key,
                                checkNotNull(newValue), ANY_VERSION,
                                checkNotNull(oldValue));
    }

    /**
     * Creates a write request, which will
     * put the specified value to the table if the previous value does not exist.
     *
     * @param tableName name of the table
     * @param key       key in the table
     * @param newValue  value to write, must not be null
     * @return WriteRequest
     */
    public static WriteRequest putIfAbsent(String tableName, String key,
                                           byte[] newValue) {
        return new WriteRequest(PUT_IF_ABSENT, tableName, key,
                                checkNotNull(newValue), ANY_VERSION, null);
    }

    /**
     * Creates a write request, which will
     * remove the specified entry from the table regardless of the previous value.
     *
     * @param tableName name of the table
     * @param key       key in the table
     * @return WriteRequest
     */
    public static WriteRequest remove(String tableName, String key) {
        return new WriteRequest(REMOVE, tableName, key,
                                null, ANY_VERSION, null);
    }

    /**
     * Creates a write request, which will
     * remove the specified entry from the table if the previous version matches.
     *
     * @param tableName name of the table
     * @param key       key in the table
     * @param previousVersion previous version expected
     * @return WriteRequest
     */
    public static WriteRequest removeIfVersionMatches(String tableName, String key,
                                      long previousVersion) {
        return new WriteRequest(REMOVE_IF_VERSION, tableName, key,
                                null, previousVersion, null);
    }

    /**
     * Creates a write request, which will
     * remove the specified entry from the table if the previous value matches.
     *
     * @param tableName name of the table
     * @param key       key in the table
     * @param oldValue  previous value expected, must not be null
     * @return WriteRequest
     */
    public static WriteRequest removeIfValueMatches(String tableName, String key,
                                      byte[] oldValue) {
        return new WriteRequest(REMOVE_IF_VALUE, tableName, key,
                                null, ANY_VERSION, checkNotNull(oldValue));
    }

    public enum Type {
        PUT,
        PUT_IF_VERSION,
        PUT_IF_VALUE,
        PUT_IF_ABSENT,
        REMOVE,
        REMOVE_IF_VERSION,
        REMOVE_IF_VALUE,
    }

    // hidden constructor
    protected WriteRequest(Type type, String tableName, String key,
                           byte[] newValue,
                           long previousVersion, byte[] oldValue) {

        checkNotNull(tableName);
        checkNotNull(key);

        this.tableName = tableName;
        this.key = key;
        this.type = type;
        this.newValue = newValue;
        this.previousVersion = previousVersion;
        this.oldValue = oldValue;
    }

    public String tableName() {
        return tableName;
    }

    public String key() {
        return key;
    }

    public WriteRequest.Type type() {
        return type;
    }

    public byte[] newValue() {
        return newValue;
    }

    public long previousVersion() {
        return previousVersion;
    }

    public byte[] oldValue() {
        return oldValue;
    }

    @Override
    public String toString() {
         return MoreObjects.toStringHelper(getClass())
                .add("type", type)
                .add("tableName", tableName)
                .add("key", key)
                .add("newValue", ByteArraySizeHashPrinter.orNull(newValue))
                .add("previousVersion", previousVersion)
                .add("oldValue", ByteArraySizeHashPrinter.orNull(oldValue))
                .toString();
    }

    // TODO: revisit hashCode, equals condition
    @Override
    public int hashCode() {
        return Objects.hash(type, key, tableName, previousVersion);
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
        WriteRequest other = (WriteRequest) obj;
        return Objects.equals(this.type, other.type) &&
                Objects.equals(this.key, other.key) &&
                Objects.equals(this.tableName, other.tableName) &&
                Objects.equals(this.previousVersion, other.previousVersion);
    }
}
