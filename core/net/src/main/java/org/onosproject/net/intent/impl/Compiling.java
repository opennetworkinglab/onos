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
import org.onosproject.net.intent.IntentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

class Compiling implements IntentUpdate {

    private static final Logger log = LoggerFactory.getLogger(Compiling.class);

    // TODO: define an interface and use it, instead of IntentManager
    private final IntentManager intentManager;
    private final Intent intent;

    Compiling(IntentManager intentManager, Intent intent) {
        this.intentManager = checkNotNull(intentManager);
        this.intent = checkNotNull(intent);
    }

    @Override
    public Optional<IntentUpdate> execute() {
        try {
            return Optional.of(new Installing(intentManager, intent, intentManager.compileIntent(intent, null)));
        } catch (PathNotFoundException e) {
            log.debug("Path not found for intent {}", intent);
            // TODO: revisit to implement failure handling
            return Optional.of(new DoNothing());
        } catch (IntentException e) {
            log.warn("Unable to compile intent {} due to:", intent.id(), e);
            // TODO: revisit to implement failure handling
            return Optional.of(new DoNothing());
        }
    }
}
