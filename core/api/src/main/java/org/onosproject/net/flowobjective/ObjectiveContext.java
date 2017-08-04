/*
 * Copyright 2015-present Open Networking Foundation
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

import com.google.common.annotations.Beta;

/**
 * The context of a objective that will become the subject of
 * the notification.
 * <p>
 * Implementations of this class must be serializable.
 * </p>
 */
@Beta
public interface ObjectiveContext {

    /**
     * Invoked on successful execution of the flow objective.
     *
     * @param objective objective to execute
     */
    default void onSuccess(Objective objective) {
    }

    /**
     * Invoked when error is encountered while executing the flow objective.
     *
     * @param objective objective to execute
     * @param error error encountered
     */
    default void onError(Objective objective, ObjectiveError error) {
    }

}
