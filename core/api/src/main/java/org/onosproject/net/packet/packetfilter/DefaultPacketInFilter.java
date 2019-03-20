/*
 * Copyright 2019-present Open Networking Foundation
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
package org.onosproject.net.packet.packetfilter;

import org.onosproject.net.packet.PacketContext;
import org.onosproject.net.packet.PacketInClassifier;
import org.onosproject.net.packet.PacketInFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.onlab.util.Tools.groupedThreads;

/**
 * Default implementation of a packet-in filter.
 */
public class DefaultPacketInFilter implements PacketInFilter {

    /**
     * Tracks the count of specific packet types (eg ARP, ND, DHCP etc)
     * to be limited in the packet queue. This count always reflects the
     * number of packets in the queue at any point in time
     */
    private AtomicInteger currentCounter = new AtomicInteger(0);

    /**
     * Tracks the number of continuous windows where the drop packet happens.
     */
    private AtomicInteger windowBlockCounter = new AtomicInteger(0);

    /**
     * Tracks the counter of the packet which are dropped.
     */
    private AtomicInteger overFlowCounter = new AtomicInteger(0);

    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * Max Allowed packet rate beyond which the packet will be dropped
     * within given window size.
     */
    private int pps = 100;

    /**
     * Window size which will be used for number of packets acceptable
     * based on the accepted pps.
     */
    private int winSize = 500;

    /**
     * Guard time in seconds which will be enabled if there are continuous
     * windows crossing winThres where the packet rate crosses the acceptable
     * packet count calculated based on accepted pps.
     * Guard time should be always greater then the the window size.
     */
    private int guardTime = 10;

    /**
     * Threshold of continuous windows where the packet drop happens post which
     * the guardTime will be triggered and no future packet processing happens
     * till the expiry of this guard time.
     */
    private int winThres = 10;


    private int maxCounter;

    private ScheduledExecutorService timerExecutor;

    private ScheduledExecutorService windowUnblockExecutor;

    private boolean windowBlocked;

    private boolean packetProcessingBlocked;


    /**
     * Name of the counter.
     */
    private String counterName;

    /**
     * PacketInclassifier associated with this filter object.
     */
    private final PacketInClassifier classifier;



    /**
     * Only one filter object per packet type to be associated.
     * Multiple filter types will result in undefined behavior.
     * @param pps Rate at which the packet is accepted in packets per second
     * @param winSize Size of window in milli seconds within which
     *                the packet rate will be analyzed
     * @param guardTime Time duration in seconds for which the packet processing
     *                  will be on hold if there is a continuous window where
     *                  cross of the rate happens and that window count crosses
     *                  winThres
     * @param winThres Continuous window threshold after which gaurdTime will be
     *                 activated
     * @param counterName Name of the counter
     * @param classifier Packet classification
     */
    public DefaultPacketInFilter(int pps, int winSize, int guardTime, int winThres,
                                 String counterName, PacketInClassifier classifier) {
        this.pps = pps;
        this.winSize = winSize;
        this.guardTime = guardTime;
        this.winThres = winThres;
        this.counterName = counterName;
        this.classifier = classifier;
        this.maxCounter = (pps * winSize) / 1000;
        timerExecutor = Executors.newScheduledThreadPool(1,
                                                         groupedThreads("packet/packetfilter",
                                                                        "packet-filter-timer-%d", log));

        windowUnblockExecutor = Executors.newScheduledThreadPool(1,
                                                                 groupedThreads("packet/packetfilter",
                                                                                "packet-filter-unblocker-%d", log));
        timerExecutor.scheduleAtFixedRate(new ClearWindowBlock(),
                                          0,
                                          winSize,
                                          TimeUnit.MILLISECONDS);


        windowBlocked = false;
        packetProcessingBlocked = false;

    }



