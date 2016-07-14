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

import java.util.List;

/**
 * Represents an OSPF link state database.
 */
public interface OspfLsdb {

    /**
     * Initializes the link state database.
     */
    public void initializeDb();

    /**
     * Gets all LSA headers.
     *
     * @param excludeMaxAgeLsa exclude the max age LSAs
     * @param isOpaqueCapable  is opaque capable or not
     * @return List of LSA headers
     */
    public List getAllLsaHeaders(boolean excludeMaxAgeLsa, boolean isOpaqueCapable);

    /**
     * Finds the LSA from appropriate LSA maps.
     *
     * @param lsType type of LSA
     * @param lsaKey key
     * @return LSA wrapper object
     */
    public LsaWrapper findLsa(int lsType, String lsaKey);

    /**
     * Adds the LSA to maxAge bin.
     *
     * @param key        key
     * @param lsaWrapper LSA wrapper instance
     */
    public void addLsaToMaxAgeBin(String key, Object lsaWrapper);

    /**
     * Removes LSA from bin.
     *
     * @param lsaWrapper LSA wrapper instance
     */
    public void removeLsaFromBin(Object lsaWrapper);
}