/*
 * Copyright 2017-present Open Networking Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.onosproject.p4runtime.ctl;


import com.google.common.collect.Maps;
import com.google.protobuf.Message;
import p4.config.P4InfoOuterClass.Action;
import p4.config.P4InfoOuterClass.ActionProfile;
import p4.config.P4InfoOuterClass.ControllerPacketMetadata;
import p4.config.P4InfoOuterClass.Counter;
import p4.config.P4InfoOuterClass.DirectCounter;
import p4.config.P4InfoOuterClass.DirectMeter;
import p4.config.P4InfoOuterClass.MatchField;
import p4.config.P4InfoOuterClass.Meter;
import p4.config.P4InfoOuterClass.P4Info;
import p4.config.P4InfoOuterClass.Preamble;
import p4.config.P4InfoOuterClass.Table;

import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

/**
 * Utility class to easily retrieve information from a P4Info protobuf message.
 */
final class P4InfoBrowser {

    private final EntityBrowser<Table> tables = new EntityBrowser<>("table");
    private final EntityBrowser<Action> actions = new EntityBrowser<>("action");
    private final EntityBrowser<ActionProfile> actionProfiles = new EntityBrowser<>("action profile");
    private final EntityBrowser<Counter> counters = new EntityBrowser<>("counter");
    private final EntityBrowser<DirectCounter> directCounters = new EntityBrowser<>("direct counter");
    private final EntityBrowser<Meter> meters = new EntityBrowser<>("meter");
    private final EntityBrowser<DirectMeter> directMeters = new EntityBrowser<>("direct meter");
    private final EntityBrowser<ControllerPacketMetadata> ctrlPktMetadatas =
            new EntityBrowser<>("controller packet metadata");
    private final Map<Integer, EntityBrowser<Action.Param>> actionParams = Maps.newHashMap();
    private final Map<Integer, EntityBrowser<MatchField>> matchFields = Maps.newHashMap();

    /**
     * Creates a new browser for the given P4Info.
     *
     * @param p4info P4Info protobuf message
     */
    P4InfoBrowser(P4Info p4info) {
        parseP4Info(p4info);
    }

    private void parseP4Info(P4Info p4info) {
        p4info.getTablesList().forEach(
                entity -> {
                    tables.addWithPreamble(entity.getPreamble(), entity);
                    // Index match fields.
                    int tableId = entity.getPreamble().getId();
                    String tableName = entity.getPreamble().getName();
                    EntityBrowser<MatchField> matchFieldBrowser = new EntityBrowser<>(format(
                            "match field for table '%s'", tableName));
                    entity.getMatchFieldsList().forEach(m -> {
                        String alias = extractMatchFieldSimpleName(m.getName());
                        matchFieldBrowser.add(m.getName(), alias, m.getId(), m);
                    });
                    matchFields.put(tableId, matchFieldBrowser);
                });

        p4info.getActionsList().forEach(
                entity -> {
                    actions.addWithPreamble(entity.getPreamble(), entity);
                    // Index action params.
                    int actionId = entity.getPreamble().getId();
                    String actionName = entity.getPreamble().getName();
                    EntityBrowser<Action.Param> paramBrowser = new EntityBrowser<>(format(
                            "param for action '%s'", actionName));
                    entity.getParamsList().forEach(p -> paramBrowser.add(p.getName(), null, p.getId(), p));
                    actionParams.put(actionId, paramBrowser);
                });

        p4info.getActionProfilesList().forEach(
                entity -> actionProfiles.addWithPreamble(entity.getPreamble(), entity));

        p4info.getCountersList().forEach(
                entity -> counters.addWithPreamble(entity.getPreamble(), entity));

        p4info.getDirectCountersList().forEach(
                entity -> directCounters.addWithPreamble(entity.getPreamble(), entity));

        p4info.getMetersList().forEach(
                entity -> meters.addWithPreamble(entity.getPreamble(), entity));

        p4info.getDirectMetersList().forEach(
                entity -> directMeters.addWithPreamble(entity.getPreamble(), entity));

        p4info.getControllerPacketMetadataList().forEach(
                entity -> ctrlPktMetadatas.addWithPreamble(entity.getPreamble(), entity));
    }

    private String extractMatchFieldSimpleName(String name) {
        // Removes the leading "hdr." or other scope identifier.
        // E.g.: "hdr.ethernet.etherType" becomes "ethernet.etherType"
        String[] pieces = name.split("\\.");
        if (pieces.length == 3) {
            return pieces[1] + "." + pieces[2];
        } else if (pieces.length == 2) {
            return name;
        } else {
            throw new UnsupportedOperationException("Invalid match field name: " + name);
        }
    }

    /**
     * Returns a browser for tables.
     *
     * @return table browser
     */
    EntityBrowser<Table> tables() {
        return tables;
    }

    /**
     * Returns a browser for actions.
     *
     * @return action browser
     */
    EntityBrowser<Action> actions() {
        return actions;
    }

    /**
     * Returns a browser for action profiles.
     *
     * @return action profile browser
     */
    EntityBrowser<ActionProfile> actionProfiles() {
        return actionProfiles;
    }

    /**
     * Returns a browser for counters.
     *
     * @return counter browser
     */
    EntityBrowser<Counter> counters() {
        return counters;
    }

