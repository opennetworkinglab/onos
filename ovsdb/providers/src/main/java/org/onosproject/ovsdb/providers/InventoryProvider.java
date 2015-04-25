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
    public ConcurrentMap<String, ConcurrentMap<String, Row>> getCache(Node n,
                                                                      String databaseName);

    public ConcurrentMap<String, Row> getTableCache(Node n,
                                                    String databaseName,
                                                    String tableName);

    public Row getRow(Node n, String databaseName, String tableName, String uuid);

    public void updateRow(Node n, String databaseName, String tableName,
                          String uuid, Row row);

    public void removeRow(Node n, String databaseName, String tableName,
                          String uuid);

    public void processTableUpdates(Node n, String databaseName,
                                    TableUpdates tableUpdates);

    public void addNode(Node n, Set<Property> props);

    public void notifyNodeAdded(Node n, InetAddress address, int port);

    public void removeNode(Node n);

}
