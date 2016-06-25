/*
 * Copyright 2014-present Open Networking Laboratory
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

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A list of BatchOperationEntry.
 *
 * @param <T> the enum of operators <br>
 *            This enum must be defined in each sub-classes.
 */
public abstract class BatchOperation<T extends BatchOperationEntry<?, ?>> {

    private final List<T> ops;

    /**
     * Creates new {@link BatchOperation} object.
     */
    public BatchOperation() {
        ops = new LinkedList<>();
    }

    /**
     * Creates {@link BatchOperation} object from a list of batch operation
     * entries.
     *
     * @param batchOperations the list of batch operation entries.
     */
    public BatchOperation(Collection<T> batchOperations) {
        ops = new LinkedList<>(checkNotNull(batchOperations));
    }

    /**
     * Removes all operations maintained in this object.
     */
    public void clear() {
        ops.clear();
    }

    /**
     * Returns the number of operations in this object.
     *
     * @return the number of operations in this object
     */
    public int size() {
        return ops.size();
    }

    /**
     * Returns the operations in this object.
     *
     * @return the operations in this object
     */
    public List<T> getOperations() {
        return Collections.unmodifiableList(ops);
    }

    /**
     * Adds an operation.
     * FIXME: Brian promises that the Intent Framework
     * will not modify the batch operation after it has submitted it.
     * Ali would prefer immutablity, but trusts brian for better or
     * for worse.
     *
     * @param entry the operation to be added
     * @return this object if succeeded, null otherwise
     */
    public BatchOperation<T> addOperation(T entry) {
        return ops.add(entry) ? this : null;
    }

    /**
     * Add all operations from another batch to this batch.
     *
     * @param another another batch
     * @return true if success
     */
    public boolean addAll(BatchOperation<T> another) {
        return ops.addAll(another.getOperations());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null) {
            return false;
        }

        if (getClass() != o.getClass()) {
            return false;
        }
        BatchOperation<?> other = (BatchOperation<?>) o;

        return this.ops.equals(other.ops);
    }

    @Override
    public int hashCode() {
        return ops.hashCode();
    }

    @Override
    public String toString() {
        return ops.toString();
    }
}