    /**
     * Returns a browser for direct counters.
     *
     * @return direct counter browser
     */
    EntityBrowser<DirectCounter> directCounters() {
        return directCounters;
    }

    /**
     * Returns a browser for meters.
     *
     * @return meter browser
     */
    EntityBrowser<Meter> meters() {
        return meters;
    }

    /**
     * Returns a browser for direct meters.
     *
     * @return table browser
     */
    EntityBrowser<DirectMeter> directMeters() {
        return directMeters;
    }

    /**
     * Returns a browser for controller packet metadata.
     *
     * @return controller packet metadata browser
     */
    EntityBrowser<ControllerPacketMetadata> controllerPacketMetadatas() {
        return ctrlPktMetadatas;
    }

    /**
     * Returns a browser for params of the given action.
     *
     * @param actionId action identifier
     * @return action params browser
     * @throws NotFoundException if the action cannot be found
     */
    EntityBrowser<Action.Param> actionParams(int actionId) throws NotFoundException {
        // Throws exception if action id is not found.
        actions.getById(actionId);
        return actionParams.get(actionId);
    }

    /**
     * Returns a browser for match fields of the given table.
     *
     * @param tableId table identifier
     * @return controller packet metadata browser
     * @throws NotFoundException if the table cannot be found
     */
    EntityBrowser<MatchField> matchFields(int tableId) throws NotFoundException {
        // Throws exception if action id is not found.
        tables.getById(tableId);
        return matchFields.get(tableId);
    }

    /**
     * Browser of P4Info entities.
     *
     * @param <T> protobuf message type
     */
    static final class EntityBrowser<T extends Message> {

        private String entityName;
        private final Map<String, T> names = Maps.newHashMap();
        private final Map<String, T> aliases = Maps.newHashMap();
        private final Map<Integer, T> ids = Maps.newHashMap();

        private EntityBrowser(String entityName) {
            this.entityName = entityName;
        }

        /**
         * Adds the given entity identified by the given name, alias (nullable) and id.
         *
         * @param name   entity name
         * @param alias  entity alias or null
         * @param id     entity id
         * @param entity entity message
         */
        void add(String name, String alias, int id, T entity) {
            checkNotNull(name);
            checkArgument(!name.isEmpty(), "Name cannot be empty");
            checkNotNull(entity);
            names.put(name, entity);
            ids.put(id, entity);
            if (alias != null && !alias.isEmpty()) {
                aliases.put(alias, entity);
            }
        }

        /**
         * Adds the given entity identified by the given P4Info preamble.
         *
         * @param preamble P4Info preamble protobuf message
         * @param entity   entity message
         */
        void addWithPreamble(Preamble preamble, T entity) {
            checkNotNull(preamble);
            add(preamble.getName(), preamble.getAlias(), preamble.getId(), entity);
        }

        /**
         * Returns true if the P4Info defines an entity with such name, false otherwise.
         *
         * @param name entity name
         * @return boolean
         */
        boolean hasName(String name) {
            return names.containsKey(name);
        }

        /**
         * Returns the entity identified by the given name, if present, otherwise, throws an exception.
         *
         * @param name entity name
         * @return entity message
         * @throws NotFoundException if the entity cannot be found
         */
        T getByName(String name) throws NotFoundException {
            if (!hasName(name)) {
                throw new NotFoundException(entityName, name);
            }
            return names.get(name);
        }

        /**
         * Returns the entity identified by the given name or alias, if present, otherwise, throws an exception.
         *
         * @param name entity name or alias
         * @return entity message
         * @throws NotFoundException if the entity cannot be found
         */
        T getByNameOrAlias(String name) throws NotFoundException {
            if (hasName(name)) {
                return names.get(name);
            } else if (hasAlias(name)) {
                return aliases.get(name);
            } else {
                throw new NotFoundException(entityName, name);
            }
        }

        /**
         * Returns true if the P4Info defines an entity with such alias, false otherwise.
         *
         * @param alias entity alias
         * @return boolean
         */
        boolean hasAlias(String alias) {
            return aliases.containsKey(alias);
        }

        /**
         * Returns the entity identified by the given alias, if present, otherwise, throws an exception.
         *
         * @param alias entity alias
         * @return entity message
         * @throws NotFoundException if the entity cannot be found
         */
        T getByAlias(String alias) throws NotFoundException {
            if (!hasName(alias)) {
                throw new NotFoundException(entityName, alias);
            }
            return aliases.get(alias);
        }

        /**
         * Returns true if the P4Info defines an entity with such id, false otherwise.
         *
         * @param id entity id
         * @return boolean
         */
        boolean hasId(int id) {
            return ids.containsKey(id);
        }

        /**
         * Returns the entity identified by the given id, if present, otherwise, throws an exception.
         *
         * @param id entity id
         * @return entity message
         * @throws NotFoundException if the entity cannot be found
         */
        T getById(int id) throws NotFoundException {
            if (!hasId(id)) {
                throw new NotFoundException(entityName, id);
            }
            return ids.get(id);
        }
    }

    /**
     * Signals tha an entity cannot be found in the P4Info.
     */
    public static final class NotFoundException extends Exception {

        NotFoundException(String entityName, String key) {
            super(format("No such %s in P4Info with name/alias '%s'", entityName, key));
        }

        NotFoundException(String entityName, int id) {
            super(format("No such %s in P4Info with id '%d'", entityName, id));
        }
    }
}
