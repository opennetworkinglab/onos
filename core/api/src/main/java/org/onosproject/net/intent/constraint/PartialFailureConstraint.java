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
package org.onosproject.net.intent.constraint;

import org.onosproject.net.intent.ConnectivityIntent;
import org.onosproject.net.intent.Intent;

/**
 * A constraint that allows intents that can only be partially compiled
 * (i.e. MultiPointToSinglePointIntent or SinglePointToMultiPointIntent)
 * to be installed when some endpoints or paths are not found.
 */
public final class PartialFailureConstraint extends MarkerConstraint {

    public static boolean intentAllowsPartialFailure(Intent intent) {
        if (intent instanceof ConnectivityIntent) {
            ConnectivityIntent connectivityIntent = (ConnectivityIntent) intent;
            return connectivityIntent.constraints().stream()
                    .anyMatch(c -> c instanceof PartialFailureConstraint);
        }
        return false;
    }

    @Override
    public String toString() {
        return "PartialFailureConstraint";
    }
}
