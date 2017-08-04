/*
 * Copyright 2017-present Open Networking Foundation
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

import static org.onosproject.security.store.SecurityModeState.INSTALLED;
import static org.onosproject.security.store.SecurityModeState.POLICY_VIOLATED;
import static org.onosproject.security.store.SecurityModeState.REVIEWED;
import static org.onosproject.security.store.SecurityModeState.SECURED;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.onosproject.core.ApplicationId;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.security.Permission;

import com.google.common.collect.ImmutableSet;

/**
 * Test adapter for SecurityModeStore.
 */
public class SecurityModeStoreAdapter implements SecurityModeStore {

    private DefaultApplicationId appId;
    private ConcurrentHashMap<ApplicationId, SecurityInfo> states;
    private ConcurrentHashMap<ApplicationId, Set<Permission>> violations;
    private SecurityInfo testSecInfo;
    private Permission testPermission;
    private Set<Permission> testPermissions;

    public SecurityModeStoreAdapter() {
        states = new ConcurrentHashMap<ApplicationId, SecurityInfo>();
        testPermissions = new HashSet<Permission>();
        testPermission = new Permission("testClass", "testName");
        testPermissions.add(testPermission);

        testSecInfo = new SecurityInfo(testPermissions, SECURED);

        appId = new DefaultApplicationId(2, "violation");
        testPermissions = new HashSet<Permission>();
        testPermission = new Permission("testClass", "testNameViolation");
        testPermissions.add(testPermission);
        violations = new ConcurrentHashMap<ApplicationId, Set<Permission>>();
        violations.put(appId, testPermissions);
    }

    @Override
    public void setDelegate(SecurityModeStoreDelegate delegate) {
    }

    @Override
    public void unsetDelegate(SecurityModeStoreDelegate delegate) {
    }

    @Override
    public boolean hasDelegate() {
        return false;
    }

    @Override
    public boolean registerApplication(ApplicationId appId) {
        states.put(appId, testSecInfo);
        return true;
    }

    @Override
    public void unregisterApplication(ApplicationId appId) {
        states.remove(appId);
    }

    @Override
    public SecurityModeState getState(ApplicationId appId) {
        return states.get(appId).getState();
    }

    @Override
    public Set<String> getBundleLocations(ApplicationId appId) {
        return null;
    }

    @Override
    public Set<ApplicationId> getApplicationIds(String location) {
        return null;
    }

    @Override
    public Set<Permission> getRequestedPermissions(ApplicationId appId) {
        Set<Permission> permissions = violations.get(appId);
        return permissions != null ? permissions : ImmutableSet.of();
    }

    @Override
    public Set<Permission> getGrantedPermissions(ApplicationId appId) {
        return states.get(appId).getPermissions();
    }

    @Override
    public void requestPermission(ApplicationId appId, Permission permission) {
    }

    @Override
    public boolean isSecured(ApplicationId appId) {
        SecurityInfo info = states.get(appId);
        return info == null ? false : info.getState().equals(SECURED);
    }

    @Override
    public void reviewPolicy(ApplicationId appId) {
        states.computeIfPresent(appId, (applicationId, securityInfo) -> {
            if (securityInfo.getState().equals(SECURED)) {
                return new SecurityInfo(ImmutableSet.of(), REVIEWED);
            }
            return securityInfo;
        });
    }

    @Override
    public void acceptPolicy(ApplicationId appId, Set<Permission> permissionSet) {
        states.compute(appId,
                (id, securityInfo) -> {
                    switch (securityInfo.getState()) {
                        case POLICY_VIOLATED:
                            return new SecurityInfo(securityInfo.getPermissions(), SECURED);
                        case SECURED:
                            return new SecurityInfo(securityInfo.getPermissions(), POLICY_VIOLATED);
                        case INSTALLED:
                            return new SecurityInfo(securityInfo.getPermissions(), REVIEWED);
                        case REVIEWED:
                            return new SecurityInfo(securityInfo.getPermissions(), INSTALLED);
                        default:
                            return securityInfo;
                    }
                });
    }

}
