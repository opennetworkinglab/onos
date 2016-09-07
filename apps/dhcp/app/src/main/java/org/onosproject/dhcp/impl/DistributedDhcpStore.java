/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.dhcp.impl;

import com.google.common.collect.ImmutableSet;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.MacAddress;
import org.onlab.util.KryoNamespace;
import org.onosproject.dhcp.DhcpStore;
import org.onosproject.dhcp.IpAssignment;
import org.onosproject.net.HostId;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.DistributedSet;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.Versioned;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Map;
import java.util.HashMap;

import static org.onosproject.dhcp.IpAssignment.AssignmentStatus.Option_Assigned;
import static org.onosproject.dhcp.IpAssignment.AssignmentStatus.Option_RangeNotEnforced;

/**
 * Manages the pool of available IP Addresses in the network and
 * Remembers the mapping between MAC ID and IP Addresses assigned.
 */

@Component(immediate = true)
@Service
public class DistributedDhcpStore implements DhcpStore {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    private ConsistentMap<HostId, IpAssignment> allocationMap;
    private DistributedSet<Ip4Address> freeIPPool;

    private static Ip4Address startIPRange;
    private static Ip4Address endIPRange;

    // Hardcoded values are default values.
    private static int timeoutForPendingAssignments = 60;

    @Activate
    protected void activate() {
        allocationMap = storageService.<HostId, IpAssignment>consistentMapBuilder()
                .withName("onos-dhcp-assignedIP")
                .withSerializer(Serializer.using(
                        new KryoNamespace.Builder()
                                .register(KryoNamespaces.API)
                                .register(IpAssignment.class,
                                        IpAssignment.AssignmentStatus.class,
                                        Date.class)
                                .build("dhcp")))
                .build();

        freeIPPool = storageService.<Ip4Address>setBuilder()
                .withName("onos-dhcp-freeIP")
                .withSerializer(Serializer.using(KryoNamespaces.API))
                .build()
                .asDistributedSet();

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        log.info("Stopped");
    }

    @Override
    public Ip4Address suggestIP(HostId hostId, Ip4Address requestedIP) {

        IpAssignment assignmentInfo;
        if (allocationMap.containsKey(hostId)) {
            assignmentInfo = allocationMap.get(hostId).value();
            IpAssignment.AssignmentStatus status = assignmentInfo.assignmentStatus();
            Ip4Address ipAddr = assignmentInfo.ipAddress();

            if (assignmentInfo.assignmentStatus().equals(Option_RangeNotEnforced)) {
                return assignmentInfo.ipAddress();
            } else if (status == Option_Assigned ||
                    status == IpAssignment.AssignmentStatus.Option_Requested) {
                // Client has a currently Active Binding.
                if (ipWithinRange(ipAddr)) {
                    return ipAddr;
                }

            } else if (status == IpAssignment.AssignmentStatus.Option_Expired) {
                // Client has a Released or Expired Binding.
                if (freeIPPool.contains(ipAddr)) {
                    assignmentInfo = IpAssignment.builder()
                            .ipAddress(ipAddr)
                            .timestamp(new Date())
                            .leasePeriod(timeoutForPendingAssignments)
                            .assignmentStatus(IpAssignment.AssignmentStatus.Option_Requested)
                            .build();
                    if (freeIPPool.remove(ipAddr)) {
                        allocationMap.put(hostId, assignmentInfo);
                        return ipAddr;
                    }
                }
            }
        } else if (requestedIP.toInt() != 0) {
            // Client has requested an IP.
            if (freeIPPool.contains(requestedIP)) {
                assignmentInfo = IpAssignment.builder()
                        .ipAddress(requestedIP)
                        .timestamp(new Date())
                        .leasePeriod(timeoutForPendingAssignments)
                        .assignmentStatus(IpAssignment.AssignmentStatus.Option_Requested)
                        .build();
                if (freeIPPool.remove(requestedIP)) {
                    allocationMap.put(hostId, assignmentInfo);
                    return requestedIP;
                }
            }
        }

        // Allocate a new IP from the server's pool of available IP.
        Ip4Address nextIPAddr = fetchNextIP();
        if (nextIPAddr != null) {
            assignmentInfo = IpAssignment.builder()
                    .ipAddress(nextIPAddr)
                    .timestamp(new Date())
                    .leasePeriod(timeoutForPendingAssignments)
                    .assignmentStatus(IpAssignment.AssignmentStatus.Option_Requested)
                    .build();

            allocationMap.put(hostId, assignmentInfo);
        }
        return nextIPAddr;

    }

    @Override
    public boolean assignIP(HostId hostId, IpAssignment ipAssignment) {
        log.trace("Assign IP Called HostId: {}, ipAssignment: {}",
                  hostId, ipAssignment);

        IpAssignment newAssignment = null;
        Versioned<IpAssignment> versionedAssignment = allocationMap.get(hostId);
        Ip4Address requestedIp = ipAssignment.ipAddress();

        if (versionedAssignment == null) {
            // this is new IP assignment of static mapping
            // dynamic assignment is done in suggestIP
            if (ipAssignment.assignmentStatus().equals(Option_RangeNotEnforced)) {
                newAssignment = ipAssignment;
            } else if (freeIPPool.remove(requestedIp)) {
                newAssignment = IpAssignment.builder(ipAssignment)
                        .assignmentStatus(Option_Assigned)
                        .timestamp(new Date())
                        .build();
            } else {
                log.trace("Failed to assign IP for {}", ipAssignment);
                return false;
            }
            log.trace("Assigned {}", newAssignment);
            return allocationMap.putIfAbsent(hostId, newAssignment) == null;
            // TODO: handle the case where map changed.
        } else {
            // this is lease renew or rebinding
            // update assignment status and time stamp, and keep the others
            IpAssignment existingAssignment = versionedAssignment.value();
            if (!existingAssignment.ipAddress().equals(requestedIp)) {
                // return false if existing assignment is not for the
                // requested host
                log.trace("Failed to assign IP for {}", ipAssignment);
                return false;
            }

            switch (existingAssignment.assignmentStatus()) {
                case Option_RangeNotEnforced:
                    newAssignment = IpAssignment.builder(existingAssignment)
                            .timestamp(new Date())
                            .build();
                    break;
                case Option_Expired:
                    if (!freeIPPool.remove(requestedIp)) {
                        // requested IP is expired for this host and reserved to the other host
                        return false;
                    }
                case Option_Assigned:
                case Option_Requested:
                    newAssignment = IpAssignment.builder(existingAssignment)
                            .timestamp(new Date())
                            .assignmentStatus(Option_Assigned)
                            .build();
                    break;
                default:
                    break;
            }
            log.trace("Assigned {}", newAssignment);
            return allocationMap.replace(hostId, versionedAssignment.version(), newAssignment);
        }
    }

