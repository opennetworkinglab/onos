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

import org.onosproject.isis.controller.IsisLsdbAge;
import org.onosproject.isis.controller.IsisLspBin;
import org.onosproject.isis.controller.LspWrapper;
import org.onosproject.isis.io.isispacket.pdu.LsPdu;
import org.onosproject.isis.io.util.IsisConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Representation of ISIS link state database ageing process.
 */
public class DefaultIsisLsdbAge implements IsisLsdbAge {
    private static final Logger log = LoggerFactory.getLogger(DefaultIsisLsdbAge.class);
    protected int ageCounter = 0;
    private InternalAgeTimer dbAgeTimer;
    private ScheduledExecutorService exServiceage;
    private Integer maxBins = IsisConstants.LSPMAXAGE;
    private Map<Integer, IsisLspBin> ageBins = new ConcurrentHashMap<>(maxBins);
    private int ageCounterRollOver = 0;
    private IsisLspQueueConsumer queueConsumer = null;
    private BlockingQueue<LspWrapper> lsaQueue = new ArrayBlockingQueue<>(1024);
    private boolean timerStarted = false;

    /**
     * Creates an instance of LSDB age.
     */
    public DefaultIsisLsdbAge() {
        // create LSBin's in the HashMap.
        for (int i = 0; i < maxBins; i++) {
            IsisLspBin lspBin = new DefaultIsisLspBin(i);
            ageBins.put(i, lspBin);
        }
    }

    /**
     * Returns age counter.
     *
     * @return age counter
     */
    public int ageCounter() {
        return ageCounter;
    }

    /**
     * Returns age counter roll over.
     *
     * @return age counter roll over
     */
    public int ageCounterRollOver() {

        return ageCounterRollOver;
    }

    /**
     * Adds LSP to LS bin for ageing.
     *
     * @param binNumber key to store in bin
     * @param lspBin    LSP bin instance
     */
    public void addLspBin(int binNumber, IsisLspBin lspBin) {
        if (!ageBins.containsKey(binNumber)) {
            ageBins.put(binNumber, lspBin);
        }
    }

    /**
     * Returns LSP from Bin.
     *
     * @param binKey key
     * @return bin instance
     */
    public IsisLspBin getLspBin(int binKey) {

        return ageBins.get(binKey);
    }

    /**
     * Removes LSP from Bin.
     *
     * @param lspWrapper wrapper instance
     */
    public void removeLspFromBin(LspWrapper lspWrapper) {
        if (ageBins.containsKey(lspWrapper.binNumber())) {
            IsisLspBin lsaBin = ageBins.get(lspWrapper.binNumber());
            lsaBin.removeIsisLsp(((LsPdu) lspWrapper.lsPdu()).lspId(), lspWrapper);
        }
    }

    /**
     * Returns the bin number.
     *
     * @param age Can be either age or ageCounter
     * @return bin number.
     */
    public int age2Bin(int age) {
        if (age <= ageCounter) {
            return (ageCounter - age);
        } else {
            return ((IsisConstants.LSPMAXAGE - 1) + (ageCounter - age));
        }
    }

    /**
     * Starts the aging timer and queue consumer.
     */
    public void startDbAging() {
        if (!timerStarted) {
            startDbAgeTimer();
            queueConsumer = new IsisLspQueueConsumer(lsaQueue);
            new Thread(queueConsumer).start();
            timerStarted = true;
        }
    }

    /**
     * Starts DB aging task.
     */
    private void startDbAgeTimer() {
        dbAgeTimer = new InternalAgeTimer();
        //from 1 sec
        exServiceage = Executors.newSingleThreadScheduledExecutor();
        exServiceage.scheduleAtFixedRate(dbAgeTimer, 1, 1, TimeUnit.SECONDS);
    }

    /**
     * Gets called every second as part of the aging process.
     */
    public void ageLsp() {
        refreshLsa();
        maxAgeLsa();

        if (ageCounter == IsisConstants.LSPMAXAGE) {
            ageCounter = 0;
            ageCounterRollOver++;
        } else {
            ageCounter++;
        }
    }

    /**
     * If the LSP have completed the MaxAge - they are moved called stop aging.
     */
    public void maxAgeLsa() {
        if (ageCounter == 0) {
            return;
        }
        //Get from Age Bins
        IsisLspBin lspBin = ageBins.get(ageCounter - 1);
        if (lspBin == null) {
            return;
        }
        Map lspBinMap = lspBin.listOfLsp();
        for (Object key : lspBinMap.keySet()) {
            LspWrapper lspWrapper = (LspWrapper) lspBinMap.get((String) key);
            if (lspWrapper.currentAge() == IsisConstants.LSPMAXAGE) {
                lspWrapper.setLspProcessing(IsisConstants.MAXAGELSP);
                log.debug("Lsp picked for maxage removal. Age Counter: {}, AgeCounterRollover: {}, " +
                                  "AgeCounterRollover WhenAddedToDb: {}, LSA Type: {}, LSA Key: {}",
                          ageCounter, ageCounterRollOver, lspWrapper.currentAge(),
                          lspWrapper.lsPdu().isisPduType(), key);
                //add it to lspQueue for processing
                try {
                    lsaQueue.put(lspWrapper);
                    //remove from bin
                    lspBin.removeIsisLsp((String) key, lspWrapper);
                } catch (InterruptedException e) {
                    log.debug("Error::LSDBAge::maxAgeLsp::{}", e.getMessage());
                }
            }
        }

    }

    /*
     * If the LSP is in age bin of 900s- it's pushed into refresh list.
     */
    public void refreshLsa() {
        int binNumber;
        if (ageCounter < IsisConstants.LSPREFRESH) {
            binNumber = ageCounter + IsisConstants.LSPREFRESH;
        } else {
            binNumber = ageCounter - IsisConstants.LSPREFRESH;
        }
        if (binNumber >= IsisConstants.LSPMAXAGE) {
            binNumber = binNumber - IsisConstants.LSPMAXAGE;
        }
        IsisLspBin lspBin = ageBins.get(binNumber);
        if (lspBin == null) {
            return;
        }
        Map lspBinMap = lspBin.listOfLsp();
        for (Object key : lspBinMap.keySet()) {
            LspWrapper lsp = (LspWrapper) lspBinMap.get((String) key);
            try {
                if (lsp.isSelfOriginated()) {
                    log.debug("Lsp picked for refreshLsp. binNumber: {}, LSA Type: {}, LSA Key: {}",
                              binNumber, lsp.lspType(), key);
                    lsp.setLspProcessing(IsisConstants.REFRESHLSP);
                    lsaQueue.put(lsp);
                    //remove from bin
                    lspBin.removeIsisLsp((String) key, lsp);
                }
            } catch (InterruptedException e) {
                log.debug("Error::LSDBAge::refreshLsp::{}", e.getMessage());
            }
        }
    }

    /**
     * Runnable task which runs every second and calls aging process.
     */
    private class InternalAgeTimer implements Runnable {

        /**
         * Creates an instance of age timer task.
         */
        InternalAgeTimer() {
            log.debug("Starts::IsisLsdbAge::AgeTimer...!!! ");
        }

        @Override
        public void run() {
            ageLsp();
        }
    }
}