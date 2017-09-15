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

import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.onlab.util.Tools.groupedThreads;
import static org.onosproject.security.store.SecurityModeState.*;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

import org.junit.Before;
import org.junit.Test;
import org.onosproject.core.ApplicationId;
import org.onosproject.core.ApplicationRole;
import org.onosproject.core.DefaultApplication;
import org.onosproject.core.DefaultApplicationId;
import org.onosproject.core.Version;
import org.onosproject.security.Permission;
import org.slf4j.Logger;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

/**
 * Unit Test on DistributedSecurityModeStore.
 */
public class DistributedSecurityModeStoreTest {

    private final Logger log = getLogger(getClass());

    private DefaultApplicationId appId;
    private DefaultApplication app;

    private Permission testPermission;
    private Set<Permission> testPermissions;
    private List<String> testFeatures;
    private List<String> testRequiredApps;
    private Set<String> testLocations;
    private ConcurrentHashMap<String, Set<ApplicationId>> localBundleAppDirectory;
    private ConcurrentHashMap<ApplicationId, Set<String>> localAppBundleDirectory;
    private ConcurrentHashMap<ApplicationId, Set<Permission>> violations;
    private SecurityInfo testSecInfo;
    private ConcurrentHashMap<ApplicationId, SecurityInfo> states;

    private ExecutorService eventHandler;

    @Before
    public void setUp() throws Exception {
        appId = new DefaultApplicationId(1, "test");
        testPermissions = new HashSet<Permission>();
        testPermission = new Permission("testClass", "testName");
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

        testLocations = new HashSet<String>();
        testLocations.add("locationA");
        testLocations.add("locationB");

        Set<ApplicationId> appIdSet = new HashSet<ApplicationId>();
        appIdSet.add(appId);
        localBundleAppDirectory = new ConcurrentHashMap<>();
        localBundleAppDirectory.put("testLocation", appIdSet);
        localAppBundleDirectory = new ConcurrentHashMap<>();
        localAppBundleDirectory.put(appId, testLocations);

        violations = new ConcurrentHashMap<ApplicationId, Set<Permission>>();
        violations.put(appId, testPermissions);

        testSecInfo = new SecurityInfo(testPermissions, SECURED);
        states = new ConcurrentHashMap<ApplicationId, SecurityInfo>();
        states.put(appId, testSecInfo);
    }

    @Test
    public void testActivate() {
        eventHandler = newSingleThreadExecutor(groupedThreads("onos/security/store", "event-handler", log));
        assertNotNull(eventHandler);
    }

    @Test
    public void testDeactivate() {
        eventHandler = newSingleThreadExecutor(groupedThreads("onos/security/store", "event-handler", log));
        eventHandler.shutdown();
        assertTrue(eventHandler.isShutdown());
    }

    @Test
    public void testGetBundleLocations() {
        Set<String> locations = localAppBundleDirectory.get(appId);
        assertTrue(locations.contains("locationA"));
    }

    @Test
    public void testGetApplicationIds() {
        Set<ApplicationId> appIds = localBundleAppDirectory.get("testLocation");
        assertTrue(appIds.contains(appId));
    }

    @Test
    public void testGetRequestedPermissions() {
        Set<Permission> permissions = violations.get(appId);
        assertTrue(permissions.contains(testPermission));
    }

    @Test
    public void testGetGrantedPermissions() {
        Set<Permission> permissions = states.get(appId).getPermissions();
        assertTrue(permissions.contains(testPermission));
    }

    @Test
    public void testRequestPermission() {
        states.compute(appId, (id, securityInfo) -> new SecurityInfo(securityInfo.getPermissions(), POLICY_VIOLATED));
        assertEquals(POLICY_VIOLATED, states.get(appId).getState());
        Permission testPermissionB = new Permission("testClassB", "testNameB");
        violations.compute(appId,
                (k, v) -> v == null ? Sets.newHashSet(testPermissionB) : addAndGet(v, testPermissionB));
        assertTrue(violations.get(appId).contains(testPermissionB));
    }

    private Set<Permission> addAndGet(Set<Permission> oldSet, Permission newPerm) {
        oldSet.add(newPerm);
        return oldSet;
    }

    @Test
    public void testIsSecured() {
        SecurityInfo info = states.get(appId);
        assertEquals(SECURED, info.getState());
    }

    @Test
    public void testReviewPolicy() {
        assertEquals(SECURED, states.get(appId).getState());
        states.computeIfPresent(appId, (applicationId, securityInfo) -> {
            if (securityInfo.getState().equals(SECURED)) {
                return new SecurityInfo(ImmutableSet.of(), REVIEWED);
            }
            return securityInfo;
        });
        assertEquals(REVIEWED, states.get(appId).getState());
    }

    @Test
    public void testAcceptPolicy() {
        assertEquals(SECURED, states.get(appId).getState());
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
        assertEquals(POLICY_VIOLATED, states.get(appId).getState());
    }

    @Test
    public void testRegisterApplication() {
        states.remove(appId);
        assertNull(states.get(appId));

        for (String location : localAppBundleDirectory.get(appId)) {
            if (!localBundleAppDirectory.containsKey(location)) {
                localBundleAppDirectory.put(location, new HashSet<>());
            }
            if (!localBundleAppDirectory.get(location).contains(appId)) {
                localBundleAppDirectory.get(location).add(appId);
            }
        }
        states.put(appId, new SecurityInfo(Sets.newHashSet(), INSTALLED));
        assertNotNull(states.get(appId));
        assertEquals(INSTALLED, states.get(appId).getState());
    }

    @Test
    public void testUnregisterApplication() {
        if (localAppBundleDirectory.containsKey(appId)) {
            for (String location : localAppBundleDirectory.get(appId)) {
                if (localBundleAppDirectory.get(location) != null) {
                    if (localBundleAppDirectory.get(location).size() == 1) {
                        localBundleAppDirectory.remove(location);
                    } else {
                        localBundleAppDirectory.get(location).remove(appId);
                    }
                }
            }
            localAppBundleDirectory.remove(appId);
        }
        assertNull(localAppBundleDirectory.get(appId));
    }

    @Test
    public void testGetState() {
        assertEquals(SECURED, states.get(appId).getState());
    }

}
