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
package org.onosproject.isis.controller.impl;

import org.onlab.packet.Ip4Address;
import org.onlab.packet.MacAddress;
import org.onosproject.isis.controller.IsisInterface;
import org.onosproject.isis.controller.IsisInterfaceState;
import org.onosproject.isis.controller.IsisNeighbor;
import org.onosproject.isis.controller.IsisPduType;
import org.onosproject.isis.controller.IsisRouterType;
import org.onosproject.isis.io.isispacket.pdu.HelloPdu;
import org.onosproject.isis.io.isispacket.pdu.L1L2HelloPdu;
import org.onosproject.isis.io.isispacket.pdu.P2PHelloPdu;
import org.onosproject.isis.io.util.IsisConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Representation of an ISIS neighbor.
 * The first thing an ISIS router must do is find its neighbors and form adjacency.
 * Each neighbor that the router finds will be represented by this class.
 */
public class DefaultIsisNeighbor implements IsisNeighbor {
    private static final Logger log = LoggerFactory.getLogger(DefaultIsisNeighbor.class);
    private String neighborAreaId;
    private String neighborSystemId;
    private Ip4Address interfaceIp;
    private MacAddress neighborMacAddress;
    private volatile int holdingTime;
    private int neighborDownInterval;
    private IsisRouterType routerType;
    private String l1LanId;
    private String l2LanId;
    private byte localCircuitId;
    private int localExtendedCircuitId;
    private IsisInterfaceState neighborState = IsisInterfaceState.INITIAL;
    private InternalInactivityTimeCheck inActivityTimeCheckTask;
    private ScheduledExecutorService exServiceInActivity;
    private InternalHoldingTimeCheck holdingTimeCheckTask;
    private ScheduledExecutorService exServiceHoldingTimeCheck;
    private boolean inActivityTimerScheduled = false;
    private IsisInterface isisInterface;

    /**
     * Creates an instance of ISIS neighbor.
     *
     * @param helloMessage  hello message instance
     * @param isisInterface ISIS interface instance
     */
    public DefaultIsisNeighbor(HelloPdu helloMessage, IsisInterface isisInterface) {
        this.neighborMacAddress = helloMessage.sourceMac();
        List<String> areaAddresses = helloMessage.areaAddress();
        this.neighborAreaId = (areaAddresses != null) ? areaAddresses.get(0) : "";
        this.neighborSystemId = helloMessage.sourceId();
        List<Ip4Address> interfaceIpAddresses = helloMessage.interfaceIpAddresses();
        this.interfaceIp = (helloMessage.interfaceIpAddresses() != null) ?
                interfaceIpAddresses.get(0) : IsisConstants.DEFAULTIP;
        this.holdingTime = helloMessage.holdingTime();
        neighborDownInterval = holdingTime;
        this.routerType = IsisRouterType.get(helloMessage.circuitType());
        if (helloMessage instanceof L1L2HelloPdu) {
            if (IsisPduType.L1HELLOPDU == helloMessage.isisPduType()) {
                l1LanId = ((L1L2HelloPdu) helloMessage).lanId();
            } else if (IsisPduType.L2HELLOPDU == helloMessage.isisPduType()) {
                l2LanId = ((L1L2HelloPdu) helloMessage).lanId();
            }
        } else if (helloMessage instanceof P2PHelloPdu) {
            this.localCircuitId = ((P2PHelloPdu) helloMessage).localCircuitId();
        }
        this.isisInterface = isisInterface;
        startHoldingTimeCheck();
        log.debug("Neighbor added - {}", neighborMacAddress);
    }

    /**
     * Returns local extended circuit ID.
     *
     * @return local extended circuit ID
     */
    public int localExtendedCircuitId() {
        return localExtendedCircuitId;
    }

    /**
     * Sets local extended circuit ID.
     *
     * @param localExtendedCircuitId neighbor extended circuit ID
     */
    public void setLocalExtendedCircuitId(int localExtendedCircuitId) {
        this.localExtendedCircuitId = localExtendedCircuitId;
    }

    /**
     * Returns neighbor area ID.
     *
     * @return neighbor area ID
     */
    public String neighborAreaId() {
        return neighborAreaId;
    }

    /**
     * Sets neighbor area ID.
     *
     * @param neighborAreaId neighbor area ID
     */
    public void setNeighborAreaId(String neighborAreaId) {
        this.neighborAreaId = neighborAreaId;
    }

    /**
     * Returns neighbor system ID.
     *
     * @return neighbor system ID
     */
    public String neighborSystemId() {
        return neighborSystemId;
    }

    /**
     * Sets neighbor system ID.
     *
     * @param neighborSystemId neighbor system ID
     */
    public void setNeighborSystemId(String neighborSystemId) {
        this.neighborSystemId = neighborSystemId;
    }

    /**
     * Returns interface IP.
     *
     * @return interface IP
     */
    public Ip4Address interfaceIp() {
        return interfaceIp;
    }

    /**
     * Sets interface IP.
     *
     * @param interfaceIp IP
     */
    public void setInterfaceIp(Ip4Address interfaceIp) {
        this.interfaceIp = interfaceIp;
    }

    /**
     * Returns neighbor mac address.
     *
     * @return neighborMacAddress neighbor mac address
     */
    public MacAddress neighborMacAddress() {
        return neighborMacAddress;
    }

