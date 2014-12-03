package org.onlab.onos.store.service;

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
