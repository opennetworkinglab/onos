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

import org.onosproject.net.flow.TrafficSelector;

import java.util.Objects;

/**
 * Represents a traceable packet composed by a traffic selector and metadata.
 */
public final class PipelineTraceablePacket {

    // stores metadata associated with the packet
    private PipelineTraceableMetadata metadata;
    // representation of the packet
    private TrafficSelector packet;

    /**
     * Builds a traceable packet without metadata.
     * Note this can be used for legacy device like ofdpa.
     *
     * @param packet the packet selector
     */
    public PipelineTraceablePacket(TrafficSelector packet) {
        this.packet = packet;
    }

    /**
     * Builds a traceable packet with metadata.
     * @param packet the packet selector
     * @param metadata the packet metadata
     */
    public PipelineTraceablePacket(TrafficSelector packet, PipelineTraceableMetadata metadata) {
        this.packet = packet;
        this.metadata = metadata;
    }

    /**
     * Getter for the metadata.
     *
     * @return the packet metadata
     */
    public PipelineTraceableMetadata metadata() {
        return metadata;
    }

    /**
     * Getter for the packet selector.
     *
     * @return the packet selector
     */
    public TrafficSelector packet() {
        return packet;
    }

    @Override
    public int hashCode() {
        return Objects.hash(metadata, packet);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof PipelineTraceablePacket) {
            PipelineTraceablePacket that = (PipelineTraceablePacket) obj;
            return Objects.equals(this.metadata, that.metadata) &&
                    Objects.equals(this.packet, that.packet);
        }
        return false;
    }

    @Override
    public String toString() {
        return "PipelineTraceablePacket{" +
                "metadata=" + metadata +
                ", packet=" + packet +
                '}';
    }
}
