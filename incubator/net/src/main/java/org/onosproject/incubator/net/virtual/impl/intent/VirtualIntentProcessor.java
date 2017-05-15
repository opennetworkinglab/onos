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

package org.onosproject.incubator.net.virtual.impl.intent;

import org.onosproject.incubator.net.virtual.NetworkId;
import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentData;

import java.util.List;
import java.util.Optional;

/**
 * A collection of methods to process an intent for virtual networks.
 *
 * This interface is public, but intended to be used only by IntentManager and
 * IntentProcessPhase subclasses stored under phase package.
 */
public interface VirtualIntentProcessor {
    /**
     * Compiles an intent recursively.
     *
     * @param networkId virtual network identifier
     * @param intent intent
     * @param previousInstallables previous intent installables
     * @return result of compilation
     */
    List<Intent> compile(NetworkId networkId, Intent intent, List<Intent> previousInstallables);

    /**
     * Applies intents.
     *
     * @param networkId virtual network identifier
     * @param toUninstall Intent data describing flows to uninstall.
     * @param toInstall Intent data describing flows to install.
     */
    void apply(NetworkId networkId, Optional<IntentData> toUninstall,
               Optional<IntentData> toInstall);
}
