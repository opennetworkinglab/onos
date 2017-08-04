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
package org.onosproject.fnl.intf;

import java.util.Set;

/**
 * Provide algorithms or methods to diagnose network.
 *
 * Strategy Pattern.
 */
public interface NetworkDiagnostic {

    /**
     * Checks for and returns all corresponding anomalies.
     * An empty set is returned if there are no anomalies.
     *
     * @return the set of all corresponding anomalies; may be empty
     */
    Set<NetworkAnomaly> findAnomalies();

    /**
     * Returns the type of diagnostic.
     *
     * @return the type of diagnostic
     */
    Type type();

    /**
     * Represents diagnostic types.
     */
    enum Type {

        /**
         * Packets round among several devices with several forwarding entries.
         *
         * Routing loops.
         */
        LOOP
    }
}
