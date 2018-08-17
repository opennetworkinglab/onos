/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.virtualbng;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.onlab.packet.IpAddress;
import org.onlab.packet.IpPrefix;
import org.onlab.packet.MacAddress;
import org.onosproject.net.ConnectPoint;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of ConfigurationService which reads virtual BNG
 * configuration from a file.
 */
@Component(immediate = true, service = VbngConfigurationService.class)
public class VbngConfigurationManager implements VbngConfigurationService {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String CONFIG_DIR = "../config";
    private static final String DEFAULT_CONFIG_FILE = "virtualbng.json";
    private String configFileName = DEFAULT_CONFIG_FILE;

    // If all the IP addresses of one IP prefix are assigned, then we
    // mark the value of this IP prefix as false, otherwise as true.
    private Map<IpPrefix, Boolean> localPublicIpPrefixes =
            new ConcurrentHashMap<>();

    // Map from private IP address to public IP address
    private Map<IpAddress, IpAddress> ipAddressMap =
            new ConcurrentHashMap<>();

    private IpAddress nextHopIpAddress;
    private MacAddress macOfPublicIpAddresses;
    private IpAddress xosIpAddress;
    private int xosRestPort;
    private Map<String, ConnectPoint> nodeToPort;

    @Activate
    public void activate() {
        readConfiguration();
        log.info("vBNG configuration service started");
    }

    @Deactivate
    public void deactivate() {
        log.info("vBNG configuration service stopped");
    }

    /**
     * Instructs the configuration reader to read the configuration from the
     * file.
     */
    public void readConfiguration() {
        readConfiguration(configFileName);
    }

    /**
     * Reads virtual BNG information contained in configuration file.
     *
     * @param configFilename the name of the configuration file for the virtual
     * BNG application
     */
    private void readConfiguration(String configFilename) {
        File configFile = new File(CONFIG_DIR, configFilename);
        ObjectMapper mapper = new ObjectMapper();

        try {
            log.info("Loading config: {}", configFile.getAbsolutePath());
            VbngConfiguration config = mapper.readValue(configFile,
                                                    VbngConfiguration.class);
            for (IpPrefix prefix : config.getLocalPublicIpPrefixes()) {
                localPublicIpPrefixes.put(prefix, true);
            }
            nextHopIpAddress = config.getNextHopIpAddress();
            macOfPublicIpAddresses = config.getPublicFacingMac();
            xosIpAddress = config.getXosIpAddress();
            xosRestPort = config.getXosRestPort();
            nodeToPort = config.getHosts();


        } catch (FileNotFoundException e) {
            log.warn("Configuration file not found: {}", configFileName);
        } catch (IOException e) {
            log.error("Error loading configuration", e);
        }
    }

    @Override
    public IpAddress getNextHopIpAddress() {
        return nextHopIpAddress;
    }

    @Override
    public MacAddress getPublicFacingMac() {
        return macOfPublicIpAddresses;
    }

    @Override
    public IpAddress getXosIpAddress() {
        return xosIpAddress;
    }

    @Override
    public int getXosRestPort() {
        return xosRestPort;
    }

    @Override
    public Map<String, ConnectPoint> getNodeToPort() {
        return nodeToPort;
    }

    // TODO handle the case: the number of public IP addresses is not enough
    // for 1:1 mapping from public IP to private IP.
    @Override
    public synchronized IpAddress getAvailablePublicIpAddress(IpAddress
                                                           privateIpAddress) {
        // If there is already a mapping entry for the private IP address,
        // then fetch the public IP address in the mapping entry and return it.
        IpAddress publicIpAddress = ipAddressMap.get(privateIpAddress);
        if (publicIpAddress != null) {
            return publicIpAddress;
        }
        // There is no mapping for the private IP address.
        Iterator<Entry<IpPrefix, Boolean>> prefixes =
                localPublicIpPrefixes.entrySet().iterator();
        while (prefixes.hasNext()) {
            Entry<IpPrefix, Boolean> prefix = prefixes.next();
            if (!prefix.getValue()) {
                continue;
            }

            if (prefix.getKey().prefixLength() == 32) {
                updateIpPrefixStatus(prefix.getKey(), false);
                publicIpAddress = prefix.getKey().address();
                ipAddressMap.put(privateIpAddress, publicIpAddress);
                return publicIpAddress;
            }

            double prefixLen = prefix.getKey().prefixLength();
            int availableIpNum = (int) Math.pow(2,
                    IpPrefix.MAX_INET_MASK_LENGTH - prefixLen) - 1;
            for (int i = 1; i <= availableIpNum; i++) {
                publicIpAddress =
                        increaseIpAddress(prefix.getKey().address(), i);
                if (publicIpAddress == null) {
                    return null;
                }
                if (ipAddressMap.values().contains(publicIpAddress)) {
                    continue;
                } else if (i == availableIpNum) {
                    // All the IP addresses are assigned out
                    // Update this IP prefix status to false
                    // Note: in this version we do not consider the
                    // IP recycling issue.
                    updateIpPrefixStatus(prefix.getKey(), false);
                    ipAddressMap.put(privateIpAddress, publicIpAddress);
                    return publicIpAddress;
                } else {
                    ipAddressMap.put(privateIpAddress, publicIpAddress);
                    return publicIpAddress;
                }
            }
        }
        return null;
    }

