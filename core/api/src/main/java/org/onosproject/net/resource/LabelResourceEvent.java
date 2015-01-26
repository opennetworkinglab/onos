package org.onosproject.net.resource;

import java.util.Collection;

import org.onosproject.event.AbstractEvent;

public final class LabelResourceEvent
        extends
        AbstractEvent<LabelResourceEvent.Type, Collection<DefaultLabelResource>> {

    public enum Type {
        POOL_CREATED, POOL_DESTROYED, POOL_CAPACITY_CHANGED
    }

    public LabelResourceEvent(Type type,
                              Collection<DefaultLabelResource> subject) {
        super(type, subject);
    }
}
