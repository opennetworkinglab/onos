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
package org.onosproject.vtnrsc;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.MoreObjects;

import java.util.UUID;
import java.util.Objects;

/**
 * Flow classification identifier.
 */
public final class FlowClassifierId {

    private final UUID flowClassifierId;

    /**
     * Constructor to create flow classifier id.
     *
     * @param flowClassifierId flow classifier id.
     */
    private FlowClassifierId(final UUID flowClassifierId) {
        checkNotNull(flowClassifierId, "Flow classifier id can not be null");
        this.flowClassifierId = flowClassifierId;
    }

    /**
     * Returns new flow classifier id.
     *
     * @param flowClassifierId flow classifier id
     * @return new flow classifier id
     */
    public static FlowClassifierId of(final UUID flowClassifierId) {
        return new FlowClassifierId(flowClassifierId);
    }

    /**
     * Returns new flow classifier id.
     *
     * @param flowClassifierId flow classifier id
     * @return new flow classifier id
     */
    public static FlowClassifierId of(final String flowClassifierId) {
        return new FlowClassifierId(UUID.fromString(flowClassifierId));
    }

    /**
     * Returns the value of flow classifier id.
     *
     * @return flow classifier id.
     */
    public UUID value() {
        return flowClassifierId;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.flowClassifierId);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof FlowClassifierId) {
            final FlowClassifierId other = (FlowClassifierId) obj;
            return Objects.equals(this.flowClassifierId, other.flowClassifierId);
        }
        return false;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("FlowClassifierId", flowClassifierId)
                .toString();
    }
}
