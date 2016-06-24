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
import java.util.Map;

/**
 * Representation of an ISIS link state database.
 */
public interface IsisLsdb {

    /**
     * Returns the ISIS LSDB.
     *
     * @return ISIS LSDB
     */
    IsisLsdb isisLsdb();

    /**
     * Initializes LSDB.
     */
    void initializeDb();

    /**
     * Returns the LSDB LSP key.
     *
     * @param systemId system ID
     * @return LSP key
     */
    String lspKey(String systemId);

    /**
     * Returns the sequence number.
     *
     * @param lspType L1 or L2 LSP
     * @return sequence number
     */
    int lsSequenceNumber(IsisPduType lspType);

    /**
     * Finds the LSP from LSDB.
     *
     * @param pduType L1 or L2 LSP
     * @param lspId   LSP ID
     * @return LSP wrapper object
     */
    LspWrapper findLsp(IsisPduType pduType, String lspId);

    /**
     * Installs a new self-originated LSA in LSDB.
     * Return true if installing was successful else false.
     *
     * @param lsPdu            PDU instance
     * @param isSelfOriginated true if self originated else false
     * @param isisInterface    ISIS interface instance
     * @return true if successfully added
     */
    boolean addLsp(IsisMessage lsPdu, boolean isSelfOriginated, IsisInterface isisInterface);

    /**
     * Checks received LSP is latest, same or old.
     *
     * @param receivedLsp received LSP
     * @param lspFromDb   existing LSP
     * @return "latest", "old" or "same"
     */
    String isNewerOrSameLsp(IsisMessage receivedLsp, IsisMessage lspFromDb);

    /**
     * Returns all LSPs (L1 and L2).
     *
     * @param excludeMaxAgeLsp exclude the max age LSPs
     * @return List of LSPs
     */
    List<LspWrapper> allLspHeaders(boolean excludeMaxAgeLsp);

    /**
     * Deletes the given LSP.
     *
     * @param lsp LSP instance
     */
    void deleteLsp(IsisMessage lsp);

    /**
     * Gets the neighbor database information.
     *
     * @return neighbor database information
     */
    Map<String, LspWrapper> getL1Db();

    /**
     * Gets the neighbor database information.
     *
     * @return neighbor database information
     */
    Map<String, LspWrapper> getL2Db();

    /**
     * Sets the level 1 link state sequence number.
     *
     * @param l1LspSeqNo link state sequence number
     */
     void setL1LspSeqNo(int l1LspSeqNo);

    /**
     * Sets the level 2 link state sequence number.
     *
     * @param l2LspSeqNo link state sequence number
     */
    void setL2LspSeqNo(int l2LspSeqNo);
    /**
     * Removes topology information when neighbor down.
     *
     * @param neighbor ISIS neighbor instance
     * @param isisInterface ISIS interface instance
     */
    void removeTopology(IsisNeighbor neighbor, IsisInterface isisInterface);
}