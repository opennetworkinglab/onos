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
 * limitations under the License.%
 */

package org.onosproject.pipelines.fabric.impl.behaviour.bng;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import org.onlab.packet.MacAddress;
import org.onlab.packet.VlanId;
import org.onosproject.net.behaviour.BngProgrammable;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * An allocator of line IDs from a fixed range/pool. Used to map attachments to
 * IDs for counters and other indirect P4 resources.
 * <p>
 * An implementation of this interface should use the {@link Handle} class to
 * uniquely identify attachments and determine whether an ID has already been
 * allocated or not.
 */
public interface FabricBngLineIdAllocator {

    /**
     * Returns a new ID for the given attachment. The implementation is expected
     * to be idempotent, i.e., if an ID was previously allocated, then no new
     * IDs should be allocated but the previous one should be returned.
     *
     * @param attachment the attachment instance
     * @return the ID
     * @throws IdExhaustedException if all IDs are currently allocated.
     */
    long allocate(BngProgrammable.Attachment attachment) throws IdExhaustedException;

    /**
     * Releases any ID previously allocated for the given attachment. If one was
     * not allocated, calling this method should be a no-op.
     *
     * @param attachment the attachment instance
     */
    void release(BngProgrammable.Attachment attachment);

    /**
     * Releases the given ID, if allocated, otherwise calling this method should
     * be a no-op.
     *
     * @param id the ID to release
     */
    void release(long id);

    /**
     * Returns the maximum number of IDs that can be allocated, independently of
     * the current state.
     *
     * @return maximum number of IDs
     */
    long size();

    /**
     * Returns the number of currently available IDs that can be allocated for
     * new attachments.
     *
     * @return free ID count
     */
    long freeCount();

    /**
     * Returns the number of currently allocated IDs.
     *
     * @return allocated ID count
     */
    long allocatedCount();

    /**
     * An identifier of an attachment in the scope of the same instance of a
     * {@link FabricBngLineIdAllocator}.
     */
    class Handle {

        private final VlanId stag;
        private final VlanId ctag;
        private final MacAddress macAddress;

        public Handle(BngProgrammable.Attachment attachment) {
            this.stag = checkNotNull(attachment.sTag());
            this.ctag = checkNotNull(attachment.cTag());
            this.macAddress = checkNotNull(attachment.macAddress());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            final Handle that = (Handle) o;
            return Objects.equal(stag, that.stag) &&
                    Objects.equal(ctag, that.ctag) &&
                    Objects.equal(macAddress, that.macAddress);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(stag, ctag, macAddress);
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this)
                    .add("stag", stag)
                    .add("ctag", ctag)
                    .add("macAddress", macAddress)
                    .toString();
        }
    }

    /**
     * Signals that no more IDs are currently available, but some have to be
     * released before allocating a new one.
     */
    class IdExhaustedException extends Exception {
    }
}
