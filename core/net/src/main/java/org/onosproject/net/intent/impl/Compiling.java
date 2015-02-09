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
package org.onosproject.net.intent.impl;

import org.onosproject.net.intent.Intent;
import org.onosproject.net.intent.IntentData;
import org.onosproject.net.intent.IntentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

class Compiling implements IntentUpdate {

    private static final Logger log = LoggerFactory.getLogger(Compiling.class);

    // TODO: define an interface and use it, instead of IntentManager
    private final IntentManager intentManager;
    private final IntentData pending;
    private final IntentData current;

    Compiling(IntentManager intentManager, IntentData pending, IntentData current) {
        this.intentManager = checkNotNull(intentManager);
        this.pending = checkNotNull(pending);
        this.current = current;
    }

    @Override
    public Optional<IntentUpdate> execute() {
        try {
            List<Intent> installables = (current != null) ? current.installables() : null;
            pending.setInstallables(intentManager.compileIntent(pending.intent(), installables));
            return Optional.of(new InstallCoordinating(intentManager, pending, current));
        } catch (IntentException e) {
            log.debug("Unable to compile intent {} due to: {}", pending.intent(), e);
            return Optional.of(new CompilingFailed(pending));
        }
    }
}
