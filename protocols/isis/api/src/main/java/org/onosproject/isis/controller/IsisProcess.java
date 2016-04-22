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
package org.onosproject.isis.controller;

import java.util.List;

/**
 * Representation of an ISIS process.
 */
public interface IsisProcess {

    /**
     * Returns process ID.
     *
     * @return process ID
     */
    public String processId();

    /**
     * Sets process ID.
     *
     * @param processId process ID
     */
    void setProcessId(String processId);

    /**
     * Sets list of ISIS interfaces.
     *
     * @param isisInterfaceList list of ISIS interface details
     */
    void setIsisInterfaceList(List<IsisInterface> isisInterfaceList);

    /**
     * Returns list of ISIS interface details.
     *
     * @return list of ISIS interface details
     */
    List<IsisInterface> isisInterfaceList();
}
