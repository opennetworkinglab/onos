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
package org.onosproject.ospf.controller.area;

import com.google.common.base.MoreObjects;
import org.onosproject.ospf.controller.OspfProcess;

import java.util.List;

/**
 * Representation of an OSPF configuration data.
 */
public class Configuration {
    private List<OspfProcess> processes;
    private String method;

    /**
     * Gets the configured processes.
     *
     * @return list of configured processes.
     */
    public List<OspfProcess> getProcesses() {
        return processes;
    }

    /**
     * Sets the configured processes.
     *
     * @param processes configured processes
     */
    public void setProcesses(List<OspfProcess> processes) {
        this.processes = processes;
    }

    /**
     * Gets whether to update, add or delete configuration.
     *
     * @return update, add or delete configuration
     */
    public String getMethod() {
        return method;
    }

    /**
     * Sets whether to update, add or delete configuration.
     *
     * @param method configuration method.
     */
    public void setMethod(String method) {
        this.method = method;
    }


    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .omitNullValues()
                .add("method", method)
                .add("processes", processes)
                .toString();
    }
}