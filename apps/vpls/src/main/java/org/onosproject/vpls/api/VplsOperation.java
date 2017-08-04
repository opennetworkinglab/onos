/*
 * Copyright 2017-present Open Networking Foundation
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

package org.onosproject.vpls.api;

import com.google.common.base.MoreObjects;

import java.util.Objects;

import static java.util.Objects.*;

/**
 * Operation for VPLS.
 */
public class VplsOperation {

    /**
     * The operation type.
     */
    public enum Operation {
        REMOVE,
        ADD,
        UPDATE
    }

    private VplsData vplsData;
    private Operation op;

    /**
     * Defines a VPLS operation by binding a given VPLS and operation type.
     *
     * @param vplsData the VPLS
     * @param op the operation
     */
    protected VplsOperation(VplsData vplsData, Operation op) {
        requireNonNull(vplsData);
        requireNonNull(op);
        // Make a copy of the VPLS data to ensure other thread won't change it.
        this.vplsData = VplsData.of(vplsData);
        this.op = op;
    }

    /**
     * Defines a VPLS operation by binding a given VPLS and operation type.
     *
     * @param vplsData the VPLS
     * @param op the operation
     * @return the VPLS operation
     */
    public static VplsOperation of(VplsData vplsData, Operation op) {
        return new VplsOperation(vplsData, op);
    }

    /**
     * Retrieves the VPLS from the operation.
     *
     * @return the VPLS
     */
    public VplsData vpls() {
        return vplsData;
    }

    /**
     * Retrieves the operation type from the operation.
     *
     * @return the operation type
     */
    public Operation op() {
        return op;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(getClass())
                .add("vplsData", vplsData.toString())
                .add("op", op.toString())
                .toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof VplsOperation)) {
            return false;
        }
        VplsOperation other = (VplsOperation) obj;
        return Objects.equals(other.vplsData, this.vplsData) &&
                Objects.equals(other.op, this.op);
    }

    @Override
    public int hashCode() {
        return hash(vplsData, op);
    }
}
