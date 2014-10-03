package org.onlab.onos.net.intent;
//TODO is this the right package?

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

    private List<T> ops;

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
    public BatchOperation(List<T> batchOperations) {
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
     *
     * @param entry the operation to be added
     * @return this object if succeeded, null otherwise
     */
    public BatchOperation<T> addOperation(T entry) {
        return ops.add(entry) ? this : null;
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
