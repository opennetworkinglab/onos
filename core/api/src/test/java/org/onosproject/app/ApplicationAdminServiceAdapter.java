/*
 * Copyright 2015-present Open Networking Foundation
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

import java.io.InputStream;
import java.util.Set;

/**
 * Adapter for testing against application admin service.
 */
public class ApplicationAdminServiceAdapter extends ApplicationServiceAdapter
        implements ApplicationAdminService {
    @Override
    public Set<Application> getApplications() {
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
    public void addListener(ApplicationListener listener) {
    }

    @Override
    public void removeListener(ApplicationListener listener) {
    }

    @Override
    public Application install(InputStream appDescStream) {
        return null;
    }

    @Override
    public void uninstall(ApplicationId appId) {
    }

    @Override
    public void activate(ApplicationId appId) {
    }

    @Override
    public void deactivate(ApplicationId appId) {
    }

    @Override
    public void setPermissions(ApplicationId appId, Set<Permission> permissions) {
    }
}
