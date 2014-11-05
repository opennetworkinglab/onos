package org.onlab.onos.store.service;

import java.util.List;

/**
 * Service interface for a strongly consistent and durable
 * key value data store.
 */
public interface DatabaseService {

    /**
     * Performs a read on the database.
     * @param request read request.
     * @return ReadResult
     * @throws DatabaseException if there is a failure in executing read.
     */
    ReadResult read(ReadRequest request);

    /**
     * Performs a batch read operation on the database.
     * The main advantage of batch read operation is parallelization.
     * @param batch batch of read requests to execute.
     * @return batch read result.
     */
    List<OptionalResult<ReadResult, DatabaseException>> batchRead(List<ReadRequest> batch);

    /**
     * Performs a write operation on the database.
     * @param request
     * @return write result.
     * @throws DatabaseException if there is failure in execution write.
     */
    WriteResult write(WriteRequest request);

    /**
     * Performs a batch write operation on the database.
     * Batch write provides transactional semantics. Either all operations
     * succeed or none of them do.
     * @param batch batch of write requests to execute as a transaction.
     * @return result of executing the batch write operation.
     */
    List<OptionalResult<WriteResult, DatabaseException>> batchWrite(List<WriteRequest> batch);
}
