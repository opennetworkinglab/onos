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
package org.onosproject.core.impl;

import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.core.IdGenerator;
import org.onosproject.core.Version;

import java.util.Set;

public class TestCoreManager implements CoreService {
    @Override
    public Version version() {
        return null;
    }

    @Override
    public Set<ApplicationId> getAppIds() {
        return null;
    }

    @Override
    public ApplicationId getAppId(Short id) {
        return null;
    }

    @Override
    public ApplicationId registerApplication(String identifier) {
        return null;
    }

    @Override
    public IdGenerator getIdGenerator(String topic) {
        IdBlockAllocator idBlockAllocator = new DummyIdBlockAllocator();
        return new BlockAllocatorBasedIdGenerator(idBlockAllocator);
    }
}
