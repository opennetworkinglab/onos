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
 * Result of a batch read operation.
 */
public class BatchReadResult {

    private final List<ReadResult> readResults;

    public BatchReadResult(List<ReadResult> readResults)  {
        this.readResults = ImmutableList.copyOf(readResults);
    }

    /**
     * Returns the results as a list.
     * @return list of results
     */
    public List<ReadResult> getAsList() {
        return readResults;
    }

    /**
     * Returns the batch size.
     * @return batch size
     */
    public int batchSize() {
        return readResults.size();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("readResults", readResults)
                .toString();
    }
}
