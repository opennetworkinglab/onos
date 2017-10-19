/*
 * Copyright 2016-present Open Networking Foundation
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
package org.onosproject.drivers.microsemi.yang;

import java.time.OffsetDateTime;

import org.onosproject.netconf.DatastoreId;
import org.onosproject.netconf.NetconfException;
import org.onosproject.netconf.NetconfSession;
import org.onosproject.yang.gen.v1.ietfsystem.rev20140806.IetfSystem;
import org.onosproject.yang.gen.v1.ietfsystem.rev20140806.IetfSystemOpParam;

/**
 * Extension of ietfSystemService to include NETCONF sessions.
 *
 * This is manually extended and should be revised if the ietf-system.yang file changes
 */
public interface IetfSystemNetconfService {
    /**
     * Returns the attribute ietfSystem.
     *
     * @param ietfSystem value of ietfSystem
     * @param session An active NETCONF session
     * @return ietfSystem
     * @throws NetconfException if the session has any error
     */
    IetfSystem getIetfSystem(IetfSystemOpParam ietfSystem, NetconfSession session) throws NetconfException;

    /**
     * Returns the result of the init query.
     *
     * @param session An active NETCONF session
     * @return ietfSystem
     * @throws NetconfException if the session has any error
     */
    IetfSystem getIetfSystemInit(NetconfSession session) throws NetconfException;

    /**
     * Sets the value to attribute ietfSystem.
     *
     * @param ietfSystem value of ietfSystem
     * @param session An active NETCONF session
     * @param ncDs datastore type running, startup or candidate
     * @return Boolean to indicate success or failure
     * @throws NetconfException if the session has any error
     */
    boolean setIetfSystem(IetfSystemOpParam ietfSystem, NetconfSession session,
          DatastoreId ncDs) throws NetconfException;

    /**
     * Service interface of setCurrentDatetime.
     *
     * @param date input of service interface setCurrentDatetime
     * @param session An active NETCONF session
     * @throws NetconfException if the session has any error
     */
    void setCurrentDatetime(OffsetDateTime date, NetconfSession session) throws NetconfException;

    /**
     * Service interface of systemShutdown.
     *
     * @param session An active NETCONF session
     * @throws NetconfException if the session has any error
     */
    void systemShutdown(NetconfSession session) throws NetconfException;

}
