package org.onlab.onos.event;

/**
 * Abstraction of an entity capable of accepting events to be posted and
 * then dispatching them to the appropriate event sink.
 */
public interface EventDeliveryService extends EventDispatcher, EventSinkRegistry {
}
