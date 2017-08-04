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

package org.onosproject.security;

import com.google.common.annotations.Beta;
import org.onosproject.core.ApplicationId;

import java.security.Permission;
import java.util.List;
import java.util.Map;

/**
 * Security-Mode ONOS service.
 */
@Beta
public interface SecurityAdminService {

    /**
     * Returns true if security policy has been enforced to specified application.
     * @param appId application identifier
     * @return true if secured.
     */
    boolean isSecured(ApplicationId appId);

    /**
     * Changes SecurityModeState of specified application to REVIEWED.
     * @param appId application identifier
     */
    void review(ApplicationId appId);

    /**
     * Accepts and enforces security policy to specified application.
     * @param appId application identifier
     */
    void acceptPolicy(ApplicationId appId);

    /**
     * Register application to SM-ONOS subsystem.
     * @param appId application identifier
     */
    void register(ApplicationId appId);

    /**
     * Returns sorted developer specified permission Map.
     * @param appId application identifier
     * @return Map of list of permissions sorted by permission type
     */
    Map<Integer, List<Permission>> getPrintableSpecifiedPermissions(ApplicationId appId);

    /**
     * Returns sorted granted permission Map.
     * @param appId application identifier
     * @return Map of list of permissions sorted by permission type
     */
    Map<Integer, List<Permission>> getPrintableGrantedPermissions(ApplicationId appId);

    /**
     * Returns sorted requested permission Map.
     * @param appId application identifier
     * @return Map of list of permissions sorted by permission type
     */
    Map<Integer, List<Permission>> getPrintableRequestedPermissions(ApplicationId appId);
}
