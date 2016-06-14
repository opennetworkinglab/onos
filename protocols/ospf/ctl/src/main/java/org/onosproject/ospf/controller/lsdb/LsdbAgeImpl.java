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

import com.google.common.base.Objects;
import org.jboss.netty.channel.Channel;
import org.onosproject.ospf.controller.LsaBin;
import org.onosproject.ospf.controller.LsaWrapper;
import org.onosproject.ospf.controller.LsdbAge;
import org.onosproject.ospf.controller.OspfArea;
import org.onosproject.ospf.controller.area.OspfAreaImpl;
import org.onosproject.ospf.protocol.util.OspfParameters;
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
 * Representation of LSDB Aging process.
 */
public class LsdbAgeImpl implements LsdbAge {

    private static final Logger log =
            LoggerFactory.getLogger(LsdbAgeImpl.class);
    protected static int ageCounter = 0;
    private InternalAgeTimer dbAgeTimer;
    private ScheduledExecutorService exServiceage;
    // creating age bins of MAXAGE
    private Map<Integer, LsaBin> ageBins = new ConcurrentHashMap<>(OspfParameters.MAXAGE);
    private LsaBin maxAgeBin = new LsaBinImpl(OspfParameters.MAXAGE);
    private int ageCounterRollOver = 0;
    private Channel channel = null;
    private LsaQueueConsumer queueConsumer = null;
    private BlockingQueue<LsaWrapper> lsaQueue = new ArrayBlockingQueue(1024);
    private OspfArea ospfArea = null;


    /**
     * Creates an instance of LSDB age.
     *
     * @param ospfArea OSPF area instance
     */
    public LsdbAgeImpl(OspfArea ospfArea) {
        // create LSBin's in the HashMap.
        for (int i = 0; i < OspfParameters.MAXAGE; i++) {
            LsaBin lsaBin = new LsaBinImpl(i);
            ageBins.put(i, lsaBin);
        }
        this.ospfArea = ospfArea;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        LsdbAgeImpl that = (LsdbAgeImpl) o;
        return Objects.equal(ageBins, that.ageBins) &&
                Objects.equal(ageCounter, that.ageCounter) &&
                Objects.equal(ageCounterRollOver, that.ageCounterRollOver) &&
                Objects.equal(lsaQueue, lsaQueue);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(ageBins, ageCounter, ageCounterRollOver, lsaQueue);
    }

    /**
     * Adds LSA to bin.
     *
     * @param binKey key to store in bin
     * @param lsaBin LSA bin instance
     */
    public void addLsaBin(Integer binKey, LsaBin lsaBin) {
        if (!ageBins.containsKey(binKey)) {
            ageBins.put(binKey, lsaBin);
        }
    }

    /**
     * Gets LSA from Bin.
     *
     * @param binKey key
     * @return bin instance
     */
    public LsaBin getLsaBin(Integer binKey) {

        return ageBins.get(binKey);
    }

    /**
     * Adds the LSA to maxAge bin.
     *
     * @param key     key
     * @param wrapper wrapper instance
     */
    public void addLsaToMaxAgeBin(String key, LsaWrapper wrapper) {
        maxAgeBin.addOspfLsa(key, wrapper);
    }

    /**
     * Removes LSA from Bin.
     *
     * @param lsaWrapper wrapper instance
     */
    public void removeLsaFromBin(LsaWrapper lsaWrapper) {
        if (ageBins.containsKey(lsaWrapper.binNumber())) {
            LsaBin lsaBin = ageBins.get(lsaWrapper.binNumber());
            lsaBin.removeOspfLsa(((OspfAreaImpl) ospfArea).getLsaKey(((LsaWrapperImpl)
                    lsaWrapper).lsaHeader()), lsaWrapper);
        }
    }

    /**
     * Starts the aging timer and queue consumer.
     */
    public void startDbAging() {
        startDbAgeTimer();
        queueConsumer = new LsaQueueConsumer(lsaQueue, channel, ospfArea);
        new Thread(queueConsumer).start();
    }


    /**
     * Gets called every 1 second as part of the timer.
     */
    public void ageLsaAndFlood() {
        //every 5 mins checksum validation
        checkAges();
        //every 30 mins - flood LSA
        refreshLsa();
        //every 60 mins - flood LSA
        maxAgeLsa();

        if (ageCounter == OspfParameters.MAXAGE) {
            ageCounter = 0;
            ageCounterRollOver++;
        } else {
            //increment age bin
            ageCounter++;
        }
    }

