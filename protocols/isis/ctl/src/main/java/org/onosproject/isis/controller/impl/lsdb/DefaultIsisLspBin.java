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
package org.onosproject.isis.controller.impl.lsdb;

import com.google.common.base.MoreObjects;
import org.onosproject.isis.controller.IsisLspBin;
import org.onosproject.isis.controller.LspWrapper;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Representation of LSP bin, where an LSP is stored for Aging.
 * A bin is identified by a bin number and can have one or more LSPs
 * store in a particular bin location
 */
public class DefaultIsisLspBin implements IsisLspBin {
    private int binNumber;
    private Map<String, LspWrapper> listOfLsp = new ConcurrentHashMap<>();

    /**
     * Creates ISIS LSP bin instance.
     *
     * @param binNumber bin number
     */
    public DefaultIsisLspBin(int binNumber) {
        this.binNumber = binNumber;
    }

    /**
     * Adds the LSP to wrapper.
     *
     * @param lspKey     key to add the LSP
     * @param lspWrapper LSP wrapper instance
     */
    public void addIsisLsp(String lspKey, LspWrapper lspWrapper) {
        if (!listOfLsp.containsKey(lspKey)) {
            listOfLsp.put(lspKey, lspWrapper);
            lspWrapper.setBinNumber(this.binNumber);
        }
    }

    /**
     * Returns the LSP wrapper.
     *
     * @param lspKey LSP key
     * @return LSP wrapper
     */
    public LspWrapper isisLsp(String lspKey) {
        return listOfLsp.get(lspKey);
    }

    /**
     * Removes ISIS LSP from database.
     *
     * @param lspKey     LSP key
     * @param lspWrapper LSP wrapper instance
     */
    public void removeIsisLsp(String lspKey, LspWrapper lspWrapper) {
        if (listOfLsp.containsKey(lspKey)) {
            listOfLsp.remove(lspKey);
        }
    }

    /**
     * Returns all LSP wrappers.
     *
     * @return all LSP wrappers
     */
    public Map<String, LspWrapper> listOfLsp() {
        return listOfLsp;
    }

    /**
     * Returns the bin number.
     *
     * @return the bin number
     */
    public int binNumber() {
        return binNumber;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .omitNullValues()
                .add("binNumber", binNumber)
                .add("listOfLsp", listOfLsp)
                .toString();
    }
}