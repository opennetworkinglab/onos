package org.onosproject.ovsdb.lib;

import org.onosproject.ovsdb.lib.message.TableUpdates;
import org.onosproject.ovsdb.lib.schema.DatabaseSchema;

/**
 * Monitor call back.
 *
 *
 */
public interface MonitorCallBack {
    /**
     * callback function.
     *
     * @param result
     * @param dbSchema
     */
    void update(TableUpdates result, DatabaseSchema dbSchema);
}
