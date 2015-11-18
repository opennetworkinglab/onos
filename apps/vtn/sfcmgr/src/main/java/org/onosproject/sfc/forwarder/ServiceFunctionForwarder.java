/*
 * Copyright 2015 Open Networking Laboratory
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
package org.onosproject.sfc.forwarder;

import org.onosproject.core.ApplicationId;
import org.onosproject.net.flowobjective.Objective;
import org.onosproject.vtnrsc.PortChain;

/**
 * Abstraction of an entity which provides Service function forwarder.
 */
public interface ServiceFunctionForwarder {

    /**
     * Install Service function chain.
     *
     * @param portChain Port chain
     */
    void install(PortChain portChain);

    /**
     * Programs forwarding object for Service Function.
     *
     * @param portChain port chain
     * @param appid application id
     * @param type forwarding objective operation type
     */
    void programServiceFunctionForwarder(PortChain portChain, ApplicationId appid,
            Objective.Operation type);
}
