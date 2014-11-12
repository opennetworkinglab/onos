package org.onlab.onos.store.service;

import java.util.Collections;
import java.util.List;

/**
 * Result of a batch write operation.
 */
public class BatchWriteResult {

    private final List<WriteResult> writeResults;

    public BatchWriteResult(List<WriteResult> writeResults) {
        this.writeResults = Collections.unmodifiableList(writeResults);
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
}
