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

package org.onosproject.provider.ospf.cfg.impl;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.onlab.osgi.DefaultServiceDirectory;
import org.onosproject.core.ApplicationId;
import org.onosproject.net.config.Config;
import org.onosproject.ospf.controller.OspfController;

/**
 * Configuration object for OSPF.
 */
public class OspfAppConfig extends Config<ApplicationId> {
    public static final String METHOD = "method";
    public static final String ATTRIBUTE = "attribute";
    public static final String PROCESSES = "processes";
    @Reference(cardinality = ReferenceCardinality.MANDATORY_UNARY)
    private OspfController ospfController;

    /**
     * Returns the configuration method, add, delete etc.
     *
     * @return the configuration method, add, delete etc
     */
    public String method() {
        return get(METHOD, null);
    }

    /**
     * Returns the configuration attribute, area, process etc.
     *
     * @return the configuration attribute, area, process etc
     */
    public String attribute() {
        return get(ATTRIBUTE, null);
    }

    /**
     * Returns the configured processes.
     *
     * @return the configured processes
     */
    public JsonNode processes() {
        JsonNode jsonNodes = object.get(PROCESSES);

        return jsonNodes;
    }

    @Override
    public boolean isValid() {
        this.ospfController = DefaultServiceDirectory.getService(OspfController.class);

        return true;
    }
}