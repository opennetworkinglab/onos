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

import org.onosproject.core.ApplicationId;
import org.onosproject.net.DeviceId;

/**
 * Default implementation class for mapping.
 */
public class DefaultMapping implements Mapping {
    @Override
    public MappingId id() {
        return null;
    }

    @Override
    public short appId() {
        return 0;
    }

    @Override
    public DeviceId deviceId() {
        return null;
    }

    public static final class Builder implements Mapping.Builder {

        @Override
        public Mapping.Builder fromApp(ApplicationId appId) {
            return null;
        }

        @Override
        public Mapping.Builder forDevice(DeviceId deviceId) {
            return null;
        }

        @Override
        public Mapping build() {
            return null;
        }
    }
}
