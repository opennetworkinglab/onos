/*
 * Copyright 2021-present Open Networking Foundation
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

package org.onosproject.net.behaviour.upf;

import com.google.common.annotations.Beta;

import java.util.Objects;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static org.onosproject.net.behaviour.upf.UpfEntityType.COUNTER;
import static org.onosproject.net.behaviour.upf.UpfEntityType.EGRESS_COUNTER;
import static org.onosproject.net.behaviour.upf.UpfEntityType.INGRESS_COUNTER;

/**
 * A structure for compactly passing UPF counter (ingress, egress or both) values
 * for a given counter ID. Contains four counts: Ingress Packets, Ingress Bytes,
 * Egress Packets, Egress Bytes. UpfCounter can be used ONLY on {@code apply}
 * and {@code readAll} calls in the {@link UpfDevice} interface.
 */
@Beta
public final class UpfCounter implements UpfEntity {
    private final int cellId;
    private final Long ingressPkts;
    private final Long ingressBytes;
    private final Long egressPkts;
    private final Long egressBytes;
    private final UpfEntityType type;

    private UpfCounter(int cellId, Long ingressPkts, Long ingressBytes,
                       Long egressPkts, Long egressBytes, UpfEntityType type) {
        this.cellId = cellId;
        this.ingressPkts = ingressPkts;
        this.ingressBytes = ingressBytes;
        this.egressPkts = egressPkts;
        this.egressBytes = egressBytes;
        this.type = type;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        switch (this.type) {
            case COUNTER:
                return String.format("UpfStats(cell_id=%d, ingress=(%dpkts,%dbytes), egress=(%dpkts,%dbytes))",
                                     cellId, ingressPkts, ingressBytes, egressPkts, egressBytes);
            case INGRESS_COUNTER:
                return String.format("UpfIngressCounter(cell_id=%d, packets=%d, bytes=%d))",
                                     cellId, ingressPkts, ingressBytes);
            case EGRESS_COUNTER:
                return String.format("UpfEgressCounter(cell_id=%d, packets=%d, bytes=%d))",
                                     cellId, egressPkts, egressBytes);
            default:
                throw new IllegalStateException("I should never reach this point!");
        }
    }

    @Override
    public boolean equals(Object object) {
        if (object == this) {
            return true;
        }
        if (object == null) {
            return false;
        }
        if (getClass() != object.getClass()) {
            return false;
        }
        UpfCounter that = (UpfCounter) object;
        return this.cellId == that.cellId && this.type == that.type;
    }

    /**
     * Returns whether this UpfCounter is exactly equal to the given UpfCounter,
     * including their packets and bytes values.
     *
     * @param that other {@link UpfCounter} instance to compare
     * @return true if exactly equals, false otherwise
     */
    public boolean exactlyEquals(UpfCounter that) {
        return this.equals(that) &&
                (this.ingressPkts == that.ingressPkts || this.ingressPkts.equals(that.ingressPkts)) &&
                (this.ingressBytes == that.ingressBytes || this.ingressBytes.equals(that.ingressBytes)) &&
                (this.egressPkts == that.egressPkts || this.egressPkts.equals(that.egressPkts)) &&
                (this.egressBytes == that.egressBytes || this.egressBytes.equals(that.egressBytes));
    }

    @Override
    public int hashCode() {
        return Objects.hash(cellId, type);
    }

    /**
     * Get the cell ID (index) of the dataplane counter that produced this set of stats.
     *
     * @return counter cell ID
     */
    public int getCellId() {
        return cellId;
    }

    /**
     * Get the number of packets that hit this counter in the dataplane ingress pipeline.
     * Return a value only if the counter is of type {@code UpfEntityType.COUNTER}
     * or {@code UpfEntityType.INGRESS_COUNTER}, otherwise an empty Optional.
     *
     * @return ingress packet count or empty if this is of type {@code UpfEntityType.EGRESS_COUNTER}
     */
    public Optional<Long> getIngressPkts() {
        return Optional.ofNullable(ingressPkts);
    }

    /**
     * Get the number of packets that hit this counter in the dataplane egress pipeline.
     * Return a value only if the counter is of type {@code UpfEntityType.COUNTER}
     * or {@code UpfEntityType.EGRESS_COUNTER}, otherwise an empty Optional.
     *
     * @return egress packet count or empty if this is of type {@code UpfEntityType.INGRESS_COUNTER}
     */
    public Optional<Long> getEgressPkts() {
        return Optional.ofNullable(egressPkts);
    }

