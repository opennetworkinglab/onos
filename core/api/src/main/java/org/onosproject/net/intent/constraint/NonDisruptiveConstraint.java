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
package org.onosproject.net.intent.constraint;

import org.onosproject.net.intent.ConnectivityIntent;
import org.onosproject.net.intent.Intent;

/**
 * Constraint to request a non-disruptive intent reallocation.
 */
public final class NonDisruptiveConstraint extends MarkerConstraint {

    private static final NonDisruptiveConstraint NON_DISRUPTIVE_CONSTRAINT = new NonDisruptiveConstraint();

    protected NonDisruptiveConstraint() {
    }

    /**
     * Determines whether the intent requires a non-disruptive reallocation.
     *
     * @param intent  intent to be inspected
     * @return        whether the intent has a NonDisruptiveConstraint
     */
    public static boolean requireNonDisruptive(Intent intent) {
        if (intent instanceof ConnectivityIntent) {
            ConnectivityIntent connectivityIntent = (ConnectivityIntent) intent;
            return connectivityIntent.constraints().stream()
                    .anyMatch(p -> p instanceof NonDisruptiveConstraint);
        }
        return false;
    }

    /**
     * Returns the nonDisruptiveConstraint.
     *
     * @return non-disruptive constraint
     */
    public static NonDisruptiveConstraint nonDisruptive() {
        return NON_DISRUPTIVE_CONSTRAINT;
    }

    @Override
    public String toString() {
        return "Non-disruptive reallocation required";
    }
}
