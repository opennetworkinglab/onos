/*
 * Copyright 2016-present Open Networking Laboratory
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

package org.onosproject.bmv2.api.model;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.onosproject.bmv2.api.runtime.Bmv2MatchParam;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Partial representation of a packet processing model for BMv2. Such a model is
 * used to define the way BMv2 should process packets (i.e. it defines the
 * device ingress/egress pipelines, parser, tables, actions, etc.) and can be
 * generated (i.e. JSON) by compiling a P4 program using p4c-bm.
 * <p>
 * It must be noted that this class exposes only a subset of the full model
 * properties (only those that are needed for the purpose of mapping ONOS types
 * to BMv2 types.
 *
 * @see <a href="https://github.com/p4lang/p4c-bm">
 * P4 front-end compiler for BMv2 (p4c-bm)</a>
 */
public final class Bmv2Model {

    private final JsonObject json;
    private final DualKeySortedMap<Bmv2ModelHeaderType> headerTypes = new DualKeySortedMap<>();
    private final DualKeySortedMap<Bmv2ModelHeader> headers = new DualKeySortedMap<>();
    private final DualKeySortedMap<Bmv2ModelAction> actions = new DualKeySortedMap<>();
    private final DualKeySortedMap<Bmv2ModelTable> tables = new DualKeySortedMap<>();

    private Bmv2Model(JsonObject json) {
        this.json = JsonObject.unmodifiableObject(json);
    }

    /**
     * Returns a new BMv2 model object by parsing the passed JSON.
     *
     * @param json json
     * @return a new BMv2 configuration object
     * @see <a href="https://github.com/p4lang/behavioral-model/blob/master/docs/JSON_format.md">
     * BMv2 JSON specification</a>
     */
    public static Bmv2Model parse(JsonObject json) {
        checkArgument(json != null, "json cannot be null");
        // TODO: implement caching, no need to parse a json if we already have the model
        Bmv2Model model = new Bmv2Model(json);
        model.doParse();
        return model;
    }

    /**
     * Returns the header type associated with the passed numeric id,
     * null if there's no such an id in the model.
     *
     * @param id integer value
     * @return header type object or null
     */
    public Bmv2ModelHeaderType headerType(int id) {
        return headerTypes.get(id);
    }

    /**
     * Returns the header type associated with the passed name,
     * null if there's no such a name in the model.
     *
     * @param name string value
     * @return header type object or null
     */
    public Bmv2ModelHeaderType headerType(String name) {
        return headerTypes.get(name);
    }

    /**
     * Returns the list of all the header types defined by in this model.
     * Values returned are sorted in ascending order based on the numeric id.
     *
     * @return list of header types
     */
    public List<Bmv2ModelHeaderType> headerTypes() {
        return ImmutableList.copyOf(headerTypes.sortedMap().values());
    }

    /**
     * Returns the header associated with the passed numeric id,
     * null if there's no such an id in the model.
     *
     * @param id integer value
     * @return header object or null
     */
    public Bmv2ModelHeader header(int id) {
        return headers.get(id);
    }

    /**
     * Returns the header associated with the passed name,
     * null if there's no such a name in the model.
     *
     * @param name string value
     * @return header object or null
     */
    public Bmv2ModelHeader header(String name) {
        return headers.get(name);
    }

    /**
     * Returns the list of all the header instances defined in this model.
     * Values returned are sorted in ascending order based on the numeric id.
     *
     * @return list of header types
     */
    public List<Bmv2ModelHeader> headers() {
        return ImmutableList.copyOf(headers.sortedMap().values());
    }

    /**
     * Returns the action associated with the passed numeric id,
     * null if there's no such an id in the model.
     *
     * @param id integer value
     * @return action object or null
     */
    public Bmv2ModelAction action(int id) {
        return actions.get(id);
    }

    /**
     * Returns the action associated with the passed name,
     * null if there's no such a name in the model.
     *
     * @param name string value
     * @return action object or null
     */
    public Bmv2ModelAction action(String name) {
        return actions.get(name);
    }

    /**
     * Returns the list of all the actions defined by in this model.
     * Values returned are sorted in ascending order based on the numeric id.
     *
     * @return list of actions
     */
    public List<Bmv2ModelAction> actions() {
        return ImmutableList.copyOf(actions.sortedMap().values());
    }

    /**
     * Returns the table associated with the passed numeric id,
     * null if there's no such an id in the model.
     *
     * @param id integer value
     * @return table object or null
     */
    public Bmv2ModelTable table(int id) {
        return tables.get(id);
    }

    /**
     * Returns the table associated with the passed name,
     * null if there's no such a name in the model.
     *
     * @param name string value
     * @return table object or null
     */
    public Bmv2ModelTable table(String name) {
        return tables.get(name);
    }

    /**
     * Returns the list of all the tables defined by in this model.
     * Values returned are sorted in ascending order based on the numeric id.
     *
     * @return list of actions
     */
    public List<Bmv2ModelTable> tables() {
        return ImmutableList.copyOf(tables.sortedMap().values());
    }

    /**
     * Return an unmodifiable view of the low-level JSON representation of this
     * model.
     *
     * @return a JSON object.
     */
    public JsonObject json() {
        return this.json;
    }

    /**
     * Generates a hash code for this BMv2 model. The hash function is based
     * solely on the low-level JSON representation.
     */
    @Override
    public int hashCode() {
        return json.hashCode();
    }

