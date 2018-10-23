/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.openstackvtap.api;

/**
 * Service for administering the inventory of openstack vtap.
 */
public interface OpenstackVtapAdminService extends OpenstackVtapService {

    /**
     * Initializes the flow rules and group tables, tunneling interface for all completed compute nodes.
     */
    void initVtap();

    /**
     * Clears the flow rules and group tables, tunneling interfaces for all compute nodes.
     */
    void clearVtap();

    /**
     * Purges all flow rules and group tables, tunneling interface for openstack vtap.
     */
    void purgeVtap();

}
