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

package org.onosproject.security;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;
import org.onosproject.core.DefaultApplicationId;

/**
 * Test of SecurityUtil.
 */
public class SecurityUtilTest {

    private static SecurityAdminServiceAdapter service = new SecurityAdminServiceAdapter();
    private DefaultApplicationId appId;

    @Before
    public void setUp() throws Exception {
        appId = new DefaultApplicationId(1, "test");
    }

    @Test
    public void testIsSecurityModeEnabled() {
        assertNull(System.getSecurityManager());
        assertNotNull(service);
    }

    @Test
    public void testGetSecurityService() {
        assertNull(System.getSecurityManager());
        assertNotNull(service);
    }

    @Test
    public void testIsAppSecured() {
        assertFalse(service.isSecured(appId));
    }

    @Test
    public void testRegister() {
        service.register(appId);
    }

}
