package org.onosproject.incubator.net.resource.label;

import com.google.common.annotations.Beta;
import org.onosproject.event.AbstractEvent;

/**
 * Describes label resource event.
 */
@Beta
public final class LabelResourceEvent
        extends AbstractEvent<LabelResourceEvent.Type, LabelResourcePool> {

    /**
     * Type of label resource event.
     */
    public enum Type {
        /**
         * Signifies that a new pool has been administratively created.
         */
        POOL_CREATED,
        /**
         * Signifies that a new pool has been administratively destroyed.
         */
        POOL_DESTROYED,
        /**
         * Signifies that a new pool has been administratively changed.
         */
        POOL_CAPACITY_CHANGED
    }

    /**
     * Creates an event of a given type and the given LabelResourcePool.
     *
     * @param type event type
     * @param subject pool
     */
    public LabelResourceEvent(Type type, LabelResourcePool subject) {
        super(type, subject);
    }
}
