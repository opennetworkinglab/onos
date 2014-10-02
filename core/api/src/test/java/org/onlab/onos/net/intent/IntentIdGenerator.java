package org.onlab.onos.net.intent;

/**
 * This interface is for generator of IntentId. It is defined only for
 * testing purpose to keep type safety on mock creation.
 *
 * <p>
 * {@link #getNewId()} generates a globally unique {@link IntentId} instance
 * on each invocation. Application developers should not generate IntentId
 * by themselves. Instead use an implementation of this interface.
 * </p>
 */
public interface IntentIdGenerator extends IdGenerator<IntentId> {
}
