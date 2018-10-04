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

import org.onosproject.store.Store;
import org.onosproject.store.service.StorageException;

import java.util.Collection;

/**
 * Interface for workplace store.
 */
public interface WorkplaceStore extends Store<WorkflowDataEvent, WorkplaceStoreDelegate> {

    /**
     * Registers workplace on workplace store.
     * @param name workplace name to register
     * @param workplace workplace
     * @throws StorageException storage exception
     */
    void registerWorkplace(String name, Workplace workplace) throws StorageException;

    /**
     * Removes workplace from workplace store.
     * @param name workplace name to remove
     * @throws StorageException storage exception
     */
    void removeWorkplace(String name) throws StorageException;

    /**
     * Gets workplace on workplace store.
     * @param name workplace name to get
     * @return workplace
     * @throws StorageException storage exception
     */
    Workplace getWorkplace(String name) throws StorageException;

    /**
     * Commits workplace on workplace store.
     * @param name workplace name to commit
     * @param workplace workplace to commit
     * @param handleEvent whether or not to handle workplace(workflow data) event
     * @throws StorageException storage exception
     */
    void commitWorkplace(String name, Workplace workplace, boolean handleEvent) throws StorageException;

    /**
     * Gets all workplaces from workplace store.
     * @return collection of workplace
     * @throws StorageException storage exception
     */
    Collection<Workplace> getWorkplaces() throws StorageException;

    /**
     * Registers workflow context on workplace store.
     * @param name workflow context name to register
     * @param context workflow context to register
     * @throws StorageException storage exception
     */
    void registerContext(String name, WorkflowContext context) throws StorageException;

    /**
     * Removes workflow context from workplace store.
     * @param name workflow context name
     * @throws StorageException storage exception
     */
    void removeContext(String name) throws StorageException;

    /**
     * Gets workflow context with name.
     * @param name workflow context name to get
     * @return workflow context
     * @throws StorageException storage exception
     */
    WorkflowContext getContext(String name) throws StorageException;

    /**
     * Commits workflow context on workplace store.
     * @param name workflow context name to commit
     * @param context workflow context to commit
     * @param handleEvent whether or not to handle workflow context(workflow data) event
     * @throws StorageException storage exception
     */
    void commitContext(String name, WorkflowContext context, boolean handleEvent) throws StorageException;

    /**
     * Gets all workflow context from workplace store.
     * @return collection of workflow context
     * @throws StorageException storage exception
     */
    Collection<WorkflowContext> getContexts() throws StorageException;

    /**
     * Gets workflow contexts belonging to a workplace.
     * @param workplaceName workplace name
     * @return collection of workflow contexts belonging to a workplace
     */
    Collection<WorkflowContext> getWorkplaceContexts(String workplaceName);

    /**
     * Removes all workflow contexts beloinging to a workplace.
     * @param workplaceName workplace name
     */
    void removeWorkplaceContexts(String workplaceName);
}
