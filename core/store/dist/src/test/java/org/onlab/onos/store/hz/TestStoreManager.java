package org.onlab.onos.store.hz;

import java.io.FileNotFoundException;
import java.util.UUID;

import com.hazelcast.config.Config;
import com.hazelcast.config.FileSystemXmlConfig;
import com.hazelcast.core.HazelcastInstance;

/**
 * Dummy StoreManager to use specified Hazelcast instance.
 */
public class TestStoreManager extends StoreManager {

    /**
     * Gets the Hazelcast Config for testing.
     *
     * @return
     */
    public static Config getTestConfig() {
        Config config;
        try {
            config = new FileSystemXmlConfig(HAZELCAST_XML_FILE);
        } catch (FileNotFoundException e) {
            // falling back to default
            config = new Config();
        }
        // avoid accidentally joining other cluster
        config.getGroupConfig().setName(UUID.randomUUID().toString());
        // quickly form single node cluster
        config.getNetworkConfig().getJoin()
            .getTcpIpConfig()
            .setEnabled(true).setConnectionTimeoutSeconds(0);
        config.getNetworkConfig().getJoin()
            .getMulticastConfig()
            .setEnabled(false);
        return config;
    }

    /**
     * Constructor.
     *
     * @param instance Hazelast instance to return on #getHazelcastInstance()
     */
    public TestStoreManager(HazelcastInstance instance) {
        this.instance = instance;
    }

    @Override
    public void activate() {
        // Hazelcast setup removed from original code.
    }
}
