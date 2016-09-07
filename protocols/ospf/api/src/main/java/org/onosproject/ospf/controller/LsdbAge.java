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
 * Representation of LSDB aging process.
 * The age of each LSA in the database must be incremented by 1 each second.
 * We put all the LSAs of a given age into a single bin. The age of an LSA is the
 * difference between its age bin and the bin representing LS age 0.
 */
public interface LsdbAge {

    /**
     * Adds LSA to bin for aging.
     *
     * @param binKey key to store the LSA in bin
     * @param lsaBin LSA bin instance
     */
    public void addLsaBin(Integer binKey, LsaBin lsaBin);

    /**
     * Gets LSA from bin, this method is used while processing ls refresh and max age on LSA.
     *
     * @param binKey key to retreive the LSA from bin
     * @return lsaBin bin instance
     */
    public LsaBin getLsaBin(Integer binKey);

    /**
     * Adds the lsa to maxAge bin if LSAs age is max age.
     *
     * @param key     key to store the LSA in bin.
     * @param wrapper wrapper instance which contains LSA
     */
    public void addLsaToMaxAgeBin(String key, LsaWrapper wrapper);

    /**
     * Gets the bin number out of LSAs age, in which the LSA can be placed.
     * so that age can be calculated.
     *
     * @param x Can be either age or ageCounter
     * @return bin number.
     */
    public int age2Bin(int x);

    /**
     * Gets the max age bin, a special bin is created which holds only max age LSAs.
     *
     * @return lsa bin instance
     */
    public LsaBin getMaxAgeBin();

    /**
     * Gets the age counter.
     *
     * @return age counter
     */
    public int getAgeCounter();


    /**
     * Refresh the LSAs which are in the refresh bin.
     */
    public void refreshLsa();

    /**
     * If the LSAs have completed the MaxAge stop aging and flood it.
     */
    public void maxAgeLsa();

    /**
     * Invoked every 1 second as part of the aging process, and increments age counter.
     * It also verifies if any LSA has reached ls refresh time or max age.
     */
    public void ageLsaAndFlood();

    /**
     * Starts the aging timer thread which gets invokes every second.
     */
    public void startDbAging();

    /**
     * Removes LSA from Bin, when ever it reaches a max age or ls refresh time.
     *
     * @param lsaWrapper wrapper instance
     */
    public void removeLsaFromBin(LsaWrapper lsaWrapper);

    /**
     * Gets the age counter roll over.
     *
     * @return the age counter roll over
     */
    public int getAgeCounterRollOver();
}