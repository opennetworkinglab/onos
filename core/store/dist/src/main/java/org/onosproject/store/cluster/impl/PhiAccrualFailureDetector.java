/*
 * Copyright 2015-present Open Networking Foundation
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
package org.onosproject.store.cluster.impl;

import java.util.Map;

import com.google.common.collect.Maps;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.onosproject.cluster.NodeId;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Phi Accrual failure detector.
 * <p>
 * Based on a paper titled: "The φ Accrual Failure Detector" by Hayashibara, et al.
 */
public class PhiAccrualFailureDetector {
    private final Map<NodeId, NodeFailureDetector> nodes = Maps.newConcurrentMap();

    // Default value
    private static final int DEFAULT_WINDOW_SIZE = 250;
    private static final int DEFAULT_MIN_SAMPLES = 25;
    private static final long DEFAULT_MIN_STANDARD_DEVIATION_MILLIS = 50;

    // If a node does not have any heartbeats, this is the phi
    // value to report. Indicates the node is inactive (from the
    // detectors perspective.
    private static final double DEFAULT_BOOTSTRAP_PHI_VALUE = 100.0;

    private final int minSamples;
    private final long minStandardDeviationMillis;
    private final double bootstrapPhiValue = DEFAULT_BOOTSTRAP_PHI_VALUE;

    public PhiAccrualFailureDetector() {
        this(DEFAULT_MIN_SAMPLES, DEFAULT_MIN_STANDARD_DEVIATION_MILLIS);
    }

    public PhiAccrualFailureDetector(long minStandardDeviationMillis) {
        this(DEFAULT_MIN_SAMPLES, minStandardDeviationMillis);
    }

    public PhiAccrualFailureDetector(int minSamples, long minStandardDeviationMillis) {
        checkArgument(minSamples > 0, "minSamples must be positive");
        checkArgument(minStandardDeviationMillis > 0, "minStandardDeviationMillis must be positive");
        this.minSamples = minSamples;
        this.minStandardDeviationMillis = minStandardDeviationMillis;
    }

    /**
     * Returns the last heartbeat time for the given node.
     *
     * @param nodeId the node identifier
     * @return the last heartbeat time for the given node
     */
    public Long getLastHeartbeatTime(NodeId nodeId) {
        NodeFailureDetector node = nodes.get(nodeId);
        return node != null ? node.lastHeartbeatTime() : null;
    }

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
        NodeFailureDetector node = nodes.get(nodeId);
        if (node == null) {
            nodes.computeIfAbsent(nodeId, key -> new NodeFailureDetector());
        } else {
            node.setLastHeartbeatTime(arrivalTime);
        }
    }

    /**
     * Resets the failure detector for the given node.
     *
     * @param nodeId node identifier for the node for which to reset the failure detector
     */
    public void reset(NodeId nodeId) {
        nodes.remove(nodeId);
    }

    /**
     * Compute phi for the specified node id.
     * @param nodeId node id
     * @return phi value
     */
    public double phi(NodeId nodeId) {
        return phi(nodeId, System.currentTimeMillis());
    }

    /**
     * Compute phi for the specified node id.
     * @param nodeId node id
     * @param currentTime the current timestamp
     * @return phi value
     */
    public double phi(NodeId nodeId, long currentTime) {
        checkNotNull(nodeId, "NodeId must not be null");
        NodeFailureDetector node = nodes.get(nodeId);
        if (node == null) {
            return bootstrapPhiValue;
        }
        return node.computePhi(currentTime);
    }

    private class NodeFailureDetector {
        private final DescriptiveStatistics samples = new DescriptiveStatistics(DEFAULT_WINDOW_SIZE);
        private volatile long lastHeartbeatTime = System.currentTimeMillis();

        /**
         * Returns the last heartbeat time.
         *
         * @return the last heartbeat time
         */
        long lastHeartbeatTime() {
            return lastHeartbeatTime;
        }

        /**
         * Sets the last heartbeat time.
         *
         * @param heartbeatTime the last heartbeat time
         */
        synchronized void setLastHeartbeatTime(long heartbeatTime) {
            samples.addValue(heartbeatTime - lastHeartbeatTime);
            lastHeartbeatTime = heartbeatTime;
        }

        /**
         * Computes the phi value for this node.
         *
         * @param currentTime the current timestamp
         * @return the phi value
         */
        synchronized double computePhi(long currentTime) {
            if (samples.getN() < minSamples) {
                return 0.0;
            }

            long elapsedTime = currentTime - lastHeartbeatTime;
            double meanMillis = samples.getMean();
            double standardDeviation = samples.getStandardDeviation();
            double y = (elapsedTime - meanMillis) / Math.max(standardDeviation, minStandardDeviationMillis);
            double e = Math.exp(-y * (1.5976 + 0.070566 * y * y));
            if (elapsedTime > meanMillis) {
                return -Math.log10(e / (1.0 + e));
            } else {
                return -Math.log10(1.0 - 1.0 / (1.0 + e));
            }
        }
    }
}