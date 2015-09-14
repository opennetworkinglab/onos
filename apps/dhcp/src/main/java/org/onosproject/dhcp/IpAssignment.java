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

    private final AssignmentStatus assignmentStatus;

    public enum AssignmentStatus {
        /**
         * IP has been requested by a host, but not assigned to it yet.
         */
        Option_Requested,

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
     * @param assignmentStatus
     */
    private IpAssignment(Ip4Address ipAddress,
                         long leasePeriod,
                         Date timestamp,
                         AssignmentStatus assignmentStatus) {
        this.ipAddress = ipAddress;
        this.leasePeriod = leasePeriod;
        this.timestamp = timestamp;
        this.assignmentStatus = assignmentStatus;
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

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("ip", ipAddress)
                .add("timestamp", timestamp)
                .add("lease", leasePeriod)
                .add("assignmentStatus", assignmentStatus)
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
            return new IpAssignment(ipAddress,
                                    leasePeriod,
                                    timeStamp,
                                    assignmentStatus);
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

        private void validateInputs() {
            checkNotNull(ipAddress, "IP Address must be specified");
            checkNotNull(assignmentStatus, "Assignment Status must be specified");
            checkNotNull(leasePeriod, "Lease Period must be specified");
            checkNotNull(timeStamp, "Timestamp must be specified");

            switch (assignmentStatus) {
                case Option_Requested:
                case Option_Assigned:
                case Option_Expired:
                    break;
                default:
                    throw new IllegalStateException("Unknown assignment status");
            }
        }
    }
}
