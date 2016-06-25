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
package org.onosproject.core;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.testing.EqualsTester;
import org.junit.Test;
import org.onosproject.security.AppPermission;
import org.onosproject.security.Permission;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.onosproject.app.DefaultApplicationDescriptionTest.*;

/**
 * Basic tests of the default app descriptor.
 */
public class DefaultApplicationTest {

    public static final ApplicationId APP_ID = new DefaultApplicationId(2, APP_NAME);

    @Test
    public void basics() {
        Application app = new DefaultApplication(APP_ID, VER, TITLE, DESC, ORIGIN,
                                                 CATEGORY, URL, README, ICON, ROLE,
                                                 PERMS, Optional.of(FURL), FEATURES, APPS);
        assertEquals("incorrect id", APP_ID, app.id());
        assertEquals("incorrect version", VER, app.version());
        assertEquals("incorrect description", DESC, app.description());
        assertEquals("incorrect origin", ORIGIN, app.origin());
        assertEquals("incorrect category", CATEGORY, app.category());
        assertEquals("incorrect URL", URL, app.url());
        assertEquals("incorrect readme", README, app.readme());
        assertArrayEquals("incorrect icon", ICON, app.icon());
        assertEquals("incorrect role", ROLE, app.role());
        assertEquals("incorrect permissions", PERMS, app.permissions());
        assertEquals("incorrect features repo", FURL, app.featuresRepo().get());
        assertEquals("incorrect features", FEATURES, app.features());
        assertEquals("incorrect apps", APPS, app.requiredApps());
        assertTrue("incorrect toString", app.toString().contains(APP_NAME));
    }

    @Test
    public void testEquality() {
        Application a1 = new DefaultApplication(APP_ID, VER, TITLE, DESC, ORIGIN,
                                                CATEGORY, URL, README, ICON, ROLE,
                                                PERMS, Optional.of(FURL), FEATURES, APPS);
        Application a2 = new DefaultApplication(APP_ID, VER, TITLE, DESC, ORIGIN,
                                                CATEGORY, URL, README, ICON, ROLE,
                                                PERMS, Optional.of(FURL), FEATURES, APPS);
        Application a3 = new DefaultApplication(APP_ID, VER, TITLE, DESC, ORIGIN,
                                                CATEGORY, URL, README, ICON, ROLE,
                                                PERMS, Optional.empty(), FEATURES, APPS);
        Application a4 = new DefaultApplication(APP_ID, VER, TITLE, DESC, ORIGIN + "asd",
                                                CATEGORY, URL, README, ICON, ROLE,
                                                PERMS, Optional.of(FURL), FEATURES, APPS);
        new EqualsTester().addEqualityGroup(a1, a2)
                .addEqualityGroup(a3).addEqualityGroup(a4).testEquals();
    }


    private static final byte[] ICON_ORIG = new byte[] {1, 2, 3, 4};

    @Test
    public void immutableIcon() {
        byte[] iconSourceData = ICON_ORIG.clone();

        Application app = new DefaultApplication(APP_ID, VER, TITLE, DESC, ORIGIN,
                CATEGORY, URL, README, iconSourceData, ROLE,
                PERMS, Optional.of(FURL), FEATURES, APPS);

        // can we modify the icon after getting a reference to the app?
        byte[] icon = app.icon();
        assertArrayEquals("did not start with orig icon", ICON_ORIG, icon);

        // now the hack
        for (int i = 0, n = ICON_ORIG.length; i < n; i++) {
            icon[i] = 0;
        }
        // if the reference to the internal array is given out, the hack
        // will succeed and this next assertion fails
        assertArrayEquals("no longer orig icon", ICON_ORIG, app.icon());

        // what if we modify the source data?
        for (int i = 0, n = ICON_ORIG.length; i < n; i++) {
            iconSourceData[i] = 0;
        }
        // if the application just saved a reference to the given array
        // this next assertion fails
        assertArrayEquals("modifying source alters appicon", ICON_ORIG, app.icon());
    }

