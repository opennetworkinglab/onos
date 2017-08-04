/*
 * Copyright 2014-present Open Networking Foundation
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
package org.onosproject.net.flow;

import java.util.Objects;

import com.google.common.base.MoreObjects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A super class for batch operation entry classes.
 * <p>
 * This is the interface to classes which are maintained by BatchOperation as
 * its entries.
 */
public class BatchOperationEntry<T extends Enum<?>, U> {

    private final T operator;
    private final U target;

    /**
     * Constructs new instance for the entry of the BatchOperation.
     *
     * @param operator the operator of this operation
     * @param target the target object of this operation
     */
    public BatchOperationEntry(T operator, U target) {
        this.operator = checkNotNull(operator);
        this.target = checkNotNull(target);
    }

    /**
     * Gets the target object of this operation.
     *
     * @return the target object of this operation
     */
    public U target() {
        return target;
    }

    /**
     * Gets the operator of this operation.
     *
     * @return the operator of this operation
     */
    public T operator() {
        return operator;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        BatchOperationEntry<?, ?> other = (BatchOperationEntry<?, ?>) o;
        return (this.operator == other.operator) &&
            Objects.equals(this.target, other.target);
    }

    @Override
    public int hashCode() {
        return Objects.hash(operator, target);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("operator", operator)
            .add("target", target)
            .toString();
    }
}
