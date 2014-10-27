/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.onlab.onos.config;

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
import org.onlab.onos.net.ConnectPoint;
import org.onlab.onos.net.DeviceId;
import org.onlab.onos.net.PortNumber;
import org.onlab.onos.net.host.HostAdminService;
import org.onlab.onos.net.host.InterfaceIpAddress;
import org.onlab.onos.net.host.PortAddresses;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.Ip4Prefix;
import org.onlab.packet.MacAddress;
import org.slf4j.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Simple configuration module to read in supplementary network configuration
 * from a file.
 */
@Component(immediate = true)
public class NetworkConfigReader {

    private final Logger log = getLogger(getClass());

    private static final String DEFAULT_CONFIG_FILE = "config/addresses.json";
    private String configFileName = DEFAULT_CONFIG_FILE;

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected HostAdminService hostAdminService;

    @Activate
    protected void activate() {
        log.info("Started network config reader");

        log.info("Config file set to {}", configFileName);

        AddressConfiguration config = readNetworkConfig();

        if (config != null) {
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
                            throw new IllegalArgumentException("Invalid IP address and prefix length format");
                        }
                        //
                        // TODO: For now we need Ip4Prefix to mask-out the
                        // subnet address.
                        //
                        Ip4Prefix subnet4 = new Ip4Prefix(strIp);
                        IpPrefix subnet = IpPrefix.valueOf(subnet4.toString());
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

                PortAddresses addresses = new PortAddresses(cp,
                        interfaceIpAddresses, macAddress);

                hostAdminService.bindAddressesToPort(addresses);
            }
        }
    }

    @Deactivate
    protected void deactivate() {
        log.info("Stopped");
    }

    private AddressConfiguration readNetworkConfig() {
        File configFile = new File(configFileName);

        ObjectMapper mapper = new ObjectMapper();

        try {
            AddressConfiguration config =
                    mapper.readValue(configFile, AddressConfiguration.class);

            return config;
        } catch (FileNotFoundException e) {
            log.warn("Configuration file not found: {}", configFileName);
        } catch (IOException e) {
            log.error("Unable to read config from file:", e);
        }

        return null;
    }

    private static String dpidToUri(String dpid) {
        return "of:" + dpid.replace(":", "");
    }
}
