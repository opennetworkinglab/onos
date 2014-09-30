package org.onlab.onos.store.cluster.impl;

/**
 * Provides means to find a worker IO loop.
 */
public interface WorkerFinder {

    /**
     * Finds a suitable worker.
     *
     * @return available worker
     */
    ClusterIOWorker findWorker();
}
