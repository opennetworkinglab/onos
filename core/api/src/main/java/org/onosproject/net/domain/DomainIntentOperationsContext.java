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

package org.onosproject.net.domain;

/**
 * Represents domain intent that does nothing on success or on error.
 */
public interface DomainIntentOperationsContext {

    /**
     * Invoked on successful execution of the domain intent.
     *
     * @param idops domain intent to execute
     */
    default void onSuccess(DomainIntentOperations idops) {
    }

    /**
     * Invoked when error is encountered while executing the domain intent.
     *
     * @param idos domain intent to execute
     */
    default void onError(DomainIntentOperations idos) {
    }
}
