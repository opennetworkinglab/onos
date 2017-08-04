/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.common.app;

import com.google.common.collect.ImmutableSet;
import com.google.common.io.ByteStreams;
import com.google.common.io.Files;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.onlab.util.Tools;
import org.onosproject.app.ApplicationDescription;
import org.onosproject.app.ApplicationException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import static org.junit.Assert.*;
import static org.onosproject.app.DefaultApplicationDescriptionTest.*;

/**
 * Suite of tests for the application archive utility.
 */
public class ApplicationArchiveTest {

    static final File STORE = Files.createTempDir();
    static final String SYSTEM = "system";

    private ApplicationArchive aar = new ApplicationArchive();

    @Before
    public void setUp() {
        aar.setRootPath(STORE.getAbsolutePath());
    }

    @After
    public void tearDown() throws IOException {
        if (STORE.exists()) {
            Tools.removeDirectory(STORE);
            Tools.removeDirectory(SYSTEM);
        }
    }

    private void validate(ApplicationDescription app) {
        assertEquals("incorrect name", APP_NAME, app.name());
        assertEquals("incorrect version", VER, app.version());
        assertEquals("incorrect origin", ORIGIN, app.origin());
        assertEquals("incorrect role", ROLE, app.role());

        assertEquals("incorrect category", CATEGORY, app.category());
        assertEquals("incorrect url", URL, app.url());
        assertEquals("incorrect readme", README, app.readme());

        assertEquals("incorrect title", TITLE, app.title());
        assertEquals("incorrect description", DESC, app.description());
        assertEquals("incorrect features URI", FURL, app.featuresRepo().get());
        assertEquals("incorrect permissions", PERMS, app.permissions());
        assertEquals("incorrect features", FEATURES, app.features());
    }

    @Test
    public void saveZippedApp() throws IOException {
        InputStream stream = getClass().getResourceAsStream("app.zip");
        ApplicationDescription app = aar.saveApplication(stream);
        validate(app);
        stream.close();
    }

    @Test
    public void savePlainApp() throws IOException {
        InputStream stream = getClass().getResourceAsStream("app.xml");
        ApplicationDescription app = aar.saveApplication(stream);
        validate(app);
        stream.close();
    }

    @Test
    public void saveSelfContainedApp() throws IOException {
        InputStream stream = getClass().getResourceAsStream("app.scj");
        ApplicationDescription app = aar.saveApplication(stream);
        validate(app);
        stream.close();
    }

    @Test
    public void loadApp() throws IOException {
        saveZippedApp();
        ApplicationDescription app = aar.getApplicationDescription(APP_NAME);
        validate(app);
    }

    @Test
    public void getAppNames() throws IOException {
        saveZippedApp();
        Set<String> names = aar.getApplicationNames();
        assertEquals("incorrect names", ImmutableSet.of(APP_NAME), names);
    }

    @Test
    public void purgeApp() throws IOException {
        saveZippedApp();
        aar.purgeApplication(APP_NAME);
        assertEquals("incorrect names", ImmutableSet.<String>of(),
                     aar.getApplicationNames());
    }

    @Test
    public void getAppZipStream() throws IOException {
        saveZippedApp();
        InputStream stream = aar.getApplicationInputStream(APP_NAME);
        byte[] orig = ByteStreams.toByteArray(getClass().getResourceAsStream("app.zip"));
        byte[] loaded = ByteStreams.toByteArray(stream);
        assertArrayEquals("incorrect stream", orig, loaded);
        stream.close();
    }

    @Test
    public void getAppXmlStream() throws IOException {
        savePlainApp();
        InputStream stream = aar.getApplicationInputStream(APP_NAME);
        byte[] orig = ByteStreams.toByteArray(getClass().getResourceAsStream("app.xml"));
        byte[] loaded = ByteStreams.toByteArray(stream);
        assertArrayEquals("incorrect stream", orig, loaded);
        stream.close();
    }

    @Test
    public void active() throws IOException {
        savePlainApp();
        assertFalse("should not be active", aar.isActive(APP_NAME));
        aar.setActive(APP_NAME);
        assertTrue("should not be active", aar.isActive(APP_NAME));
        aar.clearActive(APP_NAME);
        assertFalse("should not be active", aar.isActive(APP_NAME));
    }

    @Test(expected = ApplicationException.class)
    public void getBadAppDesc() throws IOException {
        aar.getApplicationDescription("org.foo.BAD");
    }

    @Test(expected = ApplicationException.class)
    public void getBadAppStream() throws IOException {
        aar.getApplicationInputStream("org.foo.BAD");
    }

    @Test(expected = ApplicationException.class)
    @Ignore("No longer needed")
    public void setBadActive() throws IOException {
        aar.setActive("org.foo.BAD");
    }

    @Test // (expected = ApplicationException.class)
    public void purgeBadApp() throws IOException {
        aar.purgeApplication("org.foo.BAD");
    }

}
