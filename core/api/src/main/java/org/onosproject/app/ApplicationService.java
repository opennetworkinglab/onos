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
import org.onosproject.event.ListenerService;
import org.onosproject.security.Permission;

import java.util.Set;

/**
 * Service for inspecting inventory of network control applications.
 */
public interface ApplicationService
        extends ListenerService<ApplicationEvent, ApplicationListener> {

    /**
     * Returns the set of all installed applications.
     *
     * @return set of installed apps
     */
    Set<Application> getApplications();

    /**
     * Returns the registered id of the application with the given name.
     *
     * @param name application name
     * @return registered application id
     */
    ApplicationId getId(String name);

    /**
     * Returns the application with the supplied application identifier.
     *
     * @param appId application identifier
     * @return application descriptor
     */
    Application getApplication(ApplicationId appId);

    /**
     * Return the application state.
     *
     * @param appId application identifier
     * @return application state
     */
    ApplicationState getState(ApplicationId appId);

    /**
     * Returns the permissions currently granted to the applications.
     *
     * @param appId application identifier
     * @return set of granted permissions
     */
    Set<Permission> getPermissions(ApplicationId appId);

    /**
     * Registers application pre-deactivation processing hook.
     *
     * @param appId application identifier
     * @param hook  pre-deactivation hook
     */
    void registerDeactivateHook(ApplicationId appId, Runnable hook);
}
