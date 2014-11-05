package org.onlab.onos.store.service;


/**
 * Exception that indicates a write operation is aborted.
 * Aborted operations do not mutate database state is any form.
 */
@SuppressWarnings("serial")
public class WriteAborted extends DatabaseException {
}
