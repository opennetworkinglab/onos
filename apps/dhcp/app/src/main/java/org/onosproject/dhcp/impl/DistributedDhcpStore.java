/*
 * Copyright 2015 Open Networking Laboratory
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
import org.onlab.util.Tools;
import org.onosproject.dhcp.DhcpStore;
import org.onosproject.dhcp.IpAssignment;
import org.onosproject.net.HostId;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.ConsistentMapException;
import org.onosproject.store.service.DistributedSet;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.Versioned;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

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
    private static final int MAX_RETRIES = 3;
    private static final int MAX_BACKOFF = 10;

    @Activate
    protected void activate() {
        allocationMap = storageService.<HostId, IpAssignment>consistentMapBuilder()
                .withName("onos-dhcp-assignedIP")
                .withSerializer(Serializer.using(
                        new KryoNamespace.Builder()
                                .register(KryoNamespaces.API)
                                .register(IpAssignment.class,
                                        IpAssignment.AssignmentStatus.class,
                                        Date.class,
                                        long.class,
                                        Ip4Address.class)
                                .build()))
                .build();

        freeIPPool = storageService.<Ip4Address>setBuilder()
                .withName("onos-dhcp-freeIP")
                .withSerializer(Serializer.using(KryoNamespaces.API))
                .build();

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

            if (assignmentInfo.rangeNotEnforced()) {
                return assignmentInfo.ipAddress();
            } else if (status == IpAssignment.AssignmentStatus.Option_Assigned ||
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
    public boolean assignIP(HostId hostId, Ip4Address ipAddr, int leaseTime, boolean rangeNotEnforced,
                            List<Ip4Address> addressList) {
        log.debug("Assign IP Called w/ Ip4Address: {}, HostId: {}", ipAddr.toString(), hostId.mac().toString());

        AtomicBoolean assigned = Tools.retryable(() -> {
            AtomicBoolean result = new AtomicBoolean(false);
            allocationMap.compute(
                    hostId,
                    (h, existingAssignment) -> {
                        IpAssignment assignment = existingAssignment;
                        if (existingAssignment == null) {
                            if (rangeNotEnforced) {
                                assignment = IpAssignment.builder()
                                        .ipAddress(ipAddr)
                                        .timestamp(new Date())
                                        .leasePeriod(leaseTime)
                                        .rangeNotEnforced(true)
                                        .assignmentStatus(IpAssignment.AssignmentStatus.Option_RangeNotEnforced)
                                        .subnetMask((Ip4Address) addressList.toArray()[0])
                                        .dhcpServer((Ip4Address) addressList.toArray()[1])
                                        .routerAddress((Ip4Address) addressList.toArray()[2])
                                        .domainServer((Ip4Address) addressList.toArray()[3])
                                        .build();
                                result.set(true);
                            } else if (freeIPPool.remove(ipAddr)) {
                                assignment = IpAssignment.builder()
                                        .ipAddress(ipAddr)
                                        .timestamp(new Date())
                                        .leasePeriod(leaseTime)
                                        .assignmentStatus(IpAssignment.AssignmentStatus.Option_Assigned)
                                        .build();
                                result.set(true);
                            }
                        } else if (Objects.equals(existingAssignment.ipAddress(), ipAddr) &&
                                (existingAssignment.rangeNotEnforced() || ipWithinRange(ipAddr))) {
                            switch (existingAssignment.assignmentStatus()) {
                                case Option_RangeNotEnforced:
                                    assignment = IpAssignment.builder()
                                            .ipAddress(ipAddr)
                                            .timestamp(new Date())
                                            .leasePeriod(existingAssignment.leasePeriod())
                                            .rangeNotEnforced(true)
                                            .assignmentStatus(IpAssignment.AssignmentStatus.Option_RangeNotEnforced)
                                            .subnetMask(existingAssignment.subnetMask())
                                            .dhcpServer(existingAssignment.dhcpServer())
                                            .routerAddress(existingAssignment.routerAddress())
                                            .domainServer(existingAssignment.domainServer())
                                            .build();
                                    result.set(true);
                                    break;
                                case Option_Assigned:
                                case Option_Requested:
                                    assignment = IpAssignment.builder()
                                            .ipAddress(ipAddr)
                                            .timestamp(new Date())
                                            .leasePeriod(leaseTime)
                                            .assignmentStatus(IpAssignment.AssignmentStatus.Option_Assigned)
                                            .build();
                                    result.set(true);
                                    break;
                                case Option_Expired:
                                    if (freeIPPool.remove(ipAddr)) {
                                        assignment = IpAssignment.builder()
                                                .ipAddress(ipAddr)
                                                .timestamp(new Date())
                                                .leasePeriod(leaseTime)
                                                .assignmentStatus(IpAssignment.AssignmentStatus.Option_Assigned)
                                                .build();
                                        result.set(true);
                                    }
                                    break;
                                default:
                                    break;
                            }
                        }
                        return assignment;
                    });
            return result;
        }, ConsistentMapException.class, MAX_RETRIES, MAX_BACKOFF).get();

        return assigned.get();
    }

    @Override
    public Ip4Address releaseIP(HostId hostId) {
        if (allocationMap.containsKey(hostId)) {
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
            if (assignment.assignmentStatus() == IpAssignment.AssignmentStatus.Option_Assigned
                    || assignment.assignmentStatus() == IpAssignment.AssignmentStatus.Option_RangeNotEnforced) {
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
    public boolean assignStaticIP(MacAddress macID, Ip4Address ipAddr, boolean rangeNotEnforced,
                                  List<Ip4Address> addressList) {
        HostId host = HostId.hostId(macID);
        return assignIP(host, ipAddr, -1, rangeNotEnforced, addressList);
    }

    @Override
    public boolean removeStaticIP(MacAddress macID) {
        HostId host = HostId.hostId(macID);
        if (allocationMap.containsKey(host)) {
            IpAssignment assignment = allocationMap.get(host).value();

            if (assignment.rangeNotEnforced()) {
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
    }

    @Override
    public IpAssignment getIpAssignmentFromAllocationMap(HostId hostId) {
        return allocationMap.get(hostId).value();
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

