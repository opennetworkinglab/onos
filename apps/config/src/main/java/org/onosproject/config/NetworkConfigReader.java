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
package org.onosproject.config;

import static org.slf4j.LoggerFactory.getLogger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.DeviceId;
import org.onosproject.net.PortNumber;
import org.onosproject.net.host.HostAdminService;
import org.onosproject.net.host.InterfaceIpAddress;
import org.onosproject.net.host.PortAddresses;
import org.slf4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Simple configuration module to read in supplementary network configuration
 * from a file.
 */
@Component(immediate = true)
@Service
public class NetworkConfigReader implements NetworkConfigService {

    private final Logger log = getLogger(getClass());

    // Current working is /opt/onos/apache-karaf-*
    // TODO: Set the path to /opt/onos/config
    private static final String CONFIG_DIR = "../config";
    private static final String DEFAULT_CONFIG_FILE = "addresses.json";
    private String configFileName = DEFAULT_CONFIG_FILE;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostAdminService hostAdminService;

    @Activate
    protected void activate() {
        AddressConfiguration config = readNetworkConfig();
        if (config != null) {
            applyNetworkConfig(config);
        }
        log.info("Started network config reader");
    }

    @Deactivate
    protected void deactivate() {
        log.info("Stopped");
    }

    /**
     * Reads the network configuration.
     *
     * @return the network configuration on success, otherwise null
     */
    private AddressConfiguration readNetworkConfig() {
        File configFile = new File(CONFIG_DIR, configFileName);
        ObjectMapper mapper = new ObjectMapper();

        try {
            log.info("Loading config: {}", configFile.getAbsolutePath());
            AddressConfiguration config =
                    mapper.readValue(configFile, AddressConfiguration.class);

            return config;
        } catch (FileNotFoundException e) {
            log.warn("Configuration file not found: {}", configFileName);
        } catch (IOException e) {
            log.error("Error loading configuration", e);
        }

        return null;
    }

    /**
     * Applies the network configuration.
     *
     * @param config the network configuration to apply
     */
    private void applyNetworkConfig(AddressConfiguration config) {
        for (AddressEntry entry : config.getAddresses()) {
            ConnectPoint cp = new ConnectPoint(
                        DeviceId.deviceId(dpidToUri(entry.getDpid())),
                        PortNumber.portNumber(entry.getPortNumber()));

            Set<InterfaceIpAddress> interfaceIpAddresses = new HashSet<>();
            for (String strIp : entry.getIpAddresses()) {
                // Get the IP address and the subnet mask length
                try {
                    String[] splits = strIp.split("/");
                    if (splits.length != 2) {
                        throw new IllegalArgumentException(
                            "Invalid IP address and prefix length format");
                    }
                    // NOTE: IpPrefix will mask-out the bits after the prefix length.
                    IpPrefix subnet = IpPrefix.valueOf(strIp);
                    IpAddress addr = IpAddress.valueOf(splits[0]);
                    InterfaceIpAddress ia =
                        new InterfaceIpAddress(addr, subnet);
                    interfaceIpAddresses.add(ia);
                } catch (IllegalArgumentException e) {
                    log.warn("Bad format for IP address in config: {}", strIp);
                }
            }

            MacAddress macAddress = null;
            if (entry.getMacAddress() != null) {
                try {
                    macAddress = MacAddress.valueOf(entry.getMacAddress());
                } catch (IllegalArgumentException e) {
                    log.warn("Bad format for MAC address in config: {}",
                             entry.getMacAddress());
                }
            }

            VlanId vlan = null;
            if (entry.getVlan() == null) {
                vlan = VlanId.NONE;
            } else {
                try {
                    vlan = VlanId.vlanId(entry.getVlan());
                } catch (IllegalArgumentException e) {
                    log.warn("Bad format for VLAN id in config: {}",
                             entry.getVlan());
                    vlan = VlanId.NONE;
                }
            }

            PortAddresses addresses = new PortAddresses(cp,
                        interfaceIpAddresses, macAddress, vlan);
            hostAdminService.bindAddressesToPort(addresses);
        }
    }

    private static String dpidToUri(String dpid) {
        return "of:" + dpid.replace(":", "");
    }
}
