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
package org.onosproject.vtnrsc.portchainsfmap;

import java.util.List;

import org.onosproject.vtnrsc.PortChainId;
import org.onosproject.vtnrsc.ServiceFunctionGroup;

/**
 * Service for interacting with the inventory of service functions for a given port chain.
 */
public interface PortChainSfMapService {

    /**
     * Returns true if the port chain exists.
     *
     * @param portChainId port chain identifier
     * @return true or false if one with the given identifier exists.
     */
    boolean exists(PortChainId portChainId);

    /**
     * Returns the list of service function groups available in the given port chain.
     *
     * @param portChainId port chain id
     * @return list of service functions
     */
    List<ServiceFunctionGroup> getServiceFunctions(PortChainId portChainId);

}
