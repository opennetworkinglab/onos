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
package org.onosproject.net.intent;

import com.google.common.annotations.Beta;
import org.onosproject.store.Timestamp;

/**
 * Logical clock service that issues per-intent timestamps.
 */
@Beta
public interface IntentClockService {

    /**
     * Returns a new timestamp for the specified intent.
     *
     * @param intentId identifier for the intent.
     * @return timestamp
     */
    Timestamp getTimestamp(IntentId intentId);
}
