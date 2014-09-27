package org.onlab.onos.store.common;

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

    /**
     * Serializes the specified object into bytes using one of the
     * pre-registered serializers.
     *
     * @param obj object to be serialized
     * @return serialized bytes
     */
    public byte[] serialize(final Object obj);

    /**
     * Deserializes the specified bytes into an object using one of the
     * pre-registered serializers.
     *
     * @param bytes bytes to be deserialized
     * @return deserialized object
     */
    public <T> T deserialize(final byte[] bytes);

}
