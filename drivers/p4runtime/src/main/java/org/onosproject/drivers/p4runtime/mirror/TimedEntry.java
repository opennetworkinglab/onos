/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.drivers.p4runtime.mirror;

import com.google.common.base.MoreObjects;
import org.onosproject.net.pi.runtime.PiEntity;
import org.onosproject.store.service.WallClockTimestamp;

import java.util.Objects;

public class TimedEntry<E extends PiEntity> {

    private final long timestamp;
    private final E entity;

    TimedEntry(long timestamp, E entity) {
        this.timestamp = timestamp;
        this.entity = entity;
    }

    public long timestamp() {
        return timestamp;
    }

    public E entry() {
        return entity;
    }

    public long lifeSec() {
        final long now = new WallClockTimestamp().unixTimestamp();
        return (now - timestamp) / 1000;
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestamp, entity);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final TimedEntry other = (TimedEntry) obj;
        return Objects.equals(this.timestamp, other.timestamp)
                && Objects.equals(this.entity, other.entity);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("timestamp", timestamp)
                .add("entity", entity)
                .toString();
    }
}
