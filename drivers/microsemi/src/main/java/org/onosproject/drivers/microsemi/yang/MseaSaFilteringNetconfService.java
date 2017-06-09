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

package org.onosproject.drivers.microsemi.yang;

import org.onosproject.netconf.DatastoreId;
import org.onosproject.netconf.NetconfException;
import org.onosproject.netconf.NetconfSession;
import org.onosproject.yang.gen.v1.mseasafiltering.rev20160412.MseaSaFiltering;
import org.onosproject.yang.gen.v1.mseasafiltering.rev20160412.MseaSaFilteringOpParam;
import org.onosproject.yang.gen.v1.mseasafiltering.rev20160412.mseasafiltering.sourceipaddressfiltering.interfaceeth0.SourceAddressRange;

import java.util.List;

/**
 * Extension of mseaSaFilteringService to include NETCONF sessions.
 *
 * This is manually extended and should be revised if the msea-sa-filtering.yang file changes
 */
public interface MseaSaFilteringNetconfService {
    /**
     * Returns the attribute mseaSaFiltering.
     *
     * @param mseaSaFiltering value of mseaSaFiltering
     * @param session  An active NETCONF session
     * @return mseaSaFiltering
     * @throws NetconfException if the session has any error
     */
    MseaSaFiltering getMseaSaFiltering(
            MseaSaFilteringOpParam mseaSaFiltering, final NetconfSession session)
            throws NetconfException;

    /**
     * Get a filtered subset of the config model.
     *
     * @param session  An active NETCONF session
     * @return mseaSaFiltering
     * @throws NetconfException if the session has any error
     */
    public List<SourceAddressRange> getConfigMseaSaFilterIds(NetconfSession session)
            throws NetconfException;

    /**
     * Sets the value to attribute mseaSaFiltering.
     *
     * @param mseaSaFiltering value of mseaSaFiltering
     * @param session An active NETCONF session
     * @param targetDs The NETCONF datastore to edit
     * @return Boolean to indicate success or failure
     * @throws NetconfException if the session has any error
     */
    boolean setMseaSaFiltering(MseaSaFilteringOpParam mseaSaFiltering,
            NetconfSession session, DatastoreId targetDs) throws NetconfException;

    /**
     * Deletes the value to attribute mseaSaFiltering.
     *
     * @param mseaSaFiltering value of mseaSaFiltering
     * @param session An active NETCONF session
     * @param targetDs The NETCONF datastore to edit
     * @return Boolean to indicate success or failure
     * @throws NetconfException if the session has any error
     */
    boolean deleteMseaSaFilteringRange(MseaSaFilteringOpParam mseaSaFiltering,
              NetconfSession session, DatastoreId targetDs) throws NetconfException;

}
