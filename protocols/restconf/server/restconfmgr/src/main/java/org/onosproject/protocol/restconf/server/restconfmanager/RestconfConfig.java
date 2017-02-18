/*
 * Copyright 2017-present Open Networking Laboratory
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
package org.onosproject.protocol.restconf.server.restconfmanager;

import org.onosproject.core.ApplicationId;
import org.onosproject.incubator.net.config.basics.ConfigException;
import org.onosproject.net.config.Config;

/**
 * Configuration for TE Topology parameters.
 */
public class RestconfConfig extends Config<ApplicationId> {
    private static final String CONFIG_VALUE_ERROR = "Error parsing config value";
    private static final String USE_DNY_CONFIG = "use-dyn-config";


    /**
     * Retrieves whether RESTCONF should use Dynamic Config service.
     *
     * @return string value of true or false
     * @throws ConfigException if the parameters are not correctly configured or
     *                         conversion of the parameters fails
     */
    public String useDynamicConfig() throws ConfigException {
        try {
            return object.path(USE_DNY_CONFIG).asText();
        } catch (IllegalArgumentException e) {
            throw new ConfigException(CONFIG_VALUE_ERROR, e);
        }
    }

}
