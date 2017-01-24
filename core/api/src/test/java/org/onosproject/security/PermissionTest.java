/*
 * Copyright 2017-present Open Networking Laboratory
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import org.junit.Test;

/**
 * Test of Permission.
 */
public class PermissionTest {

    @Test
    public void testHashCode() {
        Permission permission = new Permission("classname", "name", "actions");
        assertEquals(0, permission.hashCode());
    }

    @Test
    public void testPermissionStringStringString() {
        Permission permission = new Permission("classname", "name", "actions");
        assertEquals("classname", permission.getClassName());
        assertEquals("name", permission.getName());
        assertEquals("actions", permission.getActions());
    }

    @Test
    public void testPermissionStringString() {
        Permission permission = new Permission("classname", "name");
        assertEquals("classname", permission.getClassName());
        assertEquals("name", permission.getName());
        assertEquals("", permission.getActions());
    }

    @Test
    public void testGetClassName() {
        Permission permission = new Permission("classname", "name");
        assertEquals("classname", permission.getClassName());
    }

    @Test
    public void testGetName() {
        Permission permission = new Permission("classname", "name");
        assertEquals("name", permission.getName());
    }

    @Test
    public void testGetActions() {
        Permission permission = new Permission("classname", "name", "actions");
        assertEquals("actions", permission.getActions());
    }

    @Test
    public void testEqualsObject() {
        Permission permissionA = new Permission("classname", "name", "actions");
        Permission permissionB = new Permission("classname", "name", "actions");
        assertSame(permissionA, permissionA);
        assertEquals(permissionA.getClassName(), permissionB.getClassName());
        assertEquals(permissionA.getName(), permissionB.getName());
        assertEquals(permissionA.getActions(), permissionB.getActions());
    }

    @Test
    public void testToString() {
        Permission permission = new Permission("classname", "name", "actions");
        assertEquals("(classname, name, actions)", permission.toString());
    }

}
