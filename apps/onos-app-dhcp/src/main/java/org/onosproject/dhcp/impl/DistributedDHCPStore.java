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
package org.onosproject.dhcp.impl;

import com.google.common.collect.ImmutableSet;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.Service;
import org.jboss.netty.util.Timeout;
import org.jboss.netty.util.TimerTask;
import org.onlab.packet.Ip4Address;
import org.onlab.packet.MacAddress;
import org.onlab.util.KryoNamespace;
import org.onlab.util.Timer;
import org.onosproject.dhcp.DHCPStore;
import org.onosproject.dhcp.IPAssignment;
import org.onosproject.store.serializers.KryoNamespaces;
import org.onosproject.store.service.ConsistentMap;
import org.onosproject.store.service.DistributedSet;
import org.onosproject.store.service.Serializer;
import org.onosproject.store.service.StorageService;
import org.onosproject.store.service.Versioned;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Manages the pool of available IP Addresses in the network and
 * Remembers the mapping between MAC ID and IP Addresses assigned.
 */

@Component(immediate = true)
@Service
public class DistributedDHCPStore implements DHCPStore {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    protected StorageService storageService;

    private ConsistentMap<MacAddress, IPAssignment> allocationMap;

    private DistributedSet<Ip4Address> freeIPPool;

    private Timeout timeout;

    private static Ip4Address startIPRange;

    private static Ip4Address endIPRange;

    // Hardcoded values are default values.

    private static int timerDelay = 2;

    private static int timeoutForPendingAssignments = 60;

    @Activate
    protected void activate() {
        allocationMap = storageService.<MacAddress, IPAssignment>consistentMapBuilder()
                .withName("onos-dhcp-assignedIP")
                .withSerializer(Serializer.using(
                        new KryoNamespace.Builder()
                                .register(KryoNamespaces.API)
                                .register(IPAssignment.class,
                                        IPAssignment.AssignmentStatus.class,
                                        Date.class,
                                        long.class,
                                        Ip4Address.class)
                                .build()))
                .build();

        freeIPPool = storageService.<Ip4Address>setBuilder()
                .withName("onos-dhcp-freeIP")
                .withSerializer(Serializer.using(KryoNamespaces.API))
                .build();

        timeout = Timer.getTimer().newTimeout(new PurgeListTask(), timerDelay, TimeUnit.MINUTES);

        log.info("Started");
    }

    @Deactivate
    protected void deactivate() {
        timeout.cancel();
        log.info("Stopped");
    }

    @Override
    public Ip4Address suggestIP(MacAddress macID, Ip4Address requestedIP) {

        IPAssignment assignmentInfo;
        if (allocationMap.containsKey(macID)) {
            assignmentInfo = allocationMap.get(macID).value();
            IPAssignment.AssignmentStatus status = assignmentInfo.assignmentStatus();
            Ip4Address ipAddr = assignmentInfo.ipAddress();

            if (status == IPAssignment.AssignmentStatus.Option_Assigned ||
                    status == IPAssignment.AssignmentStatus.Option_Requested) {
                // Client has a currently Active Binding.
                if ((ipAddr.toInt() > startIPRange.toInt()) && (ipAddr.toInt() < endIPRange.toInt())) {
                    return ipAddr;
                }

            } else if (status == IPAssignment.AssignmentStatus.Option_Expired) {
                // Client has a Released or Expired Binding.
                if (freeIPPool.contains(ipAddr)) {
                    assignmentInfo = IPAssignment.builder()
                            .ipAddress(ipAddr)
                            .timestamp(new Date())
                            .leasePeriod(timeoutForPendingAssignments)
                            .assignmentStatus(IPAssignment.AssignmentStatus.Option_Requested)
                            .build();
                    if (freeIPPool.remove(ipAddr)) {
                        allocationMap.put(macID, assignmentInfo);
                        return ipAddr;
                    }
                }
            }
            return assignmentInfo.ipAddress();

        } else if (requestedIP.toInt() != 0) {
            // Client has requested an IP.
            if (freeIPPool.contains(requestedIP)) {
                assignmentInfo = IPAssignment.builder()
                        .ipAddress(requestedIP)
                        .timestamp(new Date())
                        .leasePeriod(timeoutForPendingAssignments)
                        .assignmentStatus(IPAssignment.AssignmentStatus.Option_Requested)
                        .build();
                if (freeIPPool.remove(requestedIP)) {
                    allocationMap.put(macID, assignmentInfo);
                    return requestedIP;
                }
            }
        }

        // Allocate a new IP from the server's pool of available IP.
        Ip4Address nextIPAddr = fetchNextIP();
        assignmentInfo = IPAssignment.builder()
                                    .ipAddress(nextIPAddr)
                                    .timestamp(new Date())
                                    .leasePeriod(timeoutForPendingAssignments)
                                    .assignmentStatus(IPAssignment.AssignmentStatus.Option_Requested)
                                    .build();

        allocationMap.put(macID, assignmentInfo);
        return nextIPAddr;

    }

