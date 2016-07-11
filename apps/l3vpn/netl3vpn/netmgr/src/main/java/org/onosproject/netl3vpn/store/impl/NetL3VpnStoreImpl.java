package org.onosproject.netl3vpn.store.impl;

import static com.google.common.base.Preconditions.checkNotNull;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.netl3vpn.entity.WebL3vpnInstance;
import org.onosproject.netl3vpn.store.NetL3vpnStore;
import org.onosproject.store.service.ConsistentMap;

/**
 * Manages the pool of available labels to vpn.
 */
@Component(immediate = true)
@Service
public class NetL3VpnStoreImpl implements NetL3vpnStore {
    protected ConsistentMap<String, WebL3vpnInstance> webL3vpnMap;

    private static final String INSTANCE_ID_NULL = "Instance id cannot be null";
    private static final String INSTANCE_NAME_NULL = "Instance name cannot be null";
    private static final String INSTANCE_ENTITY_NULL = "Instance entity cannot be null";

    @Override
    public boolean existsVpnId(String vpnId) {
        checkNotNull(vpnId, INSTANCE_ID_NULL);
        return webL3vpnMap.containsKey(vpnId);
    }

    @Override
    public boolean existsVpnName(String vpnId, String vpnName) {
        checkNotNull(vpnId, INSTANCE_ID_NULL);
        checkNotNull(vpnName, INSTANCE_ID_NULL);
        return webL3vpnMap.get(vpnId).value().getName() == vpnName ? true
                                                                   : false;
    }

    @Override
    public WebL3vpnInstance getWebL3vpnInstance(String vpnId) {
        checkNotNull(vpnId, INSTANCE_NAME_NULL);
        return webL3vpnMap.get(vpnId).value();
    }

    @Override
    public void addWebL3vpnInstance(String id,
                                    WebL3vpnInstance webL3vpnInstance) {
        checkNotNull(id, INSTANCE_ID_NULL);
        checkNotNull(webL3vpnInstance, INSTANCE_ENTITY_NULL);
        webL3vpnMap.put(id, webL3vpnInstance);
    }

}
