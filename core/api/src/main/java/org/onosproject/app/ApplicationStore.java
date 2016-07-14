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
import org.onosproject.store.Store;

import java.io.InputStream;
import java.util.Set;

/**
 * Service for managing network control applications.
 */
public interface ApplicationStore extends Store<ApplicationEvent, ApplicationStoreDelegate> {

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
     * Returns the current application state.
     *
     * @param appId application identifier
     * @return application state
     */
    ApplicationState getState(ApplicationId appId);

    /**
     * Creates the application from the specified application descriptor
     * input stream.
     *
     * @param appDescStream application archive input stream
     * @return application descriptor
     */
    Application create(InputStream appDescStream);

    /**
     * Removes the specified application.
     *
     * @param appId application identifier
     */
    void remove(ApplicationId appId);

    /**
     * Mark the application as active.
     *
     * @param appId application identifier
     */
    void activate(ApplicationId appId);

    /**
     * Mark the application as deactivated.
     *
     * @param appId application identifier
     */
    void deactivate(ApplicationId appId);

    /**
     * Returns the permissions granted to the applications.
     *
     * @param appId application identifier
     * @return set of granted permissions
     */
    Set<Permission> getPermissions(ApplicationId appId);

    /**
     * Updates the permissions granted to the applications.
     *
     * @param appId       application identifier
     * @param permissions set of granted permissions
     */
    void setPermissions(ApplicationId appId, Set<Permission> permissions);

}