    private static final Permission PERM_W =
            new Permission(AppPermission.class.getName(), "FLOWRULE_WRITE");
    private static final Permission PERM_R =
            new Permission(AppPermission.class.getName(), "FLOWRULE_READ");

    private static final Permission JUNK_PERM = new Permission("foo", "bar");

    private static final Set<Permission> PERMS_ORIG = ImmutableSet.of(PERM_W, PERM_R);
    private static final Set<Permission> PERMS_UNSAFE = new HashSet<>(PERMS_ORIG);


    @Test
    public void immutablePermissions() {
//        Set<Permission> p = PERMS_ORIG;
        Set<Permission> p = PERMS_UNSAFE;

        Application app = new DefaultApplication(APP_ID, VER, TITLE, DESC, ORIGIN,
                CATEGORY, URL, README, ICON, ROLE,
                p, Optional.of(FURL), FEATURES, APPS);

        Set<Permission> perms = app.permissions();
        try {
            perms.add(JUNK_PERM);
        } catch (UnsupportedOperationException e) {
            // set is immutable
        }
        assertTrue("no write perm", app.permissions().contains(PERM_W));
        assertTrue("no read perm", app.permissions().contains(PERM_R));
        assertEquals("extra perms", 2, app.permissions().size());

        // DONE: review - is it sufficient to expect caller to pass in ImmutableSet ?
        // Issue Resolved with Immutable collections used during construction.

        // If we just pass in a HashSet, the contents would be modifiable by
        // an external party. (Making the field final just means that the
        // reference to the set can never change; the contents may still...)

        // Similar reasoning can be applied to these two fields also:
        //     List<String> features
        //     List<String> requiredApps
    }

    private static final String FOO = "foo";
    private static final String BAR = "bar";
    private static final String FIFI = "fifi";
    private static final String EVIL = "Bwahahahaha!";

    private static final List<String> FEATURES_ORIG = ImmutableList.of(FOO, BAR);
    private static final List<String> FEATURES_UNSAFE = new ArrayList<>(FEATURES_ORIG);

    private static final List<String> REQ_APPS_ORIG = ImmutableList.of(FIFI);
    private static final List<String> REQ_APPS_UNSAFE = new ArrayList<>(REQ_APPS_ORIG);

    @Test
    public void immutableFeatures() {
//        List<String> f = FEATURES_ORIG;
        List<String> f = FEATURES_UNSAFE;

        Application app = new DefaultApplication(APP_ID, VER, TITLE, DESC, ORIGIN,
                CATEGORY, URL, README, ICON, ROLE,
                PERMS, Optional.of(FURL), f, APPS);

        List<String> features = app.features();
        try {
            features.add(EVIL);
        } catch (UnsupportedOperationException e) {
            // list is immutable
        }
        assertTrue("no foo feature", features.contains(FOO));
        assertTrue("no bar feature", features.contains(BAR));
        assertEquals("extra features!", 2, features.size());
    }

    @Test
    public void immutableRequiredApps() {
//        List<String> ra = REQ_APPS_ORIG;
        List<String> ra = REQ_APPS_UNSAFE;

        Application app = new DefaultApplication(APP_ID, VER, TITLE, DESC, ORIGIN,
                CATEGORY, URL, README, ICON, ROLE,
                PERMS, Optional.of(FURL), FEATURES, ra);

        List<String> reqApps = app.requiredApps();
        try {
            reqApps.add(EVIL);
        } catch (UnsupportedOperationException e) {
            // list is immutable
        }
        assertTrue("no fifi required app", reqApps.contains(FIFI));
        assertEquals("extra required apps!", 1, reqApps.size());
    }

    @Test
    public void nullIcon() {
        Application app = new DefaultApplication(APP_ID, VER, TITLE, DESC, ORIGIN,
                CATEGORY, URL, README, null, ROLE,
                PERMS, Optional.of(FURL), FEATURES, APPS);
        byte[] icon = app.icon();
        assertNotNull("null icon", icon);
        assertEquals("unexpected size", 0, icon.length);
    }
}