package org.onosproject.ovsdb.providers;

import java.net.InetAddress;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import org.apache.felix.scr.annotations.Property;
import org.onosproject.ovsdb.lib.message.TableUpdates;
import org.onosproject.ovsdb.lib.notation.Row;

/**
 * Inventory Provider Api.
 */
public interface InventoryProvider {

    /**
     * Get Row information.
     *
     * @param node
     * @param databaseName
     * @param tableName
     * @param uuid
     * @return Row
     */
    public Row getRow(Node node, String databaseName, String tableName,
                      String uuid);

    /**
     * Update Row information.
     *
     * @param node
     * @param databaseName
     * @param tableName
     * @param uuid
     * @param row
     */
    public void updateRow(Node node, String databaseName, String tableName,
                          String uuid, Row row);

    /**
     * Remove Row information.
     *
     * @param node
     * @param databaseName
     * @param tableName
     * @param uuid
     */
    public void removeRow(Node node, String databaseName, String tableName,
                          String uuid);

    /**
     * Process Table Updates when Table information changed.
     *
     * @param node
     * @param databaseName
     * @param tableUpdates
     */
    public void processTableUpdates(Node node, String databaseName,
                                    TableUpdates tableUpdates);

    /**
     * Get Table cache.
     *
     * @param n
     * @param databaseName
     * @param tableName
     * @return
     */
    public ConcurrentMap<String, Row> getTableCache(Node node,
                                                    String databaseName,
                                                    String tableName);

    /**
     * Get database cache.
     *
     * @param node
     * @param databaseName
     * @return
     */
    public ConcurrentMap<String, ConcurrentMap<String, Row>> getCache(Node node,
                                                                      String databaseName);

    /**
     * Add Node into cache.
     *
     * @param node
     * @param props
     */
    public void addNode(Node node, Set<Property> props);

    /**
     * Remove Node from cache.
     *
     * @param node
     */
    public void removeNode(Node node);

    /**
     * Notify Node added to the Onos Core.
     *
     * @param node
     * @param address
     * @param port
     */
    public void notifyNodeAdded(Node node, InetAddress address, int port);

}
