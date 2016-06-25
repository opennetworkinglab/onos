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
package org.onosproject.dhcp;

import com.google.common.base.MoreObjects;
import org.onlab.packet.Ip4Address;

import java.util.Date;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Stores the MAC ID to IP Address mapping details.
 */
public final class IpAssignment {

    // TODO make some dhcp options optional
    private final Ip4Address ipAddress;
    private final Date timestamp;
    private final long leasePeriod;
    private final Ip4Address subnetMask;
    private final Ip4Address broadcast;
    private final Ip4Address dhcpServer;
    private final Ip4Address routerAddress;
    private final Ip4Address domainServer;
    private final AssignmentStatus assignmentStatus;

    public enum AssignmentStatus {
        /**
         * IP has been requested by a host, but not assigned to it yet.
         */
        Option_Requested,

        /**
         * Static IP Assignment with unregistered IP range.
         * This assignment can only be added or removed by set or remove static mapping.
         */
        // TODO allow multiple IP ranges and remove this option
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
     * @param ipAddress ip address to assign
     * @param leasePeriod lease period
     * @param timestamp time stamp of the assignment
     * @param assignmentStatus statue of the assignment
     * @param subnetMask subnet mask of assigned ip range
     * @param broadcast broadcast address
     * @param dhcpServer dhcp server address
     * @param routerAddress router address
     * @param domainServer domain server address
     */
    private IpAssignment(Ip4Address ipAddress,
                         long leasePeriod,
                         Date timestamp,
                         AssignmentStatus assignmentStatus,
                         Ip4Address subnetMask,
                         Ip4Address broadcast,
                         Ip4Address dhcpServer,
                         Ip4Address routerAddress,
                         Ip4Address domainServer) {
        this.ipAddress = ipAddress;
        this.leasePeriod = leasePeriod;
        this.timestamp = timestamp;
        this.assignmentStatus = assignmentStatus;
        this.subnetMask = subnetMask;
        this.broadcast = broadcast;
        this.dhcpServer = dhcpServer;
        this.routerAddress = routerAddress;
        this.domainServer = domainServer;
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

    /**
     * Returns subnet mask of the IP assignment.
     *
     * @return subnet mask
     */
    public Ip4Address subnetMask() {
        return subnetMask;
    }

    /**
     * Returns broadcast address of the IP assignment.
     *
     * @return broadcast address
     */
    public Ip4Address broadcast() {
        return broadcast;
    }

    /**
     * Returns dhcp server of the IP assignment.
     *
     * @return dhcp server ip address
     */
    public Ip4Address dhcpServer() {
        return dhcpServer;
    }

    /**
     * Returns router address of the IP assignment.
     *
     * @return router ip address
     */
    public Ip4Address routerAddress() {
        return routerAddress;
    }

    /**
     * Returns domain server address.
     *
     * @return domain server ip address
     */
    public Ip4Address domainServer() {
        return domainServer;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("ip", ipAddress)
                .add("timestamp", timestamp)
                .add("lease", leasePeriod)
                .add("assignmentStatus", assignmentStatus)
                .add("subnetMask", subnetMask)
                .add("broadcast", broadcast)
                .add("dhcpServer", dhcpServer)
                .add("routerAddress", routerAddress)
                .add("domainServer", domainServer)
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
        private Ip4Address broadcast;
        private Ip4Address dhcpServer;
        private Ip4Address routerAddress;
        private Ip4Address domainServer;

        private Builder() {
        }

        private Builder(IpAssignment ipAssignment) {
            ipAddress = ipAssignment.ipAddress();
            timeStamp = ipAssignment.timestamp();
            leasePeriod = ipAssignment.leasePeriod();
            assignmentStatus = ipAssignment.assignmentStatus();
            subnetMask = ipAssignment.subnetMask();
            broadcast = ipAssignment.broadcast();
            dhcpServer = ipAssignment.dhcpServer();
            routerAddress = ipAssignment.routerAddress();
            domainServer = ipAssignment.domainServer();
        }

        public IpAssignment build() {
            validateInputs();
            return new IpAssignment(ipAddress,
                                    leasePeriod,
                                    timeStamp,
                                    assignmentStatus,
                                    subnetMask,
                                    broadcast,
                                    dhcpServer,
                                    routerAddress,
                                    domainServer);
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

        public Builder broadcast(Ip4Address broadcast) {
            this.broadcast = broadcast;
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

        private void validateInputs() {
            checkNotNull(ipAddress, "IP Address must be specified");
            checkNotNull(assignmentStatus, "Assignment Status must be specified");
            checkNotNull(leasePeriod, "Lease Period must be specified");
            checkNotNull(timeStamp, "Timestamp must be specified");

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
