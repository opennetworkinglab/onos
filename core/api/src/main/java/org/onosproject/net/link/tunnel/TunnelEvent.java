package org.onosproject.net.link.tunnel;

import java.util.Collection;

import org.onosproject.event.AbstractEvent;
import org.onosproject.net.DefaultTunnel;

public final class TunnelEvent
        extends AbstractEvent<TunnelEvent.Type, Collection<DefaultTunnel>> {

    enum Type {

    }

    protected TunnelEvent(Type type, Collection<DefaultTunnel> subject) {
        super(type, subject);
        // TODO Auto-generated constructor stub
    }

}
