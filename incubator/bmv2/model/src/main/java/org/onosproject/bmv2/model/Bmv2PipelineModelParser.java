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
package org.onosproject.bmv2.model;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;
import com.google.common.annotations.Beta;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.onosproject.net.pi.model.PiMatchType;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * BMv2 pipeline model parser.
 *
 * @see <a href="https://github.com/p4lang/behavioral-model/blob/master/docs/JSON_format.md">
 * BMv2 JSON specification</a>
 */
@Beta
public final class Bmv2PipelineModelParser {
    private static final Logger log = getLogger(Bmv2PipelineModelParser.class);

    // General fields and values
    private static final String NAME = "name";
    private static final String ID = "id";
    private static final int NO_ID = Integer.MIN_VALUE;

    // Hide default parser
    private Bmv2PipelineModelParser() {
    }

    /**
     * Translate BMv2 json config to Bmv2PipelineModel object.
     *
     * @param jsonObject the BMv2 json config
     * @return Bmv2PipelineModel object for the json config
     */
    public static Bmv2PipelineModel parse(JsonObject jsonObject) {
        List<Bmv2HeaderTypeModel> headerTypeModels = HeaderTypesParser.parse(jsonObject);
        Map<Integer, Integer> headerIdToIndex = HeaderStackParser.parse(jsonObject);
        List<Bmv2HeaderModel> headerModels = HeadersParser.parse(jsonObject, headerTypeModels, headerIdToIndex);
        List<Bmv2ActionModel> actionModels = ActionsParser.parse(jsonObject);
        List<Bmv2TableModel> tableModels = TablesParser.parse(jsonObject, headerModels, actionModels);

        return new Bmv2PipelineModel(headerTypeModels, headerModels,
                                     actionModels, tableModels);
    }

    /**
     * Parser for BMv2 header types.
     */
    private static class HeaderTypesParser {
        private static final String HEADER_TYPES = "header_types";
        private static final String FIELDS = "fields";
        private static final int FIELD_NAME_INDEX = 0;
        private static final int FIELD_BIT_WIDTH_INDEX = 1;
        private static final int FIELD_SIGNED_INDEX = 2;
        private static final int SIZE_WITH_SIGNED_FLAG = 3;

        private static List<Bmv2HeaderTypeModel> parse(JsonObject jsonObject) {
            List<Bmv2HeaderTypeModel> headerTypeModels = Lists.newArrayList();
            jsonObject.get(HEADER_TYPES).asArray().forEach(jsonValue ->  {
                JsonObject headerFieldType = jsonValue.asObject();
                String name = headerFieldType.getString(NAME, null);
                int id = headerFieldType.getInt(ID, NO_ID);
                if (id == NO_ID) {
                    log.warn("Can't get id from header type field {}", jsonValue);
                    return;
                }
                if (name == null) {
                    log.warn("Can't get name from header type field {}", jsonValue);
                    return;
                }
                List<Bmv2HeaderFieldTypeModel> fields = Lists.newArrayList();
                headerFieldType.get(FIELDS).asArray().forEach(fieldValue -> {
                    JsonArray fieldInfo = fieldValue.asArray();
                    boolean signed = false;
                    if (fieldInfo.size() == SIZE_WITH_SIGNED_FLAG) {
                        // 3-tuple value, third value is a boolean value
                        // true if the field is signed; otherwise false
                        signed = fieldInfo.get(FIELD_SIGNED_INDEX).asBoolean();
                    }
                    fields.add(new Bmv2HeaderFieldTypeModel(fieldInfo.get(FIELD_NAME_INDEX).asString(),
                                                            fieldInfo.get(FIELD_BIT_WIDTH_INDEX).asInt(),
                                                            signed));
                });
                headerTypeModels.add(new Bmv2HeaderTypeModel(name, id, fields));
            });
            return headerTypeModels;
        }
    }

    /**
     * Parser for BMv2 header stacks.
     */
    private static class HeaderStackParser {
        private static final String HEADER_STACK = "header_stacks";
        private static final String HEADER_IDS = "header_ids";

