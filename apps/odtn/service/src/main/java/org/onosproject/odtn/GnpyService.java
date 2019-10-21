/*
 * Copyright 2018-present Open Networking Foundation
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

package org.onosproject.odtn;

import org.apache.commons.lang3.tuple.Pair;
import org.onosproject.net.ConnectPoint;
import org.onosproject.net.intent.IntentId;

/**
 * Interface for Integration with GNPy optical tool.
 */
public interface GnpyService {


    /**
     * Connectes to an instance of GNPY optical planning and simulation tool.
     *
     * @param protocol the protocol, eg HTTP or HTTPS
     * @param ip       the ip of the service
     * @param port     the port fo the service
     * @param username the username of the service
     * @param password the password.
     * @return true if connection was successful
     */
    boolean connectGnpy(String protocol, String ip, String port, String username, String password);

    /**
     * Disconnects for the connected GNPy instance.
     *
     * @return true if successful
     */
    boolean disconnectGnpy();

    /**
     * Checks connectivity an instance of GNPY optical planning and simulation tool.
     *
     * @return true if connection was successful
     */
    boolean isConnected();

    /**
     * Obtains the best connectivity from A to B according to GSNR from GNPY, possibly bidirectional.
     *
     * @param ingress       the ingress connect point
     * @param egress        the egress connect point
     * @param bidirectional true if bidirectional connectivity is required.
     * @return the Id of the intent that was installed as a result of the computation.
     */
    Pair<IntentId, Double> obtainConnectivity(ConnectPoint ingress, ConnectPoint egress, boolean bidirectional);


}
