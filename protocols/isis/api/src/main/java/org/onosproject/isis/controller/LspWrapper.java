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
 * Representation of a LSP wrapper.
 */
public interface LspWrapper {

    /**
     * Returns bin number into which the LSP wrapper is put for aging process.
     *
     * @return bin number
     */
    int binNumber();

    /**
     * Sets bin number into which the LSP wrapper is put for aging process.
     *
     * @param binNumber bin number
     */
    void setBinNumber(int binNumber);

    /**
     * Checks the contained LSP is self originated or not.
     *
     * @return true if self originated else false
     */
    boolean isSelfOriginated();

    /**
     * Sets the contained LSP is self originated or not.
     *
     * @param selfOriginated true if self originated else false
     */
    void setSelfOriginated(boolean selfOriginated);

    /**
     * Returns the LSP type.
     *
     * @return LSP type
     */
    IsisPduType lspType();

    /**
     * Returns the LSPs remaining life time.
     *
     * @return LSPs remaining life time.
     */
    int remainingLifetime();

    /**
     * Returns the age counter value when LSP was received.
     *
     * @return age counter value when LSP was received
     */
    int ageCounterWhenReceived();

    /**
     * Returns the age counter roll over value when LSP was added to wrapper instance.
     *
     * @return age counter roll over value when LSP was added to wrapper instance
     */
    int ageCounterRollOverWhenAdded();

    /**
     * Returns the LSP instance stored in wrapper.
     *
     * @return LSP instance stored in wrapper
     */
    IsisMessage lsPdu();

    /**
     * Sets LSPs remaining life time.
     *
     * @param remainingLifetime LSPs remaining life time
     */
    void setRemainingLifetime(int remainingLifetime);

    /**
     * Returns the age of LSP when received.
     *
     * @return age of LSP when received
     */
    int lspAgeReceived();

    /**
     * Returns the LSP processing string.
     *
     * @return lsp processing value for switch case
     */
    String lspProcessing();

    /**
     * Returns ISIS interface instance.
     *
     * @return ISIS interface instance
     */
    IsisInterface isisInterface();

    /**
     * Returns the current LSP age.
     *
     * @return LSP age
     */
    int currentAge();

    /**
     * Sets the LSP processing string based on LSP to process.
     *
     * @param lspProcessing "refreshLsp" or "maxageLsp" based on LSP to process
     */
    void setLspProcessing(String lspProcessing);
}