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

import org.onosproject.core.ApplicationRole;
import org.onosproject.core.Version;
import org.onosproject.security.Permission;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Description of a network control/management application.
 */
public interface ApplicationDescription {

    /**
     * Returns the application name id.
     *
     * @return application identifier
     */
    String name();

    /**
     * Returns the application version.
     *
     * @return application version
     */
    Version version();

    /**
     * Returns the name of the application origin, group or company.
     *
     * @return application origin
     */
    String origin();

    /**
     * Returns title of the application.
     *
     * @return application title text
     */
    String title();

    /**
     * Returns description of the application.
     *
     * @return application description text
     */
    String description();

    /**
     * Returns category of the application.
     *
     * The application developer can choose one of the category from the
     * following examples to easily discern the high-level purpose of the application.
     * (Security, Traffic Steering, Monitoring, Drivers, Provider, Utility)
     *
     * @return application category text
     */
    String category();

    /**
     * Returns url of the application.
     *
     * @return application url
     */
    String url();

    /**
     * Returns readme of the application.
     *
     * @return application readme
     */
    String readme();

    /**
     * Returns icon of the application.
     *
     * @return application icon
     */
    byte[] icon();

    /**
     * Returns the role of the application.
     *
     * @return application role
     */
    ApplicationRole role();

    /**
     * Returns the permissions requested by the application.
     *
     * @return requested permissions
     */
    Set<Permission> permissions();

    /**
     * Returns the feature repository URI. Null value signifies that the
     * application did not provide its own features repository.
     *
     * @return optional feature repo URL
     */
    Optional<URI> featuresRepo();

    /**
     * Returns the list of features comprising the application. At least one
     * feature must be given.
     *
     * @return application features
     */
    List<String> features();

    /**
     * Returns list of required application names.
     *
     * @return list of application names
     */
    List<String> requiredApps();
}
