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

package org.onosproject.pipelines.fabric.impl.behaviour.pipeliner;

import org.onosproject.net.flowobjective.ObjectiveError;

/**
 * Signals an exception when translating a flow objective.
 */
class FabricPipelinerException extends Exception {

    private final ObjectiveError error;

    /**
     * Creates a new exception for the given message. Sets ObjectiveError to
     * UNSUPPORTED.
     *
     * @param message message
     */
    FabricPipelinerException(String message) {
        super(message);
        this.error = ObjectiveError.UNSUPPORTED;
    }

    /**
     * Creates a new exception for the given message and ObjectiveError.
     *
     * @param message message
     * @param error objective error
     */
    FabricPipelinerException(String message, ObjectiveError error) {
        super(message);
        this.error = error;
    }

    /**
     * Returns the ObjectiveError of this exception.
     *
     * @return objective error
     */
    ObjectiveError objectiveError() {
        return error;
    }
}
