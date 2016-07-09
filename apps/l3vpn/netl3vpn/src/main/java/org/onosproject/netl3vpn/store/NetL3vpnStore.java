package org.onosproject.netl3vpn.store;

import java.util.Map;

import org.onosproject.incubator.net.resource.label.LabelResourceId;
import org.onosproject.net.DeviceId;
import org.onosproject.net.Link;
import org.onosproject.netl3vpn.entity.WebL3vpnInstance;

/**
 * Abstraction of an entity providing pool of available labels to vpn.
 */
public interface NetL3vpnStore {
    
    /**
     * Checks whether vpn id is present in global node label store.
     *
     * @param id vpn id
     * @return success of failure
     */
    boolean existsVpnId(String vpnId);
    
    /**
     * Checks whether vpn name is present in global node label store.
     *
     * @param name vpn name
     * @return success of failure
     */
    boolean existsVpnName(String vpnId, String vpnName);
    
    /**
     * Retrieves WebL3vpnInstance by id from store.
     *
     * @return WebL3vpnInstance entity
     */
    WebL3vpnInstance getWebL3vpnInstance(String id);
    
    /**
     * Stores adjacency label into adjacency label store.
     *
     * @param link link between nodes
     * @param labelId link label id
     */
    void addWebL3vpnInstance(String id, WebL3vpnInstance webL3vpnInstance);
}
