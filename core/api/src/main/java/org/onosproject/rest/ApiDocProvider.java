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
package org.onosproject.rest;

import com.google.common.annotations.Beta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Entity capable of providing REST API documentation resources.
 */
@Beta
public class ApiDocProvider {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String DOCS = "/apidoc/swagger.json";
    private static final String MODEL = "/apidoc/model.json";

    private final String key;
    private final String name;
    private final ClassLoader classLoader;

    /**
     * Creates a new REST API documentation provider.
     *
     * @param key         REST API key
     * @param name        REST API name
     * @param classLoader class loader
     */
    public ApiDocProvider(String key, String name, ClassLoader classLoader) {
        this.key = checkNotNull(key, "Key cannot be null");
        this.name = checkNotNull(name, "Name cannot be null");
        this.classLoader = checkNotNull(classLoader, "Class loader cannot be null");
    }

    /**
     * Returns the REST API key.
     *
     * @return REST API key
     */
    public String key() {
        return key;
    }

    /**
     * Returns the REST API name.
     *
     * @return REST API name
     */
    public String name() {
        return name;
    }

    /**
     * Returns input stream containing Swagger UI compatible JSON.
     *
     * @return input stream with Swagger JSON data
     */
    public InputStream docs() {
        return get(DOCS);
    }

    /**
     * Returns input stream containing JSON model schema.
     *
     * @return input stream with JSON model schema
     */
    public InputStream model() {
        return get(MODEL);
    }

    private InputStream get(String resource) {
        InputStream stream = classLoader.getResourceAsStream(resource);
        if (stream == null) {
            log.warn("Unable to find JSON resource {}", resource);
        }
        return stream;
    }

}
