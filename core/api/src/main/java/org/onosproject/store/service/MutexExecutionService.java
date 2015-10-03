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
package org.onosproject.store.service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Service for mutually exclusive job execution.
 */
public interface MutexExecutionService {

    /**
     * Runs the specified task in a mutually exclusive fashion.
     * @param task task to run
     * @param exclusionPath path on which different instances synchronize
     * @param executor executor to use for running the task
     * @return future that is completed when the task execution completes.
     */
    CompletableFuture<Void> execute(MutexTask task, String exclusionPath, Executor executor);
}