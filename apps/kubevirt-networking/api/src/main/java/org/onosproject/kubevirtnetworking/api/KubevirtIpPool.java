/*
 * Copyright 2021-present Open Networking Foundation
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
package org.onosproject.kubevirtnetworking.api;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableSet;
import inet.ipaddr.AddressStringException;
import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressSeqRange;
import inet.ipaddr.IPAddressString;
import org.onlab.packet.IpAddress;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Kubevirt IP Pool.
 */
public class KubevirtIpPool {

    private final IpAddress start;
    private final IpAddress end;
    private final Set<IpAddress> allocatedIps;
    private final Set<IpAddress> availableIps;

    /**
     * Default constructor.
     *
     * @param start             start address of IP pool
     * @param end               end address of IP pool
     */
    public KubevirtIpPool(IpAddress start, IpAddress end) {
        this.start = start;
        this.end = end;
        this.allocatedIps = new HashSet<>();
        this.availableIps = new HashSet<>();
        try {
            this.availableIps.addAll(getRangedIps(start.toString(), end.toString()));
        } catch (AddressStringException e) {
            e.printStackTrace();
        }
    }

    /**
     * Returns the start address of IP pool.
     *
     * @return start address of IP pool
     */
    public IpAddress start() {
        return start;
    }

    /**
     * Returns the end address of IP pool.
     *
     * @return end address of IP pool
     */
    public IpAddress end() {
        return end;
    }

    /**
     * Returns the set of IP addresses that have been allocated.
     *
     * @return set of IP addresses that have been allocated
     */
    public Set<IpAddress> allocatedIps() {
        return ImmutableSet.copyOf(allocatedIps);
    }

    /**
     * Returns the set of IP addresses for allocation.
     *
     * @return set of IP addresses for allocation.
     */
    public Set<IpAddress> availableIps() {
        return ImmutableSet.copyOf(availableIps);
    }

    /**
     * Allocates a random IP address.
     *
     * @return allocated IP address
     * @throws Exception exception
     */
    public synchronized IpAddress allocateIp() throws Exception {
        if (availableIps.size() <= 0) {
            throw new Exception("No IP address is available for allocation.");
        }

        List<IpAddress> sortedList = new ArrayList<>(availableIps);
        Collections.sort(sortedList);

        IpAddress ip = sortedList.get(0);

        availableIps.remove(ip);
        allocatedIps.add(ip);

        return ip;
    }

    /**
     * Reserves the given IP address.
     *
     * @param ip IP address to be reserved
     * @return result for IP address reservation
     */
    public synchronized boolean reserveIp(IpAddress ip) {
        if (availableIps.size() <= 0) {
            return false;
        }

        if (allocatedIps.contains(ip) || !availableIps.contains(ip)) {
            return false;
        }

        availableIps.remove(ip);
        allocatedIps.add(ip);

        return true;
    }

    /**
     * Releases the given IP address.
     *
     * @param ip IP address to be released
     * @throws Exception exception
     */
    public synchronized void releaseIp(IpAddress ip) throws Exception {
        if (!allocatedIps.contains(ip)) {
            throw new Exception("The given IP address is not able to be released.");
        }

        allocatedIps.remove(ip);
        availableIps.add(ip);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        KubevirtIpPool that = (KubevirtIpPool) o;
        return start.equals(that.start) && end.equals(that.end);
    }

    @Override
    public int hashCode() {
        return Objects.hash(start, end);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("start", start)
                .add("end", end)
                .toString();
    }

    /**
     * Obtains the IP address list from the given start and end range.
     *
     * @param start start range
     * @param end   end range
     * @return IP address list from the given start and end range
     * @throws AddressStringException exception
     */
    public Set<IpAddress> getRangedIps(String start, String end) throws AddressStringException {
        Set<IpAddress> ips = new HashSet<>();
        IPAddress lower = new IPAddressString(start).toAddress();
        IPAddress upper = new IPAddressString(end).toAddress();
        IPAddressSeqRange range = lower.toSequentialRange(upper);
        for (IPAddress addr : range.getIterable()) {
            ips.add(IpAddress.valueOf(addr.toString()));
        }

        return ips;
    }
}
