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
package org.onosproject.demo;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Optional;

/**
 * Simple demo api interface.
 */
public interface DemoApi {

    enum InstallType { MESH, RANDOM }

    /**
     * Tests flow subsystem based on the parameters supplied.
     *
     * @param params the test parameters
     * @return JSON representation
     */
    JsonNode flowTest(Optional<JsonNode> params);

    /**
     * Installs intents based on the installation type.
     * @param type the installation type.
     * @param runParams run params
     */
    void setup(InstallType type, Optional<JsonNode> runParams);

    /**
     * Uninstalls all existing intents.
     */
    void tearDown();

}
