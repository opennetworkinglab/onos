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
package org.onosproject.drivers.netconf;

import org.onosproject.core.ApplicationId;
import org.onosproject.core.CoreService;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.core.IdGenerator;
import org.onosproject.core.Version;
import org.onosproject.net.intent.MockIdGenerator;

import java.util.HashSet;
import java.util.Set;

public class MockCoreService implements CoreService {

    private HashSet<ApplicationId> appIds;
    private Version version;
    private IdGenerator idGenerator;

    public MockCoreService() {
        appIds = new HashSet<ApplicationId>();
        appIds.add(new DefaultApplicationId(101, "org.onosproject.drivers.netconf"));
        version = Version.version(1, 1, "1", "1");
    }

    @Override
    public Version version() {
        return version;
    }

    @Override
    public Set<ApplicationId> getAppIds() {
        return appIds;
    }

    @Override
    public ApplicationId getAppId(Short id) {
        for (ApplicationId appId:appIds) {
            if (appId.id() == id.shortValue()) {
                return appId;
            }
        }
        return null;
    }

    @Override
    public ApplicationId getAppId(String name) {
        for (ApplicationId appId:appIds) {
            if (appId.name().equalsIgnoreCase(name)) {
                return appId;
            }
        }
        return null;
    }

    @Override
    public ApplicationId registerApplication(String name) {
        ApplicationId appId = new DefaultApplicationId(101, name);
        appIds.add(appId);
        return appId;
    }

    @Override
    public ApplicationId registerApplication(String name, Runnable preDeactivate) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public IdGenerator getIdGenerator(String topic) {
        return MockIdGenerator.INSTANCE;
    }

}
