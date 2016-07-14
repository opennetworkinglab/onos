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

package org.onosproject.incubator.component;

import org.onosproject.core.ApplicationId;

/**
 * Service for managing the components in the system.
 */
public interface ComponentService {

    /**
     * Activates the component identified by the given name. If the component
     * is not currently available, it will be activated when it becomes
     * available.
     *
     * @param appId application ID of the requesting application
     * @param name fully-qualified name of the component to activate
     */
    void activate(ApplicationId appId, String name);

    /**
     * Deactivates the component identified by the given name.
     *
     * @param appId application ID of the requesting application
     * @param name fully-qualified name of the component to deactivate
     */
    void deactivate(ApplicationId appId, String name);
}
