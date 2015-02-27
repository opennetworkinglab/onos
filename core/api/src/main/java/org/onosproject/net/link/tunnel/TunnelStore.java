package org.onosproject.net.link.tunnel;

import org.onosproject.net.Tunnel;
import org.onosproject.store.Store;

/**
 * manages inventory of tunnel; not intended for direct use.
 */
public interface TunnelStore extends Store<TunnelEvent, TunnelDelegate> {

    void createTunnel(Tunnel tunnel);
    
}
