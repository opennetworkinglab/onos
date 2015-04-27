package org.onosproject.ovsdb.providers;

import java.util.List;
import java.util.concurrent.ExecutionException;

import org.onosproject.ovsdb.lib.notation.Row;
import org.onosproject.ovsdb.lib.notation.UUID;
import org.onosproject.ovsdb.lib.schema.GenericTableSchema;

/**
 * Onos Configuration Provider for ovsdb.
 */
public interface ConfigurationProvider {

    /**
     * insert a Row in a Table of a specified Database Schema. This is a
     * convenience method on top of {@link insertRow(Node, String, String,
     * String, UUID, String, Row<GenericTableSchema>) insertRow} which assumes
     * that OVSDB schema implementation that corresponds to the databaseName
     * will provide the necessary service to populate the Parent Table Name and
     * Parent Column Name.
     *
     * This method can insert just a single Row specified in the row parameter.
     * But {@link #insertTree(Node, String, String, UUID,
     * Row<GenericTableSchema>) insertTree} can insert a hierarchy of rows with
     * parent-child relationship.
     *
     * @param node OVSDB Node
     * @param databaseName Database Name that represents the Schema supported by
     *            the node.
     * @param tableName Table on which the row is inserted
     * @param parentUuid UUID of the parent table to which this operation will
     *            result in attaching/mutating.
     * @param row Row of table Content to be inserted @ Any failure during the
     *            insert transaction will result in a specific exception.
     * @return UUID of the inserted Row
     */
    public UUID insertRow(Node node, String databaseName, String tableName,
                          UUID parentRowUuid, Row<GenericTableSchema> row);

    /**
     * insert a Row in a Table of a specified Database Schema.
     *
     * This method can insert just a single Row specified in the row parameter.
     * But {@link #insertTree(Node, String, String, UUID,
     * Row<GenericTableSchema>) insertTree} can insert a hierarchy of rows with
     * parent-child relationship.
     *
     * @param node OVSDB Node
     * @param databaseName Database Name that represents the Schema supported by
     *            the node.
     * @param tableName Table on which the row is inserted
     * @param parentTable Name of the Parent Table to which this operation will
     *            result in attaching/mutating.
     * @param parentUuid UUID of a Row in parent table to which this operation
     *            will result in attaching/mutating.
     * @param parentColumn Name of the Column in the Parent Table to be mutated
     *            with the UUID that results from the insert operation.
     * @param row Row of table Content to be inserted @ Any failure during the
     *            insert transaction will result in a specific exception.
     * @return UUID of the inserted Row
     */
    public UUID insertRow(Node node, String databaseName, String tableName,
                          String parentTable, UUID parentRowUuid,
                          String parentColumn, Row<GenericTableSchema> row);

    /**
     * inserts a Tree of Rows in multiple Tables that has parent-child
     * relationships referenced through the OVSDB schema's refTable construct.
     * This is a convenience method on top of {@link #insertTree(Node, String,
     * String, String, UUID, String, Row<GenericTableSchema>) insertTree}
     *
     * @param node OVSDB Node
     * @param databaseName Database Name that represents the Schema supported by
     *            the node.
     * @param tableName Table on which the row is inserted
     * @param parentUuid UUID of a Row in parent table to which this operation
     *            will result in attaching/mutating.
     * @param row Row Tree with parent-child relationships via column of type
     *            refTable. @ Any failure during the insert transaction will
     *            result in a specific exception.
     * @return Returns the row tree with the UUID of every inserted Row
     *         populated in the _uuid column of every row in the tree
     */
    public Row<GenericTableSchema> insertTree(Node node, String databaseName,
                                              String tableName,
                                              UUID parentRowUuid,
                                              Row<GenericTableSchema> row);

    /**
     * inserts a Tree of Rows in multiple Tables that has parent-child
     * relationships referenced through the OVSDB schema's refTable construct.
     *
     * @param node OVSDB Node
     * @param databaseName Database Name that represents the Schema supported by
     *            the node.
     * @param tableName Table on which the row is inserted
     * @param parentTable Name of the Parent Table to which this operation will
     *            result in attaching/mutating.
     * @param parentUuid UUID of a Row in parent table to which this operation
     *            will result in attaching/mutating.
     * @param parentColumn Name of the Column in the Parent Table to be mutated
     *            with the UUID that results from the insert operation.
     * @param row Row Tree with parent-child relationships via column of type
     *            refTable. @ Any failure during the insert transaction will
     *            result in a specific exception.
     * @return Returns the row tree with the UUID of every inserted Row
     *         populated in the _uuid column of every row in the tree
     */
    public Row<GenericTableSchema> insertTree(Node node, String databaseName,
                                              String tableName,
                                              String parentTable,
                                              UUID parentRowUuid,
                                              String parentColumn,
                                              Row<GenericTableSchema> row);

    /**
     * update or mutate a Row in a Table of a specified Database Schema.
     *
     * @param node OVSDB Node
     * @param databaseName Database Name that represents the Schema supported by
     *            the node.
     * @param tableName Table on which the row is updated
     * @param rowUuid UUID of the row being updated
     * @param row Row of table Content to be updated
     * @param overwrite true will overwrite/replace the existing row (matching
     *            the rowUuid) with the passed row object. false will update the
     *            existing row (matching the rowUuid) using only the columns in
     *            the passed row object. @ Any failure during the update
     *            operation will result in a specific exception.
     * @return Returns the entire Row after the update operation.
     */
    public Row<GenericTableSchema> updateRow(Node node, String databaseName,
                                             String tableName, UUID rowUuid,
                                             Row<GenericTableSchema> row,
                                             boolean overwrite);

    /**
     * update or mutate a Row in a Table of a specified Database Schema.
     *
     * @param node OVSDB Node
     * @param databaseName Database Name that represents the Schema supported by
     *            the node.
     * @param tableName Table on which the row is Updated
     * @param rowUuid UUID of the row that is being deleted @ Any failure during
     *            the delete operation will result in a specific exception.
     */

    public void deleteRow(Node node, String databaseName, String tableName,
                          UUID rowUuid);

    /**
     * update or mutate a Row in a Table of a specified Database Schema.
     *
     * @param node OVSDB Node
     * @param databaseName Database Name that represents the Schema supported by
     *            the node.
     * @param tableName Table on which the row is Updated
     * @param parentTable Name of the Parent Table to which this operation will
     *            result in mutating.
     * @param parentColumn Name of the Column in the Parent Table to be mutated.
     * @param rowUuid UUID of the row that is being deleted @ Any failure during
     *            the delete operation will result in a specific exception.
     */

    public void deleteRow(Node node, String databaseName, String tableName,
                          String parentTable, UUID parentRowUuid,
                          String parentColumn, UUID rowUuid);

    /**
     * Returns all the Tables in a given Node.
     *
     * @param node OVSDB node
     * @param databaseName Database Name that represents the Schema supported by
     *            the node. @ Any failure during the get operation will result
     *            in a specific exception.
     * @return List of Table Names that make up the schema represented by the
     *         databaseName
     */
    public List<String> getTables(Node node, String databaseName);

    /**
     * setOFController is a convenience method used by existing applications to
     * setup Openflow Controller on a Open_vSwitch Bridge. This API assumes an
     * Open_vSwitch database Schema.
     *
     * @param node Node
     * @param bridgeUUID uuid of the Bridge for which the ip-address of Openflow
     *            Controller should be programmed.
     * @return Boolean representing success or failure of the operation.
     *
     * @throws InterruptedException
     * @throws ExecutionException
     */
    public Boolean setOFController(Node node, String bridgeUUID)
            throws InterruptedException, ExecutionException;

}
