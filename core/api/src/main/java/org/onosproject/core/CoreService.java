/*
 * Copyright 2014-present Open Networking Foundation
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
package org.onosproject.core;

import org.onosproject.net.provider.ProviderId;

import java.util.Set;

/**
 * Service for interacting with the core system of the controller.
 */
public interface CoreService {

    /**
     * Name of the core "application".
     */
    String CORE_APP_NAME = "org.onosproject.core";

    /**
     * Identifier of the core "provider".
     */
    ProviderId CORE_PROVIDER_ID = new ProviderId("core", CORE_APP_NAME);

    /**
     * Returns the product version.
     *
     * @return product version
     */
    Version version();

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
     * @return an application id
     */
    ApplicationId getAppId(Short id);

    /**
     * Returns an existing application id from a given id.
     *
     * @param name the name portion of the ID to look up
     * @return an application id
     */
    ApplicationId getAppId(String name);

    /**
     * Registers a new application by its name, which is expected
     * to follow the reverse DNS convention, e.g.
     * {@code org.flying.circus.app}
     *
     * @param name string identifier
     * @return the application id
     */
    ApplicationId registerApplication(String name);

    /**
     * Registers a new application by its name, which is expected
     * to follow the reverse DNS convention, e.g.
     * {@code org.flying.circus.app}, along with its pre-deactivation hook.
     *
     * @param name          string identifier
     * @param preDeactivate pre-deactivation hook
     * @return the application id
     */
    ApplicationId registerApplication(String name, Runnable preDeactivate);

    /**
     * Returns an id generator for a given topic.
     *
     * @param topic topic identified
     * @return the id generator
     */
    IdGenerator getIdGenerator(String topic);

}
