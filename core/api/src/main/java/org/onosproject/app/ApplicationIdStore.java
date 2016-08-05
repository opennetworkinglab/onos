/*
 * Copyright 2014-present Open Networking Laboratory
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

import org.onosproject.core.ApplicationId;

import java.util.Set;

/**
 * Manages application IDs.
 */
public interface ApplicationIdStore {

    /**
     * Returns the set of currently registered application identifiers.
     *
     * @return set of application ids
     */
    Set<ApplicationId> getAppIds();

    /**
     * Returns an existing application id from a given id.
     *
     * @param id the short value of the id
     * @return an application id; null if no such app registered
     */
    ApplicationId getAppId(Short id);

    /**
     * Returns registered application id from the given name.
     *
     * @param name application name
     * @return an application id; null if no such app registered
     */
    ApplicationId getAppId(String name);

    /**
     * Registers a new application by its name, which is expected
     * to follow the reverse DNS convention, e.g.
     * {@code org.flying.circus.app}
     *
     * @param identifier string identifier
     * @return the application id
     */
    ApplicationId registerApplication(String identifier);
}
