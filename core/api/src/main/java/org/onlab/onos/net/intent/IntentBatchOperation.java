package org.onlab.onos.net.intent;

/**
 * A list of intent operations.
 */
public class IntentBatchOperation extends
        BatchOperation<BatchOperationEntry<IntentBatchOperation.Operator, ?>> {
    /**
     * The intent operators.
     */
    public enum Operator {
        ADD,
        REMOVE,
    }

    /**
     * Adds an add-intent operation.
     *
     * @param intent the intent to be added
     * @return the IntentBatchOperation object if succeeded, null otherwise
     */
    public IntentBatchOperation addAddIntentOperation(Intent intent) {
        return (null == super.addOperation(
                new BatchOperationEntry<Operator, Intent>(Operator.ADD, intent)))
                ? null : this;
    }

    /**
     * Adds a remove-intent operation.
     *
     * @param id the ID of intent to be removed
     * @return the IntentBatchOperation object if succeeded, null otherwise
     */
    public IntentBatchOperation addRemoveIntentOperation(IntentId id) {
        return (null == super.addOperation(
                new BatchOperationEntry<Operator, IntentId>(Operator.REMOVE, id)))
                ? null : this;
    }
}
