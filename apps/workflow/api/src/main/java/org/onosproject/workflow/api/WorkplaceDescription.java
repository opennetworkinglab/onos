/*
 * Copyright 2018-present Open Networking Foundation
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
package org.onosproject.workflow.api;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Optional;

/**
 * Interface for workplace description.
 */
public interface WorkplaceDescription {

    /**
     * Workplace name field name.
     */
    String WP_NAME = "name";

    /**
     * Workplace data field name.
     */
    String WP_DATA = "data";

    /**
     * Gets workplace name.
     * @return workplace name
     */
    String name();

    /**
     * Gets optional workplace data model.
     * @return workplace optData model
     */
    Optional<JsonNode> data();

    /**
     * Gets json of workflow description.
     * @return json of workflow description
     */
    JsonNode toJson();
}
