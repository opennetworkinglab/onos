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

import java.util.List;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

/**
 * Collection of write requests to be submitted as one batch.
 */
public final class BatchWriteRequest {

    private final List<WriteRequest> writeRequests;

    /**
     * Creates a new BatchWriteRequest object from the specified list of write requests.
     * @param writeRequests write requests.
     * @return BatchWriteRequest object.
     */
    public static BatchWriteRequest create(List<WriteRequest> writeRequests) {
        return new BatchWriteRequest(writeRequests);
    }

    private BatchWriteRequest(List<WriteRequest> writeRequests) {
        this.writeRequests = ImmutableList.copyOf(writeRequests);
    }

    /**
     * Returns the requests in this batch as a list.
     * @return list of write requests
     */
    public List<WriteRequest> getAsList() {
        return writeRequests;
    }

    /**
     * Returns the number of requests in this batch.
     * @return size of request batch.
     */
    public int batchSize() {
        return writeRequests.size();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("writeRequests", writeRequests)
                .toString();
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * Builder for BatchWriteRequest.
     */
    public static class Builder {

        private final List<WriteRequest> writeRequests = Lists.newLinkedList();

        public Builder put(String tableName, String key, byte[] value) {
            writeRequests.add(WriteRequest.put(tableName, key, value));
            return this;
        }

        public Builder putIfAbsent(String tableName, String key, byte[] value) {
            writeRequests.add(WriteRequest.putIfAbsent(tableName, key, value));
            return this;
        }

        public Builder putIfValueMatches(String tableName, String key, byte[] oldValue, byte[] newValue) {
            writeRequests.add(WriteRequest.putIfValueMatches(tableName, key, oldValue, newValue));
            return this;
        }

        public Builder putIfVersionMatches(String tableName, String key, byte[] value, long version) {
            writeRequests.add(WriteRequest.putIfVersionMatches(tableName, key, value, version));
            return this;
        }

        public Builder remove(String tableName, String key) {
            writeRequests.add(WriteRequest.remove(tableName, key));
            return this;
        }

        public Builder removeIfVersionMatches(String tableName, String key, long version) {
            writeRequests.add(WriteRequest.removeIfVersionMatches(tableName, key, version));
            return this;
        }

        public Builder removeIfValueMatches(String tableName, String key, byte[] value) {
            writeRequests.add(WriteRequest.removeIfValueMatches(tableName, key, value));
            return this;
        }

        public BatchWriteRequest build() {
            return new BatchWriteRequest(writeRequests);
        }
    }
}
