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

import org.onosproject.fnl.intf.NetworkDiagnostic.Type;

import java.util.Set;

/**
 * Network Troubleshooting Core Service.
 */
public interface NetworkDiagnosticService {

    /**
     * Checks for and returns all registered kinds of network anomalies.
     * An empty set is returned if there are no anomalies found.
     *
     * @return all discovered anomalies; may be empty
     */
    Set<NetworkAnomaly> findAnomalies();

    /**
     * Checks for and returns the specific kind of network anomalies.
     * <p>
     * An empty set is returned if there is no anomaly of specific type,
     * or there is no diagnostic of specific type.
     *
     * @return the specific kind of anomalies; may be empty
     */

    /**
     * Checks for and returns the specific type of network anomalies.
     * <p>
     * An empty set is returned if there is no anomaly of specific type,
     * or there is no diagnostic of specific type.
     *
     * @param type the type of network anomalies
     * @return the specific kind of anomalies; may be empty
     */
    Set<NetworkAnomaly> findAnomalies(Type type);

    /**
     * Registers the specified diagnostic module with the service.
     *
     * Each diagnostic type can have only one module,
     * and previous one will be removed.
     *
     * @param diagnostic an instance of class implemented NetworkDiagnostic
     */
    void register(NetworkDiagnostic diagnostic);

    /**
     * Unregisters the specified diagnostic module form the service.
     *
     * @param diagnostic diagnostic module to be removed
     * @return true if the module existed before and has been removed
     *         successfully; false otherwise
     */
    boolean unregister(NetworkDiagnostic diagnostic);
}
