/*
 * Copyright 2018-present Open Networking Foundation
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

package org.onosproject.incubator.net.dpi;

import static com.google.common.base.MoreObjects.toStringHelper;

/**
 * Traffic statistic information.
 */
public class TrafficStatInfo {
    long ethernetBytes;
    long discardedBytes;
    long ipPackets;
    long totalPackets;
    long ipBytes;
    int avgPktSize;
    int uniqueFlows;
    long tcpPackets;
    long udpPackets;

    double dpiThroughputPps;
    double dpiThroughputBps;
    double trafficThroughputPps;
    double trafficThroughputBps;
    double trafficDurationSec;
    int guessedFlowProtos;

    static final String PPS_STRING = "pps";
    static final String BPS_STRING = "bps";
    static final String SEC_STRING = "sec";

    /**
     * Constructor for default TrafficStatInfo class.
     */
    public TrafficStatInfo() {
        ethernetBytes = 0;
        discardedBytes = 0;
        ipPackets = 0;
        totalPackets = 0;
        ipBytes = 0;
        avgPktSize = 0;
        uniqueFlows = 0;
        tcpPackets = 0;
        udpPackets = 0;
        dpiThroughputPps = 0;
        dpiThroughputBps = 0;
        trafficThroughputPps = 0;
        trafficThroughputBps = 0;
        trafficDurationSec = 0;
        guessedFlowProtos = 0;
    }

    /**
     * Constructor for TrafficStatInfo class specified with traffic statistic parameters.
     *
     * @param ethernetBytes ethernet byte count
     * @param discardedBytes discarded byte count
     * @param ipPackets IP packet count
     * @param totalPackets total packet count
     * @param ipBytes total IP byte count
     * @param avgPktSize average packet size
     * @param uniqueFlows unique flows count
     * @param tcpPackets TCP packet count
     * @param udpPackets UDP packet count
     * @param trafficThroughputPps traffic throughput PPS
     * @param trafficThroughputBps traffic throughput BPS
     * @param dpiThroughputPps DPI throughput PPS
     * @param dpiThroughputBps DPI throughput BPS
     * @param trafficDurationSec traffic duration in seconds
     * @param guessedFlowProtos guess flow protocols
     */
    public TrafficStatInfo(long ethernetBytes, long discardedBytes, long ipPackets, long totalPackets,
                           long ipBytes, int avgPktSize, int uniqueFlows, long tcpPackets, long udpPackets,
                           double dpiThroughputPps, double dpiThroughputBps,
                           double trafficThroughputPps, double trafficThroughputBps,
                           double trafficDurationSec, int guessedFlowProtos) {
        this.ethernetBytes = ethernetBytes;
        this.discardedBytes = discardedBytes;
        this.ipPackets = ipPackets;
        this.totalPackets = totalPackets;
        this.ipBytes = ipBytes;
        this.avgPktSize = avgPktSize;
        this.uniqueFlows = uniqueFlows;
        this.tcpPackets = tcpPackets;
        this.udpPackets = udpPackets;
        this.dpiThroughputPps = dpiThroughputPps;
        this.dpiThroughputBps = dpiThroughputBps;
        this.trafficThroughputPps = trafficThroughputPps;
        this.trafficThroughputBps = trafficThroughputBps;
        this.trafficDurationSec = trafficDurationSec;
        this.guessedFlowProtos = guessedFlowProtos;
    }

    /**
     * Returns DPI traffic ethernet bytes.
     *
     * @return ethernetBytes
     */
    public long ethernetBytes() {
        return ethernetBytes;
    }

    /**
     * Returns DPI traffic discarded bytes.
     *
     * @return discardedBytes
     */
    public long discardedBytes() {
        return discardedBytes;
    }

    /**
     * Returns DPI traffic ip packets.
     *
     * @return ipPackets
     */
    public long ipPackets() {
        return ipPackets;
    }

    /**
     * Returns DPI traffic total packets.
     *
     * @return totalPackets
     */
    public long totalPackets() {
        return totalPackets;
    }

    /**
     * Returns DPI traffic ip bytes.
     *
     * @return ipBytes
     */
    public long ipBytes() {
        return ipBytes;
    }

    /**
     * Returns DPI traffic average packet size.
     *
     * @return avgPktSize
     */
    public int avgPktSize() {
        return avgPktSize;
    }