    /**
     * If the LSA have completed the MaxAge - they are moved called stop aging and flooded.
     */
    public void maxAgeLsa() {
        if (ageCounter == 0) {
            return;
        }
        //Get from Age Bins
        LsaBin lsaBin = ageBins.get(ageCounter - 1);
        if (lsaBin == null) {
            return;
        }
        Map lsaBinMap = lsaBin.listOfLsa();
        for (Object key : lsaBinMap.keySet()) {
            LsaWrapper lsa = (LsaWrapper) lsaBinMap.get((String) key);
            if (lsa.currentAge() == OspfParameters.MAXAGE) {
                lsa.setLsaProcessing(OspfParameters.MAXAGELSA);
                log.debug("Lsa picked for maxage flooding. Age Counter: {}, AgeCounterRollover: {}, " +
                                  "AgeCounterRollover WhenAddedToDb: {}, LSA Type: {}, LSA Key: {}",
                          ageCounter, ageCounterRollOver, lsa.currentAge(), lsa.lsaType(), key);
                //add it to lsaQueue for processing
                try {
                    lsaQueue.put(lsa);
                    //remove from bin
                    lsaBin.removeOspfLsa((String) key, lsa);
                } catch (InterruptedException e) {
                    log.debug("Error::LSDBAge::maxAgeLsa::{}", e.getMessage());
                }
            }
        }

        //Get from maxAgeBin
        Map lsaMaxAgeBinMap = maxAgeBin.listOfLsa();
        for (Object key : lsaMaxAgeBinMap.keySet()) {
            LsaWrapper lsa = (LsaWrapper) lsaMaxAgeBinMap.get((String) key);
            lsa.setLsaProcessing(OspfParameters.MAXAGELSA);
            log.debug("Lsa picked for maxage flooding. Age Counter: {}, LSA Type: {}, LSA Key: {}",
                      ageCounter, lsa.lsaType(), key);
            //add it to lsaQueue for processing
            try {
                lsaQueue.put(lsa);
                //remove from bin
                maxAgeBin.removeOspfLsa((String) key, lsa);
            } catch (InterruptedException e) {
                log.debug("Error::LSDBAge::maxAgeLsa::{}", e.getMessage());
            }
        }
    }


    /*
     * If the LSA is in age bin of 1800 - it's pushed into refresh list.
     */
    public void refreshLsa() {
        int binNumber;
        if (ageCounter < OspfParameters.LSREFRESHTIME) {
            binNumber = ageCounter + OspfParameters.LSREFRESHTIME;
        } else {
            binNumber = ageCounter - OspfParameters.LSREFRESHTIME;
        }
        LsaBin lsaBin = ageBins.get(binNumber);
        if (lsaBin == null) {
            return;
        }
        Map lsaBinMap = lsaBin.listOfLsa();
        for (Object key : lsaBinMap.keySet()) {
            LsaWrapper lsa = (LsaWrapper) lsaBinMap.get((String) key);
            try {
                if (lsa.isSelfOriginated()) {
                    log.debug("Lsa picked for refreshLsa. binNumber: {}, LSA Type: {}, LSA Key: {}",
                              binNumber, lsa.lsaType(), key);
                    lsa.setLsaProcessing(OspfParameters.REFRESHLSA);
                    lsaQueue.put(lsa);
                    //remove from bin
                    lsaBin.removeOspfLsa((String) key, lsa);
                }
            } catch (InterruptedException e) {
                log.debug("Error::LSDBAge::refreshLsa::{}", e.getMessage());
            }
        }
    }

    /**
     * Verify the checksum for the LSAs who are in bins of 300 and it's multiples.
     */
    public void checkAges() {
        //evry 5 min age counter + multiples of 300
        for (int age = OspfParameters.CHECKAGE; age < OspfParameters.MAXAGE;
             age += OspfParameters.CHECKAGE) {
            LsaBin lsaBin = ageBins.get(age2Bin(age));
            if (lsaBin == null) {
                continue;
            }
            Map lsaBinMap = lsaBin.listOfLsa();
            for (Object key : lsaBinMap.keySet()) {
                LsaWrapper lsa = (LsaWrapper) lsaBinMap.get((String) key);
                lsa.setLsaProcessing(OspfParameters.VERIFYCHECKSUM);
                try {
                    lsaQueue.put(lsa);
                } catch (InterruptedException e) {
                    log.debug("Error::LSDBAge::checkAges::{}", e.getMessage());
                }
            }
        }
    }


    /**
     * Starts DB age timer method start the aging task.
     */
    private void startDbAgeTimer() {
        log.debug("OSPFNbr::startWaitTimer");
        dbAgeTimer = new InternalAgeTimer();
        //from 1 sec
        exServiceage = Executors.newSingleThreadScheduledExecutor();
        exServiceage.scheduleAtFixedRate(dbAgeTimer, OspfParameters.AGECOUNTER,
                                         OspfParameters.AGECOUNTER, TimeUnit.SECONDS);
    }

    /**
     * Stops the aging task.
     */
    private void stopDbAgeTimer() {
        log.debug("OSPFNbr::stopWaitTimer ");
        exServiceage.shutdown();
    }


    /**
     * Gets the netty channel.
     *
     * @return netty channel
     */
    public Channel getChannel() {
        return channel;
    }

    /**
     * Sets the netty channel.
     *
     * @param channel netty channel
     */
    public void setChannel(Channel channel) {

        this.channel = channel;
        if (queueConsumer != null) {
            queueConsumer.setChannel(channel);
        }
    }

    /**
     * Gets the age counter.
     *
     * @return ageCounter
     */
    public int getAgeCounter() {
        return ageCounter;
    }

    /**
     * Gets the age counter roll over value.
     *
     * @return the age counter roll over value
     */
    public int getAgeCounterRollOver() {
        return ageCounterRollOver;
    }

    /**
     * Gets the max age bin.
     *
     * @return lsa bin instance
     */
    public LsaBin getMaxAgeBin() {
        return maxAgeBin;
    }

    /**
     * Gets the bin number.
     *
     * @param x Can be either age or ageCounter
     * @return bin number.
     */
    public int age2Bin(int x) {
        if (x <= ageCounter) {
            return (ageCounter - x);
        } else {
            return ((OspfParameters.MAXAGE - 1) + (ageCounter - x));
        }
    }

    /**
     * Runnable task which runs every second and calls aging process.
     */
    private class InternalAgeTimer implements Runnable {

        /**
         * Constructor.
         */
        InternalAgeTimer() {
            log.debug("Starts::LsdbAge::AgeTimer...!!! ");
        }

        @Override
        public void run() {
            ageLsaAndFlood();
        }
    }
}