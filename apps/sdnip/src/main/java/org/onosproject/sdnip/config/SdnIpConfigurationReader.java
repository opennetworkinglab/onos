/*
 * Copyright 2014 Open Networking Laboratory
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
package org.onosproject.sdnip.config;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.onlab.packet.IpAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Implementation of SdnIpConfigurationService which reads SDN-IP configuration
 * from a file.
 */
public class SdnIpConfigurationReader implements SdnIpConfigurationService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String CONFIG_DIR = "../config";
    private static final String DEFAULT_CONFIG_FILE = "sdnip.json";
    private String configFileName = DEFAULT_CONFIG_FILE;

    private Map<String, BgpSpeaker> bgpSpeakers = new ConcurrentHashMap<>();
    private Map<IpAddress, BgpPeer> bgpPeers = new ConcurrentHashMap<>();

    /**
     * Reads SDN-IP related information contained in the configuration file.
     *
     * @param configFilename the name of the configuration file for the SDN-IP
     * application
     */
    private void readConfiguration(String configFilename) {
        File configFile = new File(CONFIG_DIR, configFilename);
        ObjectMapper mapper = new ObjectMapper();

        try {
            log.info("Loading config: {}", configFile.getAbsolutePath());
            Configuration config = mapper.readValue(configFile,
                                                    Configuration.class);
            for (BgpSpeaker speaker : config.getBgpSpeakers()) {
                bgpSpeakers.put(speaker.name(), speaker);
            }
            for (BgpPeer peer : config.getPeers()) {
                bgpPeers.put(peer.ipAddress(), peer);
            }
        } catch (FileNotFoundException e) {
            log.warn("Configuration file not found: {}", configFileName);
        } catch (IOException e) {
            log.error("Error loading configuration", e);
        }
    }

    /**
     * Instructs the configuration reader to read the configuration from the file.
     */
    public void readConfiguration() {
        readConfiguration(configFileName);
    }

    @Override
    public Map<String, BgpSpeaker> getBgpSpeakers() {
        return Collections.unmodifiableMap(bgpSpeakers);
    }

    @Override
    public Map<IpAddress, BgpPeer> getBgpPeers() {
        return Collections.unmodifiableMap(bgpPeers);
    }

    /**
     * Converts DPIDs of the form xx:xx:xx:xx:xx:xx:xx to OpenFlow provider
     * device URIs.
     *
     * @param dpid the DPID string to convert
     * @return the URI string for this device
     */
    static String dpidToUri(String dpid) {
        return "of:" + dpid.replace(":", "");
    }
}
