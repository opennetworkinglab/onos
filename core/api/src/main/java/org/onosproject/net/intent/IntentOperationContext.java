/*
 * Copyright 2017-present Open Networking Laboratory
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

import com.google.common.collect.Lists;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Operation context for installable Intent.
 *
 * @param <T> the type of installable Intent
 */
public class IntentOperationContext<T extends Intent> {
    private IntentInstallationContext intentInstallationContext;
    private List<T> intentsToUninstall;
    private List<T> intentsToInstall;

    /**
     * Creates an operation context.
     *
     * @param intentsToUninstall the Intents to uninstall
     * @param intentsToInstall the Intents to install
     * @param intentInstallationContext the high level Intent installation information
     */
    public IntentOperationContext(List<T> intentsToUninstall, List<T> intentsToInstall,
                                  IntentInstallationContext intentInstallationContext) {
        this.intentsToUninstall = Lists.newArrayList(intentsToUninstall);
        this.intentsToInstall = Lists.newArrayList(intentsToInstall);
        this.intentInstallationContext = intentInstallationContext;
    }

    /**
     * Retrieves installable Intents to uninstall.
     *
     * @return the Intents to uninstall
     */
    public List<T> intentsToUninstall() {
        return intentsToUninstall;
    }

    /**
     * Retrieves installable Intents to install.
     *
     * @return the Intents to install
     */
    public List<T> intentsToInstall() {
        return intentsToInstall;
    }

    /**
     * Retrieves high level Intent installation information.
     *
     * @return the high level Intent installation information
     */
    public IntentInstallationContext intentInstallationContext() {
        return intentInstallationContext;
    }

    /**
     * Retrieves high level Intent data to uninstall.
     *
     * @return high level Intent data to uninstall
     */
    public Optional<IntentData> toUninstall() {
        return intentInstallationContext.toUninstall();
    }

    /**
     * Retrieves high level Intent data to install.
     *
     * @return high level Intent data to install
     */
    public Optional<IntentData> toInstall() {
        return intentInstallationContext.toInstall();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof IntentOperationContext)) {
            return false;
        }

        IntentOperationContext that = (IntentOperationContext) obj;
        return Objects.equals(intentsToInstall, that.intentsToInstall) &&
                Objects.equals(intentsToUninstall, that.intentsToUninstall) &&
                Objects.equals(intentInstallationContext, intentInstallationContext);
    }

    @Override
    public int hashCode() {
        return Objects.hash(intentsToInstall,
                            intentsToUninstall,
                            intentInstallationContext);
    }
}
