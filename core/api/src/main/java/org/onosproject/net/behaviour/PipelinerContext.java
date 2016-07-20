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
package org.onosproject.net.behaviour;

import org.onlab.osgi.ServiceDirectory;
import org.onosproject.net.flowobjective.FlowObjectiveStore;

/**
 * Processing context and supporting services for the pipeline behaviour.
 */
public interface PipelinerContext {

    /**
     * Returns the service directory which can be used to obtain references
     * to various supporting services.
     *
     * @return service directory
     */
    ServiceDirectory directory();

    /**
     * Returns the Objective Store where data can be stored and retrieved.
     * @return the flow objective store
     */
    FlowObjectiveStore store();

    // TODO: add means to store and access shared state
}
