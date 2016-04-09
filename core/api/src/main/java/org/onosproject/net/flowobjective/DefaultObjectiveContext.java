/*
 * Copyright 2016-present Open Networking Laboratory
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

package org.onosproject.net.flowobjective;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Implementation of objective context that delegates calls to provided
 * consumers.
 */
public class DefaultObjectiveContext implements ObjectiveContext {

    private final Consumer<Objective> onSuccess;
    private final BiConsumer<Objective, ObjectiveError> onError;

    /**
     * Creates a new objective context using the given success and error
     * consumers.
     *
     * @param onSuccess consumer to be called on success
     * @param onError consumer to be called on error
     */
    public DefaultObjectiveContext(Consumer<Objective> onSuccess,
                                   BiConsumer<Objective, ObjectiveError> onError) {
        this.onSuccess = onSuccess;
        this.onError = onError;
    }

    /**
     * Creates a new objective context using the given success consumer.
     *
     * @param onSuccess consumer to be called on success
     */
    public DefaultObjectiveContext(Consumer<Objective> onSuccess) {
        this(onSuccess, null);
    }

    /**
     * Creates a new objective context using the given error consumer.
     *
     * @param onError consumer to be called on error
     */
    public DefaultObjectiveContext(BiConsumer<Objective, ObjectiveError> onError) {
        this(null, onError);
    }

    @Override
    public void onSuccess(Objective objective) {
        if (onSuccess != null) {
            onSuccess.accept(objective);
        }
    }

    @Override
    public void onError(Objective objective, ObjectiveError error) {
        if (onError != null) {
            onError.accept(objective, error);
        }
    }
}
