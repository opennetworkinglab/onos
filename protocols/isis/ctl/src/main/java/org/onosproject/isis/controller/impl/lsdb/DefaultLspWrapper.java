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

import org.onosproject.isis.controller.IsisInterface;
import org.onosproject.isis.controller.IsisLsdbAge;
import org.onosproject.isis.controller.IsisPduType;
import org.onosproject.isis.controller.LspWrapper;
import org.onosproject.isis.io.isispacket.pdu.LsPdu;
import org.onosproject.isis.io.util.IsisConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Representation of LSP wrapper where the LSPs are stored with metadata.
 */
public class DefaultLspWrapper implements LspWrapper {
    private static final Logger log = LoggerFactory.getLogger(DefaultLspWrapper.class);
    private int binNumber = -1;
    private boolean selfOriginated = false;
    private IsisPduType lspType;
    private int lspAgeReceived;
    private int ageCounterWhenReceived;
    private LsPdu lsPdu;
    private IsisLsdbAge lsdbAge;
    private int ageCounterRollOverWhenAdded;
    private int remainingLifetime;
    private IsisInterface isisInterface;
    private String lspProcessing;

    /**
     * Returns "refreshLsp" or "maxageLsp" based on LSP to process.
     *
     * @return LSP processing string
     */
    public String lspProcessing() {
        return lspProcessing;
    }

    /**
     * Sets LSP processing "refreshLsp" or "maxageLsp" based on LSP to process.
     *
     * @param lspProcessing "refreshLsp" or "maxageLsp" based on LSP to process
     */
    public void setLspProcessing(String lspProcessing) {
        this.lspProcessing = lspProcessing;
    }

    /**
     * Returns LSP age received.
     *
     * @return LSP age received
     */
    public int lspAgeReceived() {
        return lspAgeReceived;
    }

    /**
     * Sets LSP age received.
     *
     * @param lspAgeReceived LSP age received.
     */
    public void setLspAgeReceived(int lspAgeReceived) {
        this.lspAgeReceived = lspAgeReceived;
    }

    /**
     * Returns ISIS interface instance.
     *
     * @return ISIS interface instance
     */
    public IsisInterface isisInterface() {
        return isisInterface;
    }

    /**
     * Sets ISIS interface.
     *
     * @param isisInterface ISIS interface instance
     */
    public void setIsisInterface(IsisInterface isisInterface) {
        this.isisInterface = isisInterface;
    }

    /**
     * Returns age counter when received.
     *
     * @return age counter when received
     */
    public int ageCounterWhenReceived() {

        return ageCounterWhenReceived;
    }

    /**
     * Sets age counter when received.
     *
     * @param ageCounterWhenReceived age counter when received
     */
    public void setAgeCounterWhenReceived(int ageCounterWhenReceived) {
        this.ageCounterWhenReceived = ageCounterWhenReceived;
    }

    /**
     * Returns age counter roll over.
     *
     * @return age counter roll over
     */
    public int ageCounterRollOverWhenAdded() {
        return ageCounterRollOverWhenAdded;
    }

    /**
     * Sets age counter roll over when added.
     *
     * @param ageCounterRollOverWhenAdded age counter roll over when added
     */
    public void setAgeCounterRollOverWhenAdded(int ageCounterRollOverWhenAdded) {
        this.ageCounterRollOverWhenAdded = ageCounterRollOverWhenAdded;
    }

    /**
     * Returns bin number.
     *
     * @return bin number
     */
    public int binNumber() {
        return binNumber;
    }

    /**
     * Sets bin number.
     *
     * @param binNumber bin number
     */
    public void setBinNumber(int binNumber) {
        this.binNumber = binNumber;
    }

    /**
     * Returns true if self originated.
     *
     * @return true if self originated.
     */
    public boolean isSelfOriginated() {
        return selfOriginated;
    }

    /**
     * Sets true if self originated.
     *
     * @param selfOriginated true if self originated else false
     */
    public void setSelfOriginated(boolean selfOriginated) {
        this.selfOriginated = selfOriginated;
    }

    /**
     * Returns ISIS PDU type.
     *
     * @return ISIS PDU type
     */
    public IsisPduType lspType() {
        return lspType;
    }

    /**
     * Sets ISIS PDU type.
     *
     * @param lspType ISIS PDU type
     */
    public void setLspType(IsisPduType lspType) {
        this.lspType = lspType;
    }

    /**
     * Returns LSPDU which the wrapper contains.
     *
     * @return LSPDU which the wrapper contains
     */
    public LsPdu lsPdu() {
        return lsPdu;
    }

    /**
     * Sets LSPDU which the wrapper contains.
     *
     * @param lsPdu LSPDU which the wrapper contains
     */
    public void setLsPdu(LsPdu lsPdu) {
        this.lsPdu = lsPdu;
    }

    /**
     * Returns ISIS LSDB age.
     *
     * @return ISIS LSDB age
     */
    public IsisLsdbAge lsdbAge() {
        return lsdbAge;
    }

    /**
     * Sets LSDB age.
     *
     * @param lsdbAge LSDB age
     */
    public void setLsdbAge(IsisLsdbAge lsdbAge) {
        this.lsdbAge = lsdbAge;
    }

    /**
     * Returns the current LSP Age.
     *
     * @return LSP age
     */
    public int currentAge() {

        int currentAge = 0;
        //ls age received
        if (lsdbAge.ageCounter() >= ageCounterWhenReceived) {
            if (!selfOriginated) {
                if (ageCounterRollOverWhenAdded == lsdbAge.ageCounterRollOver()) {
            currentAge = lspAgeReceived + (lsdbAge.ageCounter() - ageCounterWhenReceived);
                } else {
                    return IsisConstants.LSPMAXAGE;
                }
            } else {
                currentAge = lspAgeReceived + (lsdbAge.ageCounter() - ageCounterWhenReceived);
            }
        } else {
            currentAge = lspAgeReceived + ((IsisConstants.LSPMAXAGE + lsdbAge.ageCounter())
                    - ageCounterWhenReceived);
        }

        if (currentAge >= IsisConstants.LSPMAXAGE) {
            return IsisConstants.LSPMAXAGE;
        } else if ((currentAge == lspAgeReceived) && ageCounterRollOverWhenAdded
                != lsdbAge.ageCounterRollOver()) {
            return IsisConstants.LSPMAXAGE;
        }

        return currentAge;
    }



    /**
     * Returns remaining time.
     *
     * @return remaining time
     */
    public int remainingLifetime() {
        //Calculate the remaining lifetime
        remainingLifetime = IsisConstants.LSPMAXAGE - currentAge();
        return remainingLifetime;
    }

    /**
     * Sets remaining life time.
     *
     * @param remainingLifetime LSPs remaining life time
     */
    public void setRemainingLifetime(int remainingLifetime) {
        this.remainingLifetime = remainingLifetime;
    }
}