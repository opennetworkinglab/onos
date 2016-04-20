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
package org.onosproject.xosclient.api;

import com.fasterxml.jackson.databind.JsonNode;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.config.Config;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * XOS API access information.
 */
public class XosAccessConfig extends Config<ApplicationId> {

    protected final Logger log = getLogger(getClass());

    private static final String XOS_SERVICE_ENDPOINT = "serviceEndpoint";
    private static final String XOS_ADMIN_USER = "adminUser";
    private static final String XOS_ADMIN_PASSWORD = "adminPassword";

    /**
     * Returns XOS access information.
     *
     * @return XOS access, or null
     */
    public XosAccess xosAccess() {
        try {
            return new XosAccess(getConfig(object, XOS_SERVICE_ENDPOINT),
                                 getConfig(object, XOS_ADMIN_USER),
                                 getConfig(object, XOS_ADMIN_PASSWORD));
        } catch (NullPointerException e) {
            log.error("Failed to get XOS access");
            return null;
        }
    }

    /**
     * Returns value of a given path. If the path is missing, show log and return
     * null.
     *
     * @param path path
     * @return value or null
     */
    private String getConfig(JsonNode jsonNode, String path) {
        jsonNode = jsonNode.path(path);

        if (jsonNode.isMissingNode()) {
            log.error("{} is not configured", path);
            return null;
        } else {
            return jsonNode.asText();
        }
    }
}
