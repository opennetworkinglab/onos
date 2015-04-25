package org.onosproject.ovsdb.providers;

import java.util.List;
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
     * Connect service for ovsdb.
     *
     * @param identifier
     * @param params
     * @return Node
     */
    public Node connect(String identifier,
                        Map<ConnectionConstants, String> params);

    /**
     * Disconnect with ovsdb.
     */
    public void disconnect();

    /**
     * start the ovsdb server.
     */
    void start();

    /**
     * stop the ovsdb server.
     */
    void stop();

    public Connection getConnection(Node node);

    public List<Node> getNodes();
}
