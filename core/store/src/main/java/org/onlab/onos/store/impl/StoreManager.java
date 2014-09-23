package org.onlab.onos.store.impl;

import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.onlab.onos.store.StoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Auxiliary bootstrap of distributed store.
 */
@Component(immediate = true)
@Service
public class StoreManager implements StoreService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    protected HazelcastInstance instance;

    @Activate
    public void activate() {
        instance = Hazelcast.newHazelcastInstance();
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        log.info("Stopped");
    }

    @Override
    public HazelcastInstance getHazelcastInstance() {
        return instance;
    }
}
