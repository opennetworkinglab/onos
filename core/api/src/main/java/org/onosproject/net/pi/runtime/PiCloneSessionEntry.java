/*
 * Copyright 2019-present Open Networking Foundation
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

package org.onosproject.net.pi.runtime;


import com.google.common.annotations.Beta;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableSet;
import org.onosproject.net.DeviceId;

import java.util.Collection;
import java.util.Set;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Representation of a clone session entry of a protocol-independent packet
 * replication engine (PRE).
 */
@Beta
public final class PiCloneSessionEntry implements PiPreEntry {

    public static final int DEFAULT_CLASS_OF_SERVICE = 0;
    public static final int DO_NOT_TRUNCATE = 0;

    private final int sessionId;
    private final Set<PiPreReplica> replicas;
    private final int classOfService;
    private final int maxPacketLengthBytes;

    private PiCloneSessionEntry(int sessionId, Set<PiPreReplica> replicas,
                                int classOfService, int maxPacketBytes) {
        this.sessionId = sessionId;
        this.replicas = replicas;
        this.classOfService = classOfService;
        this.maxPacketLengthBytes = maxPacketBytes;
    }

    /**
     * Returns the identifier of this clone session, unique in the scope of a
     * PRE instance.
     *
     * @return clone session ID
     */
    public int sessionId() {
        return sessionId;
    }

    /**
     * Returns the packet replicas provided by this clone session.
     *
     * @return packet replicas
     */
    public Set<PiPreReplica> replicas() {
        return replicas;
    }

    /**
     * Returns the class of service associated to the replicas produced by this
     * clone session.
     *
     * @return class of service
     */
    public int classOfService() {
        return classOfService;
    }

    /**
     * Returns the maximum length in bytes of cloned packets. If a larger packet
     * is cloned, then the PRE is expected to truncate clones to the given size.
     * 0 means that no truncation on the clone(s) will be performed.
     *
     * @return maximum length in bytes of clones packets
     */
    public int maxPacketLengthBytes() {
        return maxPacketLengthBytes;
    }

    @Override
    public PiEntityType piEntityType() {
        return PiEntityType.PRE_ENTRY;
    }

    @Override
    public PiPreEntryType preEntryType() {
        return PiPreEntryType.CLONE_SESSION;
    }

    @Override
    public PiCloneSessionEntryHandle handle(DeviceId deviceId) {
        return PiCloneSessionEntryHandle.of(deviceId, this);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(sessionId, replicas, classOfService,
                                maxPacketLengthBytes);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final PiCloneSessionEntry other = (PiCloneSessionEntry) obj;
        return Objects.equal(this.sessionId, other.sessionId)
                && Objects.equal(this.replicas, other.replicas)
                && Objects.equal(this.classOfService, other.classOfService)
                && Objects.equal(this.maxPacketLengthBytes, other.maxPacketLengthBytes);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("sessionId", sessionId)
                .add("replicas", replicas)
                .add("classOfService", classOfService)
                .add("maxPacketLengthBytes", maxPacketLengthBytes)
                .toString();
    }

    /**
     * Returns a new builder of clone session entries.
     *
     * @return builder
     */
    public static PiCloneSessionEntry.Builder builder() {
        return new PiCloneSessionEntry.Builder();
    }

    /**
     * Builder of PI clone session entries.
     */
    public static final class Builder {

        private Integer sessionId;
        private ImmutableSet.Builder<PiPreReplica> replicaSetBuilder = ImmutableSet.builder();
        private int classOfService = DEFAULT_CLASS_OF_SERVICE;
        private int maxPacketLengthBytes = DO_NOT_TRUNCATE;

        private Builder() {
            // Hide constructor.
        }

        /**
         * Sets the identifier of this clone session.
         *
         * @param sessionId session ID
         * @return this
         */
        public PiCloneSessionEntry.Builder withSessionId(int sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        /**
         * Adds the given packet replica to this clone session.
         *
         * @param replica packet replica
         * @return this
         */
        public PiCloneSessionEntry.Builder addReplica(PiPreReplica replica) {
            checkNotNull(replica);
            replicaSetBuilder.add(replica);
            return this;
        }

        /**
         * Adds the given packet replicas to this clone session.
         *
         * @param replicas packet replicas
         * @return this
         */
        public PiCloneSessionEntry.Builder addReplicas(Collection<PiPreReplica> replicas) {
            checkNotNull(replicas);
            replicaSetBuilder.addAll(replicas);
            return this;
        }

        /**
         * Sets the class of service of this clone session. If not set, the
         * default value {@link PiCloneSessionEntry#DEFAULT_CLASS_OF_SERVICE}
         * will be used.
         *
         * @param classOfService class of service value
         * @return this
         */
        public PiCloneSessionEntry.Builder withClassOfService(
                int classOfService) {
            this.classOfService = classOfService;
            return this;
        }

        /**
         * Sets the maximum length in bytes of cloned packets. If not set, the
         * default value {@link PiCloneSessionEntry#DO_NOT_TRUNCATE} will be
         * used.
         *
         * @param maxPacketLengthBytes max length in bytes of cloned packets
         * @return this
         */
        public PiCloneSessionEntry.Builder withMaxPacketLengthBytes(
                int maxPacketLengthBytes) {
            checkArgument(maxPacketLengthBytes >= 0,
                          "maxPacketLengthBytes must be a positive integer");
            this.maxPacketLengthBytes = maxPacketLengthBytes;
            return this;
        }

        /**
         * Returns a new clone session entry.
         *
         * @return clone session entry
         */
        public PiCloneSessionEntry build() {
            checkNotNull(sessionId, "Clone session ID must be set");
            final ImmutableSet<PiPreReplica> replicas = replicaSetBuilder.build();
            return new PiCloneSessionEntry(
                    sessionId, replicas, classOfService, maxPacketLengthBytes);
        }
    }
}

