package org.onosproject.ovsdb.lib;

import org.onlab.packet.IpAddress;

public interface OvsdbConnection {
    /**
     * connect API can be used by the applications to initiate Active connection
     * from the controller towards ovsdb-server.
     * @param address IP Address of the remote server that hosts the ovsdb
     *            server.
     * @param port Layer 4 port on which the remote ovsdb server is listening
     *            on.
     * @return OvsDBClient The primary Client interface for the ovsdb
     *         connection.
     */
    public OvsdbClient connect(IpAddress address, int port);

    /**
     * Method to disconnect an existing connection.
     * @param client that represents the ovsdb connection.
     */
    public void disconnect(OvsdbClient client);

    /**
     * Method to start ovsdb server for passive connection.
     * @param ovsdbListenPort
     * @return
     */
    public boolean startOvsdbManager(final int ovsdbListenPort);
}