        /**
         * Parser header stacks, return header-id to stack index mapping.
         *
         * @param jsonObject BMv2 json config
         * @return header-id to stack index mapping
         */
        private static Map<Integer, Integer> parse(JsonObject jsonObject) {
            Map<Integer, Integer> headerIdToIndex = Maps.newHashMap();
            jsonObject.get(HEADER_STACK).asArray().forEach(jsonValue -> {
                JsonArray headerIds = jsonValue.asObject().get(HEADER_IDS).asArray();
                int index = 0;
                for (JsonValue id : headerIds.values()) {
                    headerIdToIndex.put(id.asInt(), index);
                    index++;
                }
            });
            return headerIdToIndex;
        }
    }

    /**
     * Parser for BMv2 headers.
     */
    private static class HeadersParser {
        private static final String HEADERS = "headers";
        private static final String HEADER_TYPE = "header_type";
        private static final String METADATA = "metadata";
        private static final String DEFAULT_HEADER_TYPE = "";
        private static final Integer DEFAULT_HEADER_INDEX = 0;

        private static List<Bmv2HeaderModel> parse(JsonObject jsonObject,
                                                   List<Bmv2HeaderTypeModel> headerTypeModels,
                                                   Map<Integer, Integer> headerIdToIndex) {
            List<Bmv2HeaderModel> headerModels = Lists.newArrayList();

            jsonObject.get(HEADERS).asArray().forEach(jsonValue -> {
                JsonObject header = jsonValue.asObject();
                String name = header.getString(NAME, null);
                int id = header.getInt(ID, NO_ID);
                String headerTypeName = header.getString(HEADER_TYPE, DEFAULT_HEADER_TYPE);
                boolean isMetadata = header.getBoolean(METADATA, false);

                if (name == null || id == -1) {
                    log.warn("Can't get name or id from header {}", header);
                    return;
                }
                Bmv2HeaderTypeModel headerTypeModel = headerTypeModels.stream()
                        .filter(model -> model.name().equals(headerTypeName))
                        .findFirst()
                        .orElse(null);

                if (headerTypeModel == null) {
                    log.warn("Can't get header type model {} from header {}", headerTypeName, header);
                    return;
                }

                Integer index = headerIdToIndex.get(id);

                // No index for this header, set to default
                if (index == null) {
                    index = DEFAULT_HEADER_INDEX;
                }
                headerModels.add(new Bmv2HeaderModel(name, id, index, headerTypeModel, isMetadata));
            });

            return headerModels;
        }
    }

    /**
     * Parser for BMv2 actions.
     */
    private static class ActionsParser {
        private static final String ACTIONS = "actions";
        private static final String RUNTIME_DATA = "runtime_data";
        private static final String BITWIDTH = "bitwidth";

        private static List<Bmv2ActionModel> parse(JsonObject jsonObject) {
            List<Bmv2ActionModel> actionModels = Lists.newArrayList();

            jsonObject.get(ACTIONS).asArray().forEach(jsonValue -> {
                JsonObject action = jsonValue.asObject();
                String name = action.getString(NAME, null);
                int id = action.getInt(ID, NO_ID);
                List<Bmv2ActionParamModel> paramModels = Lists.newArrayList();
                action.get(RUNTIME_DATA).asArray().forEach(paramValue -> {
                    JsonObject paramInfo = paramValue.asObject();
                    String paramName = paramInfo.getString(NAME, null);
                    int bitWidth = paramInfo.getInt(BITWIDTH, -1);

                    if (paramName == null || bitWidth == -1) {
                        log.warn("Can't get name or bit width from runtime data {}", paramInfo);
                        return;
                    }
                    paramModels.add(new Bmv2ActionParamModel(paramName, bitWidth));
                });

                actionModels.add(new Bmv2ActionModel(name, id, paramModels));
            });

            return actionModels;
        }
    }

