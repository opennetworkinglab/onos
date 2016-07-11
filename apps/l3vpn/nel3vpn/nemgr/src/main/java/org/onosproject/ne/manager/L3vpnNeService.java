package org.onosproject.ne.manager;

import org.onosproject.ne.NeData;

public interface L3vpnNeService {

    /**
     * Creates L3vpn.
     *
     * @param nedata the data of l3vpn ne.
     * @return boolean
     */
    boolean createL3vpn(NeData nedata);

}
