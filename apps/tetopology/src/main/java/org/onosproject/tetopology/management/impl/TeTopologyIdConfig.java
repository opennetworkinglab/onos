/*
 * Copyright 2016 Open Networking Laboratory
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
package org.onosproject.tetopology.management.impl;

import org.onosproject.core.ApplicationId;
import org.onosproject.incubator.net.config.basics.ConfigException;
import org.onosproject.net.config.Config;
import org.onosproject.tetopology.management.api.TeTopologyId;

/**
 * Configuration for TE Topology Identifiers.
 */
public class TeTopologyIdConfig extends Config<ApplicationId>  {
    public static final String CONFIG_VALUE_ERROR = "Error parsing config value";
    private static final String PROVIDER_ID = "provider-id";
    private static final String CLIENT_ID   = "client-id";
    private static final String TOPOLOGY_ID = "topology-id";

    /**
      * Generates TE topology identifier.
      *
      * @return encoded TE topology identifier
      * @throws ConfigException if the parameters are not correctly configured
      * or conversion of the parameters fails
      */
    public TeTopologyId getTeTopologyId() throws ConfigException {
        try {
            long providerId = object.path(PROVIDER_ID).asLong();
            long clientId = object.path(CLIENT_ID).asLong();
            String topologyId = object.path(TOPOLOGY_ID).asText();

            return new TeTopologyId(providerId, clientId, topologyId);

         } catch (IllegalArgumentException e) {
            throw new ConfigException(CONFIG_VALUE_ERROR, e);
        }
    }
}
