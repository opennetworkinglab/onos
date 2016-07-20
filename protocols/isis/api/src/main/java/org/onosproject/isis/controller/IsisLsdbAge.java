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

/**
 * Representation of an ISIS link state database aging.
 */
public interface IsisLsdbAge {

    /**
     * Starts the aging timer thread which gets invokes every second.
     */
    void startDbAging();

    /**
     * Returns the age counter.
     *
     * @return age counter
     */
    int ageCounter();

    /**
     * Returns the age counter rollover.
     *
     * @return age counter rollover
     */
    int ageCounterRollOver();

    /**
     * Returns the bin number.
     *
     * @param x can be either age or ageCounter
     * @return bin number
     */
    int age2Bin(int x);

    /**
     * Returns the LSP bin instance.
     *
     * @param binKey key to search
     * @return LSP bin instance
     */
    IsisLspBin getLspBin(int binKey);

    /**
     * Adds LSP to bin.
     *
     * @param binNumber key to store in bin
     * @param lspBin    LSP bin instance
     */
    void addLspBin(int binNumber, IsisLspBin lspBin);

    /**
     * Removes LSP from bin.
     *
     * @param lspWrapper LSP wrapper instance
     */
    void removeLspFromBin(LspWrapper lspWrapper);
}