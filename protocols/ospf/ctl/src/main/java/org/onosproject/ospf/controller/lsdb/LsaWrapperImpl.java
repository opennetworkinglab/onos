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
import com.google.common.base.Objects;
import org.onosproject.ospf.controller.LsaWrapper;
import org.onosproject.ospf.controller.LsdbAge;
import org.onosproject.ospf.controller.OspfInterface;
import org.onosproject.ospf.controller.OspfLsa;
import org.onosproject.ospf.controller.OspfLsaType;
import org.onosproject.ospf.protocol.lsa.LsaHeader;
import org.onosproject.ospf.protocol.util.OspfParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wrapper object to store LSA and associated metadata information.
 */
public class LsaWrapperImpl implements LsaWrapper {
    private static final Logger log = LoggerFactory.getLogger(LsaWrapperImpl.class);
    private LsaHeader lsaHeader;
    private int lsaAgeReceived;
    private int ageCounterWhenReceived;
    private boolean isSelfOriginated;
    private OspfLsaType lsaType;
    private OspfLsa ospfLsa;
    private int noReTransmissionLists;
    private boolean inAnAgeBin;
    private boolean changedSinceLastFlood;
    private boolean isSequenceRollOver;
    private boolean sentReplyForOlderLsa;
    private boolean checkAge; // Queued for check sum verification
    private boolean isAging;
    private String lsaProcessing; //for LSAQueueConsumer processing
    private int binNumber = -1;
    private OspfInterface ospfInterface;
    private LsdbAge lsdbAge;
    private int ageCounterRollOverWhenAdded;

    public int getAgeCounterRollOverWhenAdded() {
        return ageCounterRollOverWhenAdded;
    }

    public void setAgeCounterRollOverWhenAdded(int ageCounterRollOverWhenAdded) {
        this.ageCounterRollOverWhenAdded = ageCounterRollOverWhenAdded;
    }

    /**
     * Gets the LSA type.
     *
     * @return LSA type
     */
    public OspfLsaType lsaType() {
        return lsaType;
    }

    /**
     * Sets the LSA type.
     *
     * @param lsaType LSA type
     */
    public void setLsaType(OspfLsaType lsaType) {
        this.lsaType = lsaType;
    }

    /**
     * Gets if self originated or not.
     *
     * @return true if self originated else false
     */
    public boolean isSelfOriginated() {
        return isSelfOriginated;
    }

    /**
     * Sets if self originated or not.
     *
     * @param isSelfOriginated true if self originated else false
     */
    public void setIsSelfOriginated(boolean isSelfOriginated) {
        this.isSelfOriginated = isSelfOriginated;
    }

    /**
     * Adds the LSA in the wrapper.
     *
     * @param lsaType LSA type
     * @param ospfLsa LSA instance
     */
    public void addLsa(OspfLsaType lsaType, OspfLsa ospfLsa) {
        this.lsaType = lsaType;
        this.lsaHeader = (LsaHeader) ospfLsa.lsaHeader();
        this.ospfLsa = ospfLsa;
    }

    /**
     * Age of LSA when received.
     *
     * @return Age of LSA when received
     */
    public int lsaAgeReceived() {
        return lsaAgeReceived;
    }

    /**
     * Sets the Age of LSA when received.
     *
     * @param lsaAgeReceived Age of LSA when received
     */
    public void setLsaAgeReceived(int lsaAgeReceived) {
        this.lsaAgeReceived = lsaAgeReceived;
    }

    /**
     * Gets the LSA header.
     *
     * @return LSA header instance
     */
    public LsaHeader lsaHeader() {
        lsaHeader.setAge(currentAge());
        return lsaHeader;
    }

    /**
     * Sets LSA header.
     *
     * @param lsaHeader LSA header
     */
    public void setLsaHeader(LsaHeader lsaHeader) {
        this.lsaHeader = lsaHeader;
    }

    /**
     * Gets the LSA.
     *
     * @return LSA instance
     */
    public OspfLsa ospfLsa() {
        LsaHeader lsaHeader = (LsaHeader) ospfLsa;
        lsaHeader.setAge(currentAge());

        return lsaHeader;
    }

