package org.onlab.onos.store.hz;

import com.hazelcast.core.HazelcastInstance;

/**
 * Bootstrap service to get a handle on a share Hazelcast instance.
 */
public interface StoreService {

    /**
     * Returns the shared Hazelcast instance for use as a distributed store
     * backing.
     *
     * @return shared Hazelcast instance
     */
    HazelcastInstance getHazelcastInstance();

}