    @Override
    public IpAddress getAssignedPublicIpAddress(IpAddress privateIpAddress) {
        return ipAddressMap.get(privateIpAddress);
    }

    @Override
    public boolean isAssignedPublicIpAddress(IpAddress ipAddress) {
        return ipAddressMap.containsValue(ipAddress);
    }

    @Override
    public synchronized IpAddress recycleAssignedPublicIpAddress(IpAddress
                                                    privateIpAddress) {
        IpAddress publicIpAddress = ipAddressMap.remove(privateIpAddress);
        if (publicIpAddress == null) {
            return null;
        }

        Iterator<Entry<IpPrefix, Boolean>> prefixes =
                localPublicIpPrefixes.entrySet().iterator();
        while (prefixes.hasNext()) {
            Entry<IpPrefix, Boolean> prefixEntry = prefixes.next();
            if (prefixEntry.getKey().contains(publicIpAddress)
                    && !prefixEntry.getValue()) {
                updateIpPrefixStatus(prefixEntry.getKey(), true);
            }
        }
        log.info("[DELETE] Private IP to Public IP mapping: {} --> {}",
                 privateIpAddress, publicIpAddress);
        return publicIpAddress;
    }

    @Override
    public Map<IpAddress, IpAddress> getIpAddressMappings() {
        return Collections.unmodifiableMap(ipAddressMap);
    }

    @Override
    public synchronized boolean assignSpecifiedPublicIp(IpAddress publicIpAddress,
                                  IpAddress privateIpAddress) {

        // Judge whether this public IP address is in our public IP
        // prefix/address list.
        boolean isPublicIpExist = false;
        for (Entry<IpPrefix, Boolean> prefix: localPublicIpPrefixes.entrySet()) {
            if (prefix.getKey().contains(publicIpAddress)) {
                isPublicIpExist = true;

                // Judge whether this public IP address is already assigned
                if (!prefix.getValue() ||
                        isAssignedPublicIpAddress(publicIpAddress)) {
                    log.info("The public IP address {} is already assigned, "
                            + "and not available.", publicIpAddress);
                    return false;
                }

                // The public IP address is still available
                // Store the mapping from private IP address to public IP address
                ipAddressMap.put(privateIpAddress, publicIpAddress);

                // Update the prefix status
                if (prefix.getKey().prefixLength() == 32) {
                    updateIpPrefixStatus(prefix.getKey(), false);
                    return true;
                }

                // Judge whether the prefix of this public IP address is used
                // up, if so, update the IP prefix status.
                double prefixLen = prefix.getKey().prefixLength();
                int availableIpNum = (int) Math.pow(2,
                        IpPrefix.MAX_INET_MASK_LENGTH - prefixLen) - 1;
                int usedIpNum = 0;
                for (Entry<IpAddress, IpAddress> ipAddressMapEntry:
                    ipAddressMap.entrySet()) {
                    if (prefix.getKey().contains(ipAddressMapEntry.getValue())) {
                        usedIpNum = usedIpNum + 1;
                    }
                }
                if (usedIpNum == availableIpNum) {
                    updateIpPrefixStatus(prefix.getKey(), false);
                }

                return true;
            }
        }

        log.info("The public IP address {} retrieved from XOS mapping does "
                + "not exist", publicIpAddress);

        return false;
    }

    /**
     * Generates a new IP address base on a given IP address plus a number to
     * increase.
     *
     * @param ipAddress the IP address to increase
     * @param num the number for ipAddress to add
     * @return the new IP address after increase
     */
    private IpAddress increaseIpAddress(IpAddress ipAddress, int num) {
        if (ipAddress.isIp6()) {
            log.info("vBNG currently does not handle IPv6");
            return null;
        }
        return IpAddress.valueOf(ipAddress.getIp4Address().toInt() + num);
    }

    /**
     * Updates the IP prefix status in the local public IP prefix table.
     *
     * @param ipPprefix the IP prefix to update
     * @param b the new value for the IP prefix
     */
    private void updateIpPrefixStatus(IpPrefix ipPprefix, boolean b) {
            localPublicIpPrefixes.replace(ipPprefix, b);
    }

}
