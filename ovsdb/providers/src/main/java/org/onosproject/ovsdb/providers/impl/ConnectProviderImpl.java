package org.onosproject.ovsdb.providers.impl;

import java.util.Map;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Service;
import org.onosproject.ovsdb.lib.OvsdbConnection;
import org.onosproject.ovsdb.providers.ConnectProvider;
import org.onosproject.ovsdb.providers.Connection;
import org.onosproject.ovsdb.providers.Node;
import org.onosproject.ovsdb.providers.constant.ConnectionConstants;
import org.osgi.service.component.ComponentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ConnectService Manager class.
 */
@Component(immediate = true)
@Service
public class ConnectProviderImpl implements ConnectProvider {
    private static final Logger log = LoggerFactory
            .getLogger(ConnectProviderImpl.class);
    private static final Integer DEFAULT_OVSDB_PORT = 6640;
    private volatile OvsdbConnection connectionLib;

    @Activate
    protected void activate(ComponentContext context) {
        log.info("ConnectService started");
        this.start();
    }

    @Deactivate
    protected void deactivate() {
        log.info("ConnectService stopped");
        this.start();
    }

    @Override
    public Node connect(String identifier,
                        Map<ConnectionConstants, String> params) {
        return null;
    }

    @Override
    public Connection getConnection(Node node) {
        // TODO Auto-generated method stub
        return null;
    }

    public void start() {
        /* Start ovsdb server before getting connection clients */
        int ovsdbListenPort = DEFAULT_OVSDB_PORT;

        if (!connectionLib.startOvsdbManager(ovsdbListenPort)) {
            log.warn("Start OVSDB manager call from ConnectionService was not necessary");
        }

    }

}
