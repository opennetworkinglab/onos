/*
 * Copyright 2014-2015 Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.onosproject.store.hz;

import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import com.hazelcast.config.Config;
import com.hazelcast.config.FileSystemXmlConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.store.cluster.impl.DistributedClusterStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

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
            File hazelcastFile = new File(HAZELCAST_XML_FILE);
            if (!hazelcastFile.exists()) {
                createDefaultHazelcastFile(hazelcastFile);
            }

            Config config = new FileSystemXmlConfig(HAZELCAST_XML_FILE);

            instance = Hazelcast.newHazelcastInstance(config);
            log.info("Started");
        } catch (FileNotFoundException e) {
            log.error("Unable to configure Hazelcast", e);
        }
    }

    private void createDefaultHazelcastFile(File hazelcastFile) {
        String ip = DistributedClusterStore.getSiteLocalAddress();
        String ipPrefix = ip.replaceFirst("\\.[0-9]*$", ".*");
        InputStream his = getClass().getResourceAsStream("/hazelcast.xml");
        try {
            String hzCfg = new String(ByteStreams.toByteArray(his), "UTF-8");
            hzCfg = hzCfg.replaceFirst("@NAME", ip);
            hzCfg = hzCfg.replaceFirst("@PREFIX", ipPrefix);
            Files.write(hzCfg.getBytes("UTF-8"), hazelcastFile);
        } catch (IOException e) {
            log.error("Unable to write default hazelcast file", e);
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
