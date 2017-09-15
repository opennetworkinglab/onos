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

package org.onosproject.security.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.onosproject.security.store.SecurityModeState.POLICY_VIOLATED;
import static org.onosproject.security.store.SecurityModeState.REVIEWED;
import static org.onosproject.security.store.SecurityModeState.SECURED;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.Before;
import org.junit.Test;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.ApplicationRole;
import org.onosproject.core.DefaultApplication;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.core.Version;
import org.onosproject.security.Permission;
import org.onosproject.security.store.SecurityModeStoreAdapter;

/**
 * Unit Test on SecurityModeManager.
 */
public class SecurityModeManagerTest {

    private SecurityModeStoreAdapter store;

    private DefaultApplicationId appId;
    private DefaultApplication app;

    private Permission testPermission;
    private Set<Permission> testPermissions;
    private List<String> testFeatures;
    private List<String> testRequiredApps;
    private Set<String> testLocations;

    @Before
    public void setUp() throws Exception {
        store = new SecurityModeStoreAdapter();

        appId = new DefaultApplicationId(1, "test");
        testPermissions = new HashSet<Permission>();
        testPermission = new Permission("testClass", "testNameAdmin");
        testPermissions.add(testPermission);
        testFeatures = new ArrayList<String>();
        testFeatures.add("testFeature");
        testRequiredApps = new ArrayList<String>();
        testRequiredApps.add("testRequiredApp");
        app = DefaultApplication.builder()
                .withAppId(appId)
                .withVersion(Version.version(1, 1, "patch", "build"))
                .withTitle("testTitle")
                .withDescription("testDes")
                .withOrigin("testOri")
                .withCategory("testCT")
                .withUrl("testurl")
                .withReadme("test")
                .withIcon(null)
                .withRole(ApplicationRole.ADMIN)
                .withPermissions(testPermissions)
                .withFeaturesRepo(Optional.ofNullable(null))
                .withFeatures(testFeatures)
                .withRequiredApps(testRequiredApps)
                .build();

        store.registerApplication(appId);
    }

//    @Test
//    public void testActivate() {
//        fail("Not yet implemented");
//    }
//
//    @Test
//    public void testDeactivate() {
//        fail("Not yet implemented");
//    }

    @Test
    public void testIsSecured() {
        assertTrue(store.isSecured(appId));
    }

    @Test
    public void testReview() {
        assertEquals(SECURED, store.getState(appId));
        store.reviewPolicy(appId);
        assertEquals(REVIEWED, store.getState(appId));
    }

    @Test
    public void testAcceptPolicy() {
        assertEquals(SECURED, store.getState(appId));
        store.acceptPolicy(appId, getMaximumPermissions(appId));
        assertEquals(POLICY_VIOLATED, store.getState(appId));
    }

    @Test
    public void testRegister() {
        assertTrue(store.registerApplication(appId));
    }

    @Test
    public void testGetPrintableSpecifiedPermissions() {
        Map<Integer, List<Permission>> result = getPrintablePermissionMap(getMaximumPermissions(appId));
        assertNotNull(result.get(1).get(0));
        assertTrue(result.get(1).size() > 0);
        assertEquals("testNameAdmin", result.get(1).get(0).getName());
    }

    @Test
    public void testGetPrintableGrantedPermissions() {
        Map<Integer, List<Permission>> result = getPrintablePermissionMap(store.getGrantedPermissions(appId));
        assertNotNull(result.get(2).get(0));
        assertTrue(result.get(2).size() > 0);
        assertEquals("testName", result.get(2).get(0).getName());
    }

    @Test
    public void testGetPrintableRequestedPermissions() {
        DefaultApplicationId appIdViolation;
        appIdViolation = new DefaultApplicationId(2, "violation");
        Map<Integer, List<Permission>> result =
                getPrintablePermissionMap(store.getRequestedPermissions(appIdViolation));
        assertNotNull(result.get(2).get(0));
        assertTrue(result.get(2).size() > 0);
        assertEquals("testNameViolation", result.get(2).get(0).getName());
    }


    private Map<Integer, List<Permission>> getPrintablePermissionMap(Set<Permission> perms) {
        ConcurrentHashMap<Integer, List<Permission>> sortedMap = new ConcurrentHashMap<>();
        sortedMap.put(0, new ArrayList());
        sortedMap.put(1, new ArrayList());
        sortedMap.put(2, new ArrayList());
        sortedMap.put(3, new ArrayList());
        sortedMap.put(4, new ArrayList());
        for (Permission perm : perms) {
            if (perm.getName().contains("Admin")) {
                sortedMap.get(1).add(perm);
            } else {
                sortedMap.get(2).add(perm);
            }
        }
        return sortedMap;
    }

    private Set<Permission> getMaximumPermissions(ApplicationId appId) {
        if (app == null) {
            return null;
        }
        Set<Permission> appPerms;
        switch (app.role()) {
            case ADMIN:
                appPerms = app.permissions();
                break;
            case USER:
                appPerms = app.permissions();
                break;
            case UNSPECIFIED:
            default:
                appPerms = new HashSet<Permission>();
                break;
        }

        return appPerms;
    }
}