    @Override
    public FilterAction preProcess(PacketContext packet) {


        maxCounter = (pps * winSize) / 1000;

        // If pps is set then min value for maxCounter is 1
        if (maxCounter == 0 && pps != 0) {
            log.trace("{}: maxCounter set to 1 as was coming as 0", counterName);
            maxCounter = 1;
        }



        if (!classifier.match(packet)) {
            return FilterAction.FILTER_INVALID;
        }

        if (pps == 0 && maxCounter == 0) {
            log.trace("{}: Filter is disabled", counterName);
            return FilterAction.FILTER_DISABLED;
        }
        log.trace("{}: Preprocess called", counterName);

        // Packet block checking should be done before windowBlocked checking
        // otherwise there will be windows with packets while packet processing is suspended
        // and that may break the existing check logic
        if (packetProcessingBlocked) {
            log.trace("{}: Packet processing is blocked for sometime", counterName);
            return FilterAction.PACKET_BLOCKED;
        }

        if (windowBlocked) {
            log.trace("{}: Packet processing is blocked for the window number: {}",
                      counterName, windowBlockCounter.get());
            return FilterAction.WINDOW_BLOCKED;
        }

        if (currentCounter.getAndIncrement() < maxCounter) {
            log.trace("{}: Packet is picked for processing with currentCounter: {} and maxCounter: {}",
                      counterName, currentCounter.get(), maxCounter);
            return FilterAction.PACKET_ALLOW;
        }
        //Need to decrement the currentCounter and increment overFlowCounter
        //Need to block the window and increment the window block counter
        windowBlocked = true;
        //TODO: Review this and the complete state machine
        // If windowBlock crosses threshold then block packet processing for guard time
        if (windowBlockCounter.incrementAndGet() > winThres) {
            log.trace("{}: Packet processing blocked as current window crossed threshold " +
                       "currentWindowNumber: {} maxWindowNumber: {}",
                       counterName, windowBlockCounter.get(), winThres);
            packetProcessingBlocked = true;
            windowUnblockExecutor.schedule(new ClearPacketProcessingBlock(),
                                                         guardTime,
                                                         TimeUnit.SECONDS);
        } else {
            log.trace("{}: WindowBlockCounter: {} winThres: {}", counterName, windowBlockCounter.get(),
            winThres);
        }
        //MT: Temp change in logic to branch the code - Rolled back
        currentCounter.decrementAndGet();
        if (overFlowCounter.incrementAndGet() < 0) {
            overFlowCounter.set(0);
        }
        log.trace("{}: Overflow counter is: {}", counterName, overFlowCounter.get());
        return FilterAction.PACKET_DENY;

    }

    @Override
    public String name() {
        return counterName;
    }

    @Override
    public int pendingPackets() {
        return currentCounter.get();
    }

    @Override
    public int droppedPackets() {
       return overFlowCounter.get();
    }



    @Override
    public void setPps(int pps) {
        this.pps = pps;
    }

    @Override
    public void setWinSize(int winSize) {
        this.winSize = winSize;
    }

    @Override
    public void setGuardTime(int guardTime) {
        this.guardTime = guardTime;
    }

    @Override
    public void setWinThres(int winThres) {
        this.winThres = winThres;
    }

    @Override
    public void stop() {
        timerExecutor.shutdown();
        windowUnblockExecutor.shutdown();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DefaultPacketInFilter that = (DefaultPacketInFilter) o;
        return pps == that.pps &&
                winSize == that.winSize &&
                guardTime == that.guardTime &&
                winThres == that.winThres &&
                counterName.equals(that.counterName) &&
                classifier.equals(that.classifier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pps, winSize, guardTime, winThres, counterName, classifier);
    }


    private final class ClearWindowBlock implements Runnable {
        @Override
        public void run() {
            // If window is not already blocked and there is at least one packet processed
            // in that window then reset the window block counter:
            if (!windowBlocked) {
                log.trace("{}: WindowBlockCounter is reset as there was no blocking in current " +
                          "window with current windowBlockCounter: {}", counterName, windowBlockCounter.get());
                windowBlockCounter.set(0);
            }
            if (currentCounter.get() == 0) {
                //No packet processed in current window so do not change anything in the current state
                log.trace("{}: No packets in the current window so not doing anything in ClearWindowBlock",
                          counterName);
                return;
            }

            //Reset the counter and unblock the window
            log.trace("{}: Current counter and windowBlocked is reset in ClearWindowBlock", counterName);
            currentCounter.set(0);
            windowBlocked = false;
        }
    }

    private final class ClearPacketProcessingBlock implements Runnable {
        @Override
        public void run() {
            //Reset the counter and unblock the window and packet processing
            //CurrentCounter and windowBlocked counter setting is not required here
            //Still setting to be on safer side
            log.trace("{}: All blocks cleared in ClearPacketProcessingBlock", counterName);
            currentCounter.set(0);
            windowBlocked = false;
            packetProcessingBlocked = false;
            windowBlockCounter.set(0);
        }
    }
}
