package org.onosproject.ovsdb.providers;

import java.util.Map;

import org.onosproject.ovsdb.providers.constant.ConnectionConstants;

/**
 * Connect Service Provider.
 *
 * @author
 *
 */
public interface ConnectProvider {

    /**
     * Get Connection Information.
     *
     * @param node
     * @return Connection
     */
    public Connection getConnection(Node node);

    /**
     * Connect with the node by identifier and params.
     *
     * @param identifier
     * @param params
     * @return Node
     */
    public Node connect(String identifier,
                        Map<ConnectionConstants, String> params);

}
