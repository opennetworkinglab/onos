package org.onlab.onos.net.flow;

import java.util.Set;

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
     * Obtains a set of items which failed.
     * @return a set of failures
     */
    Set<T> failedItems();

}
