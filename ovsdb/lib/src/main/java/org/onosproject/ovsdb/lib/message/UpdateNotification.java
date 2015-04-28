/*
 * Copyright (C) 2013 EBay Software Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Ashwin Raveendran, Madhu Venugopal
 */
package org.onosproject.ovsdb.lib.message;


import org.onosproject.ovsdb.lib.notation.json.Converter.UpdateNotificationConverter;
import org.onosproject.ovsdb.lib.schema.DatabaseSchema;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(converter = UpdateNotificationConverter.class)
public class UpdateNotification {
    Object context;
    DatabaseSchema databaseSchema;
    TableUpdates update;
    private JsonNode updatesJson;

    public Object getContext() {
        return context;
    }
    public void setContext(Object context) {
        this.context = context;
    }
    public TableUpdates getUpdate() {
        return update;
    }
    public void setUpdate(TableUpdates update) {
        this.update = update;
    }

    @JsonAnySetter
    public void setValue(String key, JsonNode val) {
        System.out.println("key = " + key);
        System.out.println("val = " + val);
        System.out.println();
    }

    public void setUpdates(JsonNode jsonNode) {
        this.updatesJson = jsonNode;
    }

    public JsonNode getUpdates() {
        return updatesJson;
    }

    public DatabaseSchema getDatabaseSchema() {
        return databaseSchema;
    }
    public void setDatabaseSchema(DatabaseSchema databaseSchema) {
        this.databaseSchema = databaseSchema;
    }
}
