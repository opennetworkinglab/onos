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
package org.onosproject.dhcp;

import com.google.common.base.MoreObjects;
import org.onlab.packet.Ip4Address;

import java.util.Date;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Stores the MAC ID to IP Address mapping details.
 */
public final class IpAssignment {

    private final Ip4Address ipAddress;

    private final Date timestamp;

    private final long leasePeriod;

    private final Ip4Address subnetMask;

    private final Ip4Address dhcpServer;

    private final Ip4Address routerAddress;

    private final Ip4Address domainServer;

    private final boolean rangeNotEnforced;

    private final AssignmentStatus assignmentStatus;

    public enum AssignmentStatus {
        /**
         * IP has been requested by a host, but not assigned to it yet.
         */
        Option_Requested,

        /**
         * IP Assignment has been requested by a OpenStack.
         */
        Option_RangeNotEnforced,
        /**
         * IP has been assigned to a host.
         */
        Option_Assigned,

        /**
         * IP mapping is no longer active.
         */
        Option_Expired
    }

    /**
     * Constructor for IPAssignment, where the ipAddress, the lease period, the timestamp
     * and assignment status is supplied.
     *
     * @param ipAddress
     * @param leasePeriod
     * @param timestamp
     * @param assignmentStatus
     * @param subnetMask
     * @param dhcpServer
     * @param routerAddress
     * @param domainServer
     * @param rangeNotEnforced
     */
    private IpAssignment(Ip4Address ipAddress,
                         long leasePeriod,
                         Date timestamp,
                         AssignmentStatus assignmentStatus, Ip4Address subnetMask, Ip4Address dhcpServer,
                         Ip4Address routerAddress, Ip4Address domainServer, boolean rangeNotEnforced) {
        this.ipAddress = ipAddress;
        this.leasePeriod = leasePeriod;
        this.timestamp = timestamp;
        this.assignmentStatus = assignmentStatus;
        this.subnetMask = subnetMask;
        this.dhcpServer = dhcpServer;
        this.routerAddress = routerAddress;
        this.domainServer = domainServer;
        this.rangeNotEnforced = rangeNotEnforced;
    }

    /**
     * Returns the IP Address of the IP assignment.
     *
     * @return the IP address
     */
    public Ip4Address ipAddress() {
        return this.ipAddress;
    }

    /**
     * Returns the timestamp of the IP assignment.
     *
     * @return the timestamp
     */
    public Date timestamp() {
        return this.timestamp;
    }

    /**
     * Returns the assignment status of the IP assignment.
     *
     * @return the assignment status
     */
    public AssignmentStatus assignmentStatus() {
        return this.assignmentStatus;
    }

    /**
     * Returns the lease period of the IP assignment.
     *
     * @return the lease period in seconds
     */
    public int leasePeriod() {
        return (int) this.leasePeriod;
    }

    /**
     * Returns the lease period of the IP assignment.
     *
     * @return the lease period in milliseconds
     */
    public int leasePeriodMs() {
        return (int) this.leasePeriod * 1000;
    }

    public Ip4Address subnetMask() {
        return subnetMask;
    }

    public Ip4Address dhcpServer() {
        return dhcpServer;
    }

    public Ip4Address routerAddress() {
        return routerAddress;
    }

    public Ip4Address domainServer() {
        return domainServer;
    }

    public boolean rangeNotEnforced() {
        return rangeNotEnforced;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("ip", ipAddress)
                .add("timestamp", timestamp)
                .add("lease", leasePeriod)
                .add("assignmentStatus", assignmentStatus)
                .add("subnetMask", subnetMask)
                .add("dhcpServer", dhcpServer)
                .add("routerAddress", routerAddress)
                .add("domainServer", domainServer)
                .add("rangeNotEnforced", rangeNotEnforced)
                .toString();
    }

    /**
     * Creates and returns a new builder instance.
     *
     * @return new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates and returns a new builder instance that clones an existing IPAssignment.
     *
     * @param assignment ip address assignment
     * @return new builder
     */
    public static Builder builder(IpAssignment assignment) {
        return new Builder(assignment);
    }

    /**
     * IPAssignment Builder.
     */
    public static final class Builder {

        private Ip4Address ipAddress;

        private Date timeStamp;

        private long leasePeriod;

        private AssignmentStatus assignmentStatus;

        private Ip4Address subnetMask;

        private Ip4Address dhcpServer;

        private Ip4Address domainServer;

        private Ip4Address routerAddress;

        private boolean rangeNotEnforced = false;

        private Builder() {

        }

        private Builder(IpAssignment ipAssignment) {
            ipAddress = ipAssignment.ipAddress();
            timeStamp = ipAssignment.timestamp();
            leasePeriod = ipAssignment.leasePeriod();
            assignmentStatus = ipAssignment.assignmentStatus();
        }

        public IpAssignment build() {
            validateInputs();
            return new IpAssignment(ipAddress, leasePeriod, timeStamp, assignmentStatus, subnetMask,
                    dhcpServer, routerAddress, domainServer, rangeNotEnforced);
        }

        public Builder ipAddress(Ip4Address addr) {
            ipAddress = addr;
            return this;
        }

        public Builder timestamp(Date timestamp) {
            timeStamp = timestamp;
            return this;
        }

        public Builder leasePeriod(int leasePeriodinSeconds) {
            leasePeriod = leasePeriodinSeconds;
            return this;
        }

        public Builder assignmentStatus(AssignmentStatus status) {
            assignmentStatus = status;
            return this;
        }

        public Builder subnetMask(Ip4Address subnetMask) {
            this.subnetMask = subnetMask;
            return this;
        }

        public Builder dhcpServer(Ip4Address dhcpServer) {
            this.dhcpServer = dhcpServer;
            return this;
        }

        public Builder domainServer(Ip4Address domainServer) {
            this.domainServer = domainServer;
            return this;
        }

        public Builder routerAddress(Ip4Address routerAddress) {
            this.routerAddress = routerAddress;
            return this;
        }

        public Builder rangeNotEnforced(boolean rangeNotEnforced) {
            this.rangeNotEnforced = rangeNotEnforced;
            return this;
        }


        private void validateInputs() {
            checkNotNull(ipAddress, "IP Address must be specified");
            checkNotNull(assignmentStatus, "Assignment Status must be specified");
            checkNotNull(leasePeriod, "Lease Period must be specified");
            checkNotNull(timeStamp, "Timestamp must be specified");

            if (rangeNotEnforced) {
                checkNotNull(subnetMask, "subnetMask must be specified in case of rangeNotEnforced");
                checkNotNull(dhcpServer, "dhcpServer must be specified in case of rangeNotEnforced");
                checkNotNull(domainServer, "domainServer must be specified in case of rangeNotEnforced");
                checkNotNull(routerAddress, "routerAddress must be specified in case of rangeNotEnforced");
            }

            switch (assignmentStatus) {
                case Option_Requested:
                case Option_RangeNotEnforced:
                case Option_Assigned:
                case Option_Expired:
                    break;
                default:
                    throw new IllegalStateException("Unknown assignment status");
            }
        }
    }
}
