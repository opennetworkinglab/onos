/*
 * Copyright (C) 2014 EBay Software Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Ashwin Raveendran
 */
package org.onosproject.ovsdb.lib.schema;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.onosproject.ovsdb.lib.error.ParsingException;
import org.onosproject.ovsdb.lib.notation.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.reflect.Invokable;

/**
 * Represents an ovsdb database schema, which is comprised of a set of tables.
 */
public class DatabaseSchema {

    public static Logger logger = LoggerFactory.getLogger(DatabaseSchema.class);

    private String name;

    private Version version;
    private Map<String, TableSchema> tables;

    public DatabaseSchema(Map<String, TableSchema> tables) {
        this.tables = tables;
    }

    public DatabaseSchema(String name, Version version,
                          Map<String, TableSchema> tables) {
        this.name = name;
        this.version = version;
        this.tables = tables;
    }

    public Set<String> getTables() {
        return this.tables.keySet();
    }

    public boolean hasTable(String table) {
        return this.getTables().contains(table);
    }

    public <E extends TableSchema<E>> E table(String tableName, Class<E> clazz) {
        TableSchema<E> table = tables.get(tableName);

        if (clazz.isInstance(table)) {
            return clazz.cast(table);
        }

        return createTableSchema(clazz, table);
    }

    protected <E extends TableSchema<E>> E createTableSchema(Class<E> clazz,
                                                             TableSchema<E> table) {
        Constructor<E> declaredConstructor = null;
        try {
            declaredConstructor = clazz
                    .getDeclaredConstructor(TableSchema.class);
        } catch (NoSuchMethodException e) {
            String message = String
                    .format("Class %s does not have public constructor that accepts TableSchema object",
                            clazz);
            throw new IllegalArgumentException(message, e);
        }
        Invokable<E, E> invokable = Invokable.from(declaredConstructor);
        try {
            return invokable.invoke(null, table);
        } catch (Exception e) {
            String message = String
                    .format("Not able to create instance of class %s using public constructor "
                                    + "that accepts TableSchema object", clazz);
            throw new IllegalArgumentException(message, e);
        }
    }

    // todo : this needs to move to a custom factory
    public static DatabaseSchema fromJson(String dbName, JsonNode json) {
        if (!json.isObject() || !json.has("tables")) {
            throw new ParsingException(
                                       "bad DatabaseSchema root, expected \"tables\" as child but was not found");
        }
        if (!json.isObject() || !json.has("version")) {
            throw new ParsingException(
                                       "bad DatabaseSchema root, expected \"version\" as child but was not found");
        }

        Version dbVersion = Version.fromString(json.get("version").asText());

        Map<String, TableSchema> tables = new HashMap<>();
        for (Iterator<Map.Entry<String, JsonNode>> iter = json.get("tables")
                .fields(); iter.hasNext();) {
            Map.Entry<String, JsonNode> table = iter.next();
            logger.trace("Read schema for table[{}]:{}", table.getKey(),
                         table.getValue());

            // todo : this needs to done by a factory
            tables.put(table.getKey(),
                       new GenericTableSchema().fromJson(table.getKey(),
                                                         table.getValue()));
        }

        return new DatabaseSchema(dbName, dbVersion, tables);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Version getVersion() {
        return version;
    }

    public void setVersion(Version version) {
        this.version = version;
    }

    public void populateInternallyGeneratedColumns() {
        for (TableSchema tableSchema : tables.values()) {
            tableSchema.populateInternallyGeneratedColumns();
        }
    }
}
