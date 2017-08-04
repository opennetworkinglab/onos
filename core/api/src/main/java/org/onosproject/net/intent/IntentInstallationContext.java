/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.net.intent;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import java.util.Optional;
import java.util.Set;

/**
 * Installation context for a high level Intent.
 * Records pending and error operation contexts of installable Intents for the
 * high level Intent.
 */
public class IntentInstallationContext {
    private IntentData toUninstall;
    private IntentData toInstall;
    private Set<IntentOperationContext> pendingContexts = Sets.newConcurrentHashSet();
    private Set<IntentOperationContext> errorContexts = Sets.newConcurrentHashSet();

    /**
     * Creates an Intent installation context by given information.
     *
     * @param toUninstall the Intent to uninstall
     * @param toInstall the Intent to install
     */
    public IntentInstallationContext(IntentData toUninstall, IntentData toInstall) {
        this.toUninstall = toUninstall;
        this.toInstall = toInstall;
    }

    /**
     * Removes a pending operation context.
     *
     * @param context the operation context to be added
     */
    public void removePendingContext(IntentOperationContext context) {
        this.pendingContexts.remove(context);
    }

    /**
     * Adds a pending context.
     *
     * @param context the operation context to be added
     */
    public void addPendingContext(IntentOperationContext context) {
        this.pendingContexts.add(context);
    }

    /**
     * Adds an error context.
     *
     * @param context the error context to be added.
     */
    public void addErrorContext(IntentOperationContext context) {
        this.errorContexts.add(context);
    }

    /**
     * Retrieves the pending contexts.
     *
     * @return the pending contexts
     */
    public Set<IntentOperationContext> pendingContexts() {
        return ImmutableSet.copyOf(pendingContexts);
    }

    /**
     * Retrieves the error contexts.
     *
     * @return the error contexts
     */
    public Set<IntentOperationContext> errorContexts() {
        return ImmutableSet.copyOf(errorContexts);
    }

    /**
     * Check if pending context is empty.
     *
     * @return true if pending contexts is empty; false otherwise
     */
    public synchronized boolean isPendingContextsEmpty() {
        return pendingContexts.isEmpty();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("pendingContexts", pendingContexts)
                .add("errorContexts", errorContexts)
                .toString();
    }

    /**
     * Retrieves the Intent data which to be uninstalled.
     *
     * @return the Intent data; empty value if not exists
     */
    public Optional<IntentData> toUninstall() {
        return Optional.ofNullable(toUninstall);
    }

    /**
     * Retrieves the Intent data which to be installed.
     *
     * @return the Intent data; empty value if not exists
     */
    public Optional<IntentData> toInstall() {
        return Optional.ofNullable(toInstall);
    }

}
