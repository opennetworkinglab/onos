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

package org.onosproject.persistence.impl;

import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.onosproject.persistence.PersistenceService;
import org.onosproject.persistence.PersistentMapBuilder;
import org.onosproject.persistence.PersistentSetBuilder;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import static org.onosproject.security.AppGuard.checkPermission;
import static org.onosproject.security.AppPermission.Type.PERSISTENCE_WRITE;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * Service that maintains local disk backed maps and sets.
 * This implementation automatically deletes empty structures on shutdown.
 */
@Component(immediate = true, service = PersistenceService.class)
public class PersistenceManager implements PersistenceService {

    private static final String DATABASE_ROOT =
            System.getProperty("karaf.data") + "/db/local/";

    private static final String DATABASE_PATH = "cache";

    static final String MAP_PREFIX = "map:";
    static final String SET_PREFIX = "set:";

    private final Logger log = getLogger(getClass());

    private DB localDB = null;

    private static final int FLUSH_FREQUENCY_MILLIS = 3000;

    private Timer timer;

    private final CommitTask commitTask = new CommitTask();

    @Activate
    public void activate() {
        timer = new Timer();

        File dbFolderPath = new File(DATABASE_ROOT);
        Path dbPath = dbFolderPath.toPath().resolve(DATABASE_PATH);
        log.debug("dbPath: {}", dbPath);

        //Make sure the directory exists, if it does not, make it.
        if (!dbFolderPath.isDirectory()) {
            log.info("The specified folder location for the database did not exist and will be created.");
            try {
                Files.createDirectories(dbFolderPath.toPath());
            } catch (IOException e) {
                log.error("Could not create the required folder for the database.");
                throw new PersistenceException("Database folder could not be created.");
            }
        }
        //Notify if the database file does not exist.
        boolean dbFound = Files.exists(dbPath);
        if (!dbFound) {
            log.info("The database file could not be located, a new database will be constructed.");

        } else {
            log.info("A previous database file has been found.");
        }
        localDB = DBMaker.newFileDB(dbPath.toFile())
                .asyncWriteEnable()
                .closeOnJvmShutdown()
                .make();
        timer.schedule(commitTask, FLUSH_FREQUENCY_MILLIS, FLUSH_FREQUENCY_MILLIS);
        log.info("Started");
    }

    @Deactivate
    public void deactivate() {
        timer.cancel();
        for (Map.Entry<String, Object> entry : localDB.getAll().entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof Map) {
                // This is a map implementation to be handled as such
                Map asMap = (Map) value;
                if (asMap.isEmpty()) {
                    //the map is empty and may be deleted
                    localDB.delete(key);
                }
            } else if (value instanceof Set) {
                // This is a set implementation and can be handled as such
                Set asSet = (Set) value;
                if (asSet.isEmpty()) {
                    //the set is empty and may be deleted
                    localDB.delete(key);
                }
            }
        }
        localDB.commit();
        localDB.close();
        log.info("Stopped");
    }

    @Override
    public <K, V> PersistentMapBuilder<K, V> persistentMapBuilder() {
        checkPermission(PERSISTENCE_WRITE);
        return new DefaultPersistentMapBuilder<>(localDB);
    }

    @Override
    public <E> PersistentSetBuilder<E> persistentSetBuilder() {
        checkPermission(PERSISTENCE_WRITE);
        return new DefaultPersistentSetBuilder<>(localDB);
    }

    private class CommitTask extends TimerTask {

        @Override
        public void run() {
            localDB.commit();
        }
    }
}