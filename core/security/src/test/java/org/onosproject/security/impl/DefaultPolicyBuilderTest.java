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

import java.security.Permission;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.onosproject.app.ApplicationAdminService;
import org.onosproject.security.AppPermission;
import org.onosproject.security.SecurityAdminService;
import org.osgi.framework.AdaptPermission;
import org.osgi.framework.AdminPermission;
import org.osgi.framework.PackagePermission;
import org.osgi.framework.ServicePermission;
import org.osgi.service.cm.ConfigurationPermission;

import com.google.common.collect.Lists;

/**
 * Unit Test on DefaultPolicyBuilder.
 */
public class DefaultPolicyBuilderTest {

    private List<Permission> defaultPermissions;
    private List<Permission> adminServicePermissions;

    private org.onosproject.security.Permission testPermission;
    private Set<org.onosproject.security.Permission> testPermissions;

    private Permission testJavaPerm;
    private Set<Permission> testJavaPerms;

    @Before
    public void setUp() throws Exception {
        List<Permission> permSet = Lists.newArrayList();
        permSet.add(new PackagePermission("*", PackagePermission.EXPORTONLY));
        permSet.add(new PackagePermission("*", PackagePermission.IMPORT));
        permSet.add(new AdaptPermission("*", AdaptPermission.ADAPT));
        permSet.add(new ConfigurationPermission("*", ConfigurationPermission.CONFIGURE));
        permSet.add(new AdminPermission("*", AdminPermission.METADATA));
        defaultPermissions = permSet;

        List<Permission> adminPermSet = Lists.newArrayList();
        adminPermSet.add(new ServicePermission(ApplicationAdminService.class.getName(), ServicePermission.GET));
        adminServicePermissions = adminPermSet;

        testPermission = new org.onosproject.security.Permission("testClass", "APP_READ", "testActions");
        testPermissions = new HashSet<org.onosproject.security.Permission>();
        testPermissions.add(testPermission);

        testJavaPerm = new AppPermission("testName");
        testJavaPerms = new HashSet<Permission>();
        testJavaPerms.add(testJavaPerm);
    }

    @Test
    public void testGetUserApplicationPermissions() {
        List<Permission> perms = Lists.newArrayList();
        perms.addAll(defaultPermissions);
        assertEquals(5, defaultPermissions.size());
        perms.addAll(testJavaPerms);
        assertEquals(1, testJavaPerms.size());
        assertEquals(6, perms.size());
        assertTrue(perms.contains(testJavaPerm));
    }

    @Test
    public void testGetAdminApplicationPermissions() {
        List<Permission> perms = Lists.newArrayList();
        perms.addAll(defaultPermissions);
        perms.addAll(adminServicePermissions);
        perms.addAll(testJavaPerms);
        assertEquals(7, perms.size());
        assertTrue(perms.contains(testJavaPerm));
    }

    @Test
    public void testConvertToJavaPermissions() {
        List<Permission> result = Lists.newArrayList();
        for (org.onosproject.security.Permission perm : testPermissions) {
            Permission javaPerm = new AppPermission(perm.getName());
            if (javaPerm != null) {
                if (javaPerm instanceof AppPermission) {
                    if (((AppPermission) javaPerm).getType() != null) {
                        AppPermission ap = (AppPermission) javaPerm;
                        result.add(ap);
                    }
                } else if (javaPerm instanceof ServicePermission) {
                    if (!javaPerm.getName().contains(SecurityAdminService.class.getName())) {
                        result.add(javaPerm);
                    }
                } else {
                    result.add(javaPerm);
                }

            }
        }
        assertTrue(!result.isEmpty());
        assertEquals("APP_READ", result.get(0).getName());
    }

    @Test
    public void testConvertToOnosPermissions() {
        Permission testJavaPerm = new AppPermission("testName");

        List<org.onosproject.security.Permission> result = Lists.newArrayList();
        org.onosproject.security.Permission onosPerm =
                new org.onosproject.security.Permission(AppPermission.class.getName(), testJavaPerm.getName(), "");
        result.add(onosPerm);

        assertTrue(!result.isEmpty());
        assertEquals("TESTNAME", result.get(0).getName());
    }

    @Test
    public void testGetDefaultPerms() {
        List<Permission> permSet = Lists.newArrayList();
        assertTrue(permSet.isEmpty());
        permSet.add(new PackagePermission("*", PackagePermission.EXPORTONLY));
        permSet.add(new PackagePermission("*", PackagePermission.IMPORT));
        permSet.add(new AdaptPermission("*", AdaptPermission.ADAPT));
        permSet.add(new ConfigurationPermission("*", ConfigurationPermission.CONFIGURE));
        permSet.add(new AdminPermission("*", AdminPermission.METADATA));
        assertEquals(5, permSet.size());
    }

    @Test
    public void testGetNBServiceList() {
        Set<String> permString = new HashSet<>();
        permString.add(new ServicePermission(ApplicationAdminService.class.getName(), ServicePermission.GET).getName());
        assertEquals(1, permString.size());
        assertEquals("org.onosproject.app.ApplicationAdminService", permString.toArray()[0]);
    }

    @Test
    public void testGetOnosPermission() {
        org.onosproject.security.Permission result = null;
        if (testJavaPerm instanceof AppPermission) {
            result = new org.onosproject.security.Permission(AppPermission.class.getName(), testJavaPerm.getName(), "");
        }
        assertNotNull(result);
        assertEquals("TESTNAME", result.getName());
    }

}
