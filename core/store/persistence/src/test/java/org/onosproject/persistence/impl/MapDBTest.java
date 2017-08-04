/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onosproject.persistence.impl;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.mapdb.DB;
import org.mapdb.DBMaker;

/**
 * Utils for Tests using MapDB.
 */
public abstract class MapDBTest {

    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

    protected DB fakeDB = null;

    /**
     * Set up the database.
     *
     * @throws Exception if instantiation fails
     */
    @Before
    public void setUpDB() throws Exception {
        // Creates a db
        fakeDB = DBMaker
                .newFileDB(tmpFolder.newFile())
                .asyncWriteEnable()
                .commitFileSyncDisable()
                .mmapFileEnableIfSupported()
                .closeOnJvmShutdown()
                .deleteFilesAfterClose()
                .make();
    }

    /**
     * Closes the database.
     *
     * @throws Exception if shutdown fails
     */
    @After
    public void tearDownDB() throws Exception {
        fakeDB.close();
    }
}
