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
package org.onosproject.common.app;

import com.google.common.collect.ImmutableSet;
import com.google.common.io.ByteStreams;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.onlab.util.Tools;
import org.onosproject.app.ApplicationDescription;
import org.onosproject.app.ApplicationException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;
import java.util.Set;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.onosproject.app.DefaultApplicationDescriptionTest.*;

public class ApplicationArchiveTest {

    static final String ROOT = "/tmp/app-junit/" + new Random().nextInt();

    private ApplicationArchive aar = new ApplicationArchive();

    @Before
    public void setUp() {
        aar.setRootPath(ROOT);
    }

    @After
    public void tearDown() throws IOException {
        if (new File(aar.getRootPath()).exists()) {
            Tools.removeDirectory(aar.getRootPath());
        }
    }

    private void validate(ApplicationDescription app) {
        assertEquals("incorrect name", APP_NAME, app.name());
        assertEquals("incorrect version", VER, app.version());
        assertEquals("incorrect origin", ORIGIN, app.origin());

        assertEquals("incorrect description", DESC, app.description());
        assertEquals("incorrect features URI", FURL, app.featuresRepo().get());
        assertEquals("incorrect permissions", PERMS, app.permissions());
        assertEquals("incorrect features", FEATURES, app.features());
    }

    @Test
    public void saveApp() throws IOException {
        InputStream stream = getClass().getResourceAsStream("app.zip");
        ApplicationDescription app = aar.saveApplication(stream);
        validate(app);
    }

    @Test
    public void loadApp() throws IOException {
        saveApp();
        ApplicationDescription app = aar.getApplicationDescription(APP_NAME);
        validate(app);
    }

    @Test
    public void getAppNames() throws IOException {
        saveApp();
        Set<String> names = aar.getApplicationNames();
        assertEquals("incorrect names", ImmutableSet.of(APP_NAME), names);
    }

    @Test
    public void purgeApp() throws IOException {
        saveApp();
        aar.purgeApplication(APP_NAME);
        assertEquals("incorrect names", ImmutableSet.<String>of(),
                     aar.getApplicationNames());
    }

    @Test
    public void getAppStream() throws IOException {
        saveApp();
        InputStream stream = aar.getApplicationInputStream(APP_NAME);
        byte[] orig = ByteStreams.toByteArray(getClass().getResourceAsStream("app.zip"));
        byte[] loaded = ByteStreams.toByteArray(stream);
        assertArrayEquals("incorrect stream", orig, loaded);
    }

    @Test(expected = ApplicationException.class)
    public void getBadAppDesc() throws IOException {
        aar.getApplicationDescription("org.foo.BAD");
    }

    @Test(expected = ApplicationException.class)
    public void getBadAppStream() throws IOException {
        aar.getApplicationInputStream("org.foo.BAD");
    }

}