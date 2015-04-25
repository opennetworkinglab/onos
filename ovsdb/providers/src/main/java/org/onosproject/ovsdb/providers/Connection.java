package org.onosproject.ovsdb.providers;

import org.onosproject.ovsdb.lib.OvsdbClient;

/**
 * Connection Information.
 */
public final class Connection {
    private Node node;
    private String identifier;
    private OvsdbClient client;

    private Connection(Node node, String identifier, OvsdbClient client) {
        super();
        this.node = node;
        this.identifier = identifier;
        this.client = client;
    }

}
