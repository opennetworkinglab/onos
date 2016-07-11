package org.onosproject.netl3vpn.manager;

import org.onosproject.yang.gen.v1.net.l3vpn.rev20160701.netl3vpn.instances.Instance;

public interface NetL3vpnService {
    /**
     * Creates instances by instances.
     *
     * @param instance the instance
     * @return true if all given identifiers created successfully.
     */
    boolean createL3vpn(Instance instance);
}
