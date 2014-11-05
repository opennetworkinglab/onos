package org.onlab.onos.store.service;

import java.util.List;

/**
 * Service for running administrative tasks on a Database.
 */
public interface DatabaseAdminService {

    /**
     * Creates a new table.
     * Table creation is idempotent. Attempting to create a table
     * that already exists will be a noop.
     * @param name table name.
     * @return true if the table was created by this call, false otherwise.
     */
    public boolean createTable(String name);

    /**
     * Lists all the tables in the database.
     * @return list of table names.
     */
    public List<String> listTables();

    /**
     * Deletes a table from the database.
     * @param name name of the table to delete.
     */
    public void dropTable(String name);

    /**
     * Deletes all tables from the database.
     */
    public void dropAllTables();
}
