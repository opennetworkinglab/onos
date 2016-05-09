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
package org.onosproject.isis.controller.impl;

import org.onosproject.isis.controller.IsisInterface;
import org.onosproject.isis.controller.IsisProcess;

import java.util.List;

/**
 * Represents of an ISIS process.
 */
public class DefaultIsisProcess implements IsisProcess {
    private String processId;
    private List<IsisInterface> isisInterfaceList;

    /**
     * Gets process ID.
     *
     * @return process ID
     */
    public String processId() {
        return processId;
    }

    /**
     * Sets process ID.
     *
     * @param processId process ID
     */
    public void setProcessId(String processId) {
        this.processId = processId;
    }

    /**
     * Gets list of ISIS interface details.
     *
     * @return list of ISIS interface details
     */
    public List<IsisInterface> isisInterfaceList() {
        return isisInterfaceList;
    }

    /**
     * Sets list of ISIS interface details.
     *
     * @param isisInterfaceList list of ISIS interface details
     */
    public void setIsisInterfaceList(List<IsisInterface> isisInterfaceList) {
        this.isisInterfaceList = isisInterfaceList;
    }
}