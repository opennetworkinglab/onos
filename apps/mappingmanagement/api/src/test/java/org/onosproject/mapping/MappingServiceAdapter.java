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
import org.onosproject.mapping.MappingStore.Type;
import org.onosproject.net.DeviceId;

/**
 * Adapter for testing against mapping service.
 */
public class MappingServiceAdapter implements MappingService {
    @Override
    public void addListener(MappingListener listener) {

    }

    @Override
    public void removeListener(MappingListener listener) {

    }

    @Override
    public int getMappingCount(Type type) {
        return 0;
    }

    @Override
    public Iterable<MappingEntry> getAllMappingEntries(Type type) {
        return null;
    }


    @Override
    public Iterable<MappingEntry> getMappingEntries(Type type, DeviceId deviceId) {
        return null;
    }

    @Override
    public Iterable<MappingEntry> getMappingEntriesByAppId(Type type, ApplicationId appId) {
        return null;
    }
}
