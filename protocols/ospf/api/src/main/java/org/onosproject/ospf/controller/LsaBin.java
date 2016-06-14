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
package org.onosproject.ospf.controller;

import java.util.Map;

/**
 * Representation of a bin where an LSA is stored for aging.
 * A bin is identified by a bin number and can have one or more LSAs
 * stored in a particular bin location.
 */
public interface LsaBin {

    /**
     * Adds the given LSA to this bin with the given key.
     *
     * @param lsaKey     key of the stored LSA
     * @param lsaWrapper wrapper instance to store
     */
    public void addOspfLsa(String lsaKey, LsaWrapper lsaWrapper);

    /**
     * Retrieves the LSA from the bin for verification of max age and ls refresh.
     *
     * @param lsaKey key to search the LSA
     * @return LSA Wrapper instance
     */
    public LsaWrapper ospfLsa(String lsaKey);

    /**
     * Removes the given LSA from the bin. when ever it reaches max age or ls refresh time.
     *
     * @param lsaKey     key to search LSA
     * @param lsaWrapper wrapper instance of the particular LSA
     */
    public void removeOspfLsa(String lsaKey, LsaWrapper lsaWrapper);

    /**
     * Gets the list of LSAs in this bin as key value pair.
     * with key being the LSA key formed from the LSA header.
     *
     * @return list of LSAs in this bin as key value pair
     */
    public Map<String, LsaWrapper> listOfLsa();

    /**
     * Gets the bin number assigned during the initialization process of the bins .
     *
     * @return the bin number
     */
    public int binNumber();
}