    /**
     * Parser for BMv2 tables.
     */
    private static class TablesParser {
        private static final String PIPELINES = "pipelines";
        private static final String TABLES = "tables";
        private static final String KEY = "key";
        private static final String MATCH_TYPE = "match_type";
        private static final String TARGET = "target";
        private static final int TARGET_HEADER_INDEX = 0;
        private static final int TARGET_FIELD_INDEX = 1;
        private static final String ACTIONS = "actions";
        private static final String MAX_SIZE = "max_size";
        private static final int DEFAULT_MAX_SIZE = 0;
        private static final String WITH_COUNTERS = "with_counters";
        private static final String SUPPORT_TIMEOUT = "support_timeout";

        private static List<Bmv2TableModel> parse(JsonObject jsonObject,
                                                  List<Bmv2HeaderModel> headerModels,
                                                  List<Bmv2ActionModel> actionModels) {
            List<Bmv2TableModel> tableModels = Lists.newArrayList();
            jsonObject.get(PIPELINES).asArray().forEach(pipelineVal -> {
                JsonObject pipeline = pipelineVal.asObject();
                pipeline.get(TABLES).asArray().forEach(tableVal -> {
                    JsonObject table = tableVal.asObject();
                    String tableName = table.getString(NAME, null);
                    int tableId = table.getInt(ID, NO_ID);
                    int maxSize = table.getInt(MAX_SIZE, DEFAULT_MAX_SIZE);
                    boolean hasCounters = table.getBoolean(WITH_COUNTERS, false);
                    boolean suppportAging = table.getBoolean(SUPPORT_TIMEOUT, false);

                    // Match field
                    Set<Bmv2TableMatchFieldModel> matchFieldModels =
                            Sets.newHashSet();
                    table.get(KEY).asArray().forEach(keyVal -> {
                        JsonObject key = keyVal.asObject();
                        String matchTypeName = key.getString(MATCH_TYPE, null);

                        if (matchTypeName == null) {
                            log.warn("Can't find match type from key {}", key);
                            return;
                        }
                        PiMatchType matchType = PiMatchType.valueOf(matchTypeName.toUpperCase());

                        // convert target array to Bmv2HeaderFieldTypeModel
                        // e.g. ["ethernet", "dst"]
                        JsonArray targetArray = key.get(TARGET).asArray();
                        Bmv2HeaderFieldModel matchField;

                        String headerName = targetArray.get(TARGET_HEADER_INDEX).asString();
                        String fieldName = targetArray.get(TARGET_FIELD_INDEX).asString();

                        Bmv2HeaderModel headerModel = headerModels.stream()
                                .filter(hm -> hm.name().equals(headerName))
                                .findAny()
                                .orElse(null);

                        if (headerModel == null) {
                            log.warn("Can't find header {} for table {}", headerName, tableName);
                            return;
                        }
                        Bmv2HeaderFieldTypeModel fieldModel =
                                (Bmv2HeaderFieldTypeModel) headerModel.type()
                                        .field(fieldName)
                                        .orElse(null);

                        if (fieldModel == null) {
                            log.warn("Can't find field {} from header {}", fieldName, headerName);
                            return;
                        }
                        matchField = new Bmv2HeaderFieldModel(headerModel, fieldModel);
                        matchFieldModels.add(new Bmv2TableMatchFieldModel(matchType, matchField));
                    });

                    // Actions
                    Set<Bmv2ActionModel> actions = Sets.newHashSet();
                    table.get(ACTIONS).asArray().forEach(actionVal -> {
                        String actionName = actionVal.asString();
                        Bmv2ActionModel action = actionModels.stream()
                                .filter(am -> am.name().equals(actionName))
                                .findAny()
                                .orElse(null);
                        if (action == null) {
                            log.warn("Can't find action {}", actionName);
                            return;
                        }
                        actions.add(action);
                    });

                    tableModels.add(new Bmv2TableModel(tableName, tableId,
                                                       maxSize, hasCounters,
                                                       suppportAging,
                                                       matchFieldModels, actions));
                });
            });

            return tableModels;
        }
    }
}
