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

package org.onosproject.store.consistent.impl;

import static com.google.common.base.Preconditions.checkNotNull;
import java.io.File;
import java.io.IOException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Allows for reading and writing partitioned database definition as a JSON file.
 */
public class DatabaseDefinitionStore {

    private final File definitionfile;

    /**
     * Creates a reader/writer of the database definition file.
     *
     * @param filePath location of the definition file
     */
    public DatabaseDefinitionStore(String filePath) {
        definitionfile = new File(checkNotNull(filePath));
    }

    /**
     * Creates a reader/writer of the database definition file.
     *
     * @param filePath location of the definition file
     */
    public DatabaseDefinitionStore(File filePath) {
        definitionfile = checkNotNull(filePath);
    }

    /**
     * Returns the database definition.
     *
     * @return database definition
     * @throws IOException when I/O exception of some sort has occurred.
     */
    public DatabaseDefinition read() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        DatabaseDefinition definition = mapper.readValue(definitionfile, DatabaseDefinition.class);
        return definition;
    }

    /**
     * Writes the specified database definition to file.
     *
     * @param definition database definition
     * @throws IOException when I/O exception of some sort has occurred.
     */
    public void write(DatabaseDefinition definition) throws IOException {
        checkNotNull(definition);
        // write back to file
        final ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(definitionfile, definition);
    }
}
