/*
 * Copyright 2017-present Open Networking Laboratory
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

package org.onosproject.p4runtime.ctl;

import com.google.common.base.Objects;
import org.onlab.util.ImmutableByteSequence;
import org.onosproject.event.AbstractEvent;
import org.onosproject.net.DeviceId;
import org.onosproject.p4runtime.api.P4RuntimeEvent;
import org.onosproject.p4runtime.api.P4RuntimeEventListener;
import org.onosproject.p4runtime.api.P4RuntimeEventSubject;
import org.onosproject.p4runtime.api.P4RuntimePacketIn;

import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Default implementation of a packet-in event.
 */
final class DefaultPacketInEvent
        extends AbstractEvent<P4RuntimeEventListener.Type, P4RuntimeEventSubject>
        implements P4RuntimeEvent {

    DefaultPacketInEvent(DeviceId deviceId, ImmutableByteSequence data,
                                   List<ImmutableByteSequence> metadata) {
        super(P4RuntimeEventListener.Type.PACKET_IN, new DefaultPacketIn(deviceId, data, metadata));
    }

    /**
     * Default implementation of a packet-in in P4Runtime.
     */
    private static final class DefaultPacketIn implements P4RuntimePacketIn {

        private final DeviceId deviceId;
        private final ImmutableByteSequence data;
        private final List<ImmutableByteSequence> metadata;

        private DefaultPacketIn(DeviceId deviceId, ImmutableByteSequence data, List<ImmutableByteSequence> metadata) {
            this.deviceId = checkNotNull(deviceId);
            this.data = checkNotNull(data);
            this.metadata = checkNotNull(metadata);
        }

        @Override
        public DeviceId deviceId() {
            return deviceId;
        }

        @Override
        public ImmutableByteSequence data() {
            return data;
        }

        @Override
        public List<ImmutableByteSequence> metadata() {
            return metadata;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            DefaultPacketIn that = (DefaultPacketIn) o;
            return Objects.equal(deviceId, that.deviceId) &&
                    Objects.equal(data, that.data) &&
                    Objects.equal(metadata, that.metadata);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(deviceId, data, metadata);
        }
    }
}
