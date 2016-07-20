/*
 * Copyright 2015-present Open Networking Laboratory
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
package org.onosproject.app;

import org.onosproject.core.Application;
import org.onosproject.core.ApplicationId;
import org.onosproject.security.Permission;
import org.onosproject.store.AbstractStore;

import java.io.InputStream;
import java.util.Set;

/**
 * Adapter for application testing against application store.
 */
public class ApplicationStoreAdapter
        extends AbstractStore<ApplicationEvent, ApplicationStoreDelegate>
        implements ApplicationStore {
    @Override
    public Set<Application> getApplications() {
        return null;
    }

    @Override
    public ApplicationId getId(String name) {
        return null;
    }

    @Override
    public Application getApplication(ApplicationId appId) {
        return null;
    }

    @Override
    public ApplicationState getState(ApplicationId appId) {
        return null;
    }

    @Override
    public Application create(InputStream appDescStream) {
        return null;
    }

    @Override
    public void remove(ApplicationId appId) {
    }

    @Override
    public void activate(ApplicationId appId) {
    }

    @Override
    public void deactivate(ApplicationId appId) {
    }

    @Override
    public Set<Permission> getPermissions(ApplicationId appId) {
        return null;
    }

    @Override
    public void setPermissions(ApplicationId appId, Set<Permission> permissions) {
    }

}
