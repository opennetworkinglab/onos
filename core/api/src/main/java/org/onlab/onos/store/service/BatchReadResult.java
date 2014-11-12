package org.onlab.onos.store.service;

import java.util.Collections;
import java.util.List;

/**
 * Result of a batch read operation.
 */
public class BatchReadResult {

    private final List<ReadResult> readResults;

    public BatchReadResult(List<ReadResult> readResults)  {
        this.readResults = Collections.unmodifiableList(readResults);
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
}
