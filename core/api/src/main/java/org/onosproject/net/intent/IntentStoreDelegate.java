/*
 * Copyright 2014-present Open Networking Laboratory
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

import com.google.common.annotations.Beta;
import org.onosproject.store.StoreDelegate;

/**
 * Intent store delegate abstraction.
 */
@Beta
public interface IntentStoreDelegate extends StoreDelegate<IntentEvent> {

    /**
     * Provides an intent data object that should be processed (compiled and
     * installed) by this manager.
     *
     * @param intentData    intent data object
     */
    void process(IntentData intentData);

    /**
     * Called when a new intent has been updated for which this node is the master.
     *
     * @param intentData intent data object
     */
    default void onUpdate(IntentData intentData) {
    }
}
