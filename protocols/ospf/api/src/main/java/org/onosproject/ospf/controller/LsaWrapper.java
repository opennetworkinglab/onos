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

/**
 * Representation of a wrapper object to store LSA and associated metadata.
 * Metadata consists about the origination of LSA, age of LSA when received etc.
 */
public interface LsaWrapper {
    /**
     * Gets the type of LSA, it can be a router,network,summary,external.
     *
     * @return lsa type
     */
    public OspfLsaType lsaType();

    /**
     * Sets the LSA type during the initialization of wrapper.
     *
     * @param lsaType lsa type
     */
    public void setLsaType(OspfLsaType lsaType);

    /**
     * Determines the origination of LSA , this is called during ls refresh interval.
     *
     * @return true if self originated else false
     */
    public boolean isSelfOriginated();

    /**
     * Sets is self originated or not.
     *
     * @param isSelfOriginated true if self originated else false
     */
    public void setIsSelfOriginated(boolean isSelfOriginated);


    /**
     * Age of LSA when received during the adjacency formation.
     *
     * @return Age of LSA when received
     */
    public int lsaAgeReceived();

    /**
     * Sets the Age of LSA when received during the adjacency formation.
     *
     * @param lsaAgeReceived Age of LSA when received
     */
    public void setLsaAgeReceived(int lsaAgeReceived);

    /**
     * Gets the LSA present in the wrapper instance.
     *
     * @return LSA instance
     */
    public OspfLsa ospfLsa();

    /**
     * Sets the LSA instance to the wrapper.
     *
     * @param ospfLsa LSA instance
     */
    public void setOspfLsa(OspfLsa ospfLsa);

    /**
     * Gets the current LSA Age, using this we calculate current age.
     * It is done against the age counter which is incremented every second.
     *
     * @return lsa age
     */
    public int currentAge();

    /**
     * Gets the age counter when received.
     *
     * @return the age counter when received
     */
    public int ageCounterWhenReceived();

    /**
     * Sets the age counter when received.
     *
     * @param ageCounterWhenReceived the age counter when received
     */
    public void setAgeCounterWhenReceived(int ageCounterWhenReceived);

    /**
     * Gets the LSA process command, like max age, ls refresh, based on the command set.
     * The queue consumer will pick the LSA and start performing the actions, like flooding
     * out of the domain or generating a new LSA and flooding.
     *
     * @return lsa process command
     */
    public String lsaProcessing();

    /**
     * Sets the LSA process command, like max age , ls refresh , based on the command set.
     * The queue consumer will pick the LSA and start performing the actions, like flooding
     * out of the domain or generating a new LSA and flooding.
     *
     * @param lsaProcessing lsa process command
     */
    public void setLsaProcessing(String lsaProcessing);

    /**
     * Gets bin number into which the LSA wrapper is put for aging process.
     *
     * @return bin number
     */
    public int binNumber();

    /**
     * Sets bin number into which the LSA wrapper is put for aging process.
     *
     * @param binNumber bin number
     */
    public void setBinNumber(int binNumber);

    /**
     * Gets the interface on which the LSA was received.
     *
     * @return the interface instance
     */
    public OspfInterface ospfInterface();

    /**
     * Sets the interface on which the LSA was received, this is used later to flood the information.
     *
     * @param ospfInterface interface instance
     */
    public void setOspfInterface(OspfInterface ospfInterface);

    /**
     * Sets the LSDB age.
     * Using LSDB age we are calculating age of a particular LSA.
     *
     * @param lsdbAge lsdbAge instance
     */
    public void setLsdbAge(LsdbAge lsdbAge);
}