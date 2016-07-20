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

package org.onosproject.security.store;

import org.onosproject.core.ApplicationId;
import org.onosproject.security.Permission;
import org.onosproject.store.Store;

import java.util.Set;

/**
 * Security-Mode ONOS distributed store service.
 */
public interface SecurityModeStore extends Store<SecurityModeEvent, SecurityModeStoreDelegate> {

    /**
     * Updates the local bundle-application directories.
     * @param appId application identifier
     * @return true if successfully registered.
     */
    boolean registerApplication(ApplicationId appId);

    /**
     * Removes application info from the local bundle-application directories.
     * @param appId application identifier
     */
    void unregisterApplication(ApplicationId appId);

    /**
     * Returns state of the specified application.
     * @param appId application identifier
     * @return Security-Mode State of application
     */
    SecurityModeState getState(ApplicationId appId);

    /**
     * Returns bundle locations of specified application.
     * @param appId application identifier
     * @return set of bundle location strings
     */
    Set<String> getBundleLocations(ApplicationId appId);

    /**
     * Returns application identifiers that are associated with given bundle location.
     * @param location OSGi bundle location
     * @return set of application identifiers
     */
    Set<ApplicationId> getApplicationIds(String location);

    /**
     * Returns a list of permissions that have been requested by given application.
     * @param appId application identifier
     * @return list of permissions
     */
    Set<Permission> getRequestedPermissions(ApplicationId appId);

    /**
     * Returns an array of permissions that have been granted to given application.
     * @param appId application identifier
     * @return array of permissionInfo
     */
    Set<Permission> getGrantedPermissions(ApplicationId appId);

    /**
     * Request permission that is required to run given application.
     * @param appId application identifier
     * @param permission permission
     */
    void requestPermission(ApplicationId appId, Permission permission);

    /**
     * Returns true if given application has been secured.
     * @param appId application identifier
     * @return true indicates secured
     */
    boolean isSecured(ApplicationId appId);

    /**
     * Notifies SM-ONOS that operator has reviewed the policy.
     * @param appId application identifier
     */
    void reviewPolicy(ApplicationId appId);

    /**
     * Accept the current security policy of given application.
     * @param appId application identifier
     * @param permissionSet array of PermissionInfo
     */
    void acceptPolicy(ApplicationId appId, Set<Permission> permissionSet);

}