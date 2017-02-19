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
package org.onosproject.mapping;

import org.onlab.packet.IpPrefix;
import org.onosproject.mapping.actions.MappingAction;
import org.onosproject.mapping.actions.MappingActions;
import org.onosproject.mapping.addresses.MappingAddress;
import org.onosproject.mapping.addresses.MappingAddresses;
import org.onosproject.net.DeviceId;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Commons mocks used by the mapping management tasks.
 */
public class MappingTestMocks {

    private static final String IP = "1.2.3.4/24";

    /**
     * Mock mapping key class used for satisfying API requirements.
     */
    public static class MockMappingKey implements MappingKey {

        @Override
        public MappingAddress address() {
            IpPrefix ip = IpPrefix.valueOf(IP);
            return MappingAddresses.ipv4MappingAddress(ip);
        }
    }

    /**
     * Mock mapping value class used for satisfying API requirements.
     */
    public static class MockMappingValue implements MappingValue {

        @Override
        public MappingAction action() {
            return MappingActions.noAction();
        }

        @Override
        public List<MappingTreatment> treatments() {
            return Collections.emptyList();
        }
    }

    private static final MockMappingKey MAPPING_KEY = new MockMappingKey();
    private static final MockMappingValue MAPPING_VALUE = new MockMappingValue();

    /**
     * Mock mapping class used for satisfying API requirements.
     */
    public static class MockMapping implements Mapping {

        static int nextId = 0;

        int id;
        long timestamp;

        public MockMapping() {
            this.id = nextId++;
            this.timestamp = System.currentTimeMillis();
        }

        @Override
        public MappingId id() {
            return MappingId.valueOf(id);
        }

        @Override
        public short appId() {
            return 0;
        }

        @Override
        public DeviceId deviceId() {
            return DeviceId.deviceId("lisp:" + id);
        }

        @Override
        public MappingKey key() {
            return MAPPING_KEY;
        }

        @Override
        public MappingValue value() {
            return MAPPING_VALUE;
        }

        @Override
        public int hashCode() {
            return id;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            final MockMapping other = (MockMapping) obj;
            return Objects.equals(this.timestamp, other.timestamp) &&
                    this.id == other.id;
        }
    }
}