    /**
     * Get the number of packet bytes that hit this counter in the dataplane ingress pipeline.
     * Return value only if the counter is of type {{@code UpfEntityType.COUNTER}
     * or {@code UpfEntityType.INGRESS_COUNTER}, otherwise an empty Optional.
     *
     * @return ingress byte count or empty if this is of type {@code UpfEntityType.EGRESS_COUNTER}
     */
    public Optional<Long> getIngressBytes() {
        return Optional.ofNullable(ingressBytes);
    }

    /**
     * Get the number of packet bytes that hit this counter in the dataplane egress pipeline.
     * Return a value only if the counter is of type {@code UpfEntityType.COUNTER}
     * or {@code UpfEntityType.EGRESS_COUNTER}, otherwise an empty Optional.
     *
     * @return egress byte count or empty if this is of type {@code UpfEntityType.INGRESS_COUNTER}
     */
    public Optional<Long> getEgressBytes() {
        return Optional.ofNullable(egressBytes);
    }

    @Override
    public UpfEntityType type() {
        return type;
    }

    /**
     * Sum the content of the given UpfCounter to the counter values contained
     * in this instance.
     *
     * @param that The UpfCounter to sum to this instance
     * @return a new UpfCounter instance with sum counters.
     * @throws IllegalArgumentException if the given UpfCounter is not referring
     *                                  to the same type and id as this
     */
    public UpfCounter sum(UpfCounter that) throws IllegalArgumentException {
        if (!this.equals(that)) {
            throw new IllegalArgumentException(
                    "The given UpfCounter is not of the same type or refers to a different index");
        }
        UpfCounter.Builder builder = UpfCounter.builder().withCellId(this.getCellId());
        if (this.type.equals(UpfEntityType.COUNTER) || this.type.equals(UpfEntityType.INGRESS_COUNTER)) {
            builder.setIngress(this.ingressPkts + that.ingressPkts,
                               this.ingressBytes + that.ingressBytes);
        }
        if (this.type.equals(UpfEntityType.COUNTER) || this.type.equals(UpfEntityType.EGRESS_COUNTER)) {
            builder.setEgress(this.egressPkts + that.egressPkts,
                              this.egressBytes + that.egressBytes);
        }
        return builder.build();
    }

    /**
     * Builder for UpfCounter.
     */
    public static class Builder {
        private Integer cellId;
        private Long ingressPkts;
        private Long ingressBytes;
        private Long egressPkts;
        private Long egressBytes;
        private UpfEntityType type = COUNTER;

        public Builder() {
        }

        /**
         * Set the Cell ID (index) of the datalane counter that produced this set of stats.
         *
         * @param cellId the counter cell ID
         * @return This builder
         */
        public Builder withCellId(int cellId) {
            this.cellId = cellId;
            return this;
        }

        /**
         * Set the number of packets and bytes that hit the counter in the dataplane ingress pipeline.
         *
         * @param ingressPkts  ingress packet count
         * @param ingressBytes egress packet count
         * @return This builder
         */
        public Builder setIngress(long ingressPkts, long ingressBytes) {
            this.ingressPkts = ingressPkts;
            this.ingressBytes = ingressBytes;
            return this;
        }

        /**
         * Set the number of packets and bytes that hit the counter in the dataplane egress pipeline.
         *
         * @param egressPkts  egress packet count
         * @param egressBytes egress byte count
         * @return This builder
         */
        public Builder setEgress(long egressPkts, long egressBytes) {
            this.egressPkts = egressPkts;
            this.egressBytes = egressBytes;
            return this;
        }

        /**
         * Set the counter as ingress only counter.
         *
         * @return This builder
         */
        public Builder isIngressCounter() {
            this.type = INGRESS_COUNTER;
            return this;
        }

        /**
         * Set the counter as egress only counter.
         *
         * @return This builder
         */
        public Builder isEgressCounter() {
            this.type = EGRESS_COUNTER;
            return this;
        }

        public UpfCounter build() {
            checkNotNull(cellId, "CellID must be provided");
            switch (type) {
                case INGRESS_COUNTER:
                    checkArgument(this.ingressBytes != null && this.ingressPkts != null,
                                  "Ingress counter values must be provided");
                    this.egressBytes = null;
                    this.egressPkts = null;
                    break;
                case EGRESS_COUNTER:
                    checkArgument(this.egressBytes != null && this.egressPkts != null,
                                  "Egress counter values must be provided");
                    this.ingressBytes = null;
                    this.ingressPkts = null;
                    break;
                case COUNTER:
                    checkArgument(this.ingressBytes != null && this.ingressPkts != null &&
                                          this.egressBytes != null && this.egressPkts != null,
                                  "Ingress and egress counter values must be provided");
                    break;
                default:
                    // I should never reach this point
                    throw new IllegalArgumentException("I should never reach this point!");
            }
            return new UpfCounter(cellId, ingressPkts, ingressBytes, egressPkts, egressBytes, type);
        }
    }
}
