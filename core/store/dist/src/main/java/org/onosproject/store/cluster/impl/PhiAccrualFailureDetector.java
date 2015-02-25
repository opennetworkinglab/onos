package org.onosproject.store.cluster.impl;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.onosproject.cluster.NodeId;

import com.google.common.collect.Maps;

/**
 * Phi Accrual failure detector.
 * <p>
 * Based on a paper titled: "The φ Accrual Failure Detector" by Hayashibara, et al.
 */
public class PhiAccrualFailureDetector {
    private final Map<NodeId, History> states = Maps.newConcurrentMap();

    // TODO: make these configurable.
    private static final int WINDOW_SIZE = 250;
    private static final int MIN_SAMPLES = 25;

    // If a node does not have any heartbeats, this is the phi
    // value to report. Indicates the node is inactive (from the
    // detectors perspective.
    private static final double BOOTSTRAP_PHI_VALUE = 100.0;

    /**
     * Report a new heart beat for the specified node id.
     * @param nodeId node id
     */
    public void report(NodeId nodeId) {
        report(nodeId, System.currentTimeMillis());
    }

    /**
     * Report a new heart beat for the specified node id.
     * @param nodeId node id
     * @param arrivalTime arrival time
     */
    public void report(NodeId nodeId, long arrivalTime) {
        checkNotNull(nodeId, "NodeId must not be null");
        checkArgument(arrivalTime >= 0, "arrivalTime must not be negative");
        History nodeState =
                states.computeIfAbsent(nodeId, key -> new History());
        synchronized (nodeState) {
            long latestHeartbeat = nodeState.latestHeartbeatTime();
            if (latestHeartbeat != -1) {
                nodeState.samples().addValue(arrivalTime - latestHeartbeat);
            }
            nodeState.setLatestHeartbeatTime(arrivalTime);
        }
    }

    /**
     * Compute phi for the specified node id.
     * @param nodeId node id
     * @return phi value
     */
    public Double phi(NodeId nodeId) {
        if (!states.containsKey(nodeId)) {
            return BOOTSTRAP_PHI_VALUE;
        }
        checkNotNull(nodeId, "NodeId must not be null");
        History nodeState = states.get(nodeId);
        synchronized (nodeState) {
            long latestHeartbeat = nodeState.latestHeartbeatTime();
            DescriptiveStatistics samples = nodeState.samples();
            if (latestHeartbeat == -1 || samples.getN() < MIN_SAMPLES) {
                return 0.0;
            }
            return computePhi(samples, latestHeartbeat, System.currentTimeMillis());
        }
    }

    private double computePhi(DescriptiveStatistics samples, long tLast, long tNow) {
        long size = samples.getN();
        long t = tNow - tLast;
        return (size > 0)
               ? (1.0 / Math.log(10.0)) * t / samples.getMean()
               : BOOTSTRAP_PHI_VALUE;
    }

    private static class History {
        DescriptiveStatistics samples =
                new DescriptiveStatistics(WINDOW_SIZE);
        long lastHeartbeatTime = -1;

        public DescriptiveStatistics samples() {
            return samples;
        }

        public long latestHeartbeatTime() {
            return lastHeartbeatTime;
        }

        public void setLatestHeartbeatTime(long value) {
            lastHeartbeatTime = value;
        }
    }
}