    /**
     * Sets the LSA.
     *
     * @param ospfLsa LSA instance
     */
    public void setOspfLsa(OspfLsa ospfLsa) {
        this.ospfLsa = ospfLsa;
    }

    /**
     * Gets number of LSAs in retransmission list.
     *
     * @return number of LSAs in retransmission list
     */
    public int noReTransmissionLists() {
        return noReTransmissionLists;
    }

    /**
     * Sets number of LSAs in retransmission list.
     *
     * @param noReTransmissionLists number of LSAs in retransmission list
     */
    public void setNoReTransmissionLists(int noReTransmissionLists) {
        this.noReTransmissionLists = noReTransmissionLists;
    }

    /**
     * whether LSA in age bin or not.
     *
     * @return true if LSA in age bin else false
     */
    public boolean isInAnAgeBin() {
        return inAnAgeBin;
    }

    /**
     * Sets whether LSA in age bin or not.
     *
     * @param inAnAgeBin whether LSA in age bin or not
     */
    public void setInAnAgeBin(boolean inAnAgeBin) {
        this.inAnAgeBin = inAnAgeBin;
    }

    /**
     * Gets if LSA is changed since last flood.
     *
     * @return true if LSA is changed since last flood else false
     */
    public boolean isChangedSinceLastFlood() {
        return changedSinceLastFlood;
    }

    /**
     * Sets if LSA is changed since last flood.
     *
     * @param changedSinceLastFlood true if LSA is changed since last flood else false
     */
    public void setChangedSinceLastFlood(boolean changedSinceLastFlood) {
        this.changedSinceLastFlood = changedSinceLastFlood;
    }

    /**
     * Gets if sequence number rolled over.
     *
     * @return true if sequence rolled over else false.
     */
    public boolean isSequenceRollOver() {
        return isSequenceRollOver;
    }

    /**
     * Sets if sequence number rolled over.
     *
     * @param isSequenceRollOver true if sequence rolled over else false
     */
    public void setIsSequenceRollOver(boolean isSequenceRollOver) {
        this.isSequenceRollOver = isSequenceRollOver;
    }

    /**
     * Gets is sent reply for older LSA.
     *
     * @return true if sent reply for old LSA else false
     */
    public boolean isSentReplyForOlderLsa() {
        return sentReplyForOlderLsa;
    }

    /**
     * Sets is sent reply for older lsa.
     *
     * @param sentReplyForOlderLsa true if sent reply for older lsa else false
     */
    public void setSentReplyForOlderLsa(boolean sentReplyForOlderLsa) {
        this.sentReplyForOlderLsa = sentReplyForOlderLsa;
    }

    /**
     * Gets check age flag.
     *
     * @return true check age flag is set else false
     */
    public boolean isCheckAge() {
        return checkAge;
    }

    /**
     * Sets check age flag.
     *
     * @param checkAge check age flag.
     */
    public void setCheckAge(boolean checkAge) {
        this.checkAge = checkAge;
    }

    /**
     * Gets value of aging flag.
     *
     * @return is aging flag
     */
    public boolean isAging() {
        return isAging;
    }

    /**
     * Sets aging flag.
     *
     * @param isAging is aging flag
     */
    public void setIsAging(boolean isAging) {
        this.isAging = isAging;
    }

    /**
     * Gets the LSDB age.
     *
     * @return LSDB age
     */
    public LsdbAge getLsdbAge() {
        return lsdbAge;
    }

    /**
     * Sets the LSDB age.
     *
     * @param lsdbAge LSDB age
     */
    public void setLsdbAge(LsdbAge lsdbAge) {
        this.lsdbAge = lsdbAge;
    }

