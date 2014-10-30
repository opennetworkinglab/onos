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
package org.onlab.onos.sdnip.config;

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

// TODO: As a long term solution, a module providing general network configuration to ONOS nodes should be used.

/**
 * SDN-IP Config Reader provides IConfigInfoService by reading from an
 * SDN-IP configuration file. It must be enabled on the nodes within the cluster
 * not running SDN-IP.
 */
public class SdnIpConfigReader implements SdnIpConfigService {

    private static final Logger log = LoggerFactory.getLogger(SdnIpConfigReader.class);

    private static final String DEFAULT_CONFIG_FILE = "config/sdnip.json";
    private String configFileName = DEFAULT_CONFIG_FILE;
    private Map<String, BgpSpeaker> bgpSpeakers = new ConcurrentHashMap<>();
    private Map<IpAddress, BgpPeer> bgpPeers = new ConcurrentHashMap<>();

    /**
     * Reads the info contained in the configuration file.
     *
     * @param configFilename The name of configuration file for SDN-IP application.
     */
    private void readConfiguration(String configFilename) {
        File gatewaysFile = new File(configFilename);
        ObjectMapper mapper = new ObjectMapper();

        try {
            Configuration config = mapper.readValue(gatewaysFile, Configuration.class);
            for (BgpSpeaker speaker : config.getBgpSpeakers()) {
                bgpSpeakers.put(speaker.name(), speaker);
            }
            for (BgpPeer peer : config.getPeers()) {
                bgpPeers.put(peer.ipAddress(), peer);
            }
        } catch (FileNotFoundException e) {
            log.warn("Configuration file not found: {}", configFileName);
        } catch (IOException e) {
            log.error("Error reading JSON file", e);
        }
    }

    public void init() {
        log.debug("Config file set to {}", configFileName);

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

    static String dpidToUri(String dpid) {
        return "of:" + dpid.replace(":", "");
    }
}
