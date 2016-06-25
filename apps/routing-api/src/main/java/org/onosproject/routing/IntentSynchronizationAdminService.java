/*
 * Copyright 2015-present Open Networking Laboratory
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

package org.onosproject.routing;

/**
 * Administrative APIs for managing intent synchronization.
 */
public interface IntentSynchronizationAdminService {

    /**
     * Changes whether this instance is the primary or not based on the
     * boolean parameter.
     *
     * @param isPrimary true if the instance is primary, false if it is not
     */
    void modifyPrimary(boolean isPrimary);

    /**
     * Withdraws all intents.
     */
    void removeIntents();
}
