/*
 * Copyright 2014-2015 Open Networking Laboratory
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
package org.onosproject.store.cluster.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Files;

import java.io.File;
import java.io.IOException;

/**
 * Allows for reading and writing cluster definition as a JSON file.
 */
public class ClusterDefinitionStore {

    private final File file;

    /**
     * Creates a reader/writer of the cluster definition file.
     * @param filePath location of the definition file
     */
    public ClusterDefinitionStore(String filePath) {
        file = new File(filePath);
    }

    /**
     * Returns the cluster definition.
     * @return cluster definition
     * @throws IOException when I/O exception of some sort has occurred
     */
    public ClusterDefinition read() throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(file, ClusterDefinition.class);
    }

    /**
     * Writes the specified cluster definition to file.
     * @param definition cluster definition
     * @throws IOException when I/O exception of some sort has occurred
     */
    public void write(ClusterDefinition definition) throws IOException {
        checkNotNull(definition);
        // write back to file
        Files.createParentDirs(file);
        ObjectMapper mapper = new ObjectMapper();
        mapper.writeValue(file, definition);
    }
}