    /**
     * Gets the current LSA Age.
     *
     * @return LSA age
     */
    public int currentAge() {

        int currentAge = 0;
        //ls age received
        if (lsdbAge.getAgeCounter() >= ageCounterWhenReceived) {
            currentAge = lsaAgeReceived + (lsdbAge.getAgeCounter() - ageCounterWhenReceived);
        } else {
            currentAge = lsaAgeReceived + ((OspfParameters.MAXAGE + lsdbAge.getAgeCounter())
                    - ageCounterWhenReceived);
        }

        if (currentAge >= OspfParameters.MAXAGE) {
            return OspfParameters.MAXAGE;
        } else if ((currentAge == lsaAgeReceived) && ageCounterRollOverWhenAdded != lsdbAge.getAgeCounterRollOver()) {
            return OspfParameters.MAXAGE;
        }

        return currentAge;
    }

    /**
     * Gets the age counter when received.
     *
     * @return the age counter when received
     */
    public int ageCounterWhenReceived() {
        return ageCounterWhenReceived;
    }

    /**
     * Sets the age counter when received.
     *
     * @param ageCounterWhenReceived the age counter when received
     */
    public void setAgeCounterWhenReceived(int ageCounterWhenReceived) {
        this.ageCounterWhenReceived = ageCounterWhenReceived;
    }

    /**
     * Gets the LSA process command.
     *
     * @return LSA process command
     */
    public String lsaProcessing() {
        return lsaProcessing;
    }

    /**
     * Sets the LSA process command.
     *
     * @param lsaProcessing LSA process command
     */
    public void setLsaProcessing(String lsaProcessing) {
        this.lsaProcessing = lsaProcessing;
    }

    /**
     * Gets bin number.
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
     * Get the OSPF interface.
     *
     * @return the OSPF interface.
     */
    public OspfInterface ospfInterface() {
        return ospfInterface;
    }

    /**
     * Sets the OSPF interface.
     *
     * @param ospfInterface OSPF interface instance
     */
    @Override
    public void setOspfInterface(OspfInterface ospfInterface) {
        this.ospfInterface = ospfInterface;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .omitNullValues()
                .add("lsaAgeReceived", lsaAgeReceived)
                .add("ageCounterWhenReceived", ageCounterWhenReceived)
                .add("isSelfOriginated", isSelfOriginated)
                .add("lsaHeader", lsaHeader)
                .add("lsaType", lsaType)
                .add("ospflsa", ospfLsa)
                .add("noReTransmissionLists", noReTransmissionLists)
                .add("inAnAgeBin", inAnAgeBin)
                .add("changedSinceLasFlood", changedSinceLastFlood)
                .add("isSequenceRollOver", isSequenceRollOver)
                .add("sentReplyForOlderLSA", sentReplyForOlderLsa)
                .add("checkAge", checkAge)
                .add("isAging", isAging)
                .add("lsaProcessing", lsaProcessing)
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        LsaWrapperImpl that = (LsaWrapperImpl) o;
        return Objects.equal(lsaAgeReceived, that.lsaAgeReceived) &&
                Objects.equal(ageCounterWhenReceived, that.ageCounterWhenReceived) &&
                Objects.equal(isSelfOriginated, that.isSelfOriginated) &&
                Objects.equal(lsaHeader, that.lsaHeader) &&
                Objects.equal(ospfLsa, that.ospfLsa) &&
                Objects.equal(noReTransmissionLists, that.noReTransmissionLists) &&
                Objects.equal(inAnAgeBin, that.inAnAgeBin) &&
                Objects.equal(changedSinceLastFlood, that.changedSinceLastFlood) &&
                Objects.equal(isSequenceRollOver, that.isSequenceRollOver) &&
                Objects.equal(sentReplyForOlderLsa, that.sentReplyForOlderLsa) &&
                Objects.equal(checkAge, that.checkAge) &&
                Objects.equal(isAging, that.isAging) &&
                Objects.equal(lsaProcessing, that.lsaProcessing);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(lsaAgeReceived, lsaAgeReceived, ageCounterWhenReceived, isSelfOriginated,
                                lsaHeader, lsaType, ospfLsa, noReTransmissionLists, inAnAgeBin,
                                changedSinceLastFlood, isSequenceRollOver, sentReplyForOlderLsa,
                                checkAge, isAging, lsaProcessing);
    }
}