    /**
     * Returns DPI traffic the number of unique flows.
     *
     * @return uniqueFlows
     */
    public int uniqueFlows() {
        return uniqueFlows;
    }

    /**
     * Returns DPI traffic TCP packets.
     *
     * @return tcpPackets
     */
    public long tcpPackets() {
        return tcpPackets;
    }

    /**
     * Returns DPI traffic UDP packets.
     *
     * @return udpPackets
     */
    public long udpPackets() {
        return udpPackets;
    }

    /**
     * Returns DPI traffic throughput Pps(Packet per second).
     *
     * @return dpiThroughputPps
     */
    public double dpiThroughputPps() {
        return dpiThroughputPps;
    }

    /**
     * Returns DPI traffic throughput Bps(Byte per second).
     *
     * @return dpiThroughputBps
     */
    public double dpiThroughputBps() {
        return dpiThroughputBps;
    }

    /**
     * Returns total traffic throughput Pps(Packet per second).
     *
     * @return trafficThroughputPps
     */
    public double trafficThroughputPps() {
        return trafficThroughputPps;
    }

    /**
     * Returns total traffic throughput Bps(Byte per second).
     *
     * @return trafficThroughputBps
     */
    public double trafficThroughputBps() {
        return trafficThroughputBps;
    }

    /**
     * Returns DPI traffic duration second.
     *
     * @return trafficDurationSec
     */
    public double trafficDurationSec() {
        return trafficDurationSec;
    }

    /**
     * Returns DPI traffic the number of guessed flow protocols.
     *
     * @return guessedFlowProtos
     */
    public int guessedFlowProtos() {
        return guessedFlowProtos;
    }


    public void setEthernetBytes(long ethernetBytes) {
        this.ethernetBytes = ethernetBytes;
    }

    public void setDiscardedBytes(long discardedBytes) {
        this.discardedBytes = discardedBytes;
    }

    public void setIpPackets(long ipPackets) {
        this.ipPackets = ipPackets;
    }

    public void setTotalPackets(long totalPackets) {
        this.totalPackets = totalPackets;
    }

    public void setIpBytes(long ipBytes) {
        this.ipBytes = ipBytes;
    }

    public void setAvgPktSize(int avgPktSize) {
        this.avgPktSize = avgPktSize;
    }

    public void setUniqueFlows(int uniqueFlows) {
        this.uniqueFlows = uniqueFlows;
    }

    public void setTcpPackets(long tcpPackets) {
        this.tcpPackets = tcpPackets;
    }

    public void setUdpPackets(long udpPackets) {
        this.udpPackets = udpPackets;
    }

    public void setDpiThroughputPps(double dpiThroughputPps) {
        this.dpiThroughputPps = dpiThroughputPps;
    }

    public void setDpiThroughputBps(double dpiThroughputBps) {
        this.dpiThroughputBps = dpiThroughputBps;
    }

    public void setTrafficThroughputPps(double trafficThroughputPps) {
        this.trafficThroughputPps = trafficThroughputPps;
    }

    public void setTrafficThroughputBps(double trafficThroughputBps) {
        this.trafficThroughputBps = trafficThroughputBps;
    }

    public void setTrafficDurationSec(double trafficDurationSec) {
        this.trafficDurationSec = trafficDurationSec;
    }

    public void setGuessedFlowProtos(int guessedFlowProtos) {
        this.guessedFlowProtos = guessedFlowProtos;
    }

    @Override
    public String toString() {
        return toStringHelper(this)
                .add("ethernetBytes", ethernetBytes)
                .add("discardedBytes", discardedBytes)
                .add("ipPackets", ipPackets)
                .add("totalPackets", totalPackets)
                .add("ipBytes", ipBytes)
                .add("avgPktSize", avgPktSize)
                .add("uniqueFlows", uniqueFlows)
                .add("tcpPackets", tcpPackets)
                .add("udpPackets", udpPackets)
                .add("dpiThroughputPps", dpiThroughputPps + " " + PPS_STRING)
                .add("dpiThroughputBps", dpiThroughputBps + " " + BPS_STRING)
                .add("trafficThroughputPps", trafficThroughputPps + " " + PPS_STRING)
                .add("trafficThroughputBps", trafficThroughputBps + " " + BPS_STRING)
                .add("trafficDurationSec", trafficDurationSec + " " + SEC_STRING)
                .add("guessedFlowProtos", guessedFlowProtos)
                .toString();
    }
}
