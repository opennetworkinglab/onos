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
package org.onosproject.ospf.controller.lsdb;

import com.google.common.base.MoreObjects;
import org.onosproject.ospf.controller.LsaBin;
import org.onosproject.ospf.controller.LsaWrapper;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a bin, where an LSA is stored for Aging.
 * A bin is identified by a bin number and can have one or more LSAs
 * store in a particular bin location.
 */
public class LsaBinImpl implements LsaBin {

    private int binNumber;
    private Map<String, LsaWrapper> listOfLsa = new ConcurrentHashMap<>();

    /**
     * Creates an instance of LSA bin.
     *
     * @param binNumber unique number of this bin
     */
    public LsaBinImpl(int binNumber) {
        this.binNumber = binNumber;
    }

    /**
     * Adds the LSA to this bin with the given key.
     *
     * @param lsaKey     key of the LSA
     * @param lsaWrapper wrapper instance to store
     */
    public void addOspfLsa(String lsaKey, LsaWrapper lsaWrapper) {
        if (!listOfLsa.containsKey(lsaKey)) {
            listOfLsa.put(lsaKey, lsaWrapper);
            lsaWrapper.setBinNumber(this.binNumber);
        }
    }

    /**
     * Gets the LSA from the bin.
     *
     * @param lsaKey key to search the LSA
     * @return LSA Wrapper instance
     */
    public LsaWrapper ospfLsa(String lsaKey) {

        return listOfLsa.get(lsaKey);
    }

    /**
     * Removes LSA from the bin.
     *
     * @param lsaKey     key to search LSA
     * @param lsaWrapper wrapper object to remove
     */
    public void removeOspfLsa(String lsaKey, LsaWrapper lsaWrapper) {
        if (listOfLsa.containsKey(lsaKey)) {
            listOfLsa.remove(lsaKey);
        }
    }

    /**
     * Gets the list of LSAs in this bin as key value pair.
     *
     * @return list of LSAs in this bin as key value pair
     */
    public Map<String, LsaWrapper> listOfLsa() {
        return listOfLsa;
    }

    /**
     * Gets the bin number.
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
                .add("listOfLsa", listOfLsa)
                .toString();
    }
}