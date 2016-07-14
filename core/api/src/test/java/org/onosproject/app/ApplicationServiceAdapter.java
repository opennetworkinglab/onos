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

import java.util.Set;

/**
 * Adapter for testing against application service.
 */
public class ApplicationServiceAdapter implements ApplicationService {
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
    public Set<Permission> getPermissions(ApplicationId appId) {
        return null;
    }

    @Override
    public void registerDeactivateHook(ApplicationId appId, Runnable hook) {
    }

    @Override
    public void addListener(ApplicationListener listener) {
    }

    @Override
    public void removeListener(ApplicationListener listener) {
    }
}
