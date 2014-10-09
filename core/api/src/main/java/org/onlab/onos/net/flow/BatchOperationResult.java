package org.onlab.onos.net.flow;

import java.util.List;

/**
 * Interface capturing the result of a batch operation.
 *
 */
public interface BatchOperationResult<T> {

    /**
     * Returns whether the operation was successful.
     * @return true if successful, false otherwise
     */
    boolean isSuccess();

    /**
     * Obtains a list of items which failed.
     * @return a list of failures
     */
    List<T> failedItems();

}
