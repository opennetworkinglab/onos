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

package org.onosproject.net.pi.runtime;

import com.google.common.annotations.Beta;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import org.onosproject.net.DeviceId;
import org.onosproject.net.pi.model.PiData;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A register cell entry of a protocol-independent pipeline.
 */
@Beta
public final class PiRegisterCell implements PiEntity {

    private final PiRegisterCellId registerCellId;
    private final PiData piData;

    private PiRegisterCell(PiRegisterCellId registerCellId, PiData piData) {
        this.registerCellId = registerCellId;
        this.piData = piData;
    }

    /**
     * Returns the cell identifier.
     *
     * @return cell identifier
     */
    public PiRegisterCellId cellId() {
        return registerCellId;
    }

    /**
     * Returns the data contained by this cell ID.
     *
     * @return PI data or null
     */
    public PiData data() {
        return piData;
    }

    @Override
    public PiEntityType piEntityType() {
        return PiEntityType.REGISTER_CELL;
    }

    @Override
    public PiHandle handle(DeviceId deviceId) {
        // TODO: implement support for register cell handles
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final PiRegisterCell other = (PiRegisterCell) obj;
        return Objects.equal(this.registerCellId, other.registerCellId) &&
                Objects.equal(this.piData, other.piData);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(registerCellId, piData);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("cellId", registerCellId)
                .add("piData", piData)
                .toString();
    }

    /**
     * Returns a register cell entry builder.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private PiRegisterCellId cellId;
        private PiData piData;


        private Builder() {
            // Hides constructor.
        }

        /**
         * Sets the register cell identifier for this register.
         *
         * @param registerCellId register cell identifier
         * @return this
         */
        public PiRegisterCell.Builder withCellId(PiRegisterCellId registerCellId) {
            this.cellId = registerCellId;
            return this;
        }


        /**
         * Sets the data of this register cell.
         *
         * @param data protocol-independent data
         * @return this
         */
        public PiRegisterCell.Builder withData(PiData data) {
            this.piData = data;
            return this;
        }

        /**
         * Builds the register cell entry.
         *
         * @return a new register cell entry
         */
        public PiRegisterCell build() {
            checkNotNull(cellId);
            return new PiRegisterCell(cellId, piData);
        }
    }
}
