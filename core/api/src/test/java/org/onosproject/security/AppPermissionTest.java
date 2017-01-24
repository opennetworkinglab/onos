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

import org.junit.Test;
import org.onosproject.security.AppPermission.Type;

/**
 * Test of AppPermission.
 */
public class AppPermissionTest {

    @Test
    public void testAppPermissionString() {
        String name = "app_read".toUpperCase();
        assertEquals(Type.APP_READ, Type.valueOf(name));
    }

    @Test
    public void testAppPermissionStringString() {
        String name = "app_read".toUpperCase();
        assertEquals(Type.APP_READ, Type.valueOf(name));
    }

    @Test
    public void testAppPermissionType() {
        Type type = Type.APP_WRITE;
        assertEquals(Type.APP_WRITE, type);
    }

    @Test
    public void testGetType() {
        AppPermission appPermission = new AppPermission(Type.APP_WRITE);
        assertEquals(Type.APP_WRITE, appPermission.getType());
    }

}
