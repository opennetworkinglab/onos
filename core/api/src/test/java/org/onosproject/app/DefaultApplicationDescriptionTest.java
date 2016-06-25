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
package org.onosproject.app;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.junit.Test;
import org.onosproject.core.ApplicationRole;
import org.onosproject.core.Version;
import org.onosproject.security.AppPermission;
import org.onosproject.security.Permission;

import java.net.URI;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


/**
 * Basic tests of the default app description.
 */
public class DefaultApplicationDescriptionTest {

    public static final String APP_NAME = "org.foo.app";
    public static final Version VER = Version.version(1, 2, "a", null);
    public static final String TITLE = "Awesome App";
    public static final String DESC = "Awesome application from Circus, Inc.";
    public static final String ORIGIN = "Circus";
    public static final String CATEGORY = "other";
    public static final String URL = "http://www.onosproject.org";
    public static final String README = "Awesome application from Circus, Inc.";
    public static final byte[] ICON = new byte[] {};
    public static final ApplicationRole ROLE = ApplicationRole.ADMIN;
    public static final Set<Permission> PERMS = ImmutableSet.of(
                            new Permission(AppPermission.class.getName(), "FLOWRULE_WRITE"),
                            new Permission(AppPermission.class.getName(), "FLOWRULE_READ"));
    public static final URI FURL = URI.create("mvn:org.foo-features/1.2a/xml/features");
    public static final List<String> FEATURES = ImmutableList.of("foo", "bar");
    public static final List<String> APPS = ImmutableList.of("fifi");

    @Test
    public void basics() {
        ApplicationDescription app =
                new DefaultApplicationDescription(APP_NAME, VER, TITLE, DESC, ORIGIN,
                                                  CATEGORY, URL, README, ICON,
                                                  ROLE, PERMS, FURL, FEATURES, APPS);
        assertEquals("incorrect id", APP_NAME, app.name());
        assertEquals("incorrect version", VER, app.version());
        assertEquals("incorrect title", TITLE, app.title());
        assertEquals("incorrect description", DESC, app.description());
        assertEquals("incorrect origin", ORIGIN, app.origin());
        assertEquals("incorrect category", CATEGORY, app.category());
        assertEquals("incorrect URL", URL, app.url());
        assertEquals("incorrect readme", README, app.readme());
        assertEquals("incorrect role", ROLE, app.role());
        assertEquals("incorrect permissions", PERMS, app.permissions());
        assertEquals("incorrect features repo", FURL, app.featuresRepo().get());
        assertEquals("incorrect features", FEATURES, app.features());
        assertEquals("incorrect apps", APPS, app.requiredApps());
        assertTrue("incorrect toString", app.toString().contains(APP_NAME));
    }
}