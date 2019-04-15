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

import java.net.URI;
import java.util.Collection;

/**
 * Store for managing workflow.
 */
public interface WorkflowStore {

    /**
     * Registers workflow.
     *
     * @param workflow registering workflow
     */
    void register(Workflow workflow);

    /**
     * Unregisters workflow.
     *
     * @param id id of workflow
     */
    void unregister(URI id);

    /**
     * Gets workflow.
     *
     * @param id id of workflow
     * @return workflow
     */
    Workflow get(URI id);

    /**
     * Gets all workflow.
     *
     * @return collection of workflow
     */
    Collection<Workflow> getAll();

    /**
     * Registers local class loader.
     *
     * @param loader class loader
     */
    void registerLocal(ClassLoader loader);

    /**
     * Unregisters local class loader.
     *
     * @param loader class loader
     */
    void unregisterLocal(ClassLoader loader);

    /**
     * Gets class from registered class loaders.
     *
     * @param name name of class
     * @return class
     * @throws ClassNotFoundException class not found exception
     */
    Class getClass(String name) throws ClassNotFoundException;

}
