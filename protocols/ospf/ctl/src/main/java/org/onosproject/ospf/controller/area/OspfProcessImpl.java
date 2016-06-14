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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import org.onosproject.ospf.controller.OspfArea;
import org.onosproject.ospf.controller.OspfProcess;

import java.util.List;

/**
 * Representation of the configuration data for OSPF Process, which will be configured using rest URI.
 */
public class OspfProcessImpl implements OspfProcess {

    private String processId;
    private List<OspfArea> areas;

    /**
     * Gets the list of areas belonging to this process.
     *
     * @return list of areas belonging to this process
     */
    public List<OspfArea> areas() {
        return areas;
    }

    /**
     * Sets the list of areas belonging to this process.
     *
     * @param areas list of areas belonging to this process
     */
    @JsonProperty("areas")
    public void setAreas(List<OspfArea> areas) {
        this.areas = areas;
    }

    /**
     * Gets the process id.
     *
     * @return process id
     */
    public String processId() {
        return processId;
    }

    /**
     * Sets the process id.
     *
     * @param processId the process id
     */
    @JsonProperty("processId")
    public void setProcessId(String processId) {
        this.processId = processId;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .omitNullValues()
                .add("areas", areas)
                .toString();
    }
}