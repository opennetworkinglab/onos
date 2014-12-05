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

/**
 * Result of a batch write operation.
 */
public class BatchWriteResult {

    private final List<WriteResult> writeResults;

    public BatchWriteResult(List<WriteResult> writeResults) {
        this.writeResults = ImmutableList.copyOf(writeResults);
    }

    /**
     * Returns true if this batch write operation was successful.
     * @return true if successful, false otherwise.
     */
    public boolean isSuccessful() {
        for (WriteResult result : writeResults) {
            if (result.status() != WriteStatus.OK) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the results as a List.
     * @return list of batch results.
     */
    public List<WriteResult> getAsList() {
        return this.writeResults;
    }

    /**
     * Returns the size of this batch.
     * @return batch size.
     */
    public int batchSize() {
        return writeResults.size();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("writeResults", writeResults)
                .toString();
    }
}