    /**
     * Indicates whether some other BMv2 model is equal to this one.
     * Equality is based solely on the low-level JSON representation.
     *
     * @param obj other object
     * @return true if equals, false elsewhere
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final Bmv2Model other = (Bmv2Model) obj;
        return Objects.equal(this.json, other.json);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("jsonHash", json.hashCode())
                .toString();
    }

    /**
     * Parse the JSON object and build the corresponding objects.
     */
    private void doParse() {
        // parse header types
        json.get("header_types").asArray().forEach(val -> {

            JsonObject jHeaderType = val.asObject();

            // populate fields list
            List<Bmv2ModelFieldType> fieldTypes = Lists.newArrayList();

            jHeaderType.get("fields").asArray().forEach(x -> fieldTypes.add(
                    new Bmv2ModelFieldType(
                            x.asArray().get(0).asString(),
                            x.asArray().get(1).asInt())));

            // add header type instance
            String name = jHeaderType.get("name").asString();
            int id = jHeaderType.get("id").asInt();

            Bmv2ModelHeaderType headerType = new Bmv2ModelHeaderType(name,
                                                                     id,
                                                                     fieldTypes);

            headerTypes.put(name, id, headerType);
        });

        // parse headers
        json.get("headers").asArray().forEach(val -> {

            JsonObject jHeader = val.asObject();

            String name = jHeader.get("name").asString();
            int id = jHeader.get("id").asInt();
            String typeName = jHeader.get("header_type").asString();

            Bmv2ModelHeader header = new Bmv2ModelHeader(name,
                                                         id,
                                                         headerTypes.get(typeName),
                                                         jHeader.get("metadata").asBoolean());

            // add instance
            headers.put(name, id, header);
        });

        // parse actions
        json.get("actions").asArray().forEach(val -> {

            JsonObject jAction = val.asObject();

            // populate runtime data list
            List<Bmv2ModelRuntimeData> runtimeDatas = Lists.newArrayList();

            jAction.get("runtime_data").asArray().forEach(jData -> runtimeDatas.add(
                    new Bmv2ModelRuntimeData(
                            jData.asObject().get("name").asString(),
                            jData.asObject().get("bitwidth").asInt()
                    )));

            // add action instance
            String name = jAction.get("name").asString();
            int id = jAction.get("id").asInt();

            Bmv2ModelAction action = new Bmv2ModelAction(name,
                                                         id,
                                                         runtimeDatas);

            actions.put(name, id, action);
        });

        // parse tables
        json.get("pipelines").asArray().forEach(pipeline -> {

            pipeline.asObject().get("tables").asArray().forEach(val -> {

                JsonObject jTable = val.asObject();

                // populate keys
                List<Bmv2ModelTableKey> keys = Lists.newArrayList();

                jTable.get("key").asArray().forEach(jKey -> {
                    JsonArray target = jKey.asObject().get("target").asArray();

                    Bmv2ModelHeader header = header(target.get(0).asString());
                    String typeName = target.get(1).asString();

                    Bmv2ModelField field = new Bmv2ModelField(
                            header, header.type().field(typeName));

                    String matchTypeStr = jKey.asObject().get("match_type").asString();

                    Bmv2MatchParam.Type matchType;

                    switch (matchTypeStr) {
                        case "ternary":
                            matchType = Bmv2MatchParam.Type.TERNARY;
                            break;
                        case "exact":
                            matchType = Bmv2MatchParam.Type.EXACT;
                            break;
                        case "lpm":
                            matchType = Bmv2MatchParam.Type.LPM;
                            break;
                        case "valid":
                            matchType = Bmv2MatchParam.Type.VALID;
                            break;
                        default:
                            throw new RuntimeException(
                                    "Unable to parse match type: " + matchTypeStr);
                    }

                    keys.add(new Bmv2ModelTableKey(matchType, field));
                });

                // populate actions set
                Set<Bmv2ModelAction> actionzz = Sets.newHashSet();
                jTable.get("actions").asArray().forEach(
                        jAction -> actionzz.add(action(jAction.asString())));

                // add table instance
                String name = jTable.get("name").asString();
                int id = jTable.get("id").asInt();

                Bmv2ModelTable table = new Bmv2ModelTable(name,
                                                          id,
                                                          jTable.get("match_type").asString(),
                                                          jTable.get("type").asString(),
                                                          jTable.get("max_size").asInt(),
                                                          jTable.get("with_counters").asBoolean(),
                                                          jTable.get("support_timeout").asBoolean(),
                                                          keys,
                                                          actionzz);

                tables.put(name, id, table);
            });
        });
    }

    /**
     * Handy class for a map indexed by two keys, a string and an integer.
     *
     * @param <T> type of value stored by the map
     */
    private class DualKeySortedMap<T> {
        private final SortedMap<Integer, T> intMap = Maps.newTreeMap();
        private final Map<String, Integer> strToIntMap = Maps.newHashMap();

        private void put(String name, int id, T object) {
            strToIntMap.put(name, id);
            intMap.put(id, object);
        }

        private T get(int id) {
            return intMap.get(id);
        }

        private T get(String name) {
            return get(strToIntMap.get(name));
        }

        private SortedMap<Integer, T> sortedMap() {
            return intMap;
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(intMap, strToIntMap);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            final DualKeySortedMap other = (DualKeySortedMap) obj;
            return Objects.equal(this.intMap, other.intMap)
                    && Objects.equal(this.strToIntMap, other.strToIntMap);
        }
    }
}