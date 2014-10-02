package org.onlab.onos.net.intent;
//TODO is this the right package?

import java.util.Objects;

import com.google.common.base.MoreObjects;

/**
 * A super class for batch operation entry classes.
 * <p>
 * This is the interface to classes which are maintained by BatchOperation as
 * its entries.
 */
public class BatchOperationEntry<T extends Enum<?>, U extends BatchOperationTarget> {
    private final T operator;
    private final U target;

    /**
     * Default constructor for serializer.
     */
    @Deprecated
    protected BatchOperationEntry() {
        this.operator = null;
        this.target = null;
    }

    /**
     * Constructs new instance for the entry of the BatchOperation.
     *
     * @param operator the operator of this operation
     * @param target the target object of this operation
     */
    public BatchOperationEntry(T operator, U target) {
        this.operator = operator;
        this.target = target;
    }

    /**
     * Gets the target object of this operation.
     *
     * @return the target object of this operation
     */
    public U getTarget() {
        return target;
    }

    /**
     * Gets the operator of this operation.
     *
     * @return the operator of this operation
     */
    public T getOperator() {
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
