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

/**
 * Interface definition for a transaction context builder.
 */
public interface TransactionContextBuilder {

    /**
     * Disables distribution of map entries across multiple database partitions.
     * <p>
     * When partitioning is disabled, the returned map will have a single
     * partition that spans the entire cluster. Furthermore, the changes made to
     * the map are ephemeral and do not survive a full cluster restart.
     * </p>
     * <p>
     * Note: By default, partitions are enabled. This feature is intended to
     * simplify debugging.
     * </p>
     *
     * @return this TransactionalContextBuilder
     */
    TransactionContextBuilder withPartitionsDisabled();

    /**
     * Builds a TransactionContext based on configuration options supplied to this
     * builder.
     *
     * @return a new TransactionalContext
     * @throws java.lang.RuntimeException if a mandatory parameter is missing
     */
    TransactionContext build();
}