    /**
     * Sets neighbor mac address.
     *
     * @param neighborMacAddress mac address
     */
    public void setNeighborMacAddress(MacAddress neighborMacAddress) {
        this.neighborMacAddress = neighborMacAddress;
    }

    /**
     * Returns holding time.
     *
     * @return holding time
     */
    public int holdingTime() {
        return holdingTime;
    }

    /**
     * Sets holding time.
     *
     * @param holdingTime holding time
     */
    public void setHoldingTime(int holdingTime) {
        this.holdingTime = holdingTime;
    }

    /**
     * Returns router type.
     *
     * @return router type
     */
    public IsisRouterType routerType() {
        return routerType;
    }

    /**
     * Sets router type.
     *
     * @param routerType router type
     */
    public void setRouterType(IsisRouterType routerType) {
        this.routerType = routerType;
    }

    /**
     * Returns L1 lan ID.
     *
     * @return L1 lan ID
     */
    public String l1LanId() {
        return l1LanId;
    }

    /**
     * Sets L1 lan ID.
     *
     * @param l1LanId L1 lan ID
     */
    public void setL1LanId(String l1LanId) {
        this.l1LanId = l1LanId;
    }

    /**
     * Returns L2 lan ID.
     *
     * @return L2 lan ID
     */
    public String l2LanId() {
        return l2LanId;
    }

    /**
     * Sets L2 lan ID.
     *
     * @param l2LanId L2 lan ID
     */
    public void setL2LanId(String l2LanId) {
        this.l2LanId = l2LanId;
    }

    /**
     * Gets the neighbor interface state.
     *
     * @return neighbor interface state
     */
    public IsisInterfaceState interfaceState() {
        return neighborState;
    }

    /**
     * Sets the neighbor interface state.
     *
     * @param neighborState the neighbor interface state
     */
    public void setNeighborState(IsisInterfaceState neighborState) {
        this.neighborState = neighborState;
    }

    /**
     * Returns local circuit ID.
     *
     * @return local circuit ID
     */
    public byte localCircuitId() {
        return localCircuitId;
    }

    /**
     * Sets local circuit ID.
     *
     * @param localCircuitId local circuit ID
     */
    public void setLocalCircuitId(byte localCircuitId) {
        this.localCircuitId = localCircuitId;
    }

    /**
     * Returns neighbor state.
     *
     * @return neighbor state
     */
    public IsisInterfaceState neighborState() {
        return neighborState;
    }

    /**
     * Starts the holding time check timer.
     */
    public void startHoldingTimeCheck() {
        log.debug("IsisNeighbor::startHoldingTimeCheck");
        holdingTimeCheckTask = new InternalHoldingTimeCheck();
        exServiceHoldingTimeCheck = Executors.newSingleThreadScheduledExecutor();
        exServiceHoldingTimeCheck.scheduleAtFixedRate(holdingTimeCheckTask, 1,
                                                      1, TimeUnit.SECONDS);
    }

    /**
     * Stops the holding time check timer.
     */
    public void stopHoldingTimeCheck() {
        log.debug("IsisNeighbor::stopHoldingTimeCheck ");
        exServiceHoldingTimeCheck.shutdown();
    }

    /**
     * Starts the inactivity timer.
     */
    public void startInactivityTimeCheck() {
        if (!inActivityTimerScheduled) {
            log.debug("IsisNeighbor::startInactivityTimeCheck");
            inActivityTimeCheckTask = new InternalInactivityTimeCheck();
            exServiceInActivity = Executors.newSingleThreadScheduledExecutor();
            exServiceInActivity.scheduleAtFixedRate(inActivityTimeCheckTask, neighborDownInterval,
                                                    neighborDownInterval, TimeUnit.SECONDS);
            inActivityTimerScheduled = true;
        }
    }

    /**
     * Stops the inactivity timer.
     */
    public void stopInactivityTimeCheck() {
        if (inActivityTimerScheduled) {
            log.debug("IsisNeighbor::stopInactivityTimeCheck ");
            exServiceInActivity.shutdown();
            inActivityTimerScheduled = false;
        }
    }

    /**
     * Called when neighbor is down.
     */
    public void neighborDown() {
        log.debug("Neighbor Down {} and NeighborSystemId {}", neighborMacAddress,
                  neighborSystemId);
        stopInactivityTimeCheck();
        isisInterface.setL1LanId(IsisConstants.DEFAULTLANID);
        isisInterface.setL2LanId(IsisConstants.DEFAULTLANID);

        neighborState = IsisInterfaceState.DOWN;
        stopInactivityTimeCheck();
        stopHoldingTimeCheck();
        isisInterface.removeNeighbor(this);

        isisInterface.isisLsdb().removeTopology(this, isisInterface);
    }

    /**
     * Represents a Task which will do an inactivity time check.
     */
    private class InternalInactivityTimeCheck implements Runnable {
        /**
         * Creates an instance.
         */
        InternalInactivityTimeCheck() {
        }

        @Override
        public void run() {
            log.debug("Neighbor Not Heard till the past router dead interval .");
            neighborDown();
        }
    }

    /**
     * Represents a Task which will decrement holding time for this neighbor.
     */
    private class InternalHoldingTimeCheck implements Runnable {
        /**
         * Creates an instance.
         */
        InternalHoldingTimeCheck() {
        }

        @Override
        public void run() {
            holdingTime--;
            if (holdingTime <= 0) {
                log.debug("Calling neighbor down. Holding time is 0.");
                neighborDown();
            }
        }
    }
}