    @Override
    public boolean assignIP(MacAddress macID, Ip4Address ipAddr, int leaseTime) {

        IPAssignment assignmentInfo;
        if (allocationMap.containsKey(macID)) {
            assignmentInfo = allocationMap.get(macID).value();
            if ((assignmentInfo.ipAddress().toInt() == ipAddr.toInt()) &&
                    (ipAddr.toInt() > startIPRange.toInt()) && (ipAddr.toInt() < endIPRange.toInt())) {

                assignmentInfo = IPAssignment.builder()
                        .ipAddress(ipAddr)
                        .timestamp(new Date())
                        .leasePeriod(leaseTime)
                        .assignmentStatus(IPAssignment.AssignmentStatus.Option_Assigned)
                        .build();
                allocationMap.put(macID, assignmentInfo);
                return true;
            }
        } else if (freeIPPool.contains(ipAddr)) {
            assignmentInfo = IPAssignment.builder()
                                    .ipAddress(ipAddr)
                                    .timestamp(new Date())
                                    .leasePeriod(leaseTime)
                                    .assignmentStatus(IPAssignment.AssignmentStatus.Option_Assigned)
                                    .build();
            if (freeIPPool.remove(ipAddr)) {
                allocationMap.put(macID, assignmentInfo);
                return true;
            }
        }
        return false;
    }

    @Override
    public void releaseIP(MacAddress macID) {
        if (allocationMap.containsKey(macID)) {
            IPAssignment newAssignment = IPAssignment.builder(allocationMap.get(macID).value())
                                                    .assignmentStatus(IPAssignment.AssignmentStatus.Option_Expired)
                                                    .build();
            Ip4Address freeIP = newAssignment.ipAddress();
            allocationMap.put(macID, newAssignment);
            freeIPPool.add(freeIP);
        }
    }

    @Override
    public void setDefaultTimeoutForPurge(int timeInSeconds) {
        timeoutForPendingAssignments = timeInSeconds;
    }

    @Override
    public void setTimerDelay(int timeInSeconds) {
        timerDelay = timeInSeconds;
    }

    @Override
    public Map<MacAddress, Ip4Address> listMapping() {

        Map<MacAddress, Ip4Address> allMapping = new HashMap<>();
        for (Map.Entry<MacAddress, Versioned<IPAssignment>> entry: allocationMap.entrySet()) {
            IPAssignment assignment = entry.getValue().value();
            if (assignment.assignmentStatus() == IPAssignment.AssignmentStatus.Option_Assigned) {
                allMapping.put(entry.getKey(), assignment.ipAddress());
            }
        }

        return allMapping;
    }

    @Override
    public boolean assignStaticIP(MacAddress macID, Ip4Address ipAddr) {
        return assignIP(macID, ipAddr, -1);
    }

    @Override
    public boolean removeStaticIP(MacAddress macID) {
        if (allocationMap.containsKey(macID)) {
            IPAssignment assignment = allocationMap.get(macID).value();
            Ip4Address freeIP = assignment.ipAddress();
            if (assignment.leasePeriod() < 0) {
                allocationMap.remove(macID);
                freeIPPool.add(freeIP);
                return true;
            }
        }
        return false;
    }

    @Override
    public Iterable<Ip4Address> getAvailableIPs() {
        return ImmutableSet.<Ip4Address>copyOf(freeIPPool);
    }

    @Override
    public void populateIPPoolfromRange(Ip4Address startIP, Ip4Address endIP) {
        // Clear all entries from previous range.
        startIPRange = startIP;
        endIPRange = endIP;
        freeIPPool.clear();

        int lastIP = endIP.toInt();
        Ip4Address nextIP;
        for (int loopCounter = startIP.toInt(); loopCounter <= lastIP; loopCounter++) {
            nextIP = Ip4Address.valueOf(loopCounter);
            freeIPPool.add(nextIP);
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
     * Purges the IP allocation map to remove expired entries and returns the freed IPs to the free pool.
     */
    private class PurgeListTask implements TimerTask {

        @Override
        public void run(Timeout to) {
            IPAssignment ipAssignment, newAssignment;
            Date dateNow = new Date();
            for (Map.Entry<MacAddress, Versioned<IPAssignment>> entry: allocationMap.entrySet()) {
                ipAssignment = entry.getValue().value();
                long timeLapsed = dateNow.getTime() - ipAssignment.timestamp().getTime();
                if ((ipAssignment.assignmentStatus() != IPAssignment.AssignmentStatus.Option_Expired) &&
                        (ipAssignment.leasePeriod() > 0) && (timeLapsed > (ipAssignment.leasePeriod()))) {
                    Ip4Address freeIP = ipAssignment.ipAddress();

                    newAssignment = IPAssignment.builder(ipAssignment)
                            .assignmentStatus(IPAssignment.AssignmentStatus.Option_Expired)
                            .build();
                    allocationMap.put(entry.getKey(), newAssignment);

                    if ((freeIP.toInt() > startIPRange.toInt()) && (freeIP.toInt() < endIPRange.toInt())) {
                        freeIPPool.add(freeIP);
                    }
                }
            }
            timeout = Timer.getTimer().newTimeout(new PurgeListTask(), timerDelay, TimeUnit.MINUTES);
        }

    }

}
