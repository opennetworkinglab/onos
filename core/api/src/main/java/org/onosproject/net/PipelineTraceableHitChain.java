/*
 * Copyright 2020-present Open Networking Foundation
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

package org.onosproject.net;

import com.google.common.collect.Lists;

import java.util.List;
import java.util.Objects;

/**
 * Class to represent the pipeline hit chain and the result of the pipeline processing.
 */
public final class PipelineTraceableHitChain {

    private ConnectPoint outputPort;
    private List<DataPlaneEntity> hitChain;
    private PipelineTraceablePacket egressPacket;
    // By default packets are dropped
    private boolean dropped = true;

    private PipelineTraceableHitChain() {
        hitChain = Lists.newArrayList();
    }

    /**
     * Creates a new PipelineTraceableHitChain.
     *
     * @param output   the output connect point
     * @param hits     the hits in the pipeline (flows, groups and other abstractions)
     * @param packet   the traceable packet representing the final packet
     */
    public PipelineTraceableHitChain(ConnectPoint output, List<DataPlaneEntity> hits,
                                     PipelineTraceablePacket packet) {
        this.outputPort = output;
        this.hitChain = hits;
        this.egressPacket = packet;
    }

    /**
     * Creates an empty pipeline hit chain.
     *
     * @return an empty pipeline hit chain
     */
    public static PipelineTraceableHitChain emptyHitChain() {
        return new PipelineTraceableHitChain();
    }

    /**
     * Returns the output connect point.
     *
     * @return the connect point
     */
    public ConnectPoint outputPort() {
        return outputPort;
    }

    /**
     * Sets the output port.
     *
     * @param outputPort the output port
     */
    public void setOutputPort(ConnectPoint outputPort) {
        this.outputPort = outputPort;
    }

    /**
     * Returns the hit chain.
     *
     * @return flows and groups that matched.
     */
    public List<DataPlaneEntity> hitChain() {
        return hitChain;
    }

    /**
     * Adds the provided dataplane entity to the end of the chain.
     *
     * @param dataPlaneEntity the dataplane entity
     */
    public void addDataPlaneEntity(DataPlaneEntity dataPlaneEntity) {
        if (!hitChain.contains(dataPlaneEntity)) {
            hitChain.add(dataPlaneEntity);
        }
    }

    /**
     * Removes the provided dataplane entity from the chain.
     *
     * @param dataPlaneEntity the dataplane entity
     */
    public void removeDataPlaneEntity(DataPlaneEntity dataPlaneEntity) {
        if (hitChain.isEmpty()) {
            return;
        }
        hitChain.remove(dataPlaneEntity);
    }

    /**
     * Returns the egress packet after traversing the pipeline.
     *
     * @return the traceable packet representing the packet infos
     */
    public PipelineTraceablePacket egressPacket() {
        return egressPacket;
    }

    /**
     * Sets the egress packet.
     *
     * @param egressPacket the egress packet
     */
    public void setEgressPacket(PipelineTraceablePacket egressPacket) {
        this.egressPacket = egressPacket;
    }

    /**
     * Return whether or not the packet has been dropped by the pipeline.
     *
     * @return true if the packet has been dropped. False, otherwise.
     */
    public boolean isDropped() {
        return dropped;
    }

    /**
     * Set the dropped flag.
     */
    public void dropped() {
        this.dropped = true;
    }

    /**
     * Unset the dropped flag.
     */
    public void pass() {
        this.dropped = false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(outputPort, hitChain, egressPacket, dropped);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof PipelineTraceableHitChain) {
            PipelineTraceableHitChain that = (PipelineTraceableHitChain) obj;
            return Objects.equals(this.outputPort, that.outputPort) &&
                    Objects.equals(this.hitChain, that.hitChain) &&
                    Objects.equals(this.egressPacket, that.egressPacket) &&
                    Objects.equals(this.dropped, that.dropped);
        }
        return false;
    }

    @Override
    public String toString() {
        return "PipelineTraceableHitChain{" +
                "outputPort=" + outputPort +
                ", hitChain=" + hitChain +
                ", egressPacket=" + egressPacket +
                ", dropped=" + dropped +
                '}';
    }
}
