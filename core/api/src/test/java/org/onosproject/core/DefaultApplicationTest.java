/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.core;

import com.google.common.testing.EqualsTester;
import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.onosproject.app.DefaultApplicationDescriptionTest.*;

/**
 * Basic tests of the default app descriptor.
 */
public class DefaultApplicationTest {

    public static final ApplicationId APP_ID = new DefaultApplicationId(2, APP_NAME);

    @Test
    public void basics() {
        Application app = new DefaultApplication(APP_ID, VER, DESC, ORIGIN, ROLE,
                                                 PERMS, Optional.of(FURL), FEATURES, APPS);
        assertEquals("incorrect id", APP_ID, app.id());
        assertEquals("incorrect version", VER, app.version());
        assertEquals("incorrect description", DESC, app.description());
        assertEquals("incorrect origin", ORIGIN, app.origin());
        assertEquals("incorrect role", ROLE, app.role());
        assertEquals("incorrect permissions", PERMS, app.permissions());
        assertEquals("incorrect features repo", FURL, app.featuresRepo().get());
        assertEquals("incorrect features", FEATURES, app.features());
        assertEquals("incorrect apps", APPS, app.requiredApps());
        assertTrue("incorrect toString", app.toString().contains(APP_NAME));
    }

    @Test
    public void testEquality() {
        Application a1 = new DefaultApplication(APP_ID, VER, DESC, ORIGIN, ROLE,
                                                PERMS, Optional.of(FURL), FEATURES, APPS);
        Application a2 = new DefaultApplication(APP_ID, VER, DESC, ORIGIN, ROLE,
                                                PERMS, Optional.of(FURL), FEATURES, APPS);
        Application a3 = new DefaultApplication(APP_ID, VER, DESC, ORIGIN, ROLE,
                                                PERMS, Optional.empty(), FEATURES, APPS);
        Application a4 = new DefaultApplication(APP_ID, VER, DESC, ORIGIN + "asd", ROLE,
                                                PERMS, Optional.of(FURL), FEATURES, APPS);
        new EqualsTester().addEqualityGroup(a1, a2)
                .addEqualityGroup(a3).addEqualityGroup(a4).testEquals();
    }

}