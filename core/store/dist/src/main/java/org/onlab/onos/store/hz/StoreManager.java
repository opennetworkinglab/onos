package org.onlab.onos.store.hz;

import com.hazelcast.config.Config;
import com.hazelcast.config.FileSystemXmlConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;

/**
 * Auxiliary bootstrap of distributed store.
 */
@Component(immediate = true)
@Service
public class StoreManager implements StoreService {

    protected static final String HAZELCAST_XML_FILE = "etc/hazelcast.xml";

    private final Logger log = LoggerFactory.getLogger(getClass());

    protected HazelcastInstance instance;

    @Activate
    public void activate() {
        try {
            Config config = new FileSystemXmlConfig(HAZELCAST_XML_FILE);
            instance = Hazelcast.newHazelcastInstance(config);
            log.info("Started");
        } catch (FileNotFoundException e) {
            log.error("Unable to configure Hazelcast", e);
        }
    }

    @Deactivate
    public void deactivate() {
        instance.shutdown();
        log.info("Stopped");
    }

    @Override
    public HazelcastInstance getHazelcastInstance() {
        return instance;
    }

}