    @Override
    public Ip4Address releaseIP(HostId hostId) {
        if (allocationMap.containsKey(hostId)) {
            // If the IP has been assigned with Option_RangeNotEnforced,
            // we do not release the IP address nor remove the host from HostService.
            // Therefore, if the IP is assigned statically, the IP needs to be released statically.
            Versioned<IpAssignment> assignmentVersioned = allocationMap.get(hostId);
            if (Versioned.valueOrNull(assignmentVersioned) != null &&
                    assignmentVersioned.value().assignmentStatus().equals(Option_RangeNotEnforced)) {
                return null;
            }

            IpAssignment newAssignment = IpAssignment.builder(allocationMap.get(hostId).value())
                    .assignmentStatus(IpAssignment.AssignmentStatus.Option_Expired)
                    .build();
            Ip4Address freeIP = newAssignment.ipAddress();
            allocationMap.put(hostId, newAssignment);
            if (ipWithinRange(freeIP)) {
                freeIPPool.add(freeIP);
            }
            return freeIP;
        }
        return null;
    }

    @Override
    public void setDefaultTimeoutForPurge(int timeInSeconds) {
        timeoutForPendingAssignments = timeInSeconds;
    }

    @Override
    public Map<HostId, IpAssignment> listAssignedMapping() {

        Map<HostId, IpAssignment> validMapping = new HashMap<>();
        IpAssignment assignment;
        for (Map.Entry<HostId, Versioned<IpAssignment>> entry: allocationMap.entrySet()) {
            assignment = entry.getValue().value();
            if (assignment.assignmentStatus() == Option_Assigned
                    || assignment.assignmentStatus() == Option_RangeNotEnforced) {
                validMapping.put(entry.getKey(), assignment);
            }
        }
        return validMapping;
    }

    @Override
    public Map<HostId, IpAssignment> listAllMapping() {
        Map<HostId, IpAssignment> validMapping = new HashMap<>();
        for (Map.Entry<HostId, Versioned<IpAssignment>> entry: allocationMap.entrySet()) {
            validMapping.put(entry.getKey(), entry.getValue().value());
        }
        return validMapping;
    }

    @Override
    public boolean assignStaticIP(MacAddress macAddress, IpAssignment ipAssignment) {
        HostId host = HostId.hostId(macAddress);
        return assignIP(host, ipAssignment);
    }

    @Override
    public boolean removeStaticIP(MacAddress macID) {
        HostId host = HostId.hostId(macID);
        if (allocationMap.containsKey(host)) {
            IpAssignment assignment = allocationMap.get(host).value();

            if (assignment.assignmentStatus().equals(Option_RangeNotEnforced)) {
                allocationMap.remove(host);
                return true;
            }

            Ip4Address freeIP = assignment.ipAddress();
            if (assignment.leasePeriod() < 0) {
                allocationMap.remove(host);
                if (ipWithinRange(freeIP)) {
                    freeIPPool.add(freeIP);
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public Iterable<Ip4Address> getAvailableIPs() {
        return ImmutableSet.copyOf(freeIPPool);
    }

    @Override
    public void populateIPPoolfromRange(Ip4Address startIP, Ip4Address endIP) {
        // Clear all entries from previous range.
        allocationMap.clear();
        freeIPPool.clear();
        startIPRange = startIP;
        endIPRange = endIP;

        int lastIP = endIP.toInt();
        Ip4Address nextIP;
        for (int loopCounter = startIP.toInt(); loopCounter <= lastIP; loopCounter++) {
            nextIP = Ip4Address.valueOf(loopCounter);
            freeIPPool.add(nextIP);
        }
        log.debug("Updated free IP pool {}:{} size:{}", startIP, endIP, freeIPPool.size());
    }

    @Override
    public IpAssignment getIpAssignmentFromAllocationMap(HostId hostId) {
        if (allocationMap.get(hostId) != null) {
            return allocationMap.get(hostId).value();
        } else {
            return null;
        }
    }

    /**
     * Fetches the next available IP from the free pool pf IPs.
     *
     * @return the next available IP address
     */
    private Ip4Address fetchNextIP() {
        for (Ip4Address freeIP : freeIPPool) {
            if (freeIPPool.remove(freeIP)) {
                return freeIP;
            }
        }
        return null;
    }

    /**
     * Returns true if the given ip is within the range of available IPs.
     *
     * @param ip given ip address
     * @return true if within range, false otherwise
     */
    private boolean ipWithinRange(Ip4Address ip) {
        if ((ip.toInt() >= startIPRange.toInt()) && (ip.toInt() <= endIPRange.toInt())) {
            return true;
        }
        return false;
    }
}

