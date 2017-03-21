/*
 * Copyright 2017-present Open Networking Foundation
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
package org.onosproject.incubator.net.l2monitoring.cfm.service;

import java.util.Collection;

import org.onosproject.incubator.net.l2monitoring.cfm.Mep;
import org.onosproject.incubator.net.l2monitoring.cfm.MepEntry;
import org.onosproject.incubator.net.l2monitoring.cfm.MepLbCreate;
import org.onosproject.incubator.net.l2monitoring.cfm.MepLtCreate;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MaIdShort;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MdId;
import org.onosproject.incubator.net.l2monitoring.cfm.identifier.MepId;

/**
 * For the management of Maintenance Association Endpoints.
 *
 * These are dependent on the Maintenance Domain service which maintains the
 * Maintenance Domain and Maintenance Associations
 */
public interface CfmMepService {
    /**
     * Retrieve all {@link org.onosproject.incubator.net.l2monitoring.cfm.MepEntry}(s) belonging to an MA.
     * @param mdName A Maintenance Domain
     * @param maName A Maintetance Association in the MD
     * @return A collection of MEP Entries
     * @throws CfmConfigException If there a problem with the MD or MA
     */
    Collection<MepEntry> getAllMeps(MdId mdName, MaIdShort maName)
            throws CfmConfigException;

    /**
     * Retrieve a named {@link org.onosproject.incubator.net.l2monitoring.cfm.MepEntry} belonging to an MA.
     * @param mdName A Maintenance Domain
     * @param maName A Maintetance Association in the MD
     * @param mepId A Mep Id
     * @return A MEP Entry or null if none found
     * @throws CfmConfigException If there a problem with the MD, MA or MEP
     */
    MepEntry getMep(MdId mdName, MaIdShort maName, MepId mepId)
            throws CfmConfigException;

    /**
     * Delete a named {@link org.onosproject.incubator.net.l2monitoring.cfm.Mep} belonging to an MA.
     * @param mdName A Maintenance Domain
     * @param maName A Maintetance Association in the MD
     * @param mepId A Mep Id
     * @return true if the MEP was deleted successfully. false if it was not found
     * @throws CfmConfigException If there a problem with the MD or MA
     */
    boolean deleteMep(MdId mdName, MaIdShort maName, MepId mepId)
            throws CfmConfigException;

    /**
     * Create a named {@link org.onosproject.incubator.net.l2monitoring.cfm.Mep} on an MA.
     * @param mdName A Maintenance Domain
     * @param maName A Maintetance Association in the MD
     * @param mep A Mep object
     * @return False if it was created successfully. True if the object already exists.
     * @throws CfmConfigException If there a problem with the MD, MA or MEP
     */
    boolean createMep(MdId mdName, MaIdShort maName, Mep mep)
            throws CfmConfigException;

    /**
     * Create a {@link org.onosproject.incubator.net.l2monitoring.cfm.MepLbEntry Loopback} session on the named Mep.
     * @param mdName A Maintenance Domain
     * @param maName A Maintetance Association in the MD
     * @param mepId A Mep Id
     * @param lbCreate The Loopback session details
     * @throws CfmConfigException If there a problem with the MD, MA or MEP
     */
    void transmitLoopback(MdId mdName, MaIdShort maName, MepId mepId,
            MepLbCreate lbCreate) throws CfmConfigException;

    /**
     * Abort a {@link org.onosproject.incubator.net.l2monitoring.cfm.MepLbEntry Loopback} session on the named Mep.
     * @param mdName A Maintenance Domain
     * @param maName A Maintetance Association in the MD
     * @param mepId A Mep Id
     * @throws CfmConfigException If there a problem with the MD, MA or MEP
     */
    void abortLoopback(MdId mdName, MaIdShort maName, MepId mepId)
            throws CfmConfigException;

    /**
     * Create a {@link org.onosproject.incubator.net.l2monitoring.cfm.MepLtEntry Linktrace} session on the named Mep.
     * @param mdName A Maintenance Domain
     * @param maName A Maintetance Association in the MD
     * @param mepId A Mep Id
     * @param ltCreate The Linktrace session details
     * @throws CfmConfigException If there a problem with the MD, MA or MEP
     */
    void transmitLinktrace(MdId mdName, MaIdShort maName, MepId mepId,
            MepLtCreate ltCreate) throws CfmConfigException;
}
