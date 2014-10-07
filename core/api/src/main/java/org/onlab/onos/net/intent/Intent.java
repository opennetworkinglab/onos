package org.onlab.onos.net.intent;

/**
 * Abstraction of an application level intent.
 * <p/>
 * Make sure that an Intent should be immutable when a new type is defined.
 */
public interface Intent extends BatchOperationTarget {
    /**
     * Returns the intent identifier.
     *
     * @return intent identifier
     */
    IntentId id();
}
