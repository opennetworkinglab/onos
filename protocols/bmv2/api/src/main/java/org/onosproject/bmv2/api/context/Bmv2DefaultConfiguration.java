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

package org.onosproject.bmv2.api.context;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.google.common.annotations.Beta;
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
 * Default implementation of a BMv2 configuration backed by a JSON object.
 */
@Beta
public final class Bmv2DefaultConfiguration implements Bmv2Configuration {

    private final JsonObject json;
    private final DualKeySortedMap<Bmv2HeaderTypeModel> headerTypes = new DualKeySortedMap<>();
    private final DualKeySortedMap<Bmv2HeaderModel> headers = new DualKeySortedMap<>();
    private final DualKeySortedMap<Bmv2ActionModel> actions = new DualKeySortedMap<>();
    private final DualKeySortedMap<Bmv2TableModel> tables = new DualKeySortedMap<>();

    private Bmv2DefaultConfiguration(JsonObject json) {
        this.json = JsonObject.unmodifiableObject(json);
    }

    /**
     * Returns a new BMv2 configuration object by parsing the passed JSON.
     *
     * @param json json
     * @return a new BMv2 configuration object
     * @see <a href="https://github.com/p4lang/behavioral-configuration/blob/master/docs/JSON_format.md">
     * BMv2 JSON specification</a>
     */
    public static Bmv2DefaultConfiguration parse(JsonObject json) {
        checkArgument(json != null, "json cannot be null");
        // TODO: implement caching, no need to parse a json if we already have the configuration
        Bmv2DefaultConfiguration configuration = new Bmv2DefaultConfiguration(json);
        configuration.doParse();
        return configuration;
    }

    @Override
    public Bmv2HeaderTypeModel headerType(int id) {
        return headerTypes.get(id);
    }

    @Override
    public Bmv2HeaderTypeModel headerType(String name) {
        return headerTypes.get(name);
    }

    @Override
    public List<Bmv2HeaderTypeModel> headerTypes() {
        return ImmutableList.copyOf(headerTypes.sortedMap().values());
    }

    @Override
    public Bmv2HeaderModel header(int id) {
        return headers.get(id);
    }

    @Override
    public Bmv2HeaderModel header(String name) {
        return headers.get(name);
    }

    @Override
    public List<Bmv2HeaderModel> headers() {
        return ImmutableList.copyOf(headers.sortedMap().values());
    }

    @Override
    public Bmv2ActionModel action(int id) {
        return actions.get(id);
    }

    @Override
    public Bmv2ActionModel action(String name) {
        return actions.get(name);
    }

    @Override
    public List<Bmv2ActionModel> actions() {
        return ImmutableList.copyOf(actions.sortedMap().values());
    }

    @Override
    public Bmv2TableModel table(int id) {
        return tables.get(id);
    }

    @Override
    public Bmv2TableModel table(String name) {
        return tables.get(name);
    }

    @Override
    public List<Bmv2TableModel> tables() {
        return ImmutableList.copyOf(tables.sortedMap().values());
    }

    @Override
    public JsonObject json() {
        return this.json;
    }

    /**
     * Generates a hash code for this BMv2 configuration. The hash function is based solely on the JSON backing this
     * configuration.
     */
    @Override
    public int hashCode() {
        return json.hashCode();
    }

    /**
     * Indicates whether some other BMv2 configuration is equal to this one.
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
        final Bmv2DefaultConfiguration other = (Bmv2DefaultConfiguration) obj;
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
            List<Bmv2FieldTypeModel> fieldTypes = Lists.newArrayList();

            jHeaderType.get("fields").asArray().forEach(x -> fieldTypes.add(
                    new Bmv2FieldTypeModel(
                            x.asArray().get(0).asString(),
                            x.asArray().get(1).asInt())));

            // add header type instance
            String name = jHeaderType.get("name").asString();
            int id = jHeaderType.get("id").asInt();

            Bmv2HeaderTypeModel headerType = new Bmv2HeaderTypeModel(name,
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

            Bmv2HeaderModel header = new Bmv2HeaderModel(name,
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
            List<Bmv2RuntimeDataModel> runtimeDatas = Lists.newArrayList();

            jAction.get("runtime_data").asArray().forEach(jData -> runtimeDatas.add(
                    new Bmv2RuntimeDataModel(
                            jData.asObject().get("name").asString(),
                            jData.asObject().get("bitwidth").asInt()
                    )));

            // add action instance
            String name = jAction.get("name").asString();
            int id = jAction.get("id").asInt();

            Bmv2ActionModel action = new Bmv2ActionModel(name,
                                                         id,
                                                         runtimeDatas);

            actions.put(name, id, action);
        });

        // parse tables
        json.get("pipelines").asArray().forEach(pipeline -> {

            pipeline.asObject().get("tables").asArray().forEach(val -> {

                JsonObject jTable = val.asObject();

                // populate keys
                List<Bmv2TableKeyModel> keys = Lists.newArrayList();

                jTable.get("key").asArray().forEach(jKey -> {
                    JsonArray target = jKey.asObject().get("target").asArray();

                    Bmv2HeaderModel header = header(target.get(0).asString());
                    String typeName = target.get(1).asString();

                    Bmv2FieldModel field = new Bmv2FieldModel(
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

                    keys.add(new Bmv2TableKeyModel(matchType, field));
                });

                // populate actions set
                Set<Bmv2ActionModel> actionzz = Sets.newHashSet();
                jTable.get("actions").asArray().forEach(
                        jAction -> actionzz.add(action(jAction.asString())));

                // add table instance
                String name = jTable.get("name").asString();
                int id = jTable.get("id").asInt();

                Bmv2TableModel table = new Bmv2TableModel(name,
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
            return strToIntMap.get(name) == null ? null : get(strToIntMap.get(name));
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