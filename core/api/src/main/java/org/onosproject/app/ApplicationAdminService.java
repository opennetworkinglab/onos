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

import java.io.InputStream;
import java.util.Set;

/**
 * Service for managing network control applications.
 */
public interface ApplicationAdminService extends ApplicationService {

    /**
     * Installs the application contained in the specified application archive
     * input stream. This can be either a ZIP stream containing a compressed
     * application archive or a plain XML stream containing just the
     * {@code app.xml} application descriptor file.
     *
     * @param appDescStream application descriptor input stream
     * @return installed application descriptor
     * @throws org.onosproject.app.ApplicationException if unable to read the app archive stream
     */
    Application install(InputStream appDescStream);

    /**
     * Uninstalls the specified application.
     *
     * @param appId application identifier
     */
    void uninstall(ApplicationId appId);

    /**
     * Activates the specified application.
     *
     * @param appId application identifier
     */
    void activate(ApplicationId appId);

    /**
     * Deactivates the specified application.
     *
     * @param appId application identifier
     */
    void deactivate(ApplicationId appId);

    /**
     * Updates the permissions granted to the applications.
     *
     * @param appId       application identifier
     * @param permissions set of granted permissions
     */
    void setPermissions(ApplicationId appId, Set<Permission> permissions);
}
